package com.hitsuji.sheepplayer2.service

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.service.auth.GoogleDriveAuthenticator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Collections



/**
 * Refactored Google Drive service with clean architecture and separation of concerns.
 * 
 * This implementation delegates authentication to GoogleDriveAuthenticator,
 * file discovery to GoogleDriveFileDiscovery, and metadata operations to
 * GoogleDriveMetadataService, following the Single Responsibility Principle.
 * 
 * @param context Android context for service operations
 * 
 * @author SheepPlayer Team
 * @version 2.0
 * @since 2.0
 */
class GoogleDriveService(private val context: Context) : GoogleDriveServiceInterface {

    private var driveService: Drive? = null
    private val metadataCache = MetadataCache(context)
    
    // Dependency injection of focused service components
    private lateinit var authenticator: GoogleDriveAuthenticator
    private lateinit var fileDiscovery: GoogleDriveFileDiscovery
    private lateinit var metadataService: GoogleDriveMetadataService
    
    companion object {
        private const val TAG = "GoogleDriveService"
        private const val APPLICATION_NAME = "SheepPlayer"
        
        // Shared cache for the latest Google Drive music data across all instances
        @Volatile
        private var latestGoogleDriveArtists: List<Artist> = emptyList()
    }

    init {
        authenticator = GoogleDriveAuthenticator(context)
        initializeServicesIfSignedIn()
    }

    /**
     * Initializes dependent services if user is already signed in.
     */
    private fun initializeServicesIfSignedIn() {
        try {
            if (authenticator.isSignedIn()) {
                val account = authenticator.getCurrentAccount()
                if (account != null) {
                    initializeDriveService(account)
                    initializeDependentServices()
                    Log.d(TAG, "Services initialized for existing account: ${account.email}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing services for existing sign-in", e)
        }
    }
    
    /**
     * Initializes services that depend on an authenticated Drive service.
     */
    private fun initializeDependentServices() {
        driveService?.let { drive ->
            fileDiscovery = GoogleDriveFileDiscovery(drive)
            metadataService = GoogleDriveMetadataService(context, drive, metadataCache)
        }
    }
    
    /**
     * Exception thrown by Google Drive service operations.
     */
    class GoogleDriveServiceException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    override suspend fun signIn(): GoogleDriveResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Delegating sign-in to authenticator")
                val result = authenticator.signIn()
                
                return@withContext when (result) {
                    is GoogleDriveResult.Success -> {
                        val account = authenticator.getCurrentAccount()
                        if (account != null) {
                            initializeDriveService(account)
                            initializeDependentServices()
                            Log.d(TAG, "Sign-in successful, services initialized")
                        }
                        GoogleDriveResult.Success(Unit)
                    }
                    is GoogleDriveResult.Error -> GoogleDriveResult.Error(result.message, result.exception)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign in", e)
                GoogleDriveResult.Error("Sign-in failed: ${e.message}", e)
            }
        }
    }


