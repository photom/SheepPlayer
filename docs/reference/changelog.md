# Documentation Changelog

## [2.1] - 2026-02-23

### Major Updates

-   **Security Refactoring**: 
    -   Introduced `PathValidator` domain service to centralize file security.
    -   Introduced `BinarySignatureValidator` for magic number verification.
    -   Added strict invariants to the `Track` entity.
-   **Unified UI Feedback**: 
    -   Implemented a persistent "Syncing..." chip in `MainActivity` for cross-fragment visibility.
    -   Removed redundant and repetitive Toast notifications during the Google Drive synchronization process to improve UX and reduce UI clutter.
    -   Refactored `MetadataLoadingService` to emit `Started` events for immediate feedback.
-   **Stability & Playback Fixes**:
    -   Resolved "MediaPlayer not ready" errors by introducing an `isPreparing` state in `MusicPlayerManager`.
    -   Fixed critical path validation bug that blocked playback of cached Google Drive tracks.
    -   Updated Google Drive library to `v3-rev20251210-2.0.0` for 2026 compatibility.

### Added Documentation

-   **Google Drive Security Guide**: Added SHA-1 fingerprints and App Ownership verification steps (anti-impersonation).
-   **New Test Suite**: Added 7+ unit test files covering domain, security, and use case logic.

### New Features Documented

-   Global "running icon" for whole play list updates.
-   Improved Google Drive authentication error reporting with actionable advice.

## [2.0] - 2025-01-13

### Major Updates

- **Google Drive Integration**: Complete documentation for cloud music playback
    - GoogleDriveService, MetadataLoadingService, and authentication system
    - Background metadata loading with progress tracking
    - SQLite-based metadata caching system
    - Hybrid local/cloud music library support

- **Enhanced Playback Features**:
    - Album sequential playback with automatic track progression
    - Album track list in Playing fragment with navigation
    - Enhanced MainActivity API with album context management
    - Track positioning and duration APIs

- **Updated Data Models**:
    - Track model with Google Drive file ID, track numbers, and metadata loading status
    - CachedMetadata model for performance optimization
    - Enhanced API documentation with all new methods and properties

- **Architecture Updates**:
    - Package name updated to com.hitsuji.sheepplayer2
    - Expanded service layer with Google Drive components
    - Enhanced UI components with ViewModels and improved adapters
    - Menu system with Google Drive authentication controls

### Updated Documentation

- **API_REFERENCE.md**: Added Google Drive services, enhanced MainActivity API, updated data models
- **PROJECT_STRUCTURE.md**: Complete restructure reflecting current codebase organization
- **README.md**: Updated capabilities and getting started guide
- **ui-feature-specification.md**: Google Drive integration features, enhanced Playing fragment, album playback
- **GOOGLE_DRIVE_SETUP.md**: Updated setup instructions for current implementation

### New Features Documented

- Background metadata loading service with broadcast communication
- Metadata caching with SQLite database
- Album sequential playback with track navigation
- Google Drive authentication flow
- Enhanced swipe gestures for albums
- Improved error handling and user feedback

## [1.1] - 2025-08-11

### Added

- **IMAGE_VALIDATION.md**: Comprehensive documentation for image magic number validation
    - Supported image formats (JPEG, PNG, GIF, WebP, BMP, ICO, TIFF)
    - Magic number specifications and security benefits
    - Implementation details and usage examples

### Updated

- **ui-feature-specification.md**:
    - Updated Pictures fragment from placeholder to active functionality
    - Added artist image gallery features and security specifications
    - Documented image validation and magic number checking

- **API_REFERENCE.md**:
    - Added ArtistImageService class documentation
    - Added PicturesFragment and ArtistImageAdapter API references
    - Added image validation security features section
    - Added supported image formats table

- **PROJECT_STRUCTURE.md**:
    - Added service layer with ArtistImageService
    - Updated pictures package structure with component details
    - Reflected new architecture additions

### Features Documented

- Dynamic artist image downloading from multiple search engines
- Animated GIF placeholder with sheep character
- Magic number validation for security
- Sequential image display with placeholder replacement
- Memory-efficient bitmap handling
- Network resilience and error handling

---

## [1.0] - 2025-01-08

- Initial documentation set
- Core music player functionality
- Basic UI specifications
- Security features documentation