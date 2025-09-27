package com.hitsuji.sheepplayer2

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.interfaces.MusicPlayerInterface
import com.hitsuji.sheepplayer2.interfaces.PlaybackStateListener
import com.hitsuji.sheepplayer2.service.GoogleDriveResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MusicPlayer(private val context: Context) : MusicPlayerInterface {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: Track? = null
    private var isInitialized = false
    private var shouldAutoPlay = false
    private var googleDriveService: GoogleDriveServiceInterface? = null

    private var playbackStateListener: PlaybackStateListener? = null

    override fun setOnPlaybackStateChangeListener(listener: PlaybackStateListener) {
        playbackStateListener = listener
    }

    fun setGoogleDriveService(service: GoogleDriveServiceInterface) {
        googleDriveService = service
    }

    override fun loadTrack(track: Track, autoPlay: Boolean): Boolean {
        try {
            release() // Release any existing MediaPlayer
            shouldAutoPlay = autoPlay
            currentTrack = track

            // Check if this is a Google Drive file
            if (track.filePath.startsWith("gdrive://")) {
                loadGoogleDriveTrack(track)
                return true
            } else {
                // Handle local file
                return loadLocalTrack(track)
            }

        } catch (e: Exception) {
            Log.e("MusicPlayer", "Unexpected error loading track: ${track.title}", e)
            playbackStateListener?.onPlaybackError(track, "Unexpected error: ${e.message}")
            return false
        }
    }

    private fun loadGoogleDriveTrack(track: Track) {
        val driveService = googleDriveService
        if (driveService == null) {
            playbackStateListener?.onPlaybackError(track, "Google Drive service not available")
            return
        }

        // Extract file ID from gdrive:// URL
        val fileId = track.filePath.removePrefix("gdrive://")

        // Download file in background
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("MusicPlayer", "Downloading Google Drive file: ${track.title}")
            when (val result = driveService.downloadFile(fileId)) {
                is GoogleDriveResult.Success -> {
                    try {
                        // Save to temporary file
                        val tempFile = File(context.cacheDir, "gdrive_${fileId}.tmp")
                        FileOutputStream(tempFile).use { fos: FileOutputStream ->
                            fos.write(result.data)
                        }

                        // Load the temporary file
                        withContext(Dispatchers.Main) {
                            loadLocalTrackFromFile(track, tempFile.absolutePath)
                        }
                    } catch (e: Exception) {
                        Log.e("MusicPlayer", "Error saving downloaded file", e)
                        withContext(Dispatchers.Main) {
                            playbackStateListener?.onPlaybackError(
                                track,
                                "Error saving downloaded file: ${e.message}"
                            )
                        }
                    }
                }
                is GoogleDriveResult.Error -> {
                    Log.e("MusicPlayer", "Failed to download file: ${result.message}", result.exception)
                    withContext(Dispatchers.Main) {
                        playbackStateListener?.onPlaybackError(
                            track,
                            "Failed to download from Google Drive: ${result.message}"
                        )
                    }
                }
            }
        }
    }

    private fun loadLocalTrack(track: Track): Boolean {
        // Validate file path for security
        if (!isValidTrackFile(track.filePath)) {
            Log.e("MusicPlayer", "Invalid or unsafe file path: ${track.filePath}")
            playbackStateListener?.onPlaybackError(track, "Invalid file path")
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
                    // Clean up temporary file if it's a Google Drive file
                    if (track.filePath.startsWith("gdrive://")) {
                        try {
                            File(filePath).delete()
                        } catch (e: Exception) {
                            Log.w("MusicPlayer", "Failed to delete temp file", e)
                        }
                    }
                    playbackStateListener?.onPlaybackCompleted(track)
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("MusicPlayer", "MediaPlayer error: what=$what, extra=$extra")
                    // Clean up temporary file if it's a Google Drive file
                    if (track.filePath.startsWith("gdrive://")) {
                        try {
                            File(filePath).delete()
                        } catch (e: Exception) {
                            Log.w("MusicPlayer", "Failed to delete temp file", e)
                        }
                    }
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

    private fun isValidTrackFile(filePath: String): Boolean {
        return try {
            // Basic input validation
            if (filePath.isBlank() || filePath.length > 4096) {
                Log.w("MusicPlayer", "Invalid file path: empty or too long")
                return false
            }

            // Security: Check for various path traversal patterns
            val suspiciousPatterns = listOf("../", "..\\", "//", "\\\\", "%2e%2e", "..%2f", "..%5c")
            if (suspiciousPatterns.any { filePath.contains(it, ignoreCase = true) }) {
                Log.w("MusicPlayer", "Path traversal attempt detected: $filePath")
                return false
            }

            // Security: Validate allowed directories
            val normalizedPath = filePath.lowercase()
            val allowedPrefixes = listOf("/storage/", "/sdcard/", "/data/media/", "/android_asset/")
            if (!allowedPrefixes.any { normalizedPath.startsWith(it) }) {
                Log.w("MusicPlayer", "File path outside allowed directories: $filePath")
                return false
            }

            // Security: Check for symbolic link attacks
            val file = File(filePath)
            val canonicalPath = file.canonicalPath
            if (canonicalPath != filePath && !canonicalPath.lowercase().startsWith("/storage/")) {
                Log.w("MusicPlayer", "Potential symbolic link attack: $filePath -> $canonicalPath")
                return false
            }

            // Verify file properties
            file.exists() && file.canRead() && file.isFile && file.length() > 0
        } catch (e: SecurityException) {
            Log.e("MusicPlayer", "Security exception validating file path: $filePath", e)
            false
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error validating file path: $filePath", e)
            false
        }
    }
}