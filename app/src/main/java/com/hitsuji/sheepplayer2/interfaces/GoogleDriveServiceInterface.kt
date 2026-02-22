package com.hitsuji.sheepplayer2.interfaces

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.service.GoogleDriveResult

interface GoogleDriveServiceInterface {
    suspend fun signIn(activity: AppCompatActivity): GoogleDriveResult<Unit>
    suspend fun signOut(): GoogleDriveResult<Unit>
    fun isSignedIn(): Boolean
    fun getCurrentAccount(): GoogleSignInAccount?
    fun getLatestGoogleDriveArtists(): List<Artist>
    suspend fun downloadFile(fileId: String): GoogleDriveResult<ByteArray>
}