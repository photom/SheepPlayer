package com.hitsuji.sheepplayer2.ui.playing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.domain.usecase.PlayTrackUseCase
import com.hitsuji.sheepplayer2.domain.usecase.TogglePlaybackUseCase
import com.hitsuji.sheepplayer2.interfaces.PlaybackManagerInterface
import kotlinx.coroutines.launch

class PlayingViewModel(
    private val togglePlaybackUseCase: TogglePlaybackUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val playbackManager: PlaybackManagerInterface
) : ViewModel() {

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentTrack = MutableLiveData<Track?>()
    val currentTrack: LiveData<Track?> = _currentTrack

    init {
        playbackManager.setOnPlaybackStateChangeListener {
            _isPlaying.postValue(playbackManager.isPlaying)
            _currentTrack.postValue(playbackManager.currentPlayingTrack)
        }
        _isPlaying.value = playbackManager.isPlaying
        _currentTrack.value = playbackManager.currentPlayingTrack
    }

    fun togglePlayback() {
        togglePlaybackUseCase()
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            playTrackUseCase(track)
        }
    }
}
