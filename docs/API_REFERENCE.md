# Reference Documentation

This reference provides detailed technical information about SheepPlayer's classes, methods, and
APIs. Use this as a lookup resource when working with the codebase.

## 📊 Data Models

### `Artist`

Represents a music artist with their albums.

```kotlin
data class Artist(
    val id: Long,           // MediaStore ID for the artist
    val name: String,       // Artist name
    val albums: MutableList<Album> = mutableListOf()
)
```

**Usage Example:**

```kotlin
val artist = Artist(
    id = 12345L,
    name = "The Beatles",
    albums = mutableListOf()
)
```

### `Album`

Represents an album with its tracks.

```kotlin
data class Album(
    val id: Long,           // MediaStore ID for the album
    val title: String,      // Album title
    val artistName: String, // Artist name for this album
    val tracks: MutableList<Track> = mutableListOf()
)
```

### `Track`

Represents an individual music track.

```kotlin
data class Track(
    val id: Long,           // MediaStore ID for the track
    val title: String,      // Track title
    val artistName: String, // Artist name
    val albumName: String,  // Album name
    val duration: Long,     // Duration in milliseconds
    val filePath: String,   // URI path to the file
    val albumArtUri: String? = null, // Optional album art URI
    val trackNumber: Int? = null, // Track number for ordering within albums
    val googleDriveFileId: String? = null, // Google Drive file ID for caching
    val isMetadataLoaded: Boolean = true // Track if metadata is fully loaded
)
```

## 🎵 Music Player Components

### `MusicPlayer`

Core music playback functionality with security enhancements and Google Drive integration.

#### Key Methods

```kotlin
class MusicPlayer(private val context: Context) {
    
    /**
     * Loads a track for playback with security validation
     * @param track The track to load
     * @param autoPlay Whether to start playing immediately
     * @return true if loading succeeded, false otherwise
     */
    fun loadTrack(track: Track, autoPlay: Boolean = false): Boolean
    
    /**
     * Starts playback of the currently loaded track
     * @return true if playback started successfully
     */
    fun play(): Boolean
    
    /**
     * Pauses current playback
     * @return true if paused successfully
     */
    fun pause(): Boolean
    
    /**
     * Stops playback and releases resources
     * @return true if stopped successfully
     */
    fun stop(): Boolean
    
    /**
     * Checks if music is currently playing
     * @return true if playing, false otherwise
     */
    fun isPlaying(): Boolean
    
    /**
     * Gets the currently loaded track
     * @return current track or null if none loaded
     */
    fun getCurrentTrack(): Track?
    
    /**
     * Gets current playback position
     * @return position in milliseconds
     */
    fun getCurrentPosition(): Int
    
    /**
     * Gets duration of current track
     * @return duration in milliseconds
     */
    fun getDuration(): Int
    
    /**
     * Seeks to a specific position in the track
     * @param position Position in milliseconds
     */
    fun seekTo(position: Int)
    
    /**
     * Sets Google Drive service for cloud playback
     * @param googleDriveService Service instance for cloud operations
     */
    fun setGoogleDriveService(googleDriveService: GoogleDriveService)
    
    /**
     * Releases all resources
     */
    fun release()
}
```

#### Security Features

- **File Path Validation**: Prevents directory traversal attacks
- **File Existence Check**: Verifies file accessibility before playback
- **Exception Handling**: Comprehensive error handling with logging

**Usage Example:**

```kotlin
val musicPlayer = MusicPlayer(context)
musicPlayer.setOnPlaybackStateChangeListener(object : MusicPlayer.OnPlaybackStateChangeListener {
    override fun onPlaybackStarted(track: Track) {
        // Update UI for playing state
    }
    override fun onPlaybackPaused(track: Track) {
        // Update UI for paused state
    }
    // ... other callbacks
})

// Load and play a track
if (musicPlayer.loadTrack(selectedTrack, autoPlay = true)) {
    // Track loaded successfully
}
```

### `MusicPlayerManager`

Higher-level music player management with state handling.

```kotlin
class MusicPlayerManager(private val musicPlayer: MusicPlayer) {
    
    val currentPlayingTrack: Track?  // Currently playing track
    val isPlaying: Boolean           // Current playback state
    
    /**
     * Plays a specific track
     */
    fun playTrack(track: Track)
    
    /**
     * Toggles between play and pause
     * @return true if action was successful
     */
    fun togglePlayback(): Boolean
    
    /**
     * Syncs internal state with actual player state
     */
    fun syncPlaybackState()
    
    /**
     * Sets callback for playback state changes
     */
    fun setOnPlaybackStateChangeListener(listener: () -> Unit)
}
```

