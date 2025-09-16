package com.hitsuji.sheepplayer2.interfaces

import com.hitsuji.sheepplayer2.Track

interface MusicPlayerInterface {
    fun loadTrack(track: Track, autoPlay: Boolean = false): Boolean
    fun play(): Boolean
    fun pause(): Boolean
    fun stop(): Boolean
    fun isPlaying(): Boolean
    fun getCurrentTrack(): Track?
    fun getCurrentPosition(): Int
    fun getDuration(): Int
    fun seekTo(position: Int)
    fun release()
    fun setOnPlaybackStateChangeListener(listener: PlaybackStateListener)
}

interface PlaybackStateListener {
    fun onPlaybackStarted(track: Track)
    fun onPlaybackPaused(track: Track)
    fun onPlaybackStopped()
    fun onPlaybackError(track: Track, error: String)
    fun onPlaybackCompleted(track: Track)
}