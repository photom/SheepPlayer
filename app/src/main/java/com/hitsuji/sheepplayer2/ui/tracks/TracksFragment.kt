package com.hitsuji.sheepplayer2.ui.tracks

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.hitsuji.sheepplayer2.MainActivity
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.databinding.FragmentTracksBinding

class TracksFragment : Fragment() {

    private var _binding: FragmentTracksBinding? = null
    private lateinit var treeAdapter: TreeAdapter
    private lateinit var swipeHelper: SwipeToPlayHelper
    private var needsDataRefresh = false
    private var hasLoadedInitialData = false
    private var lastLoadedArtistCount = 0

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val tracksViewModel =
            ViewModelProvider(this).get(TracksViewModel::class.java)

        _binding = FragmentTracksBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupWindowInsetsHandling()
        // Data loading moved to onResume() for proper lifecycle management

        return root
    }

    private fun setupRecyclerView() {
        // Only create adapter if it doesn't exist
        if (!::treeAdapter.isInitialized) {
            treeAdapter = TreeAdapter(
                onTrackClick = { track -> onTrackSelected(track) },
                onTrackSwipe = { track -> onTrackSwiped(track) }
            )
        }

        swipeHelper = SwipeToPlayHelper(
            treeAdapter,
            onTrackSwipe = { track -> onTrackSwiped(track) },
            onAlbumSwipe = { album -> onAlbumSwiped(album) }
        )

        binding.recyclerViewTracks.apply {
            adapter = treeAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(false) // Allow dynamic sizing for expand/collapse
            isNestedScrollingEnabled = true // Enable nested scrolling
            itemAnimator?.changeDuration = 0 // Disable change animations for better performance
        }

        ItemTouchHelper(swipeHelper).attachToRecyclerView(binding.recyclerViewTracks)
    }

    private fun setupWindowInsetsHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerViewTracks) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Get the maximum bottom inset (either system bars or navigation bars)
            val bottomInset = maxOf(systemBars.bottom, navigationBars.bottom)

            // Add extra padding to ensure last item can scroll above bottom navigation
            // Bottom navigation height is typically 56dp (approximately 168px on most devices)
            val bottomNavigationHeight = (56 * resources.displayMetrics.density).toInt()
            val totalBottomPadding = bottomInset + bottomNavigationHeight + 16 // Extra 16dp for comfortable spacing

            // Use clipToPadding=false so content can scroll behind padding
            // This ensures all items remain fully visible when scrolled
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                totalBottomPadding
            )
            insets
        }
    }

    private fun loadTracksFromMainActivity() {
        val mainActivity = requireActivity() as MainActivity

        // First load any immediately available data
        loadTracksFromSource(mainActivity.allArtists, "MainActivity")

        // If Google Drive is logged in, check for cached data first
        if (isGoogleDriveLoggedIn()) {
            loadCachedGoogleDriveData(mainActivity)
        }
    }

    private fun loadTracksFromSource(artists: List<com.hitsuji.sheepplayer2.Artist>, source: String) {
        val treeItems = mutableListOf<TreeItem>()

        Log.d("TracksFragment", "*** Loading artists from $source: ${artists.size} artists ***")

        // Create tree structure: only show top-level artists initially
        artists.forEach { artist ->
            Log.d("TracksFragment", "Adding artist: ${artist.name} with ${artist.albums.size} albums")
            treeItems.add(TreeItem.ArtistItem(artist, false))
        }

        Log.d("TracksFragment", "*** Submitting ${treeItems.size} items to adapter ***")
        treeAdapter.submitList(treeItems)

        // Update tracking variables
        lastLoadedArtistCount = artists.size
        hasLoadedInitialData = true

        // Log data source information for debugging
        val localTracks = artists.sumOf { artist ->
            artist.albums.sumOf { album ->
                album.tracks.count { track -> track.googleDriveFileId == null }
            }
        }
        val googleDriveTracks = artists.sumOf { artist ->
            artist.albums.sumOf { album ->
                album.tracks.count { track -> track.googleDriveFileId != null }
            }
        }

        Log.d("TracksFragment", "Data loaded - Artists: ${artists.size}, Local: $localTracks tracks, Google Drive: $googleDriveTracks tracks, Source: $source")
    }

    private fun isGoogleDriveLoggedIn(): Boolean {
        val mainActivity = requireActivity() as MainActivity
        return try {
            mainActivity.getGoogleDriveService().isSignedIn()
        } catch (e: Exception) {
            Log.w("TracksFragment", "Error checking Google Drive sign-in status", e)
            false
        }
    }

    private fun loadCachedGoogleDriveData(mainActivity: MainActivity) {
        Log.d("TracksFragment", "Google Drive is logged in, checking for cached data")

        try {
            val cachedArtists = mainActivity.getGoogleDriveService().getLatestGoogleDriveArtists()
            if (cachedArtists.isNotEmpty()) {
                Log.d("TracksFragment", "Found cached Google Drive data: ${cachedArtists.size} artists")

                // Merge local and cached Google Drive data
                val allArtists = mutableListOf<com.hitsuji.sheepplayer2.Artist>()
                allArtists.addAll(mainActivity.allArtists.filter { artist ->
                    // Include local-only artists (those without Google Drive tracks)
                    artist.albums.any { album ->
                        album.tracks.any { track -> track.googleDriveFileId == null }
                    }
                })
                allArtists.addAll(cachedArtists)

                loadTracksFromSource(allArtists, "Cached Data (Local + Google Drive)")

                // Show loading indicator for background sync
                showSyncIndicator(true)

                // Trigger background sync to get fresh data
                triggerBackgroundSync(mainActivity)
            } else {
                Log.d("TracksFragment", "No cached Google Drive data available, will wait for fresh data")
                // Still trigger sync since there's no cached data
                triggerBackgroundSync(mainActivity)
            }
        } catch (e: Exception) {
            Log.e("TracksFragment", "Error loading cached Google Drive data", e)
        }
    }

    private fun onTrackSelected(track: Track) {
        // Handle track selection - can be implemented later for playback
    }

    private fun onTrackSwiped(track: Track) {
        // Play the track using MainActivity's music player
        val mainActivity = requireActivity() as MainActivity
        mainActivity.playTrack(track)

        // Switch to Playing tab
        mainActivity.switchToPlayingTab()
    }

    private fun onAlbumSwiped(album: com.hitsuji.sheepplayer2.Album) {
        // Play the album using MainActivity's music player
        val mainActivity = requireActivity() as MainActivity
        mainActivity.playAlbum(album)

        // Switch to Playing tab
        mainActivity.switchToPlayingTab()
    }

    override fun onResume() {
        super.onResume()
        Log.d("TracksFragment", "*** onResume() called, needsDataRefresh: $needsDataRefresh, hasLoadedInitialData: $hasLoadedInitialData, adapterItemCount: ${if (::treeAdapter.isInitialized) treeAdapter.itemCount else 0} ***")

        // Only reload if we haven't loaded initial data yet or if we explicitly need to refresh data
        if (!hasLoadedInitialData) {
            Log.d("TracksFragment", "*** First time loading - initializing tracks data ***")
            loadTracksFromMainActivity()
            hasLoadedInitialData = true
        } else if (needsDataRefresh) {
            Log.d("TracksFragment", "*** Data refresh needed - reloading tracks ***")
            loadTracksFromMainActivity()
            needsDataRefresh = false
        } else {
            Log.d("TracksFragment", "*** Fragment resumed - keeping existing data (${if (::treeAdapter.isInitialized) treeAdapter.itemCount else 0} items) ***")
            // Data is already loaded and current, no need to reload
        }
    }

    private fun triggerBackgroundSync(mainActivity: MainActivity) {
        Log.d("TracksFragment", "Triggering background Google Drive sync")

        try {
            // Check if Google Drive is still signed in before attempting sync
            if (mainActivity.getGoogleDriveService().isSignedIn()) {
                // The background sync is triggered through the existing menu action
                // which calls musicDataHandler.refreshGoogleDriveMusic()
                // We'll use the same pattern as MenuFragment
                triggerGoogleDriveRefresh()
            } else {
                Log.d("TracksFragment", "Google Drive not signed in, skipping background sync")
                showSyncIndicator(false)
            }
        } catch (e: Exception) {
            Log.e("TracksFragment", "Error triggering background sync", e)
            showSyncIndicator(false)
        }
    }

    private fun triggerGoogleDriveRefresh() {
        // Trigger the same action as the menu's refresh option
        // This will load fresh Google Drive data in the background
        val mainActivity = requireActivity() as MainActivity

        try {
            Log.d("TracksFragment", "Triggering Google Drive refresh...")
            mainActivity.refreshGoogleDriveMusic()
            Log.d("TracksFragment", "Google Drive refresh triggered successfully")
        } catch (e: Exception) {
            Log.e("TracksFragment", "Error triggering Google Drive refresh", e)
            showSyncIndicator(false)
        }
    }

    private fun showSyncIndicator(show: Boolean) {
        try {
            if (show) {
                Log.d("TracksFragment", "🔄 Syncing with Google Drive...")
                binding.syncIndicator.visibility = android.view.View.VISIBLE
            } else {
                Log.d("TracksFragment", "✅ Sync completed")
                binding.syncIndicator.visibility = android.view.View.GONE
            }
        } catch (e: Exception) {
            Log.w("TracksFragment", "Error updating sync indicator", e)
        }
    }

    fun onMusicDataLoaded() {
        Log.d("TracksFragment", "=== onMusicDataLoaded() called ===")
        Log.d("TracksFragment", "isResumed: $isResumed, view: $view")

        // Hide sync indicator since fresh data has arrived
        showSyncIndicator(false)

        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            val currentArtistCount = mainActivity.allArtists.size
            Log.d("TracksFragment", "*** MainActivity found, artists count: $currentArtistCount (previous: $lastLoadedArtistCount) ***")

            // Check if the data has actually changed
            val dataHasChanged = currentArtistCount != lastLoadedArtistCount || !hasLoadedInitialData

            if (!dataHasChanged && hasLoadedInitialData) {
                Log.d("TracksFragment", "*** Data unchanged, skipping update ***")
                return
            }

            // Ensure UI update happens on main thread
            requireActivity().runOnUiThread {
                if (isResumed && view != null) {
                    // Fragment is visible, update immediately with fresh data
                    Log.d("TracksFragment", "*** Fragment visible - loading fresh data ***")
                    loadTracksFromSource(mainActivity.allArtists, "Fresh Data")
                    lastLoadedArtistCount = currentArtistCount
                    hasLoadedInitialData = true
                } else {
                    // Fragment is not visible, mark for update when it becomes visible
                    // But only if data has actually changed
                    if (dataHasChanged) {
                        Log.d("TracksFragment", "*** Fragment not visible but data changed - setting needsDataRefresh = true ***")
                        needsDataRefresh = true
                        lastLoadedArtistCount = currentArtistCount
                    } else {
                        Log.d("TracksFragment", "*** Fragment not visible and data unchanged - no action needed ***")
                    }
                }
            }
        } else {
            Log.w("TracksFragment", "*** MainActivity not found! ***")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Note: Don't reset hasLoadedInitialData and lastLoadedArtistCount here
        // as we want to persist this state across view recreation
    }

    override fun onDestroy() {
        super.onDestroy()
        // Reset state when fragment is completely destroyed
        hasLoadedInitialData = false
        lastLoadedArtistCount = 0
        needsDataRefresh = false
    }
}