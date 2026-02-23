package com.hitsuji.sheepplayer2.interfaces

interface FragmentNotifier {
    fun notifyDataLoaded(showLoading: Boolean = true)
    fun notifyPlaybackStateChanged()
}
