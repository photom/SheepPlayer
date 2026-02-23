package com.hitsuji.sheepplayer2

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.hitsuji.sheepplayer2.domain.service.PathValidator
import com.hitsuji.sheepplayer2.interfaces.MusicPlayerInterface
import com.hitsuji.sheepplayer2.interfaces.PlaybackStateListener
import java.io.File
import java.io.IOException

class MusicPlayer(
    private val context: Context,
    private val pathValidator: PathValidator
) : MusicPlayerInterface {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: Track? = null
    private var isInitialized = false
    private var shouldAutoPlay = false
    
    private var playbackStateListener: PlaybackStateListener? = null

    override fun setOnPlaybackStateChangeListener(listener: PlaybackStateListener) {
        playbackStateListener = listener
    }

    override fun loadTrack(track: Track, autoPlay: Boolean): Boolean {
        try {
            release() // Release any existing MediaPlayer
            shouldAutoPlay = autoPlay
            currentTrack = track

            // MusicPlayer now only handles local files.
            // Remote file downloading is handled by PlayTrackUseCase.
            return loadLocalTrack(track)

        } catch (e: Exception) {
            Log.e("MusicPlayer", "Unexpected error loading track: ${track.title}", e)
            playbackStateListener?.onPlaybackError(track, "Unexpected error: ${e.message}")
            return false
        }
    }

    private fun loadLocalTrack(track: Track): Boolean {
        // Validate file path for security using domain service
        if (!pathValidator.isValidPath(track.filePath)) {
            Log.e("MusicPlayer", "Invalid or unsafe file path: ${track.filePath}")
            playbackStateListener?.onPlaybackError(track, "Invalid file path")
            return false
        }

        // Double check existence & readability as per security test plan
        val file = File(track.filePath)
        if (!file.exists() || !file.canRead()) {
            Log.e("MusicPlayer", "File not found or not readable: ${track.filePath}")
            playbackStateListener?.onPlaybackError(track, "File not readable")
            return false
        }

        return loadLocalTrackFromFile(track, track.filePath)
    }

    private fun loadLocalTrackFromFile(track: Track, filePath: String): Boolean {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepareAsync()

                setOnPreparedListener {
                    isInitialized = true
                    Log.d("MusicPlayer", "Track prepared: ${track.title}")
                    if (shouldAutoPlay) {
                        start()
                        playbackStateListener?.onPlaybackStarted(track)
                        shouldAutoPlay = false
                    }
                }

                setOnCompletionListener {
                    Log.d("MusicPlayer", "Track completed: ${track.title}")
                    playbackStateListener?.onPlaybackCompleted(track)
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("MusicPlayer", "MediaPlayer error: what=$what, extra=$extra")
                    playbackStateListener?.onPlaybackError(track, "Playback error: $what")
                    false
                }
            }

            return true

        } catch (e: IOException) {
            Log.e("MusicPlayer", "Failed to load track: ${track.title}", e)
            playbackStateListener?.onPlaybackError(track, "Failed to load track: ${e.message}")
            return false
        }
    }

    override fun play(): Boolean {
        return try {
            val player = mediaPlayer
            val track = currentTrack

            if (player != null && track != null && isInitialized) {
                if (!player.isPlaying) {
                    player.start()
                    playbackStateListener?.onPlaybackStarted(track)
                    Log.d("MusicPlayer", "Started playing: ${track.title}")
                }
                true
            } else {
                Log.w("MusicPlayer", "Cannot play - MediaPlayer not ready")
                false
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error starting playback", e)
            currentTrack?.let { track ->
                playbackStateListener?.onPlaybackError(
                    track,
                    "Error starting playback: ${e.message}"
                )
            }
            false
        }
    }

    override fun pause(): Boolean {
        return try {
            val player = mediaPlayer
            val track = currentTrack

            if (player != null && track != null && player.isPlaying) {
                player.pause()
                playbackStateListener?.onPlaybackPaused(track)
                Log.d("MusicPlayer", "Paused: ${track.title}")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error pausing playback", e)
            false
        }
    }

    override fun stop(): Boolean {
        return try {
            val player = mediaPlayer

            if (player != null) {
                if (player.isPlaying) {
                    player.stop()
                }
                playbackStateListener?.onPlaybackStopped()
                Log.d("MusicPlayer", "Stopped playback")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error stopping playback", e)
            false
        }
    }

    override fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun getCurrentTrack(): Track? {
        return currentTrack
    }

    override fun getCurrentPosition(): Int {
        return try {
            if (isInitialized) {
                mediaPlayer?.currentPosition ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    override fun getDuration(): Int {
        return try {
            if (isInitialized) {
                mediaPlayer?.duration ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    override fun seekTo(position: Int) {
        try {
            if (isInitialized) {
                mediaPlayer?.seekTo(position)
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error seeking to position $position", e)
        }
    }

    override fun release() {
        try {
            mediaPlayer?.apply {
                // Clear all listeners before release to prevent unhandled events
                setOnPreparedListener(null)
                setOnCompletionListener(null)
                setOnErrorListener(null)

                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
            mediaPlayer = null
            currentTrack = null
            isInitialized = false
            shouldAutoPlay = false
            Log.d("MusicPlayer", "MediaPlayer released")
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error releasing MediaPlayer", e)
        }
    }
}
