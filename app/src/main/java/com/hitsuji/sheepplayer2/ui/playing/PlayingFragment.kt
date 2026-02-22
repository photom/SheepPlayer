package com.hitsuji.sheepplayer2.ui.playing

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hitsuji.sheepplayer2.MainActivity
import com.hitsuji.sheepplayer2.R
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.SheepApplication
import com.hitsuji.sheepplayer2.factory.ViewModelFactory
import com.hitsuji.sheepplayer2.databinding.FragmentPlayingBinding
import com.hitsuji.sheepplayer2.utils.Constants
import com.hitsuji.sheepplayer2.utils.TimeUtils

class PlayingFragment : Fragment() {

    private var _binding: FragmentPlayingBinding? = null
    private val handler = Handler(Looper.getMainLooper())
    private var positionUpdateRunnable: Runnable? = null
    private lateinit var albumTrackAdapter: AlbumTrackAdapter
    private lateinit var playingViewModel: PlayingViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireActivity().application as SheepApplication
        val factory = ViewModelFactory(application.container)
        playingViewModel = ViewModelProvider(this, factory).get(PlayingViewModel::class.java)

        _binding = FragmentPlayingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupWindowInsetsHandling()
        setupAlbumTracksList()
        setupUI()
        setupObservers()
        return root
    }

    private fun setupObservers() {
        playingViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            updateButtonIcon(isPlaying)
            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()
            }
            // Update album tracks list highlight
            val mainActivity = requireActivity() as MainActivity
            updateAlbumTracksList(mainActivity)
        }

        playingViewModel.currentTrack.observe(viewLifecycleOwner) { track ->
            if (track != null) {
                displayTrack(track)
            } else {
                showNoTrackMessage()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncWithMediaPlayer()
        startPositionUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopPositionUpdates()
    }

    private fun syncWithMediaPlayer() {
        val mainActivity = requireActivity() as MainActivity
        mainActivity.syncPlaybackState()
    }

    private fun setupWindowInsetsHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.albumTracksRecyclerView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Get the maximum bottom inset (either system bars or navigation bars)
            val bottomInset = maxOf(systemBars.bottom, navigationBars.bottom)

            // Add extra padding to ensure last track can scroll above bottom navigation
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

    private fun setupUI() {
        setupButtonClickListeners()
    }

    private fun setupAlbumTracksList() {
        albumTrackAdapter = AlbumTrackAdapter { track, index ->
            val mainActivity = requireActivity() as MainActivity
            mainActivity.playTrackInAlbum(track, index)
        }

        binding.albumTracksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = albumTrackAdapter
        }
    }

    private fun setupButtonClickListeners() {
        binding.playStopButton.setOnClickListener {
            handlePlayStopToggle()
        }
    }

    private fun handlePlayStopToggle() {
        playingViewModel.togglePlayback()
    }

    fun onPlaybackStateChanged() {
        // This is still called by MainActivity, but now we also have observers
        val mainActivity = requireActivity() as MainActivity
        updateButtonIcon(mainActivity.isPlaying)
        updateTimeDisplay()
        updateAlbumTracksList(mainActivity)

        if (mainActivity.isPlaying) {
            startPositionUpdates()
        } else {
            stopPositionUpdates()
        }
    }

    private fun displayTrack(track: Track) {
        val mainActivity = requireActivity() as MainActivity

        binding.apply {
            noTrackMessage.visibility = View.GONE
            albumArtLarge.visibility = View.VISIBLE
            trackTitle.visibility = View.VISIBLE
            artistName.visibility = View.VISIBLE
            albumName.visibility = View.VISIBLE
            duration.visibility = View.VISIBLE
            playStopButton.visibility = View.VISIBLE

            trackTitle.text = track.title
            artistName.text = track.artistName
            albumName.text = track.albumName
            updateTimeDisplay()

            loadAlbumArt(track.albumArtUri, albumArtLarge)
            updateButtonIcon(mainActivity.isPlaying)
            updateAlbumTracksList(mainActivity)
        }
    }

    private fun showNoTrackMessage() {
        binding.apply {
            noTrackMessage.visibility = View.VISIBLE
            albumArtLarge.visibility = View.GONE
            trackTitle.visibility = View.GONE
            artistName.visibility = View.GONE
            albumName.visibility = View.GONE
            duration.visibility = View.GONE
            playStopButton.visibility = View.GONE
            albumTracksRecyclerView.visibility = View.GONE
        }
    }

    private fun updateAlbumTracksList(mainActivity: MainActivity) {
        val currentAlbum = mainActivity.currentPlayingAlbum
        val albumTracks = mainActivity.currentAlbumTracks
        val currentTrackIndex = mainActivity.currentTrackIndexInAlbum

        if (currentAlbum != null && albumTracks.isNotEmpty()) {
            binding.albumTracksRecyclerView.visibility = View.VISIBLE
            albumTrackAdapter.submitTracks(albumTracks, currentTrackIndex)
        } else {
            binding.albumTracksRecyclerView.visibility = View.GONE
        }
    }

    private fun updateButtonIcon(isPlaying: Boolean) {
        binding.playStopButton.setImageResource(
            if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play
        )
        binding.playStopButton.contentDescription =
            if (isPlaying) Constants.UI.STOP_BUTTON_DESCRIPTION
            else Constants.UI.PLAY_BUTTON_DESCRIPTION
    }


    private fun updateTimeDisplay() {
        val mainActivity = requireActivity() as MainActivity
        val currentPos = mainActivity.currentPosition
        val totalDuration = mainActivity.duration

        val currentTime = TimeUtils.formatDuration(currentPos.toLong())
        val totalTime = TimeUtils.formatDuration(totalDuration.toLong())

        binding.duration.text = "$currentTime / $totalTime"
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateRunnable = Runnable {
            updateTimeDisplay()
            val mainActivity = requireActivity() as MainActivity
            if (mainActivity.isPlaying) {
                handler.postDelayed(positionUpdateRunnable!!, 1000)
            }
        }
        handler.post(positionUpdateRunnable!!)
    }

    private fun stopPositionUpdates() {
        positionUpdateRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
        }
        positionUpdateRunnable = null
    }

    private fun loadAlbumArt(albumArtUri: String?, imageView: android.widget.ImageView) {
        albumArtUri?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                imageView.setImageURI(uri)
            } catch (e: Exception) {
                android.util.Log.w("PlayingFragment", "Failed to load album art", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPositionUpdates()
        _binding = null
    }
}
