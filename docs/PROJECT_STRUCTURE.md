# Project Structure 🏗️

This document outlines the architecture and organization of the SheepPlayer Android application.

## 📁 Directory Structure

```
app/src/main/
├── java/com/hitsuji/sheepplayer2/
│   ├── Data.kt                     # Data models (Artist, Album, Track, CachedMetadata)
│   ├── MainActivity.kt             # Main application activity with Google Drive integration
│   ├── MusicPlayer.kt              # Core music playback functionality
│   ├── manager/
│   │   └── MusicPlayerManager.kt   # Music player state management
│   ├── repository/
│   │   └── MusicRepository.kt      # Media data access layer
│   ├── service/
│   │   ├── ArtistImageService.kt              # Artist image search and download
│   │   ├── GoogleDriveService.kt              # Google Drive authentication and API
│   │   ├── GoogleDriveRepository.kt           # Google Drive data operations
│   │   ├── GoogleDriveFileDiscovery.kt        # Google Drive file discovery
│   │   ├── GoogleDriveMetadataService.kt      # Google Drive metadata extraction
│   │   ├── MetadataLoadingService.kt          # Background metadata loading service
│   │   ├── MetadataCache.kt                   # Metadata caching system
│   │   ├── MetadataCacheDbHelper.kt           # SQLite database helper
│   │   ├── MusicMetadataExtractor.kt          # Music file metadata extraction
│   │   └── auth/
│   │       └── GoogleDriveAuthenticator.kt    # Google Drive authentication
│   ├── ui/
│   │   ├── pictures/                          # Artist image gallery components
│   │   │   ├── ArtistImageAdapter.kt          # RecyclerView adapter for images
│   │   │   ├── PicturesFragment.kt            # Image display fragment
│   │   │   └── PicturesViewModel.kt           # Image loading logic
│   │   ├── playing/                           # Currently playing UI components
│   │   │   ├── PlayingFragment.kt             # Playing track display
│   │   │   ├── PlayingViewModel.kt            # Playing state management
│   │   │   └── AlbumTrackAdapter.kt           # Album track list adapter
│   │   ├── tracks/                            # Music library browsing UI
│   │   │   ├── TracksFragment.kt              # Music library display
│   │   │   ├── TracksViewModel.kt             # Tracks state management
│   │   │   ├── TreeAdapter.kt                 # Hierarchical music adapter
│   │   │   ├── TreeItem.kt                    # Tree item types
│   │   │   ├── TrackAdapter.kt                # Track list adapter
│   │   │   └── SwipeToPlayHelper.kt           # Swipe gesture handling
│   │   └── menu/                              # Menu and settings UI
│   │       ├── MenuFragment.kt                # Settings fragment
│   │       └── MenuViewModel.kt               # Menu state management
│   └── utils/
│       ├── Constants.kt                       # Application constants
│       └── TimeUtils.kt                       # Time formatting utilities
├── res/
│   ├── drawable/                   # Vector drawables and icons
│   ├── layout/                     # XML layout files
│   ├── mipmap-*/                   # App icons for different densities
│   ├── navigation/                 # Navigation graph
│   ├── values/                     # Colors, strings, themes, dimensions
│   └── xml/                        # Security and backup configurations
└── AndroidManifest.xml             # App permissions and components
```

## 🏛️ Architecture Overview

SheepPlayer follows modern Android architecture principles with clear separation of concerns:

### MVVM + Repository Pattern

```
┌─────────────────┐
│   MainActivity  │ ← Entry point, handles navigation
└─────────┬───────┘
          │
┌─────────▼───────┐
│   UI Fragments  │ ← PlayingFragment, TracksFragment, etc.
└─────────┬───────┘
          │
┌─────────▼───────┐
│   ViewModels    │ ← UI state management (future enhancement)
└─────────┬───────┘
          │
┌─────────▼───────┐
│   Repository    │ ← MusicRepository - data access layer
└─────────┬───────┘
          │
┌─────────▼───────┐
│   MediaStore    │ ← Android system media database
└─────────────────┘
```

## 🔧 Core Components

### Data Layer

- **`Data.kt`**: Contains data classes for `Artist`, `Album`, and `Track`
- **`MusicRepository.kt`**: Handles MediaStore queries and data processing
- **Security**: Implements file path validation and input sanitization

### Business Logic

- **`MusicPlayerManager.kt`**: Manages playback state and player interactions
- **`MusicPlayer.kt`**: Wraps Android MediaPlayer with security enhancements
- **Separation**: Clear boundaries between data, business logic, and UI

### UI Layer

- **`MainActivity.kt`**: Navigation host and permission handling
- **Fragment-based UI**: Modular screens for different app sections
- **Adapters**: TreeAdapter for hierarchical music browsing
- **Material Design**: Consistent with Android design guidelines

### Utilities

- **`Constants.kt`**: Centralized application constants
- **`TimeUtils.kt`**: Duration formatting utilities
- **Security Configs**: Network security, backup rules, data extraction rules

## 🔄 Data Flow

1. **App Launch**: MainActivity requests media permissions
2. **Data Loading**: MusicRepository queries MediaStore API
3. **Data Processing**: Raw media data organized into Artist → Album → Track hierarchy
4. **UI Update**: Fragments receive processed data and update RecyclerViews
5. **User Interaction**: Swipe gestures trigger playback through MusicPlayerManager
6. **Playback**: MusicPlayer handles media playback with security validations

## 🛡️ Security Architecture

### Input Validation

- File path sanitization in MusicRepository
- Extension validation for audio files
- Null safety throughout the codebase

### Component Security

- Minimal exported components (only MainActivity)
- Proper intent filtering
- Network security configuration

### Data Protection

- No sensitive data storage
- Disabled automatic backups
- Secure file access patterns

## 📦 Dependencies

### Core Android

- `androidx.core:core-ktx` - Kotlin extensions
- `androidx.appcompat:appcompat` - Backward compatibility
- `androidx.fragment` - Fragment navigation
- `androidx.lifecycle` - Lifecycle-aware components

### UI Components

- `com.google.android.material:material` - Material Design components
- `androidx.constraintlayout:constraintlayout` - Flexible layouts
- `androidx.navigation` - Fragment navigation

### Testing

- `junit:junit` - Unit testing framework
- `androidx.test.ext:junit` - Android testing extensions
- `androidx.test.espresso:espresso-core` - UI testing

## 🔮 Future Enhancements

- **ViewModels**: Add proper MVVM architecture with ViewModels
- **Room Database**: Local caching for improved performance
- **Dependency Injection**: Implement Hilt for better testability
- **Coroutines**: Replace callback-based async operations
- **DataBinding**: Eliminate findViewById calls