# Testing Guide 🧪

This document outlines the testing strategy, guidelines, and implementation details for the
SheepPlayer Android application.

## 📋 Testing Strategy

SheepPlayer follows a comprehensive testing approach with multiple layers:

```
┌─────────────────────┐
│    UI Tests (E2E)   │ ← Espresso tests for user workflows
├─────────────────────┤
│  Integration Tests  │ ← Component interaction testing
├─────────────────────┤
│    Unit Tests       │ ← Logic and utility testing
└─────────────────────┘
```

## 🎯 Testing Objectives

- **Functionality**: Verify core music player operations
- **Security**: Validate file path sanitization and input validation
- **UI/UX**: Ensure proper user interface behavior
- **Performance**: Test with large music libraries
- **Reliability**: Handle edge cases and error conditions

## 🔬 Unit Testing

### Current Test Structure

```
app/src/test/java/com/hitsuji/sheepplayer2/
└── ExampleUnitTest.kt
```

### Recommended Unit Tests

#### `TimeUtils` Tests

```kotlin
@Test
fun testFormatDuration() {
    // Test time formatting utility
    assertEquals("0:00", TimeUtils.formatDuration(0))
    assertEquals("0:30", TimeUtils.formatDuration(30000))
    assertEquals("1:30", TimeUtils.formatDuration(90000))
    assertEquals("10:05", TimeUtils.formatDuration(605000))
}

@Test
fun testFormatDurationEdgeCases() {
    // Test negative values
    assertEquals("0:00", TimeUtils.formatDuration(-1000))
    
    // Test very large values
    val largeValue = Long.MAX_VALUE
    assertNotNull(TimeUtils.formatDuration(largeValue))
}
```

#### `MusicRepository` Security Tests

```kotlin
class MusicRepositoryTest {
    
    @Test
    fun testValidAudioFileExtensions() {
        val repository = MusicRepository(mockContext)
        
        assertTrue(repository.isValidAudioFile("/path/song.mp3"))
        assertTrue(repository.isValidAudioFile("/path/song.flac"))
        assertFalse(repository.isValidAudioFile("/path/document.txt"))
        assertFalse(repository.isValidAudioFile("/path/image.jpg"))
    }
    
    @Test
    fun testPathTraversalPrevention() {
        val repository = MusicRepository(mockContext)
        
        // Should reject path traversal attempts
        assertFalse(repository.isValidAudioFile("../../../etc/passwd"))
        assertFalse(repository.isValidAudioFile("/music/../../../sensitive.mp3"))
        assertFalse(repository.isValidAudioFile("music\\..\\..\\file.mp3"))
    }
    
    @Test
    fun testNullAndEmptyPaths() {
        val repository = MusicRepository(mockContext)
        
        assertFalse(repository.isValidAudioFile(""))
        assertFalse(repository.isValidAudioFile("   "))
        // Note: null check handled at call site
    }
}
```

#### `MusicPlayer` Tests

```kotlin
class MusicPlayerTest {
    
    private lateinit var mockContext: Context
    private lateinit var musicPlayer: MusicPlayer
    
    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        musicPlayer = MusicPlayer(mockContext)
    }
    
    @Test
    fun testValidFilePathAcceptance() {
        val validTrack = Track(
            id = 1L,
            title = "Test Song",
            artistName = "Test Artist",
            albumName = "Test Album",
            duration = 180000L,
            filePath = "/storage/music/song.mp3"
        )
        
        // Mock file system validation
        mockkStatic(File::class)
        val mockFile = mockk<File>()
        every { File(any<String>()) } returns mockFile
        every { mockFile.exists() } returns true
        every { mockFile.canRead() } returns true
        every { mockFile.isFile } returns true
        
        // Should accept valid file
        assertTrue(musicPlayer.loadTrack(validTrack))
        
        unmockkStatic(File::class)
    }
    
    @Test
    fun testInvalidFilePathRejection() {
        val invalidTrack = Track(
            id = 1L,
            title = "Test Song",
            artistName = "Test Artist", 
            albumName = "Test Album",
            duration = 180000L,
            filePath = "../../../malicious.mp3"
        )
        
        // Should reject path traversal
        assertFalse(musicPlayer.loadTrack(invalidTrack))
    }
    
    @After
    fun tearDown() {
        musicPlayer.release()
    }
}
```

