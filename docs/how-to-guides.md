# How-to Guides

Step-by-step instructions for solving specific problems with SheepPlayer development.

**Prerequisites:** Basic Android development knowledge and SheepPlayer codebase familiarity.

## Development

### Add Support for New Audio Formats

**Problem:** You want to support audio formats not currently recognized by SheepPlayer.

1. **Update the validation method** in `MusicRepository.kt`:

```kotlin
private fun isValidAudioFile(filePath: String): Boolean {
    return try {
        val validExtensions = setOf(
            ".mp3", ".m4a", ".wav", ".flac", ".ogg", ".aac", 
            ".opus", ".wma"  // Add new formats here
        )
        val extension = filePath.substringAfterLast(".", "").lowercase()
        validExtensions.contains(".$extension") && !filePath.contains("../")
    } catch (e: Exception) {
        false
    }
}
```

2. **Test the new format** by adding a file with the new extension to your test device.

3. Update documentation to reflect the newly supported formats.

### Implement Dark Mode Theme

**Problem:** Users want a dark theme option for better nighttime usage.

1. **Create theme variants** in `res/values/themes.xml`:

```xml
<style name="Theme.SheepPlayer.Dark" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">@color/purple_200</item>
    <item name="colorPrimaryVariant">@color/purple_700</item>
    <item name="colorOnPrimary">@color/black</item>
    <!-- Add more color attributes -->
</style>
```

2. **Add theme switching logic** in MainActivity:

```kotlin
private fun applyTheme(isDarkMode: Boolean) {
    val themeId = if (isDarkMode) R.style.Theme_SheepPlayer_Dark 
                 else R.style.Theme_SheepPlayer
    setTheme(themeId)
}
```

3. **Save theme preference** using SharedPreferences or DataStore.

4. Update all fragments to respond to theme changes.

### Add Playlist Management

**Problem:** Users want to create and organize custom playlists.

1. **Create playlist data model**:

```kotlin
data class Playlist(
    val id: Long,
    val name: String,
    val tracks: MutableList<Track> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)
```

2. **Add playlist storage** using Room database or SharedPreferences.

3. **Create playlist management UI** with RecyclerView for playlist list.

4. Implement add/remove track functionality in existing track displays.

### Integrate System Media Controls

**Problem:** Your app needs to work with Android's media control notifications and lock screen
controls.

1. **Add MediaSession dependency** to `build.gradle.kts`:

```kotlin
implementation("androidx.media:media:1.6.0")
```

2. **Create MediaSession** in MusicPlayerManager:

```kotlin
private lateinit var mediaSession: MediaSessionCompat

private fun initializeMediaSession() {
    mediaSession = MediaSessionCompat(context, "SheepPlayerSession")
    mediaSession.setCallback(object : MediaSessionCompat.Callback() {
        override fun onPlay() { /* Handle play */ }
        override fun onPause() { /* Handle pause */ }
        // Add other callbacks
    })
}
```

3. **Update playback metadata** when tracks change:

```kotlin
private fun updateMetadata(track: Track) {
    val metadata = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artistName)
        .build()
    mediaSession.setMetadata(metadata)
}
```

## Security

### Implement File Access Security

**Problem:** You need to validate all file access to prevent security vulnerabilities.

1. **Create security validator class**:

```kotlin
object SecurityValidator {
    fun isSecureFilePath(filePath: String): Boolean {
        return !filePath.contains("..") && 
               filePath.startsWith("/storage/") &&
               isValidAudioExtension(filePath)
    }
    
    private fun isValidAudioExtension(path: String): Boolean {
        // Implementation here
    }
}
```

2. **Apply validation** in all file access points:

```kotlin
fun loadTrack(track: Track, autoPlay: Boolean = false): Boolean {
    if (!SecurityValidator.isSecureFilePath(track.filePath)) {
        Log.w("Security", "Rejected unsafe file path: ${track.filePath}")
        return false
    }
    // Continue with loading
}
```

3. Add logging for security events for monitoring.

### Enable Code Obfuscation

**Problem:** You need to protect your code and reduce APK size for production.

1. **Update build configuration** in `app/build.gradle.kts`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

2. **Configure ProGuard rules** in `proguard-rules.pro`:

```
-keep class com.hitsuji.sheepplayer.** { *; }
-keepclassmembers class * {
    public <init>(...);
}
-dontwarn com.hitsuji.sheepplayer.**
```

3. Test thoroughly with release builds to ensure functionality isn't broken.

## Testing

### Add Unit Tests for New Features

**Problem:** You've added a feature and need comprehensive tests.

1. **Create test class** in `app/src/test/java`:

```kotlin
class NewFeatureTest {
    
    @Test
    fun `should return expected result when given valid input`() {
        // Arrange
        val input = createTestInput()
        val feature = NewFeature()
        
        // Act
        val result = feature.processInput(input)
        
        // Assert
        assertEquals(expectedOutput, result)
    }
}
```

2. **Add mock dependencies** using Mockito:

```kotlin
@Mock
private lateinit var mockRepository: MusicRepository

@Before
fun setUp() {
    MockitoAnnotations.openMocks(this)
}
```

3. **Test edge cases** including null values, empty collections, and error conditions.

4. Run tests with `./gradlew test` to verify they pass.

### Add Device UI Tests

