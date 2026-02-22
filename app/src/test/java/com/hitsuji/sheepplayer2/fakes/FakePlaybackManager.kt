package com.hitsuji.sheepplayer2.fakes

import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.interfaces.PlaybackManagerInterface

class FakePlaybackManager : PlaybackManagerInterface {

    override var currentPlayingTrack: Track? = null
        private set
    
    override var isPlaying: Boolean = false
        private set
        
    private var duration: Int = 0
    private var currentPosition: Int = 0
    private var stateChangeListener: (() -> Unit)? = null
    private var completionListener: ((Track) -> Unit)? = null

    override fun playTrack(track: Track) {
        currentPlayingTrack = track
        isPlaying = true
        stateChangeListener?.invoke()
    }

    override fun togglePlayback(): Boolean {
        if (currentPlayingTrack != null) {
            isPlaying = !isPlaying
            stateChangeListener?.invoke()
            return true
        }
        return false
    }

    override fun stopPlayback() {
        isPlaying = false
        stateChangeListener?.invoke()
    }

    override fun syncPlaybackState() {
        // No-op for fake
    }

    override fun getCurrentPosition(): Int = currentPosition

    override fun getDuration(): Int = duration

    override fun release() {
        isPlaying = false
        currentPlayingTrack = null
    }

    override fun setOnPlaybackStateChangeListener(listener: () -> Unit) {
        stateChangeListener = listener
    }

    override fun setOnPlaybackCompletionListener(listener: (Track) -> Unit) {
        completionListener = listener
    }
    
    // Helper methods for testing
    fun simulateTrackCompletion() {
        currentPlayingTrack?.let { track ->
            completionListener?.invoke(track)
        }
    }
    
    fun setDuration(newDuration: Int) {
        this.duration = newDuration
    }
    
    fun setCurrentPosition(newPosition: Int) {
        this.currentPosition = newPosition
    }
}