## 🎭 Integration Testing

### Fragment Integration Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class TracksFragmentTest {
    
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun testTrackListDisplay() {
        // Test that tracks are displayed correctly
        onView(withId(R.id.recyclerViewTracks))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testSwipeToPlay() {
        // Test swipe gesture functionality
        onView(withId(R.id.recyclerViewTracks))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeRight()))
            
        // Verify navigation to playing tab
        onView(withId(R.id.navigation_playing))
            .check(matches(isSelected()))
    }
}
```

## 🤖 UI Testing (Espresso)

### Current Test Structure

```
app/src/androidTest/java/com/hitsuji/sheepplayer2/
└── ExampleInstrumentedTest.kt
```

### Recommended UI Tests

#### Permission Flow Test

```kotlin
@RunWith(AndroidJUnit4::class)
class PermissionFlowTest {
    
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun testPermissionRequestFlow() {
        // Test permission request and handling
        
        // If permission not granted, should show permission dialog
        // If permission granted, should load music library
        
        onView(withId(R.id.recyclerViewTracks))
            .check(matches(isDisplayed()))
    }
}
```

#### Music Playback UI Test

```kotlin
@RunWith(AndroidJUnit4::class)
class PlaybackUITest {
    
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun testPlayButtonToggle() {
        // Navigate to playing fragment
        onView(withId(R.id.navigation_playing)).perform(click())
        
        // If track is loaded, test play/pause toggle
        onView(withId(R.id.playStopButton))
            .perform(click())
            
        // Verify button state changes
        onView(withId(R.id.playStopButton))
            .check(matches(hasContentDescription("Stop")))
    }
    
    @Test
    fun testNoTrackMessage() {
        // Navigate to playing fragment when no track is loaded
        onView(withId(R.id.navigation_playing)).perform(click())
        
        // Should show "no track" message
        onView(withId(R.id.noTrackMessage))
            .check(matches(isDisplayed()))
    }
}
```

#### Navigation Test

```kotlin
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun testBottomNavigationTabs() {
        // Test tracks tab
        onView(withId(R.id.navigation_tracks)).perform(click())
        onView(withId(R.id.recyclerViewTracks)).check(matches(isDisplayed()))
        
        // Test playing tab
        onView(withId(R.id.navigation_playing)).perform(click())
        // Should show playing fragment content
        
        // Test pictures tab
        onView(withId(R.id.navigation_pictures)).perform(click())
        // Should show pictures fragment content
    }
}
```

## 📊 Test Data Management

### Mock Data Creation

```kotlin
object TestDataFactory {
    
    fun createTestTrack(
        id: Long = 1L,
        title: String = "Test Song",
        artist: String = "Test Artist",
        album: String = "Test Album",
        duration: Long = 180000L,
        filePath: String = "/test/path/song.mp3"
    ) = Track(id, title, artist, album, duration, filePath)
    
    fun createTestAlbum(
        id: Long = 1L,
        title: String = "Test Album",
        artistName: String = "Test Artist",
        trackCount: Int = 3
    ) = Album(id, title, artistName).apply {
        repeat(trackCount) { index ->
            tracks.add(createTestTrack(
                id = index.toLong(),
                title = "Track ${index + 1}",
                artist = artistName,
                album = title
            ))
        }
    }
    
    fun createTestArtist(
        id: Long = 1L,
        name: String = "Test Artist",
        albumCount: Int = 2
    ) = Artist(id, name).apply {
        repeat(albumCount) { index ->
            albums.add(createTestAlbum(
                id = index.toLong(),
                title = "Album ${index + 1}",
                artistName = name
            ))
        }
    }
}
```

## 🔐 Security Testing

### File Security Tests

```kotlin
class FileSecurityTest {
    
