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
import com.hitsuji.sheepplayer2.SheepApplication
import com.hitsuji.sheepplayer2.factory.ViewModelFactory
import com.hitsuji.sheepplayer2.databinding.FragmentTracksBinding

class TracksFragment : Fragment() {

    private var _binding: FragmentTracksBinding? = null
    private lateinit var tracksViewModel: TracksViewModel
    private lateinit var treeAdapter: TreeAdapter
    private lateinit var swipeHelper: SwipeToPlayHelper
    private var hasLoadedInitialData = false

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireActivity().application as SheepApplication
        val factory = ViewModelFactory(application.container)
        tracksViewModel = ViewModelProvider(this, factory).get(TracksViewModel::class.java)

        _binding = FragmentTracksBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupWindowInsetsHandling()
        setupObservers()

        return root
    }

    private fun setupObservers() {
        tracksViewModel.artists.observe(viewLifecycleOwner) { artists ->
            updateUIWithArtists(artists)
        }
    }

    private fun setupRecyclerView() {
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
            setHasFixedSize(false)
            isNestedScrollingEnabled = true
            itemAnimator?.changeDuration = 0
        }

        ItemTouchHelper(swipeHelper).attachToRecyclerView(binding.recyclerViewTracks)
    }

    private fun setupWindowInsetsHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerViewTracks) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomInset = maxOf(systemBars.bottom, navigationBars.bottom)
            val bottomNavigationHeight = (56 * resources.displayMetrics.density).toInt()
            val totalBottomPadding = bottomInset + bottomNavigationHeight + 16

            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                totalBottomPadding
            )
            insets
        }
    }

    private fun updateUIWithArtists(artists: List<com.hitsuji.sheepplayer2.Artist>) {
        val treeItems = mutableListOf<TreeItem>()
        artists.forEach { artist ->
            treeItems.add(TreeItem.ArtistItem(artist, false))
        }
        treeAdapter.submitList(treeItems)
    }

    private fun onTrackSelected(track: Track) {
        // Handle track selection if needed
    }

    private fun onTrackSwiped(track: Track) {
        val mainActivity = requireActivity() as MainActivity
        mainActivity.playTrack(track)
        mainActivity.switchToPlayingTab()
    }

    private fun onAlbumSwiped(album: com.hitsuji.sheepplayer2.Album) {
        val mainActivity = requireActivity() as MainActivity
        mainActivity.playAlbum(album)
        mainActivity.switchToPlayingTab()
    }

    override fun onResume() {
        super.onResume()
        if (!hasLoadedInitialData) {
            tracksViewModel.loadTracks(showLoading = true)
            hasLoadedInitialData = true
        }
    }

    fun onMusicDataLoaded(showLoading: Boolean = true) {
        tracksViewModel.loadTracks(showLoading = showLoading)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        hasLoadedInitialData = false
    }
}
