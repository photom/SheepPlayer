package com.hitsuji.sheepplayer2.service.auth

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.hitsuji.sheepplayer2.service.GoogleDriveResult
import com.hitsuji.sheepplayer2.service.GoogleDriveServiceException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.app.Activity

/**
 * Handles Google Drive authentication and account management.
 * 
 * This class is responsible for all authentication-related operations
 * including sign-in, sign-out, and account state management.
 * 
 * @param context Android context for Google Sign-In operations
 * 
 * @author SheepPlayer Team
 * @version 1.0
 * @since 1.0
 */
class GoogleDriveAuthenticator(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleDriveAuthenticator"
    }
    
    private val googleSignInClient: GoogleSignInClient
    private var currentAccount: GoogleSignInAccount? = null
    private var signInLauncher: ActivityResultLauncher<android.content.Intent>? = null
    
    init {
        googleSignInClient = GoogleSignIn.getClient(context, createSignInOptions())
        Log.d(TAG, "Google Sign-In client created successfully")
        initializeSignInLauncher()
        checkExistingAccount()
    }
    
    /**
     * Creates Google Sign-In options with required scopes.
     */
    private fun createSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .build()
    }
    
    /**
     * Initializes the sign-in result launcher if context is an AppCompatActivity.
     */
    private fun initializeSignInLauncher() {
        if (context is AppCompatActivity) {
            try {
                signInLauncher = context.activityResultRegistry.register(
                    "google_sign_in_${System.currentTimeMillis()}",
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    Log.d(TAG, "Sign-in result received: ${result.resultCode}")
                }
                Log.d(TAG, "Sign-in launcher initialized")
            } catch (e: Exception) {
                Log.w(TAG, "Could not initialize sign-in launcher", e)
            }
        } else {
            Log.w(TAG, "Context is not AppCompatActivity, cannot register launcher")
        }
    }
    
    /**
     * Checks for existing signed-in account and initializes current account.
     */
    private fun checkExistingAccount() {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_READONLY))) {
                currentAccount = account
                Log.d(TAG, "Found existing signed-in account: ${account.email}")
            } else {
                Log.d(TAG, "No valid existing account found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking existing account", e)
        }
    }
    
    /**
     * Initiates the Google Drive sign-in process.
     * 
     * @return GoogleDriveResult indicating success or failure of sign-in
     */
    suspend fun signIn(): GoogleDriveResult<Boolean> {
        return try {
            Log.d(TAG, "Starting Google Drive sign-in process")
            
            // Check for existing account first
            val existingAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (existingAccount != null && GoogleSignIn.hasPermissions(existingAccount, Scope(DriveScopes.DRIVE_READONLY))) {
                currentAccount = existingAccount
                Log.d(TAG, "Using existing signed-in account: ${existingAccount.email}")
                return GoogleDriveResult.Success(true)
            }
            
            if (signInLauncher == null) {
                Log.e(TAG, "Sign-in launcher not available - context may not be an Activity")
                return GoogleDriveResult.Error("Google Drive sign-in failed. Sign-in launcher not available")
            }
            
            // Perform the actual sign-in with suspendCancellableCoroutine
            signInWithLauncher()
            
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            GoogleDriveResult.Error("Sign-in failed: ${e.message}", e)
        }
    }
    
    /**
     * Performs sign-in using the activity result launcher with proper coroutine suspension.
     */
    private suspend fun signInWithLauncher(): GoogleDriveResult<Boolean> = suspendCancellableCoroutine { continuation ->
        if (context !is AppCompatActivity || signInLauncher == null) {
            Log.e(TAG, "Cannot sign in: context is not AppCompatActivity or launcher is null")
            continuation.resume(GoogleDriveResult.Error("Sign-in context not available"))
            return@suspendCancellableCoroutine
        }

        // Create a temporary launcher for this specific sign-in attempt
        var tempLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>? = null
        
        try {
            tempLauncher = context.activityResultRegistry.register(
                "google_sign_in_temp_${System.currentTimeMillis()}",
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                handleSignInResult(result, continuation)
                tempLauncher?.unregister()
            }

            val signInIntent = googleSignInClient.signInIntent
            Log.d(TAG, "Launching sign in intent")
            tempLauncher.launch(signInIntent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error launching sign in intent", e)
            tempLauncher?.unregister()
            continuation.resume(GoogleDriveResult.Error("Failed to launch sign-in: ${e.message}"))
        }
    }
    
    /**
     * Handles the sign-in result from the activity.
     */
    private fun handleSignInResult(
        result: androidx.activity.result.ActivityResult,
        continuation: kotlin.coroutines.Continuation<GoogleDriveResult<Boolean>>
    ) {
        try {
            Log.d(TAG, "Sign in result code: ${result.resultCode}")
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult()
                        Log.d(TAG, "Account: ${account?.email}")
                        if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_READONLY))) {
                            currentAccount = account
                            Log.d(TAG, "Successfully signed in: ${account.email}")
                            continuation.resume(GoogleDriveResult.Success(true))
                        } else {
                            val hasScopes = account?.let { GoogleSignIn.hasPermissions(it, Scope(DriveScopes.DRIVE_READONLY)) } ?: false
                            Log.w(TAG, "Account null: ${account == null}, missing scopes: ${!hasScopes}")
                            continuation.resume(GoogleDriveResult.Error("Sign-in failed: missing permissions"))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting account from task", e)
                        continuation.resume(GoogleDriveResult.Error("Sign-in failed: ${e.message}"))
                    }
                }
                Activity.RESULT_CANCELED -> {
                    Log.i(TAG, "Sign in cancelled by user")
                    continuation.resume(GoogleDriveResult.Error("Sign-in cancelled by user"))
                }
                else -> {
                    Log.w(TAG, "Sign in failed with result code: ${result.resultCode}")
                    continuation.resume(GoogleDriveResult.Error("Sign-in failed with code: ${result.resultCode}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing sign in result", e)
            continuation.resume(GoogleDriveResult.Error("Error processing sign-in: ${e.message}"))
        }
    }
    
    /**
     * Signs out from Google Drive and clears cached credentials.
     * 
     * @return GoogleDriveResult indicating success or failure of sign-out
     */
    suspend fun signOut(): GoogleDriveResult<Unit> {
        return try {
            Log.d(TAG, "Signing out from Google Drive")
            
            googleSignInClient.signOut().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Successfully signed out")
                } else {
                    Log.e(TAG, "Sign-out failed", task.exception)
                }
            }
            
            currentAccount = null
            cleanup()
            
            GoogleDriveResult.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed", e)
            GoogleDriveResult.Error("Sign-out failed: ${e.message}", e)
        }
    }
    
    /**
     * Checks if the user is currently signed in to Google Drive.
     * 
     * @return true if signed in with valid permissions, false otherwise
     */
    fun isSignedIn(): Boolean {
        val account = currentAccount ?: GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_READONLY))
    }
    
    /**
     * Gets the currently signed-in Google account.
     * 
     * @return GoogleSignInAccount if signed in, null otherwise
     */
    fun getCurrentAccount(): GoogleSignInAccount? {
        if (currentAccount == null) {
            currentAccount = GoogleSignIn.getLastSignedInAccount(context)
        }
        return currentAccount
    }
    
    /**
     * Gets the email address of the currently signed-in account.
     * 
     * @return email address if signed in, null otherwise
     */
    fun getAccountEmail(): String? {
        return getCurrentAccount()?.email
    }
    
    /**
     * Processes the sign-in result from the activity.
     * 
     * @param resultCode The result code from the sign-in activity
     * @return GoogleDriveResult indicating the outcome of sign-in processing
     */
    suspend fun processSignInResult(resultCode: Int): GoogleDriveResult<Boolean> {
        return try {
            Log.d(TAG, "Processing sign-in result with code: $resultCode")
            
            when (resultCode) {
                android.app.Activity.RESULT_OK -> {
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account != null) {
                        currentAccount = account
                        Log.d(TAG, "Sign-in successful: ${account.email}")
                        GoogleDriveResult.Success(true)
                    } else {
                        Log.e(TAG, "Sign-in completed but no account found")
                        GoogleDriveResult.Error("Sign-in completed but no account available")
                    }
                }
                android.app.Activity.RESULT_CANCELED -> {
                    Log.d(TAG, "Sign-in was cancelled by user")
                    GoogleDriveResult.Error("Sign-in cancelled by user")
                }
                else -> {
                    Log.e(TAG, "Sign-in failed with unknown result code: $resultCode")
                    GoogleDriveResult.Error("Sign-in failed with code: $resultCode")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing sign-in result", e)
            GoogleDriveResult.Error("Error processing sign-in: ${e.message}", e)
        }
    }
    
    /**
     * Cleans up resources and prepares for destruction.
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up GoogleDriveAuthenticator")
        currentAccount = null
        signInLauncher?.unregister()
        signInLauncher = null
    }
}