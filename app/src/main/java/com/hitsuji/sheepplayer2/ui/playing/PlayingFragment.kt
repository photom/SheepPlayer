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
import com.hitsuji.sheepplayer2.databinding.FragmentPlayingBinding
import com.hitsuji.sheepplayer2.utils.Constants
import com.hitsuji.sheepplayer2.utils.TimeUtils

class PlayingFragment : Fragment() {

    private var _binding: FragmentPlayingBinding? = null
    private val handler = Handler(Looper.getMainLooper())
    private var positionUpdateRunnable: Runnable? = null
    private lateinit var albumTrackAdapter: AlbumTrackAdapter

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val playingViewModel =
            ViewModelProvider(this).get(PlayingViewModel::class.java)

        _binding = FragmentPlayingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupWindowInsetsHandling()
        setupAlbumTracksList()
        setupUI()
        return root
    }

    override fun onResume() {
        super.onResume()
        syncWithMediaPlayer()
        updateTrackDisplay()
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
            // Bottom navigation height is typically 56dp (approximately 168px on most devices)
            val bottomNavigationHeight = (56 * resources.displayMetrics.density).toInt()
            val totalBottomPadding = bottomInset + bottomNavigationHeight + 16 // Extra 16dp for comfortable spacing

            // Apply bottom padding to the RecyclerView so last track can scroll above bottom navigation
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
        updateTrackDisplay()
    }

    private fun setupAlbumTracksList() {
        albumTrackAdapter = AlbumTrackAdapter { track, index ->
            // Handle track click - play the selected track in the album without clearing album context
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
        val mainActivity = requireActivity() as MainActivity
        mainActivity.togglePlayback()
    }

    fun onPlaybackStateChanged() {
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

    private fun updatePlaybackState() {
        val mainActivity = requireActivity() as MainActivity
        updateButtonIcon(mainActivity.isPlaying)
    }

    private fun updateTrackDisplay() {
        val mainActivity = requireActivity() as MainActivity
        val track = mainActivity.currentPlayingTrack

        if (track != null) {
            displayTrack(track)
        } else {
            showNoTrackMessage()
        }
    }

    private fun displayTrack(track: Track) {
        val mainActivity = requireActivity() as MainActivity

        binding.apply {
            // Hide no track message
            noTrackMessage.visibility = View.GONE

            // Show track info and controls
            albumArtLarge.visibility = View.VISIBLE
            trackTitle.visibility = View.VISIBLE
            artistName.visibility = View.VISIBLE
            albumName.visibility = View.VISIBLE
            duration.visibility = View.VISIBLE
            playStopButton.visibility = View.VISIBLE

            // Set track information
            trackTitle.text = track.title
            artistName.text = track.artistName
            albumName.text = track.albumName
            updateTimeDisplay()

            // Load album art
            loadAlbumArt(track.albumArtUri, albumArtLarge)

            // Update playback state
            updateButtonIcon(mainActivity.isPlaying)

            // Show album track list if playing an album
            updateAlbumTracksList(mainActivity)
        }
    }

    private fun showNoTrackMessage() {
        binding.apply {
            // Show no track message
            noTrackMessage.visibility = View.VISIBLE

            // Hide track info and controls
            albumArtLarge.visibility = View.GONE
            trackTitle.visibility = View.GONE
            artistName.visibility = View.GONE
            albumName.visibility = View.GONE
            duration.visibility = View.GONE
            playStopButton.visibility = View.GONE

            // Hide album track list
            albumTracksRecyclerView.visibility = View.GONE
        }
    }

    private fun updateAlbumTracksList(mainActivity: MainActivity) {
        val currentAlbum = mainActivity.currentPlayingAlbum
        val albumTracks = mainActivity.currentAlbumTracks
        val currentTrackIndex = mainActivity.currentTrackIndexInAlbum

        if (currentAlbum != null && albumTracks.isNotEmpty()) {
            // Show album track list
            binding.albumTracksRecyclerView.visibility = View.VISIBLE

            albumTrackAdapter.submitTracks(albumTracks, currentTrackIndex)
        } else {
            // Hide album track list
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
                // Keep default background if loading fails
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPositionUpdates()
        _binding = null
    }
}