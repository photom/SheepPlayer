package com.hitsuji.sheepplayer2.service

import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import com.google.api.services.drive.Drive
import com.hitsuji.sheepplayer2.service.GoogleDriveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles Google Drive file discovery operations.
 * 
 * This service is responsible for finding and listing audio files
 * from Google Drive based on MIME types and search criteria.
 * 
 * @param driveService The authenticated Google Drive service instance
 * 
 * @author SheepPlayer Team
 * @version 1.1
 * @since 1.0
 */
class GoogleDriveFileDiscovery(private val driveService: Drive) {
    
    companion object {
        private const val TAG = "GoogleDriveFileDiscovery"
        private const val PAGE_SIZE = 100
        
        private val SUPPORTED_AUDIO_MIME_TYPES = listOf(
            "audio/mpeg",
            "audio/mp4", 
            "audio/flac",
            "audio/ogg",
            "audio/wav",
            "audio/x-wav",
            "audio/aac"
        )
        
        private const val DRIVE_QUERY_FIELDS = "nextPageToken, files(id,name,parents,size,mimeType)"
    }
    
    /**
     * Discovers all music files from Google Drive.
     */
    suspend fun discoverAllMusicFiles(): GoogleDriveResult<List<com.google.api.services.drive.model.File>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting music file discovery")
                
                val musicFiles = mutableListOf<com.google.api.services.drive.model.File>()
                
                for (mimeType in SUPPORTED_AUDIO_MIME_TYPES) {
                    try {
                        val filesForType = discoverFilesByMimeType(mimeType)
                        musicFiles.addAll(filesForType)
                        Log.d(TAG, "Found ${filesForType.size} files of type $mimeType")
                    } catch (e: GoogleAuthIOException) {
                        Log.e(TAG, "Auth error discovering files for type $mimeType", e)
                        return@withContext GoogleDriveResult.Error<List<com.google.api.services.drive.model.File>>(
                            "Authorization failed. Please check your Google Cloud configuration and SHA-1 fingerprint.",
                            e
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error discovering files for type $mimeType", e)
                    }
                }
                
                Log.d(TAG, "Completed file discovery: ${musicFiles.size} total files found")
                GoogleDriveResult.Success(musicFiles)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during file discovery", e)
                GoogleDriveResult.Error("File discovery failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Discovers all music files for a specific MIME type.
     */
    suspend fun discoverAllMusicFilesByType(mimeType: String): GoogleDriveResult<List<com.google.api.services.drive.model.File>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting discovery for MIME type: $mimeType")
                
                val files = discoverFilesByMimeType(mimeType)
                Log.d(TAG, "Completed discovery for $mimeType: ${files.size} files found")
                
                GoogleDriveResult.Success(files)
                
            } catch (e: GoogleAuthIOException) {
                Log.e(TAG, "Auth error discovering files for $mimeType", e)
                GoogleDriveResult.Error(
                    "Authentication failed for $mimeType. Ensure SHA-1 is correctly registered in Google Cloud Console.",
                    e
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error discovering files for $mimeType", e)
                GoogleDriveResult.Error("Discovery failed for $mimeType: ${e.message}", e)
            }
        }
    }
    
    /**
     * Discovers music files by MIME type with pagination support.
     * 
     * This method provides efficient discovery of files by processing
     * pages of results for a specific MIME type.
     * 
     * @param mimeType The audio MIME type to search for
     * @param onPageDiscovered Callback invoked for each page of results
     * @return GoogleDriveResult containing list of all files for the MIME type
     */
    suspend fun discoverFilesByMimeTypeWithPagination(
        mimeType: String,
        onPageDiscovered: suspend (List<com.google.api.services.drive.model.File>) -> Unit
    ): GoogleDriveResult<List<com.google.api.services.drive.model.File>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting paginated discovery for type: $mimeType")
                
                val allFiles = mutableListOf<com.google.api.services.drive.model.File>()
                val query = "mimeType='$mimeType' and trashed=false"
                var nextPageToken: String? = null
                
                do {
                    val result = driveService.files().list()
                        .setQ(query)
                        .setFields(DRIVE_QUERY_FIELDS)
                        .setPageSize(PAGE_SIZE)
                        .setPageToken(nextPageToken)
                        .execute()
                    
                    result.files?.let { pageFiles ->
                        if (pageFiles.isNotEmpty()) {
                            Log.d(TAG, "Discovered page with ${pageFiles.size} files of type $mimeType")
                            allFiles.addAll(pageFiles)
                            onPageDiscovered(pageFiles)
                        }
                    }
                    
                    nextPageToken = result.nextPageToken
                } while (nextPageToken != null)
                
                Log.d(TAG, "Completed paginated discovery for $mimeType: ${allFiles.size} files")
                GoogleDriveResult.Success(allFiles)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in paginated discovery for $mimeType", e)
                GoogleDriveResult.Error("Paginated discovery failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Gets all supported audio MIME types.
     * 
     * @return List of supported audio MIME types
     */
    fun getSupportedAudioMimeTypes(): List<String> {
        return SUPPORTED_AUDIO_MIME_TYPES
    }
    
    /**
     * Checks if a MIME type is supported for audio files.
     * 
     * @param mimeType The MIME type to check
     * @return true if supported, false otherwise
     */
    fun isSupportedAudioMimeType(mimeType: String): Boolean {
        return mimeType in SUPPORTED_AUDIO_MIME_TYPES
    }
    
    /**
     * Discovers files for a specific MIME type.
     * 
     * @param mimeType The audio MIME type to search for
     * @return List of discovered files
     * @throws Exception if discovery fails for this MIME type
     */
    private suspend fun discoverFilesByMimeType(mimeType: String): List<com.google.api.services.drive.model.File> {
        val files = mutableListOf<com.google.api.services.drive.model.File>()
        val query = "mimeType='$mimeType' and trashed=false"
        var nextPageToken: String? = null
        
        do {
            val result = driveService.files().list()
                .setQ(query)
                .setFields(DRIVE_QUERY_FIELDS)
                .setPageSize(PAGE_SIZE)
                .setPageToken(nextPageToken)
                .execute()
            
            result.files?.let { pageFiles ->
                files.addAll(pageFiles)
                Log.d(TAG, "Found ${pageFiles.size} files in current page for type $mimeType")
            }
            
            nextPageToken = result.nextPageToken
        } while (nextPageToken != null)
        
        return files
    }
}