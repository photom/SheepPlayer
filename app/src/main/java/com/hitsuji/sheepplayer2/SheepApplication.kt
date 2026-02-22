package com.hitsuji.sheepplayer2

import android.app.Application

class SheepApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
