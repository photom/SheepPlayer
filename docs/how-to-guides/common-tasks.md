# How-to Guides (Android Music Player)

This document provides step-by-step instructions for common tasks when developing or maintaining SheepPlayer, an Android music player built on **DDD** and **Clean Architecture**.

## 🛠️ Development Guides

### 1. Add Support for a New Audio Format

**Problem**: You want the music library to recognize a new format (e.g., `.opus`).

1.  **Analyze the Domain**: Update the `Track` entity to recognize the new format (if needed).
2.  **Modify the Data Layer**: In `MusicRepositoryImpl` or the `LocalMediaDataSource`, update the extension whitelist to include `.opus`.
3.  **Update Sanitization Logic**: Ensure the `isValidAudioFile` method (in the Data layer) allows the new extension.
4.  **Test the Feature**:
    -   **Red**: Create a unit test with an `.opus` file path and verify the repository initially rejects it.
    -   **Green**: Implement the change.
    -   **Refactor**: Ensure the whitelisting is clean and follows project standards.

### 2. Implement a New Playback Control (e.g., Pause)

**Problem**: You want to add a "Pause" button to the UI.

1.  **Create a Use Case**: In the Domain layer, add a `PauseMusicUseCase`.
2.  **Define the Contract**: If the `MusicPlayer` interface doesn't have a `pause()` method, add it.
3.  **Implement the Player**: Update the `MusicPlayer` implementation (Infrastructure/Data) to call the underlying Android `MediaPlayer.pause()`.
4.  **Update the ViewModel**: In `PlayerViewModel`, add a `pause()` method that executes the `PauseMusicUseCase`.
5.  **Modify the UI**: In `PlayingFragment`, bind the new UI button to the ViewModel's `pause()` method.
6.  **Verify State**: Ensure the `PlayerUiState` is updated to reflecting the "Paused" status.

### 3. Add Custom Playlist Management

**Problem**: You want users to be able to create and save playlists.

1.  **Define Domain Entities**: Create a `Playlist` entity (ID, Name, List of Tracks).
2.  **Define a Repository Interface**: Add `PlaylistRepository` to the Domain layer with methods for `save()`, `delete()`, and `getAll()`.
3.  **Implement Use Cases**: Create `CreatePlaylistUseCase`, `AddTrackToPlaylistUseCase`, and `GetPlaylistsUseCase`.
4.  **Implement the Data Layer**: Create `PlaylistRepositoryImpl` (using Room or a similar persistence framework).
5.  **Build the UI**: Create a new `PlaylistViewModel` and corresponding fragment to manage the new feature.

### 3. Implement Material 3 Dynamic Color

**Problem**: You want the app to adapt its theme colors to the user's wallpaper.

1.  **Modify the Theme**: Ensure the application theme inherits from a `Material3` base.
2.  **Initialize in Activity**: In the `MainActivity`, check for dynamic color support and apply it before `setContentView`.
3.  **Use Theme Attributes**: In all layouts, use `?attr/colorPrimary` or `?attr/colorOnSurface` instead of hardcoded hex values to ensure the app responds to the system-wide color scheme.

### 4. Enable Edge-to-Edge Display

**Problem**: You want the app to render behind the status and navigation bars.

1.  **Configure Window**: Use the Android SDK's `enableEdgeToEdge()` function in the `MainActivity`.
2.  **Handle Insets**: Use `WindowInsetsCompat` to apply padding to the main UI components (like the SearchBar or BottomNavigationView) to prevent them from overlapping with the system UI elements.
3.  **Visual Verification**: Test on multiple devices with different gesture and button navigation settings to ensure no interactive elements are clipped.

## 🔐 Security Guides

### 1. Sanitize a New Data Input

**Problem**: You are adding a new metadata field (e.g., "Composer") that needs to be sanitized.

1.  **Apply Logic in the Mapper**: In the `data/mapper/` package, find the `TrackMapper`.
2.  **Implement Sanitization**: Before creating the `Track` domain entity, sanitize the raw string to remove potentially malicious content or unexpected characters.
3.  **Verify in Use Case**: Ensure the `Track` entity rejects invalid strings during creation (if applicable).

### 2. Harden Image Validation

**Problem**: You want to add support for a new image format (e.g., AVIF) to the artist gallery.

1.  **Analyze the Data Layer**: In `ArtistImageService`, locate the magic number validation logic.
2.  **Add Binary Signature**: Add the AVIF magic number (e.g., `00 00 00 20 66 74 79 70 61 76 69 66`) to the supported signatures whitelist.
3.  **Test Against Malicious Files**: Create a test case where a text file is renamed to `.avif` and verify it's rejected by the service.

## 🧪 Testing Guides

### 1. Write a Unit Test for a Use Case

**Problem**: You've added a new business rule and need to verify it.

1.  **Identify Dependencies**: Determine which repositories the Use Case needs.
2.  **Create a Test Suite**: In `app/src/test/java/`, create a new test class.
3.  **Mock Repositories**: Use Mockito or MockK to mock the repository interfaces.
4.  **Verify Logic**: Call the Use Case and assert that it interacts with the repository correctly and returns the expected result.

### 2. Verify UI State Transitions

**Problem**: You want to ensure the loading spinner appears and disappears correctly.

1.  **Use a ViewModel Test**: Mock the Use Case and emit a slow-loading result.
2.  **Assert States**: Verify the ViewModel's `uiState` first contains `Loading`, then `Success`.
3.  **Instrumented Test**: Use Espresso to verify that the spinner view's visibility changes accordingly in the fragment.
