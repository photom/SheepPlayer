package com.hitsuji.sheepplayer2.ui.tracks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TracksViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is tracks Fragment"
    }
    val text: LiveData<String> = _text
}