package com.hitsuji.sheepplayer2.manager

import com.hitsuji.sheepplayer2.MusicPlayer
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.interfaces.PlaybackStateListener

class MusicPlayerManager(private val musicPlayer: MusicPlayer) {
    var currentPlayingTrack: Track? = null
        private set

    var isPlaying: Boolean = false
        private set

    init {
        setupMusicPlayerCallbacks()
    }

    private fun setupMusicPlayerCallbacks() {
        musicPlayer.setOnPlaybackStateChangeListener(object : PlaybackStateListener {
            override fun onPlaybackStarted(track: Track) {
                isPlaying = true
                onPlaybackStateChangeListener?.invoke()
            }

            override fun onPlaybackPaused(track: Track) {
                isPlaying = false
                onPlaybackStateChangeListener?.invoke()
            }

            override fun onPlaybackStopped() {
                isPlaying = false
                onPlaybackStateChangeListener?.invoke()
            }

            override fun onPlaybackError(track: Track, error: String) {
                isPlaying = false
                onPlaybackStateChangeListener?.invoke()
            }

            override fun onPlaybackCompleted(track: Track) {
                isPlaying = false
                onPlaybackCompletionListener?.invoke(track)
                onPlaybackStateChangeListener?.invoke()
            }
        })
    }

    private var onPlaybackStateChangeListener: (() -> Unit)? = null
    private var onPlaybackCompletionListener: ((Track) -> Unit)? = null

    fun setOnPlaybackStateChangeListener(listener: () -> Unit) {
        onPlaybackStateChangeListener = listener
    }

    fun setOnPlaybackCompletionListener(listener: (Track) -> Unit) {
        onPlaybackCompletionListener = listener
    }

    fun playTrack(track: Track) {
        currentPlayingTrack = track
        isPlaying = true
        musicPlayer.loadTrack(track, autoPlay = true)
    }

    fun syncPlaybackState() {
        isPlaying = musicPlayer.isPlaying()
    }

    fun togglePlayback(): Boolean {
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

    fun stopPlayback() {
        musicPlayer.stop()
    }

    fun getCurrentPosition(): Int {
        return musicPlayer.getCurrentPosition()
    }

    fun getDuration(): Int {
        return musicPlayer.getDuration()
    }

    fun release() {
        musicPlayer.release()
    }
}