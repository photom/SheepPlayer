package com.hitsuji.sheepplayer2.ui.playing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayingViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is playing Fragment"
    }
    val text: LiveData<String> = _text
}