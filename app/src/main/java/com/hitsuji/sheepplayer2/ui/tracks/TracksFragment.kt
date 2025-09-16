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
        loadTracksFromMainActivity()

        return root
    }

    private fun setupRecyclerView() {
        treeAdapter = TreeAdapter(
            onTrackClick = { track -> onTrackSelected(track) },
            onTrackSwipe = { track -> onTrackSwiped(track) }
        )

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
        val treeItems = mutableListOf<TreeItem>()

        Log.d("TracksFragment", "*** Loading artists from MainActivity: ${mainActivity.allArtists.size} artists ***")
        
        // Create tree structure: only show top-level artists initially
        mainActivity.allArtists.forEach { artist ->
            Log.d("TracksFragment", "Adding artist: ${artist.name} with ${artist.albums.size} albums")
            treeItems.add(TreeItem.ArtistItem(artist, false))
        }

        Log.d("TracksFragment", "*** Submitting ${treeItems.size} items to adapter ***")
        treeAdapter.submitList(treeItems)
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
        // Reload tracks when returning to this fragment in case new music was loaded
        if (needsDataRefresh || !::treeAdapter.isInitialized) {
            loadTracksFromMainActivity()
            needsDataRefresh = false
        }
    }

    fun onMusicDataLoaded() {
        Log.d("TracksFragment", "=== onMusicDataLoaded() called ===")
        Log.d("TracksFragment", "isResumed: $isResumed, view: $view")
        
        if (isResumed && view != null) {
            // Fragment is visible, update immediately
            Log.d("TracksFragment", "*** Fragment visible - calling loadTracksFromMainActivity() ***")
            loadTracksFromMainActivity()
            // Force notify adapter of data change
            if (::treeAdapter.isInitialized) {
                Log.d("TracksFragment", "*** Forcing adapter data refresh ***")
                treeAdapter.notifyDataSetChanged()
            }
        } else {
            // Fragment is not visible, mark for update when it becomes visible
            Log.d("TracksFragment", "*** Fragment not visible - setting needsDataRefresh = true ***")
            needsDataRefresh = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}