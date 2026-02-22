package com.hitsuji.sheepplayer2.fakes

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.service.GoogleDriveResult

class FakeGoogleDriveService : GoogleDriveServiceInterface {

    private var isSignedIn = false
    private var artists: List<Artist> = emptyList()
    private val files = mutableMapOf<String, ByteArray>()
    
    override suspend fun signIn(activity: AppCompatActivity): GoogleDriveResult<Unit> {
        isSignedIn = true
        return GoogleDriveResult.Success(Unit)
    }

    override suspend fun signOut(): GoogleDriveResult<Unit> {
        isSignedIn = false
        return GoogleDriveResult.Success(Unit)
    }

    override fun isSignedIn(): Boolean = isSignedIn

    override fun getCurrentAccount(): GoogleSignInAccount? = null // Can mock if needed

    override fun getLatestGoogleDriveArtists(): List<Artist> = artists

    override suspend fun downloadFile(fileId: String): GoogleDriveResult<ByteArray> {
        val data = files[fileId]
        return if (data != null) {
            GoogleDriveResult.Success(data)
        } else {
            GoogleDriveResult.Error("File not found")
        }
    }
    
    fun setArtists(newArtists: List<Artist>) {
        this.artists = newArtists
    }
    
    fun addFile(fileId: String, data: ByteArray) {
        files[fileId] = data
    }
    
    fun setSignedIn(signedIn: Boolean) {
        this.isSignedIn = signedIn
    }
}