## 🖼️ Image Service Layer

### `ArtistImageService`

Handles artist image search, download, and validation with security features.

```kotlin
class ArtistImageService(private val context: Context? = null) {
    
    /**
     * Searches for artist images from multiple search engines
     * @param artistName Name of the artist to search for
     * @param maxImages Maximum number of image URLs to return
     * @return List of image URLs
     */
    suspend fun searchArtistImages(artistName: String, maxImages: Int = 10): List<String>
    
    /**
     * Downloads an image from URL with magic number validation
     * @param imageUrl URL of the image to download
     * @return Bitmap if successful, null if failed or invalid
     */
    suspend fun downloadImage(imageUrl: String): Bitmap?
    
    /**
     * Gets animated GIF placeholder for loading state
     * @return Bitmap of the sheep loading placeholder, or fallback bitmap
     */
    fun getLoadingPlaceholderBitmap(): Bitmap?
    
    /**
     * Validates image magic numbers to ensure valid image files
     * @param bytes Byte array to validate
     * @return true if valid image format detected
     */
    private fun isValidImageMagicNumber(bytes: ByteArray): Boolean
    
    /**
     * Releases network resources
     */
    fun cleanup()
}
```

#### Security Features

- **Magic Number Validation**: Validates file signatures for JPEG, PNG, GIF, WebP, BMP, ICO, TIFF
- **Content Filtering**: Excludes non-image files masquerading as images
- **Size Filtering**: Rejects images smaller than 150x150 pixels (thumbnails)
- **Network Security**: Uses secure HTTPS requests with proper headers
- **Memory Management**: Efficient bitmap handling with sample size calculation

#### Search Strategy

- **Multi-Engine**: Searches Google, Bing, and DuckDuckGo simultaneously
- **Multiple Terms**: Uses various search terms for better coverage
- **URL Validation**: Validates image URLs before attempting download
- **Duplicate Prevention**: Uses Set collection to avoid duplicate URLs
- **Error Resilience**: Continues searching even if one engine fails

#### Supported Image Formats

| Format | Magic Numbers                 | Description                 |
|--------|-------------------------------|-----------------------------|
| JPEG   | `FF D8 FF`                    | JPEG/JFIF images            |
| PNG    | `89 50 4E 47 0D 0A 1A 0A`     | Portable Network Graphics   |
| GIF    | `47 49 46 38`                 | Graphics Interchange Format |
| WebP   | `52 49 46 46...57 45 42 50`   | Google WebP format          |
| BMP    | `42 4D`                       | Windows Bitmap              |
| ICO    | `00 00 01 00`                 | Windows Icon                |
| TIFF   | `49 49 2A 00` / `4D 4D 00 2A` | Tagged Image File Format    |

**Usage Example:**

```kotlin
val imageService = ArtistImageService(context)
val imageUrls = imageService.searchArtistImages("The Beatles", 10)
val bitmap = imageService.downloadImage(imageUrls.first())
if (bitmap != null) {
    // Valid image downloaded successfully
}
```

## 🔄 Service Layer

### `GoogleDriveService`

Handles Google Drive authentication and file operations.

```kotlin
class GoogleDriveService(private val context: Context) {
    
    /**
     * Signs in to Google Drive
     * @return GoogleDriveResult indicating success or failure
     */
    suspend fun signIn(): GoogleDriveResult<Unit>
    
    /**
     * Signs out from Google Drive
     * @return GoogleDriveResult indicating success or failure
     */
    suspend fun signOut(): GoogleDriveResult<Unit>
    
    /**
     * Checks if user is currently signed in
     * @return true if signed in, false otherwise
     */
    fun isSignedIn(): Boolean
    
    /**
     * Gets current Google account
     * @return GoogleSignInAccount or null if not signed in
     */
    fun getCurrentAccount(): GoogleSignInAccount?
    
    /**
     * Gets latest cached Google Drive artists
     * @return List of artists from Google Drive
     */
    fun getLatestGoogleDriveArtists(): List<Artist>
}
```

### `MetadataLoadingService`

Background service for loading Google Drive music metadata.

