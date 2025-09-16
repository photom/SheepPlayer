package com.hitsuji.sheepplayer2.service

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.hitsuji.sheepplayer2.Artist
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Google Drive integration.
 * 
 * This interface defines the contract for all Google Drive-related operations
 * including authentication, file discovery, and metadata extraction.
 * 
 * @author SheepPlayer Team
 * @version 1.0
 * @since 1.0
 */
interface GoogleDriveRepository {
    
    // Authentication Operations
    
    /**
     * Initiates the Google Drive sign-in process.
     * 
     * @return GoogleDriveResult containing success/failure status
     * @throws GoogleDriveServiceException if sign-in cannot be initiated
     */
    suspend fun signIn(): GoogleDriveResult<Boolean>
    
    /**
     * Signs out from Google Drive and clears cached credentials.
     * 
     * @return GoogleDriveResult indicating success/failure of sign-out
     */
    suspend fun signOut(): GoogleDriveResult<Unit>
    
    /**
     * Checks if the user is currently signed in to Google Drive.
     * 
     * @return true if signed in, false otherwise
     */
    fun isSignedIn(): Boolean
    
    /**
     * Gets the currently signed-in Google account.
     * 
     * @return GoogleSignInAccount if signed in, null otherwise
     */
    fun getCurrentAccount(): GoogleSignInAccount?
    
    /**
     * Gets the email address of the currently signed-in account.
     * 
     * @return email address if signed in, null otherwise
     */
    fun getAccountEmail(): String?
    
    // Music Library Operations
    
    /**
     * Loads the complete music library from Google Drive.
     * 
     * This method performs a full scan of Google Drive for supported audio files
     * and extracts metadata synchronously. Use [loadMusicFromGoogleDriveSequentially]
     * for progressive loading with real-time UI updates.
     * 
     * @return GoogleDriveResult containing the complete list of artists
     * @see loadMusicFromGoogleDriveSequentially
     */
    suspend fun loadMusicFromGoogleDrive(): GoogleDriveResult<List<Artist>>
    
    /**
     * Loads music from Google Drive with progressive updates.
     * 
     * This method emits intermediate results as files are discovered and processed,
     * allowing for real-time UI updates during the loading process. Ideal for
     * large music libraries or slow network connections.
     * 
     * @return Flow of GoogleDriveResult containing progressive updates
     * @see loadMusicFromGoogleDrive
     */
    fun loadMusicFromGoogleDriveSequentially(): Flow<GoogleDriveResult<List<Artist>>>
    
    /**
     * Gets the cached music library data.
     * 
     * Returns the most recently loaded music data without triggering a new
     * Google Drive scan. Returns empty list if no data has been loaded yet.
     * 
     * @return List of artists from the most recent load operation
     */
    fun getLatestGoogleDriveArtists(): List<Artist>
    
    // File Operations
    
    /**
     * Downloads a specific file from Google Drive.
     * 
     * @param fileId The Google Drive file ID to download
     * @return GoogleDriveResult containing the file data as ByteArray
     * @throws GoogleDriveServiceException if file cannot be downloaded
     */
    suspend fun downloadFile(fileId: String): GoogleDriveResult<ByteArray>
    
    // Lifecycle Management
    
    /**
     * Cleans up resources and prepares for destruction.
     * 
     * This method should be called when the service is no longer needed
     * to prevent memory leaks and resource retention.
     */
    fun destroy()
}

/**
 * Sealed class representing the result of Google Drive operations.
 * 
 * @param T The type of data contained in successful results
 */
sealed class GoogleDriveResult<out T> {
    /**
     * Represents a successful operation with data.
     * 
     * @param data The result data
     */
    data class Success<T>(val data: T) : GoogleDriveResult<T>()
    
    /**
     * Represents a failed operation with error information.
     * 
     * @param message Human-readable error message
     * @param exception Optional exception that caused the error
     */
    data class Error<T>(
        val message: String,
        val exception: Throwable? = null
    ) : GoogleDriveResult<T>()
}

/**
 * Exception thrown by Google Drive service operations.
 * 
 * @param message Error message describing the failure
 * @param cause Optional cause of the exception
 */
class GoogleDriveServiceException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)