    @Test
    fun testMaliciousFilePathPrevention() {
        val maliciousPaths = listOf(
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            "/data/data/other.app/sensitive.file",
            "content://malicious.provider/data",
            "file:///proc/version"
        )
        
        maliciousPaths.forEach { path ->
            assertFalse(
                "Should reject malicious path: $path",
                isValidAudioFile(path)
            )
        }
    }
    
    @Test
    fun testValidAudioPaths() {
        val validPaths = listOf(
            "/storage/emulated/0/Music/song.mp3",
            "/storage/sdcard1/Music/album/track.flac",
            "/data/media/0/Music/artist/song.m4a"
        )
        
        validPaths.forEach { path ->
            // Note: This would require file system mocking
            // The path structure should be considered valid
            assertTrue(
                "Should accept valid audio path: $path",
                hasValidAudioExtension(path)
            )
        }
    }
}
```

## 🎯 Performance Testing

### Large Dataset Tests

```kotlin
class PerformanceTest {
    
    @Test
    fun testLargeMusicLibraryHandling() {
        // Test with large number of tracks (1000+)
        val largeLibrary = generateLargeMusicLibrary(1000)
        
        val startTime = System.currentTimeMillis()
        val processedData = processAndStructureMusicData(largeLibrary)
        val endTime = System.currentTimeMillis()
        
        // Should process within reasonable time (e.g., < 5 seconds)
        assertTrue(
            "Processing should complete within 5 seconds",
            (endTime - startTime) < 5000
        )
        
        assertNotNull(processedData)
        assertTrue(processedData.isNotEmpty())
    }
    
    @Test
    fun testMemoryUsageWithLargeLibrary() {
        // Test memory consumption with large datasets
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val largeLibrary = generateLargeMusicLibrary(5000)
        processAndStructureMusicData(largeLibrary)
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Memory increase should be reasonable (e.g., < 100MB)
        assertTrue(
            "Memory increase should be reasonable",
            memoryIncrease < 100 * 1024 * 1024 // 100MB
        )
    }
}
```

## 🛠️ Testing Tools & Frameworks

### Dependencies Required

```kotlin
// Unit Testing
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:4.6.1'
testImplementation 'org.mockito.kotlin:mockito-kotlin:4.0.0'
testImplementation 'org.robolectric:robolectric:4.9'

// Android Testing
androidTestImplementation 'androidx.test.ext:junit:1.1.5'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
androidTestImplementation 'androidx.test.espresso:espresso-contrib:3.5.1'
androidTestImplementation 'androidx.test:runner:1.5.2'
androidTestImplementation 'androidx.test:rules:1.5.0'
```

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "TimeUtilsTest"

# Run all instrumented tests
./gradlew connectedAndroidTest

# Run specific instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.hitsuji.sheepplayer.NavigationTest

# Generate test coverage report
./gradlew jacocoTestReport
```

### CI/CD Integration

```yaml
# Example GitHub Actions workflow
name: Android CI

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
        restore-keys: ${{ runner.os }}-gradle
        
    - name: Run unit tests
      run: ./gradlew test
      
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: test-results
        path: app/build/reports/tests/
```

## 📈 Test Coverage Goals

- **Unit Tests**: 80%+ code coverage
- **Integration Tests**: Cover major user flows
- **UI Tests**: Test critical user interactions
- **Security Tests**: Cover all input validation paths
- **Performance Tests**: Validate with realistic data sizes

## 🎯 Testing Best Practices

1. **Arrange-Act-Assert Pattern**: Structure tests clearly
2. **Independent Tests**: Each test should run independently
3. **Descriptive Names**: Test names should describe expected behavior
4. **Mock External Dependencies**: Use mocks for MediaStore, file system
5. **Test Edge Cases**: Include boundary conditions and error scenarios
6. **Continuous Integration**: Automate test execution on code changes

## 🔍 Test Review Checklist

- [ ] All public methods are tested
- [ ] Security validations are covered
- [ ] Error conditions are handled
- [ ] Performance requirements are met
- [ ] UI interactions work correctly
- [ ] Tests run independently
- [ ] Test data is properly cleaned up
- [ ] Documentation is updated