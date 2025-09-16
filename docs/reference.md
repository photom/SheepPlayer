# Reference

Complete technical specification of SheepPlayer classes, methods, and constants.

- **[SOLID Compliance](SOLID_COMPLIANCE.md)** - SOLID principles adherence analysis

## Classes

### Data Models

#### `Artist`

```kotlin
data class Artist(
    val id: Long,
    val name: String,
    val albums: MutableList<Album> = mutableListOf()
)
```

**Properties:**

- `id: Long` - MediaStore ID for the artist
- `name: String` - Artist name
- `albums: MutableList<Album>` - Collection of albums by this artist

#### `Album`

```kotlin
data class Album(
    val id: Long,
    val title: String,
    val artistName: String,
    val tracks: MutableList<Track> = mutableListOf()
)
```

**Properties:**

- `id: Long` - MediaStore ID for the album
- `title: String` - Album title
- `artistName: String` - Name of the artist who created this album
- `tracks: MutableList<Track>` - Collection of tracks in this album

#### `Track`

```kotlin
data class Track(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val duration: Long,
    val filePath: String,
    val albumArtUri: String? = null
)
```

**Properties:**

- `id: Long` - MediaStore ID for the track
- `title: String` - Track title
- `artistName: String` - Name of the performing artist
- `albumName: String` - Name of the album containing this track
- `duration: Long` - Track duration in milliseconds
- `filePath: String` - Full file system path to the audio file
- `albumArtUri: String?` - Optional URI for album artwork

### Core Classes

#### `MusicPlayer`

Audio playback controller using Android MediaPlayer.

**Constructor:**

- `MusicPlayer(context: Context)`

**Methods:**

##### `loadTrack(track: Track, autoPlay: Boolean = false): Boolean`

Loads track for playback with validation.

- **Parameters:** `track` - Track to load, `autoPlay` - Start playing immediately
- **Returns:** `true` if successful
- **Exceptions:** Handled internally

##### `play(): Boolean`

Starts playback of loaded track.

- **Returns:** `true` if successful

##### `pause(): Boolean`

Pauses current playback.

- **Returns:** `true` if successful

##### `stop(): Boolean`

Stops playback and releases resources.

- **Returns:** `true` if successful

##### `isPlaying(): Boolean`

Checks current playback state.

- **Returns:** `true` if currently playing

##### `getCurrentTrack(): Track?`

Gets the currently loaded track.

- **Returns:** Current track or `null`

##### `getCurrentPosition(): Int`

Gets current playback position.

- **Returns:** Position in milliseconds

##### `getDuration(): Int`

Gets duration of current track.

- **Returns:** Duration in milliseconds

##### `seekTo(position: Int)`

Seeks to specific position.

- **Parameters:** `position` - Target position in milliseconds

##### `release()`

Releases all resources.

**Interfaces:**

##### `OnPlaybackStateChangeListener`

Callback interface for playback state changes.

**Methods:**

- `onPlaybackStarted(track: Track)`
- `onPlaybackPaused(track: Track)`
- `onPlaybackStopped()`
- `onPlaybackError(track: Track, error: String)`
- `onPlaybackCompleted(track: Track)`

#### `MusicPlayerManager`

MusicPlayer wrapper with state management.

**Constructor:**

- `MusicPlayerManager(musicPlayer: MusicPlayer)`

**Properties:**

- `currentPlayingTrack: Track?` - Currently playing track (read-only)
- `isPlaying: Boolean` - Current playback state (read-only)

**Methods:**

##### `playTrack(track: Track)`

Starts playing specified track.

##### `togglePlayback(): Boolean`

Toggles between play and pause.

- **Returns:** `true` if action succeeded

##### `syncPlaybackState()`

Synchronizes internal state with MediaPlayer.

##### `setOnPlaybackStateChangeListener(listener: () -> Unit)`

Sets callback for state changes.

##### `release()`

Releases resources.

#### `MusicRepository`

Music data access from MediaStore.

**Constructor:**

- `MusicRepository(context: Context)`

**Methods:**

##### `suspend fun loadMusicData(): List<Artist>`

Loads music data from device.

- **Returns:** List of artists with albums and tracks
- **Exceptions:** MediaStore access errors

### UI Classes

#### `TreeAdapter`

RecyclerView adapter for hierarchical music display.

**Constructor:**

- `TreeAdapter(onTrackClick: (Track) -> Unit, onTrackSwipe: (Track) -> Unit)`

**Methods:**

##### `submitList(newItems: List<TreeItem>)`

