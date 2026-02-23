package com.hitsuji.sheepplayer2.manager

import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.interfaces.MusicPlayerInterface
import com.hitsuji.sheepplayer2.interfaces.PlaybackManagerInterface
import com.hitsuji.sheepplayer2.interfaces.PlaybackStateListener

class MusicPlayerManager(private val musicPlayer: MusicPlayerInterface) : PlaybackManagerInterface {
    override var currentPlayingTrack: Track? = null
        private set

    override var isPlaying: Boolean = false
        private set

    private var isPreparing: Boolean = false

    init {
        setupMusicPlayerCallbacks()
    }

    private fun setupMusicPlayerCallbacks() {
        musicPlayer.setOnPlaybackStateChangeListener(object : PlaybackStateListener {
            override fun onPlaybackStarted(track: Track) {
                isPreparing = false
                isPlaying = true
                onPlaybackStateChangeListener?.invoke()
            }

            override fun onPlaybackPaused(track: Track) {
                isPreparing = false
                isPlaying = false
                onPlaybackStateChangeListener?.invoke()
            }

            override fun onPlaybackStopped() {
                isPreparing = false
                isPlaying = false
                onPlaybackStateChangeListener?.invoke()
            }

            override fun onPlaybackError(track: Track, error: String) {
                isPreparing = false
                isPlaying = false
                onPlaybackStateChangeListener?.invoke()
            }

            override fun onPlaybackCompleted(track: Track) {
                isPreparing = false
                isPlaying = false
                onPlaybackCompletionListener?.invoke(track)
                onPlaybackStateChangeListener?.invoke()
            }
        })
    }

    private var onPlaybackStateChangeListener: (() -> Unit)? = null
    private var onPlaybackCompletionListener: ((Track) -> Unit)? = null

    override fun setOnPlaybackStateChangeListener(listener: () -> Unit) {
        onPlaybackStateChangeListener = listener
    }

    override fun setOnPlaybackCompletionListener(listener: (Track) -> Unit) {
        onPlaybackCompletionListener = listener
    }

    override fun playTrack(track: Track) {
        currentPlayingTrack = track
        isPlaying = true
        isPreparing = true
        musicPlayer.loadTrack(track, autoPlay = true)
    }

    override fun syncPlaybackState() {
        // Only sync if we are not in the middle of preparing a track
        if (!isPreparing) {
            isPlaying = musicPlayer.isPlaying()
        }
    }

    override fun togglePlayback(): Boolean {
        if (isPreparing) return true // Ignore toggle during preparation

        return if (isPlaying) {
            musicPlayer.pause()
        } else {
            val track = currentPlayingTrack
            if (track != null) {
                if (musicPlayer.getCurrentTrack() == track) {
                    musicPlayer.play()
                } else {
                    playTrack(track)
                }
                true
            } else {
                false
            }
        }
    }

    override fun stopPlayback() {
        musicPlayer.stop()
    }

    override fun getCurrentPosition(): Int {
        return musicPlayer.getCurrentPosition()
    }

    override fun getDuration(): Int {
        return musicPlayer.getDuration()
    }

    override fun release() {
        musicPlayer.release()
    }
}