```kotlin
class MetadataLoadingService : IntentService() {
    
    companion object {
        const val BROADCAST_METADATA_UPDATE = "com.hitsuji.sheepplayer2.METADATA_UPDATE"
        const val BROADCAST_LOADING_COMPLETE = "com.hitsuji.sheepplayer2.LOADING_COMPLETE"
        const val BROADCAST_LOADING_ERROR = "com.hitsuji.sheepplayer2.LOADING_ERROR"
        
        /**
         * Starts the metadata loading service
         * @param context Application context
         */
        fun startService(context: Context)
    }
}
```

### `MetadataCache`

Caches Google Drive file metadata for improved performance.

```kotlin
class MetadataCache(context: Context) {
    
    /**
     * Saves metadata to cache
     * @param metadata Cached metadata to save
     */
    fun saveMetadata(metadata: CachedMetadata)
    
    /**
     * Gets cached metadata by file ID
     * @param fileId Google Drive file ID
     * @return Cached metadata or null if not found
     */
    fun getMetadata(fileId: String): CachedMetadata?
    
    /**
     * Clears all cached metadata
     */
    fun clearCache()
}
```

## 📁 Repository Layer

### `MusicRepository`

Handles data access from Android MediaStore with security validations.

```kotlin
class MusicRepository(private val context: Context) {
    
    /**
     * Loads all music data from MediaStore
     * @return List of artists with albums and tracks
     */
    suspend fun loadMusicData(): List<Artist>
    
    /**
     * Validates if a file is a supported audio format
     * @param filePath Path to the audio file
     * @return true if valid audio file
     */
    private fun isValidAudioFile(filePath: String): Boolean
}
```

#### Security Features

- **Input Validation**: Sanitizes file paths and validates extensions
- **Supported Formats**: `.mp3`, `.m4a`, `.wav`, `.flac`, `.ogg`, `.aac`
- **Path Traversal Protection**: Prevents `../` directory traversal attacks

**MediaStore Query Process:**

1. Query Android MediaStore for audio files
2. Validate file paths and extensions
3. Organize data into hierarchical structure
4. Return sanitized, structured data

## 🎨 UI Components

### `TreeAdapter`

RecyclerView adapter for hierarchical music browsing.

```kotlin
class TreeAdapter(
    private val onTrackClick: (Track) -> Unit = {},
    private val onTrackSwipe: (Track) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    /**
     * Updates the adapter with new tree items
     */
    fun submitList(newItems: List<TreeItem>)
    
    /**
     * Gets item at specific position
     */
    fun getItem(position: Int): TreeItem?
}
```

#### ViewHolder Classes

- **`ArtistViewHolder`**: Displays artist information and album count
- **`AlbumViewHolder`**: Shows album details with track count and artwork
- **`TrackViewHolder`**: Presents individual track information

### `TreeItem` (Sealed Class)

```kotlin
sealed class TreeItem {
    data class ArtistItem(val artist: Artist, var isExpanded: Boolean) : TreeItem()
    data class AlbumItem(val album: Album, var isExpanded: Boolean) : TreeItem()
    data class TrackItem(val track: Track) : TreeItem()
}
```

## 🛠️ Utility Classes

### `TimeUtils`

Time formatting utilities for duration display.

```kotlin
object TimeUtils {
    /**
     * Formats duration from milliseconds to MM:SS format
     * @param durationMs Duration in milliseconds
     * @return Formatted time string (e.g., "3:45")
     */
    fun formatDuration(durationMs: Long): String
}
```

**Example:**

```kotlin
val duration = TimeUtils.formatDuration(225000L) // Returns "3:45"
```

### `Constants`

Application-wide constants for consistency and maintainability.

```kotlin
object Constants {
    object ViewTypes {
        const val ARTIST = 0
        const val ALBUM = 1
        const val TRACK = 2
    }
    
    object MediaStore {
        const val UNKNOWN_TITLE = "Unknown Title"
        const val UNKNOWN_ARTIST = "Unknown Artist"
        const val UNKNOWN_ALBUM = "Unknown Album"
        const val ALBUM_ART_URI_BASE = "content://media/external/audio/albumart"
    }
    
    object UI {
        const val PLAY_BUTTON_DESCRIPTION = "Play"
        const val STOP_BUTTON_DESCRIPTION = "Stop"
    }
}
```

## 🔧 MainActivity API

### Key Methods

