package com.hitsuji.sheepplayer2.ui.tracks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.domain.usecase.GetMusicLibraryUseCase
import kotlinx.coroutines.launch

class TracksViewModel(
    private val getMusicLibraryUseCase: GetMusicLibraryUseCase
) : ViewModel() {

    private val _artists = MutableLiveData<List<Artist>>()
    val artists: LiveData<List<Artist>> = _artists

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadTracks(showLoading: Boolean = true) {
        if (showLoading) _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = getMusicLibraryUseCase()
                _artists.value = result
            } catch (e: Exception) {
                // Handle error
            } finally {
                if (showLoading) _isLoading.value = false
            }
        }
    }
}
