package com.hitsuji.sheepplayer2.handlers

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.service.GoogleDriveResult
import kotlinx.coroutines.launch

class GoogleDriveAuthHandler(
    private val googleDriveService: GoogleDriveServiceInterface,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    
    interface AuthCallback {
        fun onSignInSuccess()
        fun onSignInError(message: String, exception: Throwable?)
        fun onSignOutSuccess()
        fun onSignOutError(message: String, exception: Throwable?)
    }
    
    private var callback: AuthCallback? = null
    
    fun setCallback(callback: AuthCallback) {
        this.callback = callback
    }
    
    fun signIn() {
        lifecycleScope.launch {
            when (val result = googleDriveService.signIn()) {
                is GoogleDriveResult.Success -> {
                    callback?.onSignInSuccess()
                }
                is GoogleDriveResult.Error -> {
                    callback?.onSignInError(result.message, result.exception)
                }
            }
        }
    }
    
    fun signOut() {
        lifecycleScope.launch {
            when (val result = googleDriveService.signOut()) {
                is GoogleDriveResult.Success -> {
                    callback?.onSignOutSuccess()
                }
                is GoogleDriveResult.Error -> {
                    callback?.onSignOutError(result.message, result.exception)
                }
            }
        }
    }
    
    fun checkExistingSignIn(): Boolean {
        return try {
            val isSignedIn = googleDriveService.isSignedIn()
            val currentAccount = googleDriveService.getCurrentAccount()
            
            Log.d("GoogleDriveAuth", "Checking sign-in: isSignedIn=$isSignedIn, account=${currentAccount?.email}")
            isSignedIn
        } catch (e: Exception) {
            Log.w("GoogleDriveAuth", "Error checking sign-in status", e)
            false
        }
    }
}