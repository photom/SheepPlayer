package com.hitsuji.sheepplayer2.handlers

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.appcompat.app.AppCompatActivity
import com.hitsuji.sheepplayer2.domain.usecase.IsSignedInUseCase
import com.hitsuji.sheepplayer2.domain.usecase.SignInUseCase
import com.hitsuji.sheepplayer2.domain.usecase.SignOutUseCase
import com.hitsuji.sheepplayer2.service.GoogleDriveResult
import kotlinx.coroutines.launch

class GoogleDriveAuthHandler(
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val isSignedInUseCase: IsSignedInUseCase,
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
    
    fun signIn(activity: AppCompatActivity) {
        lifecycleScope.launch {
            when (val result = signInUseCase(activity)) {
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
            when (val result = signOutUseCase()) {
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
            val isSignedIn = isSignedInUseCase()
            Log.d("GoogleDriveAuth", "Checking sign-in: isSignedIn=$isSignedIn")

            if (isSignedIn) {
                callback?.onSignInSuccess()
            }
            isSignedIn
        } catch (e: Exception) {
            Log.w("GoogleDriveAuth", "Error checking sign-in status", e)
            false
        }
    }
}