```kotlin
class MainActivity : AppCompatActivity() {
    
    val allArtists: MutableList<Artist>     // Main music collection (local + Google Drive)
    val currentPlayingTrack: Track?         // Currently playing track
    val isPlaying: Boolean                  // Playback state
    val currentPosition: Int                // Current playback position
    val duration: Int                       // Current track duration
    val currentPlayingAlbum: Album?         // Album being played (for sequential playback)
    val currentAlbumTracks: List<Track>     // Tracks in current album
    val currentTrackIndexInAlbum: Int       // Current track index in album
    
    /**
     * Plays a specific track (clears album context)
     */
    fun playTrack(track: Track)
    
    /**
     * Plays an album from first track (sets album context)
     */
    fun playAlbum(album: Album)
    
    /**
     * Plays specific track within album context
     */
    fun playTrackInAlbum(track: Track, trackIndex: Int)
    
    /**
     * Toggles between play and pause
     */
    fun togglePlayback(): Boolean
    
    /**
     * Stops current playback
     */
    fun stopPlayback()
    
    /**
     * Switches to the playing tab
     */
    fun switchToPlayingTab()
    
    /**
     * Syncs playback state with media player
     */
    fun syncPlaybackState()
    
    /**
     * Loads music from Google Drive in background
     */
    fun refreshGoogleDriveMusic()
    
    /**
     * Gets Google Drive service instance
     */
    fun getGoogleDriveService(): GoogleDriveService
}
```

## 🔐 Security APIs

### File Validation

```kotlin
// In MusicPlayer.kt
private fun isValidTrackFile(filePath: String): Boolean {
    // Validates file existence, readability, and security
}

// In MusicRepository.kt
private fun isValidAudioFile(filePath: String): Boolean {
    // Validates file extension and prevents path traversal
}
```

### App Integrity Check

```kotlin
// In MainActivity.kt
private fun verifyAppIntegrity() {
    // Performs basic integrity checks for security
}
```

## 📱 Fragment APIs

### `PlayingFragment`

```kotlin
class PlayingFragment : Fragment() {
    /**
     * Called when playback state changes
     */
    fun onPlaybackStateChanged()
    
    /**
     * Updates the UI with current track information
     */
    private fun updateTrackDisplay()
    
    /**
     * Loads album art securely
     */
    private fun loadAlbumArt(albumArtUri: String?, imageView: ImageView)
}
```

### `TracksFragment`

```kotlin
class TracksFragment : Fragment() {
    /**
     * Called when music data is loaded
     */
    fun onMusicDataLoaded()
    
    /**
     * Handles track selection
     */
    private fun onTrackSelected(track: Track)
    
    /**
     * Handles track swipe gestures
     */
    private fun onTrackSwiped(track: Track)
}
```

### `PicturesFragment`

```kotlin
class PicturesFragment : Fragment() {
    /**
     * Sets up RecyclerView for image display
     */
    private fun setupRecyclerView()
    
    /**
     * Sets up LiveData observers for image updates
     */
    private fun setupObservers()
    
    /**
     * Checks if there's a currently playing track and loads images
     */
    private fun checkForPlayingTrack()
}
```

### `ArtistImageAdapter`

RecyclerView adapter for displaying artist images with animation support.

```kotlin
class ArtistImageAdapter : RecyclerView.Adapter<ArtistImageAdapter.ImageViewHolder>() {
    
    /**
     * Adds a new image bitmap to the list
     */
    fun addImage(image: Bitmap)
    
    /**
     * Adds animated GIF placeholder at bottom of list
     */
    fun addAnimatedPlaceholderAtBottom()
    
    /**
     * Replaces placeholder with downloaded image
     */
    fun replacePlaceholderWithImage(realImage: Bitmap)
    
    /**
     * Clears all images from the adapter
     */
    fun clearImages()
}
```

#### ViewHolder Classes

- **`ImageViewHolder`**:
    - `bind(bitmap: Bitmap)`: Binds static bitmap to ImageView
    - `bindAnimatedPlaceholder()`: Uses Glide to load animated GIF
    - `clearImage()`: Clears ImageView and Glide loading

## 🎯 Best Practices

### Error Handling

- Always check return values from player methods
- Use try-catch blocks for MediaStore operations
- Log errors with appropriate severity levels

### Security

- Validate all file paths before use
- Use constants instead of magic strings
- Implement proper null checks

### Performance

- Use coroutines for background operations
- Implement proper lifecycle management
- Release resources in onDestroy methods

## 🔄 Lifecycle Management

```kotlin
// Proper resource management
override fun onDestroy() {
    super.onDestroy()
    musicPlayerManager.release()
}

// Memory leak prevention
override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
}
```