Updates adapter with new data.

##### `getItem(position: Int): TreeItem?`

Gets item at position.

- **Returns:** TreeItem or `null` if invalid position

**ViewHolder Classes:**

- `ArtistViewHolder` - Displays artist information
- `AlbumViewHolder` - Displays album information
- `TrackViewHolder` - Displays track information

#### `TreeItem` (Sealed Class)

Represents different types of items in the music hierarchy.

**Subclasses:**

- `TreeItem.ArtistItem(artist: Artist, isExpanded: Boolean)`
- `TreeItem.AlbumItem(album: Album, isExpanded: Boolean)`
- `TreeItem.TrackItem(track: Track)`

### Fragment Classes

#### `MainActivity`

Main application activity.

**Properties:**

- `allArtists: MutableList<Artist>` - Music collection
- `currentPlayingTrack: Track?` - Current track
- `isPlaying: Boolean` - Playback state

**Methods:**

##### `playTrack(track: Track)`

Starts playing track, switches to playing tab.

##### `togglePlayback(): Boolean`

Toggles play/pause state.

##### `stopPlayback()`

Stops playback.

##### `switchToPlayingTab()`

Navigates to playing tab.

##### `syncPlaybackState()`

Syncs state with player.

#### `PlayingFragment`

Fragment displaying currently playing track.

**Methods:**

##### `onPlaybackStateChanged()`

Called when playback state changes.

#### `TracksFragment`

Fragment displaying music library in tree view.

**Methods:**

##### `onMusicDataLoaded()`

Called when music data loading completes.

### Utility Classes

#### `TimeUtils`

Time formatting utilities.

**Methods:**

##### `formatDuration(durationMs: Long): String`

Formats milliseconds to MM:SS or H:MM:SS format.

- **Parameters:** `durationMs` - Duration in milliseconds
- **Returns:** Formatted string (e.g., "3:45" or "1:23:45" for tracks ≥ 1 hour)

#### `Constants`

Application constants.

**Objects:**

- `ViewTypes` - RecyclerView view type constants
- `MediaStore` - MediaStore-related constants
- `UI` - UI string constants

## Permissions

### Required Permissions

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

## MediaStore Queries

### Audio Files Query

```kotlin
val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
} else {
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
}

val projection = arrayOf(
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.TITLE,
    MediaStore.Audio.Media.ARTIST,
    MediaStore.Audio.Media.ALBUM,
    MediaStore.Audio.Media.DURATION,
    MediaStore.Audio.Media.DATA,
    MediaStore.Audio.Media.ALBUM_ID
)

val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
```

## Supported Audio Formats

- MP3 (`.mp3`)
- M4A (`.m4a`)
- WAV (`.wav`)
- FLAC (`.flac`)
- OGG (`.ogg`)
- AAC (`.aac`)

## Security Validations

### File Path Validation

File paths are validated to prevent:

- Directory traversal attacks (`../`)
- Access to system files
- Invalid file extensions

### Input Sanitization

All MediaStore data is sanitized:

- Null values replaced with defaults
- File paths validated before use
- Extensions checked against whitelist

## Error Codes

### MusicPlayer Errors

- `"Invalid file path"` - File path failed security validation
- `"Failed to load track"` - IOException during file access
- `"Unexpected error"` - General exception during loading

### Permission Errors

- `PERMISSION_DENIED` - User denied media access permission
- `PERMISSION_GRANTED` - User granted media access permission

## View IDs

### Activity Main

- `@+id/nav_view` - Bottom navigation
- `@+id/nav_host_fragment_activity_main` - Navigation host

### Fragment Playing

- `@+id/albumArtLarge` - Large album artwork
- `@+id/trackTitle` - Track title text
- `@+id/artistName` - Artist name text
- `@+id/albumName` - Album name text
- `@+id/duration` - Duration text
- `@+id/playStopButton` - Play/stop button
- `@+id/noTrackMessage` - No track loaded message

### Fragment Tracks

- `@+id/recyclerViewTracks` - Music library RecyclerView

### Item Layouts

- `@+id/artistName` - Artist name (item_artist)
- `@+id/albumCount` - Album count (item_artist)
- `@+id/expandIcon` - Expand/collapse icon
- `@+id/albumTitle` - Album title (item_album)
- `@+id/trackCount` - Track count (item_album)
- `@+id/albumArt` - Album artwork (item_album)
- `@+id/trackTitle` - Track title (item_track_tree)
- `@+id/duration` - Track duration (item_track_tree)