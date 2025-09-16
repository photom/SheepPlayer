package com.hitsuji.sheepplayer2.service

import android.content.Context
import android.util.Log
import com.google.api.services.drive.Drive
import com.hitsuji.sheepplayer2.Album
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.CachedMetadata
import com.hitsuji.sheepplayer2.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles Google Drive metadata extraction and caching operations.
 * 
 * This service is responsible for extracting metadata from audio files,
 * managing caching for performance, and organizing tracks into artists and albums.
 * 
 * @param context Android context for cache operations
 * @param driveService The authenticated Google Drive service instance
 * @param metadataCache Cache instance for storing extracted metadata
 * 
 * @author SheepPlayer Team
 * @version 1.0
 * @since 1.0
 */
class GoogleDriveMetadataService(
    private val context: Context,
    private val driveService: Drive,
    private val metadataCache: MetadataCache
) {
    
    companion object {
        private const val TAG = "GoogleDriveMetadataService"
        private const val GOOGLE_DRIVE_URI_PREFIX = "gdrive://"
    }
    
    /**
     * Data class representing file metadata extracted from audio files.
     */
    data class FileMetadata(
        val artistName: String,
        val albumName: String,
        val trackName: String,
        val duration: Long = 0L,
        val year: String? = null,
        val genre: String? = null
    )
    
    /**
     * Data class representing the result of metadata extraction.
     */
    data class MetadataExtractionResult(
        val metadata: FileMetadata,
        val trackNumber: Int? = null,
        val artwork: ByteArray? = null,
        val wasSuccessful: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MetadataExtractionResult

            if (metadata != other.metadata) return false
            if (trackNumber != other.trackNumber) return false
            if (artwork != null) {
                if (other.artwork == null) return false
                if (!artwork.contentEquals(other.artwork)) return false
            } else if (other.artwork != null) return false
            if (wasSuccessful != other.wasSuccessful) return false

            return true
        }

        override fun hashCode(): Int {
            var result = metadata.hashCode()
            result = 31 * result + (trackNumber ?: 0)
            result = 31 * result + (artwork?.contentHashCode() ?: 0)
            result = 31 * result + wasSuccessful.hashCode()
            return result
        }
    }
    
    /**
     * Extracts metadata from a Google Drive audio file.
     * 
     * This method downloads partial file data and extracts metadata
     * including title, artist, album, duration, track number, and artwork.
     * 
     * @param file The Google Drive file to extract metadata from
     * @param fileName The name of the file for fallback metadata
     * @return MetadataExtractionResult containing extracted metadata
     */
    suspend fun extractMetadata(
        file: com.google.api.services.drive.model.File,
        fileName: String
    ): MetadataExtractionResult = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Extracting metadata from file: $fileName")
            
            val musicExtractor = MusicMetadataExtractor()
            val musicMetadata = musicExtractor.extractMetadataFromDrive(
                driveService,
                file.id,
                fileName,
                file.mimeType ?: ""
            )

            if (musicMetadata != null) {
                Log.d(TAG, "Successfully extracted metadata for: $fileName")
                MetadataExtractionResult(
                    metadata = FileMetadata(
                        artistName = musicMetadata.artist ?: "Unknown Artist",
                        albumName = musicMetadata.album ?: "Unknown Album",
                        trackName = musicMetadata.title ?: fileName.substringBeforeLast('.'),
                        duration = musicMetadata.duration
                    ),
                    trackNumber = musicMetadata.trackNumber,
                    artwork = musicMetadata.artwork,
                    wasSuccessful = true
                )
            } else {
                Log.w(TAG, "Metadata extraction returned null for $fileName")
                createFallbackMetadata(fileName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting metadata for $fileName", e)
            createFallbackMetadata(fileName)
        }
    }
    
    /**
     * Loads and caches metadata for multiple files efficiently.
     * 
     * This method processes files in batches, checking cache first
     * and only extracting metadata for uncached files.
     * 
     * @param files List of Google Drive files to process
     * @param onProgressUpdate Callback for progress updates
     * @return List of successful metadata extractions
     */
    suspend fun loadAndCacheMetadataBatch(
        files: List<com.google.api.services.drive.model.File>,
        onProgressUpdate: suspend (Int, Int) -> Unit = { _, _ -> }
    ): List<Pair<String, MetadataExtractionResult>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<String, MetadataExtractionResult>>()
        
        Log.d(TAG, "Processing metadata for ${files.size} files")
        
        files.forEachIndexed { index, file ->
            try {
                val fileName = file.name ?: "Unknown File"
                val fileId = file.id
                
                // Check cache first
                val cachedMetadata = metadataCache.getCachedMetadata(fileId)
                
                val extractionResult = if (cachedMetadata != null) {
                    Log.d(TAG, "Using cached metadata for: $fileName")
                    MetadataExtractionResult(
                        metadata = FileMetadata(
                            artistName = cachedMetadata.artistName,
                            albumName = cachedMetadata.albumName,
                            trackName = cachedMetadata.title,
                            duration = cachedMetadata.duration
                        ),
                        trackNumber = cachedMetadata.trackNumber,
                        artwork = cachedMetadata.artwork,
                        wasSuccessful = true
                    )
                } else {
                    Log.d(TAG, "Extracting fresh metadata for: $fileName")
                    val result = extractMetadata(file, fileName)
                    
                    // Cache the result if successful
                    if (result.wasSuccessful) {
                        val cacheData = CachedMetadata(
                            fileId = fileId,
                            title = result.metadata.trackName,
                            artistName = result.metadata.artistName,
                            albumName = result.metadata.albumName,
                            duration = result.metadata.duration,
                            trackNumber = result.trackNumber,
                            artwork = result.artwork
                        )
                        metadataCache.cacheMetadata(cacheData)
                        Log.d(TAG, "Cached metadata for: $fileName")
                    }
                    
                    result
                }
                
                if (extractionResult.wasSuccessful) {
                    results.add(Pair(fileId, extractionResult))
                }
                
                onProgressUpdate(index + 1, files.size)
                
            } catch (e: Exception) {
                Log.w(TAG, "Error processing file: ${file.name}", e)
            }
        }
        
        Log.d(TAG, "Completed metadata processing: ${results.size}/${files.size} successful")
        return@withContext results
    }
    
    /**
     * Creates Track objects from files and their cached metadata.
     * 
     * This method is optimized for quick track creation using cached data,
     * falling back to placeholder data when metadata isn't available.
     * 
     * @param files List of Google Drive files
     * @return List of Track objects
     */
    suspend fun createTracksFromFiles(
        files: List<com.google.api.services.drive.model.File>
    ): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        
        for (file in files) {
            try {
                val fileName = file.name ?: continue
                val fileId = file.id
                val cachedMetadata = metadataCache.getCachedMetadata(fileId)

                val (artistName, albumName, trackName, duration, trackNumber, artwork) = if (cachedMetadata != null) {
                    Tuple6(
                        cachedMetadata.artistName,
                        cachedMetadata.albumName,
                        cachedMetadata.title,
                        cachedMetadata.duration,
                        cachedMetadata.trackNumber,
                        cachedMetadata.artwork
                    )
                } else {
                    val fallbackName = fileName.substringBeforeLast('.')
                    Tuple6("Unknown Artist", "Unknown Album", fallbackName, 0L, null, null)
                }

                // Create artwork URI if cached artwork is available
                val albumArtUri = artwork?.let { artworkBytes ->
                    try {
                        createArtworkUri(fileId, artworkBytes)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to create artwork URI for $fileName", e)
                        null
                    }
                }

                val track = Track(
                    id = fileId.hashCode().toLong(),
                    title = trackName,
                    artistName = artistName,
                    albumName = albumName,
                    duration = duration,
                    filePath = "$GOOGLE_DRIVE_URI_PREFIX$fileId",
                    albumArtUri = albumArtUri,
                    trackNumber = trackNumber,
                    googleDriveFileId = fileId,
                    isMetadataLoaded = cachedMetadata != null
                )

                tracks.add(track)
                
            } catch (e: Exception) {
                Log.w(TAG, "Error creating track from file: ${file.name}", e)
            }
        }
        
        return@withContext tracks
    }
    
    /**
     * Organizes tracks into artists and albums with proper sorting.
     * 
     * This method groups tracks by artist and album, then sorts them
     * according to track numbers and titles.
     * 
     * @param tracks List of tracks to organize
     * @return List of Artist objects with nested Album and Track structures
     */
    fun organizeTracksIntoArtists(tracks: List<Track>): List<Artist> {
        val artistsMap = mutableMapOf<String, MutableMap<String, MutableList<Track>>>()
        
        // Group tracks by artist and album
        tracks.forEach { track ->
            val artistAlbums = artistsMap.getOrPut(track.artistName) { mutableMapOf() }
            val albumTracks = artistAlbums.getOrPut(track.albumName) { mutableListOf() }
            albumTracks.add(track)
        }
        
        // Convert to Artist objects with proper sorting
        val artists = artistsMap.map { (artistName, albums) ->
            val artistAlbums = albums.map { (albumName, albumTracks) ->
                // Sort tracks by track number, then by title
                val sortedTracks = albumTracks.sortedWith(
                    compareBy<Track> { it.trackNumber ?: Int.MAX_VALUE }
                        .thenBy { it.title }
                ).toMutableList()
                
                Album(
                    id = albumName.hashCode().toLong(),
                    title = albumName,
                    artistName = artistName,
                    tracks = sortedTracks
                )
            }.toMutableList()
            
            Artist(
                id = artistName.hashCode().toLong(),
                name = artistName,
                albums = artistAlbums
            )
        }.sortedBy { it.name }
        
        Log.d(TAG, "Organized ${tracks.size} tracks into ${artists.size} artists")
        return artists
    }
    
    /**
     * Updates a track with fresh metadata extraction.
     * 
     * @param track The track to update
     * @return Updated track with fresh metadata, or original track if extraction fails
     */
    suspend fun updateTrackWithFreshMetadata(track: Track): Track = withContext(Dispatchers.IO) {
        val fileId = track.googleDriveFileId ?: return@withContext track
        
        try {
            // Create a mock file object for extraction
            val mockFile = com.google.api.services.drive.model.File().apply {
                id = fileId
                name = track.title
                mimeType = "audio/mpeg" // Default assumption
            }
            
            val extractionResult = extractMetadata(mockFile, track.title)
            
            if (extractionResult.wasSuccessful) {
                val metadata = extractionResult.metadata
                val albumArtUri = extractionResult.artwork?.let { artworkBytes ->
                    createArtworkUri(fileId, artworkBytes)
                }
                
                return@withContext track.copy(
                    title = metadata.trackName,
                    artistName = metadata.artistName,
                    albumName = metadata.albumName,
                    duration = metadata.duration,
                    trackNumber = extractionResult.trackNumber,
                    albumArtUri = albumArtUri,
                    isMetadataLoaded = true
                )
            }
            
            return@withContext track
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update track metadata for: ${track.title}", e)
            return@withContext track
        }
    }
    
    /**
     * Creates a local file URI for artwork caching.
     * 
     * @param fileId The Google Drive file ID
     * @param artworkBytes The artwork image data
     * @return Local file URI for the cached artwork
     */
    private fun createArtworkUri(fileId: String, artworkBytes: ByteArray): String {
        val cacheDir = context.cacheDir
        val artworkDir = File(cacheDir, "artwork")
        if (!artworkDir.exists()) {
            artworkDir.mkdirs()
        }
        
        val artworkFile = File(artworkDir, "$fileId.jpg")
        artworkFile.writeBytes(artworkBytes)
        
        return "file://${artworkFile.absolutePath}"
    }
    
    /**
     * Creates fallback metadata for files where extraction fails.
     */
    private fun createFallbackMetadata(fileName: String): MetadataExtractionResult {
        return MetadataExtractionResult(
            metadata = FileMetadata(
                artistName = "Unknown Artist",
                albumName = "Unknown Album",
                trackName = fileName.substringBeforeLast('.'),
                duration = 0L
            ),
            trackNumber = null,
            artwork = null,
            wasSuccessful = false
        )
    }
}

/**
 * Helper data class for multiple return values.
 */
private data class Tuple6<T1, T2, T3, T4, T5, T6>(
    val first: T1,
    val second: T2,
    val third: T3,
    val fourth: T4,
    val fifth: T5,
    val sixth: T6
)