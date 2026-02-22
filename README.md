# SheepPlayer

A secure Android music player for browsing and playing local music files with Google Drive integration.

## What is SheepPlayer?

SheepPlayer is an Android music player application that organizes your music library by artists,
albums, and tracks. It provides secure access to your local music files with a clean, intuitive
interface.

**Key capabilities:**

- Browse music in hierarchical structure (Artists → Albums → Tracks)
- Play audio files with swipe gestures
- View album artwork and track information
- Secure file access with validation
- Google Drive integration for cloud music playback
- Album sequential playback with track navigation
- Artist image gallery with dynamic downloading
- Metadata caching for improved performance

## Getting Started

**For new users:**

1. Download and install the app
2. Grant permission to access your music files
3. (Optional) Sign in to Google Drive for cloud music access
4. Browse your music library in the Tracks tab
5. Swipe right on any track or album to start playing
6. Use the Pictures tab to view artist images while playing

**For developers:**

1. Clone this repository
2. Open in Android Studio
3. Build and run on your device

## Documentation

Documentation is organized following [Diátaxis](https://diataxis.fr/) principles:

### Learning-oriented

- **[Tutorial](docs/tutorials/getting-started.md)** - Learn to build a music player from scratch

### Information-oriented

- **[Reference](docs/reference/technical-reference.md)** - API documentation and specifications
- **[UI & Feature Specification](docs/reference/ui-specification.md)** - User interface and feature
  specifications
- **[API Reference](docs/reference/api.md)** - Complete API documentation
- **[Project Structure](docs/reference/project-structure.md)** - Codebase organization and structure

### Understanding-oriented

- **[Explanation](docs/explanation/architecture-decisions.md)** - Architecture concepts and design decisions

### Project information

- **[Contributing](CONTRIBUTING.md)** - How to contribute to SheepPlayer
- **[Build setup](docs/how-to-guides/build-setup.md)** - Development environment configuration
- **[Testing](docs/explanation/testing-strategy.md)** - Testing approach and guidelines

## Technical Requirements

**Development:**

- Android Studio Arctic Fox (2020.3.1)+
- JDK 11+
- Android SDK API 33+

**Runtime:**

- Android 13+ (API 33)
- Storage permission for music access

**Supported formats:**
MP3, M4A, WAV, FLAC, OGG, AAC

## License

MIT License - see [LICENSE](LICENSE) file.
