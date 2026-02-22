# Reference Documentation (DDD & Clean Architecture)

This reference defines the system's components through the lens of **Domain-Driven Design (DDD)** and **Clean Architecture**, specifically optimized for a modern Android music player.

## 🏛️ Domain Layer (`domain/`)

The heart of the application, containing the core business logic and definitions of music entities. This layer is pure Kotlin and independent of the Android framework.

### 🧩 Domain Model (`domain/model/`)

#### 📀 Aggregates
-   **`Artist`**: An aggregate root representing a music performer. It contains their unique identity and a list of their **Albums**.
-   **`Album`**: An entity representing a collection of tracks. It belongs to an **Artist**.
-   **`PlaybackSession`**: An aggregate that manages the current state of audio playback, including the active track and queue.

#### 📄 Entities
-   **`Track`**: A unique audio file identified by its content/location ID. It contains metadata and physical file references.
-   **`User`**: Represents the authenticated user (e.g., via Google Drive).

#### 💎 Value Objects
-   **`Duration`**: Encapsulates time in milliseconds, providing validation and formatting logic.
-   **`FilePath`**: Validates that a string is a secure, readable audio file location.
-   **`PlaybackState`**: Represents the enumeration of valid player states (Playing, Paused, Buffering, Stopped, Idle).

### 🛠️ Domain Services (`domain/service/`)
-   **`AudioScannerService`**: Logic for coordinating the scanning of local and remote sources to build the unified library.
-   **`ImageValidatorService`**: Business rules for validating binary signatures of artist imagery.

### 📜 Repository Interfaces (`domain/repository/`)
-   **`MusicRepository`**: Defines the contract for persistence and retrieval of the music library.
-   **`AuthRepository`**: Contract for identity management and cloud service authentication.

### 🚀 Use Cases (Interactors) (`domain/usecase/`)
-   **`GetMusicLibraryUseCase`**: Orchestrates the assembly of the Artist hierarchy from all repositories.
-   **`PlayMusicUseCase`**: Coordinates track validation and starts the audio session.
-   **`TogglePlaybackUseCase`**: Handles the logic for switching between playing and paused states.

## 💾 Data Layer (`data/`)

Adapts the domain requirements to specific technical frameworks and storage mechanisms.

### 🏛️ Repository Implementations
-   **`MusicRepositoryImpl`**: Implements the logic to merge results from the `MediaStore` and `GoogleDrive` data sources.
-   **`AuthRepositoryImpl`**: Implements Google Sign-In logic and token management.

### 🔌 Data Sources (`data/datasource/`)
-   **`LocalMediaDataSource`**: Low-level interaction with Android `MediaStore` (ContentResolver).
-   **`RemoteCloudDataSource`**: Interaction with Google Drive API and metadata extraction services.
-   **`LocalCacheDataSource`**: SQLite-based persistence for caching remote metadata.

### 🔄 Mappers
-   **`EntityMappers`**: Responsibility for converting DTOs (Data Transfer Objects) from system APIs into Domain Entities.

## 🖥️ Presentation Layer (`presentation/`)

Handles user interaction and renders the current state of the Domain.

### 🎮 ViewModels
-   **`LibraryViewModel`**: Manages the UI state for the music explorer (Tracks tab).
-   **`PlayerViewModel`**: Orchestrates the UI state for the control center (Playing tab).
-   **`ArtistGalleryViewModel`**: Manages the dynamic loading of artist images (Pictures tab).

### 🎨 UI Components
-   **`MainActivity`**: The navigation host and controller.
-   **Fragments**: Modular screens observing ViewModel states (e.g., `PlayingFragment`).
-   **Adapters**: Specialized rendering logic for the hierarchical library list.

## ⚙️ Infrastructure Layer (`infrastructure/`)

Framework-specific code that supports the other layers.

### 🔊 Audio Infrastructure
-   **`AndroidMusicPlayer`**: Implementation of a domain-defined player interface using Android's `MediaPlayer` or `ExoPlayer`.

### 🔑 Security & Validation
-   **`PathValidator`**: Android-specific implementation of file path security checks.
-   **`BinarySignatureValidator`**: Logic for checking magic numbers in downloaded files.

### 🛠️ Dependency Injection
-   **`Hilt/Koin Modules`**: Wiring of interfaces to concrete implementations.
