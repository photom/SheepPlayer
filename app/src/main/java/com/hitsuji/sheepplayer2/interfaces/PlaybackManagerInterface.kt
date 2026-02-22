package com.hitsuji.sheepplayer2.interfaces

import com.hitsuji.sheepplayer2.Track

interface PlaybackManagerInterface {
    val currentPlayingTrack: Track?
    val isPlaying: Boolean
    fun playTrack(track: Track)
    fun togglePlayback(): Boolean
    fun stopPlayback()
    fun syncPlaybackState()
    fun getCurrentPosition(): Int
    fun getDuration(): Int
    fun release()
    fun setOnPlaybackStateChangeListener(listener: () -> Unit)
    fun setOnPlaybackCompletionListener(listener: (Track) -> Unit)
}
