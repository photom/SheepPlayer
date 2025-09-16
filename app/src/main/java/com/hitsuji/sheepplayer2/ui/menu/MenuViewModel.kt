package com.hitsuji.sheepplayer2.ui.menu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hitsuji.sheepplayer2.service.GoogleDriveService

class MenuViewModel(application: Application) : AndroidViewModel(application) {

    private val _googleAccountStatus = MutableLiveData<String>().apply {
        value = "Not signed in"
    }
    val googleAccountStatus: LiveData<String> = _googleAccountStatus

    private val _currentAccountEmail = MutableLiveData<String>().apply {
        value = ""
    }
    val currentAccountEmail: LiveData<String> = _currentAccountEmail

    private val _isSignedIn = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isSignedIn: LiveData<Boolean> = _isSignedIn

    private val _musicCount = MutableLiveData<String>().apply {
        value = "Music files: Loading..."
    }
    val musicCount: LiveData<String> = _musicCount

    fun updateGoogleAccountStatus(googleDriveService: GoogleDriveService) {
        val isSignedIn = googleDriveService.isSignedIn()
        _isSignedIn.value = isSignedIn

        if (isSignedIn) {
            val account = googleDriveService.getCurrentAccount()
            _googleAccountStatus.value = "Signed in"
            _currentAccountEmail.value = account?.email ?: "Unknown account"
        } else {
            _googleAccountStatus.value = "Not signed in"
            _currentAccountEmail.value = ""
        }
    }

    fun updateMusicCount(localCount: Int, googleDriveCount: Int) {
        val total = localCount + googleDriveCount
        _musicCount.value = if (googleDriveCount > 0) {
            "Music files: $total ($localCount local, $googleDriveCount Google Drive)"
        } else {
            "Music files: $localCount (local only)"
        }
    }
}