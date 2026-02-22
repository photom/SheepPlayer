package com.hitsuji.sheepplayer2.ui.menu

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuji.sheepplayer2.domain.usecase.*
import com.hitsuji.sheepplayer2.service.GoogleDriveResult
import kotlinx.coroutines.launch

class MenuViewModel(
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val isSignedInUseCase: IsSignedInUseCase,
    private val getAccountEmailUseCase: GetAccountEmailUseCase,
    private val getMusicLibraryUseCase: GetMusicLibraryUseCase
) : ViewModel() {

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

    fun updateGoogleAccountStatus() {
        val signedIn = isSignedInUseCase()
        _isSignedIn.value = signedIn

        if (signedIn) {
            val email = getAccountEmailUseCase()
            _googleAccountStatus.value = "Signed in"
            _currentAccountEmail.value = email ?: "Unknown account"
        } else {
            _googleAccountStatus.value = "Not signed in"
            _currentAccountEmail.value = ""
        }
    }

    fun updateMusicCount() {
        viewModelScope.launch {
            try {
                val artists = getMusicLibraryUseCase()
                val totalTracks = artists.sumOf { it.albums.sumOf { it.tracks.size } }
                val googleDriveTracks = artists.sumOf { artist ->
                    artist.albums.sumOf { album ->
                        album.tracks.count { it.filePath.startsWith("gdrive://") }
                    }
                }
                val localTracks = totalTracks - googleDriveTracks

                _musicCount.value = if (googleDriveTracks > 0) {
                    "Music files: $totalTracks ($localTracks local, $googleDriveTracks Google Drive)"
                } else {
                    "Music files: $localTracks (local only)"
                }
            } catch (e: Exception) {
                _musicCount.value = "Error loading music count"
            }
        }
    }

    suspend fun signIn(activity: AppCompatActivity): GoogleDriveResult<Unit> = signInUseCase(activity)

    suspend fun signOut(): GoogleDriveResult<Unit> = signOutUseCase()
}