**Problem:** You need to test UI interactions on actual devices or emulators.

1. **Create test class** in `app/src/androidTest/java`:

```kotlin
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun testPermissionFlow() {
        // Test permission request and handling
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }
}
```

2. **Use Espresso** for UI interactions:

```kotlin
onView(withId(R.id.play_button))
    .perform(click())
    .check(matches(hasContentDescription("Stop")))
```

3. Run instrumented tests with `./gradlew connectedAndroidTest`.

## User Interface

### Add List Animations

**Problem:** You want smooth animations when users expand/collapse artists and albums.

1. **Create animator resources** in `res/animator/`:

```xml
<!-- expand_animation.xml -->
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <objectAnimator
        android:propertyName="rotation"
        android:duration="300"
        android:valueFrom="0"
        android:valueTo="90" />
</set>
```

2. **Apply animations** in TreeAdapter:

```kotlin
private fun animateExpansion(view: View, isExpanding: Boolean) {
    val animator = if (isExpanding) {
        AnimatorInflater.loadAnimator(context, R.animator.expand_animation)
    } else {
        AnimatorInflater.loadAnimator(context, R.animator.collapse_animation)
    }
    animator.setTarget(view)
    animator.start()
}
```

3. Call animations during expand/collapse operations.

### Implement Music Search

**Problem:** Users need to quickly find specific songs, artists, or albums.

1. **Add SearchView** to the toolbar in your fragments:

```xml
<androidx.appcompat.widget.SearchView
    android:id="@+id/search_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:queryHint="Search music..." />
```

2. **Implement filtering logic**:

```kotlin
fun filterMusic(query: String): List<Artist> {
    if (query.isBlank()) return originalArtists
    
    return originalArtists.mapNotNull { artist ->
        val matchingAlbums = artist.albums.mapNotNull { album ->
            val matchingTracks = album.tracks.filter { track ->
                track.title.contains(query, ignoreCase = true) ||
                track.artistName.contains(query, ignoreCase = true) ||
                track.albumName.contains(query, ignoreCase = true)
            }
            
            if (matchingTracks.isNotEmpty()) {
                album.copy(tracks = matchingTracks.toMutableList())
            } else null
        }
        
        if (matchingAlbums.isNotEmpty()) {
            artist.copy(albums = matchingAlbums.toMutableList())
        } else null
    }
}
```

3. Update RecyclerView with filtered results as user types.

## Performance

### Handle Large Music Collections

**Problem:** Your app becomes slow with users who have thousands of music files.

1. **Implement pagination** in MusicRepository:

```kotlin
fun loadMusicDataPaginated(page: Int, pageSize: Int = 100): List<Artist> {
    val offset = page * pageSize
    // Modify MediaStore query to include LIMIT and OFFSET
}
```

2. **Add lazy loading** to RecyclerView:

```kotlin
recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (!recyclerView.canScrollVertically(1)) {
            loadMoreItems()
        }
    }
})
```

3. **Cache processed data** to avoid repeated MediaStore queries.

4. Use DiffUtil for efficient RecyclerView updates:

```kotlin
class MusicDiffCallback : DiffUtil.ItemCallback<TreeItem>() {
    override fun areItemsTheSame(oldItem: TreeItem, newItem: TreeItem): Boolean {
        // Compare IDs
    }
    
    override fun areContentsTheSame(oldItem: TreeItem, newItem: TreeItem): Boolean {
        // Compare contents
    }
}
```

### Reduce Memory Usage

**Problem:** Your app uses too much memory and crashes on low-end devices.

1. **Implement image loading optimization**:

```kotlin
fun loadAlbumArt(uri: String, imageView: ImageView) {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeStream(contentResolver.openInputStream(Uri.parse(uri)), null, options)
    
    options.inSampleSize = calculateInSampleSize(options, 200, 200)
    options.inJustDecodeBounds = false
    
    val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(Uri.parse(uri)), null, options)
    imageView.setImageBitmap(bitmap)
}
```

2. **Use ViewHolder pattern properly** and avoid memory leaks:

```kotlin
override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
    super.onViewRecycled(holder)
    // Clear image references
    (holder as? TrackViewHolder)?.clearImage()
}
```

3. Profile memory usage with Android Studio Memory Profiler.

## Deployment

### Create Production Release

**Problem:** You need to build and distribute your app through the Play Store.

1. **Update version information** in `build.gradle.kts`:

```kotlin
versionCode = 2
versionName = "1.1.0"
```

2. **Generate signed bundle**:
    - Build → Generate Signed Bundle/APK
    - Choose "Android App Bundle"
    - Create or select keystore
    - Build release variant

3. **Test thoroughly** on multiple devices and Android versions.

4. **Verify ProGuard** hasn't broken any functionality.

5. Upload to Play Console using the generated AAB file.

### Set Up Automated Building

**Problem:** You want to automatically test and build your app when code changes.

1. **Create workflow file** `.github/workflows/ci.yml`:

```yaml
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: ~/.gradle/caches
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle') }}
        
    - name: Run tests
      run: ./gradlew test
      
    - name: Upload test results
      uses: actions/upload-artifact@v3
      with:
        name: test-results
        path: app/build/reports/tests/
```

2. Commit and push the workflow file to trigger the first build.

3. Monitor build results in the Actions tab of your GitHub repository.

---