    private fun initializeDriveService(account: GoogleSignInAccount) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_READONLY)
            )
            credential.selectedAccount = account.account

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(APPLICATION_NAME)
                .build()

            Log.d(TAG, "Drive service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Drive service", e)
            throw GoogleDriveServiceException("Failed to initialize Drive service", e)
        }
    }

    override suspend fun signOut(): GoogleDriveResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Delegating sign-out to authenticator")
                val result = authenticator.signOut()
                cleanup()
                Log.d(TAG, "Sign-out completed, services cleaned up")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign out", e)
                GoogleDriveResult.Error("Sign-out failed: ${e.message}", e)
            }
        }
    }

    private fun cleanup() {
        driveService = null
        // Dependent services will be reinitialized on next sign-in
    }

    suspend fun loadMusicFromGoogleDrive(): GoogleDriveResult<List<Artist>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isSignedIn()) {
                    return@withContext GoogleDriveResult.Error("Not signed in to Google Drive")
                }

                Log.d(TAG, "Starting complete music loading from Google Drive")

                // Use file discovery service to find all music files
                val discoveryResult = fileDiscovery.discoverAllMusicFiles()
                if (discoveryResult is GoogleDriveResult.Error) {
                    return@withContext GoogleDriveResult.Error<List<Artist>>(discoveryResult.message, discoveryResult.exception)
                }
                
                val musicFiles = (discoveryResult as GoogleDriveResult.Success).data
                if (musicFiles.isEmpty()) {
                    Log.d(TAG, "No music files found in Google Drive")
                    return@withContext GoogleDriveResult.Success(emptyList())
                }

                Log.d(TAG, "Found ${musicFiles.size} music files, extracting metadata")
                
                // Extract metadata for all files
                val metadataResults = metadataService.loadAndCacheMetadataBatch(musicFiles) { current, total ->
                    Log.d(TAG, "Metadata extraction progress: $current/$total")
                }
                
                // Create tracks from files with metadata
                val tracks = metadataService.createTracksFromFiles(musicFiles)
                val artists = metadataService.organizeTracksIntoArtists(tracks)
                
                // Update shared cache
                Companion.latestGoogleDriveArtists = artists

                Log.d(TAG, "Successfully organized into ${artists.size} artists")
                GoogleDriveResult.Success(artists)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading music from Google Drive", e)
                GoogleDriveResult.Error("Failed to load music: ${e.message}", e)
            }
        }
    }

    fun loadMusicFromGoogleDriveSequentially(): Flow<GoogleDriveResult<List<Artist>>> =
        flow {
            try {
                if (!isSignedIn()) {
                    emit(GoogleDriveResult.Error("Not signed in to Google Drive"))
                    return@flow
                }

                Log.d(TAG, "Starting sequential metadata loading from Google Drive")

                // Create shared track list that will be updated incrementally
                val allTracks = mutableListOf<Track>()

                // Process each MIME type sequentially with pagination
                for (mimeType in fileDiscovery.getSupportedAudioMimeTypes()) {
                    try {
                        // Discover all files for this MIME type first
                        val discoveryResult = fileDiscovery.discoverAllMusicFilesByType(mimeType)
                        if (discoveryResult is GoogleDriveResult.Error) {
                            Log.w(TAG, "Error discovering $mimeType files: ${discoveryResult.message}")
                            continue
                        }
                        
                        val allFilesForType = (discoveryResult as GoogleDriveResult.Success).data
                        if (allFilesForType.isEmpty()) {
                            continue
                        }
                        
                        Log.d(TAG, "Processing ${allFilesForType.size} files of type $mimeType")
                        
                        // Process files in pages to provide incremental updates
                        val pageSize = 50
                        val pages = allFilesForType.chunked(pageSize)
                        
                        for (pageFiles in pages) {
                            // Create tracks from files (with cached metadata where available)
                            val pageTracks = metadataService.createTracksFromFiles(pageFiles)
                            allTracks.addAll(pageTracks)
                            
                            // Organize current tracks and emit update
                            val currentArtists = metadataService.organizeTracksIntoArtists(allTracks)
                            Companion.latestGoogleDriveArtists = currentArtists // Update cache
                            emit(GoogleDriveResult.Success(currentArtists))
                            
                            // Load fresh metadata for uncached files
                            val uncachedFiles = pageFiles.filter { file ->
                                metadataCache.getCachedMetadata(file.id) == null
                            }
                            
                            if (uncachedFiles.isNotEmpty()) {
                                Log.d(TAG, "Loading metadata for ${uncachedFiles.size} uncached files")
                                
                                // Process files in smaller batches to provide more frequent updates
                                val metadataBatchSize = 5 // Emit update every 5 files
                                val metadataBatches = uncachedFiles.chunked(metadataBatchSize)
                                
                                for (metadataBatch in metadataBatches) {
                                    // Extract metadata for this small batch
                                    metadataService.loadAndCacheMetadataBatch(metadataBatch) { current, total ->
                                        Log.d(TAG, "Metadata batch progress: $current/$total")
                                    }
                                    
                                    // Refresh tracks with new metadata for the entire page
                                    val refreshedTracks = metadataService.createTracksFromFiles(pageFiles)
                                    
                                    // Update the tracks in our collection
                                    pageFiles.forEach { file ->
                                        val refreshedTrack = refreshedTracks.find { it.googleDriveFileId == file.id }
                                        if (refreshedTrack != null) {
                                            val existingIndex = allTracks.indexOfFirst { it.googleDriveFileId == file.id }
                                            if (existingIndex != -1) {
                                                allTracks[existingIndex] = refreshedTrack
                                            }
                                        }
                                    }
                                    
                                    // Emit update with refreshed metadata after each small batch
                                    val updatedArtists = metadataService.organizeTracksIntoArtists(allTracks)
                                    Companion.latestGoogleDriveArtists = updatedArtists // Update cache
                                    emit(GoogleDriveResult.Success(updatedArtists))
                                    
                                    Log.d(TAG, "Emitted update after processing metadata batch")
                                }
                            }
                        }
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "Error processing $mimeType files", e)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in sequential metadata loading", e)
                emit(GoogleDriveResult.Error("Failed to load music: ${e.message}", e))
            }
        }.flowOn(Dispatchers.IO)
    
    




    
    
    /**
     * Caches artwork for a specific track.
     * 
     * This method delegates to the metadata service for artwork extraction
     * and caching operations.
     * 
     * @param track The track to cache artwork for
     * @return URI of cached artwork, or null if unavailable
     */
    suspend fun cacheArtworkForTrack(track: Track): String? {
        if (track.googleDriveFileId == null || track.albumArtUri != null) {
            return track.albumArtUri
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val updatedTrack = metadataService.updateTrackWithFreshMetadata(track)
                return@withContext updatedTrack.albumArtUri
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache artwork for track: ${track.title}", e)
                return@withContext null
            }
        }
    }

    override suspend fun downloadFile(fileId: String): GoogleDriveResult<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                val drive = driveService
                    ?: return@withContext GoogleDriveResult.Error("Drive service not initialized")

                val outputStream = ByteArrayOutputStream()
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)

                Log.d(TAG, "Successfully downloaded file: $fileId")
                GoogleDriveResult.Success(outputStream.toByteArray())
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading file $fileId", e)
                GoogleDriveResult.Error("Failed to download file: ${e.message}", e)
            }
        }
    }

    override fun isSignedIn(): Boolean {
        return authenticator.isSignedIn() && driveService != null
    }

    override fun getCurrentAccount(): GoogleSignInAccount? {
        return authenticator.getCurrentAccount()
    }

    fun getAccountEmail(): String? {
        return authenticator.getAccountEmail()
    }

    /**
     * Gets the cached music library data.
     * 
     * Returns the most recently loaded music data without triggering a new
     * Google Drive scan. Returns empty list if no data has been loaded yet.
     * 
     * @return List of artists from the most recent load operation
     */
    override fun getLatestGoogleDriveArtists(): List<Artist> {
        return Companion.latestGoogleDriveArtists
    }
    
    fun destroy() {
        cleanup()
        authenticator.cleanup()
        metadataCache.close()
        Log.d(TAG, "GoogleDriveService destroyed")
    }
}