# Project Structure (DDD & Clean Architecture) 🏗️

This document outlines the architecture of SheepPlayer, refactored into a strictly layered **Clean Architecture** with a **Domain-Driven Design (DDD)** core.

## 📁 Architectural Layering

The codebase is partitioned into four primary circles, with the dependency rule pointing strictly inward.

```mermaid
graph TD
    subgraph Infrastructure ["Infrastructure Layer (Frameworks)"]
        MediaPlayer[MediaPlayer Engine]
        Network[Network/API Clients]
        DI[Dependency Injection]
    end
    subgraph Presentation ["Presentation Layer (UI)"]
        Views[Activities/Fragments]
        ViewModels[State Holders]
    end
    subgraph Data ["Data Layer (Interface Adapters)"]
        RepoImpl[Repository Implementations]
        DataSources[MediaStore/Drive DataSources]
        Mappers[DTO to Entity Mappers]
    end
    subgraph Domain ["Domain Layer (Core Logic)"]
        Entities[Entities/Value Objects]
        UseCases[Use Cases/Interactors]
        RepoInterfaces[Repository Interfaces]
    end

    Presentation --> Domain
    Data --> Domain
    Infrastructure --> Data
    Infrastructure --> Presentation
```

## 📂 Package Organization

### 1. `domain/` (The Core)
Contains the essential business rules of the music player. No Android dependencies.
-   **`model/`**: `Track`, `Album`, `Artist` (Entities) and `Duration`, `FilePath` (Value Objects).
-   **`service/`**: Stateless domain logic like `PathValidator` and `BinarySignatureValidator`.
-   **`usecase/`**:
    -   **`GetMusicLibraryUseCase`**: Orchestrates fetching and merging music from local and remote sources.
    -   **`PlayTrackUseCase`**: Handles track preparation (including resolving/downloading remote files) and initiating playback.
    -   **`TogglePlaybackUseCase`**: Manages play/pause state transitions.
-   **`repository/`**: Interfaces defining how the system accesses music data.

### 2. `data/` (The Persistence)
Adapts domain requests to technical storage.
-   **`repository/`**: Concrete implementations of domain repositories (e.g., `MusicRepositoryImpl`).
-   **`datasource/`**: Wrappers for Android `MediaStore` and the Google Drive API.
-   **`mapper/`**: Logic to convert raw system data into domain models.

### 3. `presentation/` (The UI)
Renders the domain state and captures user intent.
-   **`ui/`**: Android-specific views (Fragments, Activities, Adapters).
-   **`viewmodel/`**: State holders that communicate with the domain Use Cases via reactive streams.
-   **`factory/`**: `ViewModelFactory` for providing dependencies to ViewModels via constructor injection.

### 4. `infrastructure/` (The Platform)
Framework-specific tools that implement domain or data requirements.
-   **`player/`**: The actual `MediaPlayer` implementation that fulfills the domain's audio requirements.
-   **`security/`**: Real-world implementation of path and image validation.

### 5. `interfaces/` (Contracts)
Defines the boundary between layers via dependency inversion.
-   **Repository Interfaces**: `MusicRepositoryInterface`, `ArtistImageRepository`.
-   **Service Interfaces**: `GoogleDriveServiceInterface`.
-   **Manager Interfaces**: `PlaybackManagerInterface`.

## 🔄 Interaction Flow: "Swipe to Play"

This sequence illustrates how a user action traverses the architecture.

```mermaid
sequenceDiagram
    participant User
    participant Fragment as Presentation (UI)
    participant VM as Presentation (ViewModel)
    participant UC as Domain (Use Case)
    participant Player as Infrastructure (Audio Engine)
    participant Repo as Data (Repository)

    User->>Fragment: Swipe Right on Track
    Fragment->>VM: triggerPlay(trackId)
    VM->>UC: PlayTrackUseCase.execute(trackId)
    UC->>Repo: GetTrackDetails(trackId)
    Repo-->>UC: Return Track Entity
    UC->>UC: Validate Track Safety
    UC->>Player: Load & Play Audio
    Player-->>VM: Notify "Playing" State
    VM-->>Fragment: Update UI to "Playing"
```

### Swipe to Play Class Diagram

```mermaid
classDiagram
    class TracksFragment {
        -TreeAdapter treeAdapter
        +onTrackSwiped(track)
    }
    class MainActivity {
        -MusicPlayerManager musicPlayerManager
        +playTrack(track)
    }
    class MusicPlayerManager {
        -MusicPlayer musicPlayer
        +playTrack(track)
    }
    class MusicPlayer {
        -MediaPlayer mediaPlayer
        +loadTrack(track, autoPlay)
    }
    
    TracksFragment --> MainActivity : calls
    MainActivity --> MusicPlayerManager : delegates to
    MusicPlayerManager --> MusicPlayer : controls
```

## 🔄 Interaction Flow: Google Drive Login

This sequence illustrates the authentication process for cloud-based music services.

```mermaid
sequenceDiagram
    participant User
    participant Fragment as Presentation (MenuFragment)
    participant VM as Presentation (MenuViewModel)
    participant Auth as Infrastructure (GoogleDriveAuthenticator)
    participant Google as External (Google Identity Service)

    User->>Fragment: Tap "Sign In"
    Fragment->>VM: initiateSignIn()
    VM->>Auth: signIn()
    Auth->>Auth: Check for active session
    
    alt Session Not Found
        Auth->>Google: Launch Intent (OAuth 2.0)
        Google-->>User: Request Permissions
        User->>Google: Approve Access
        Google-->>Auth: Return Account Tokens
    end
    
    Auth->>Auth: Validate Scopes (DRIVE_READONLY)
    Auth-->>VM: Return Result (Success/Error)
    VM-->>Fragment: Update UI State (LoggedIn/LoggedOut)
```

### Google Drive Login Class Diagram

```mermaid
classDiagram
    class MenuFragment {
        -MenuViewModel menuViewModel
        -GoogleDriveServiceInterface googleDriveService
        +signInToGoogleDrive()
    }
    class MenuViewModel {
        +updateGoogleAccountStatus(service)
    }
    class GoogleDriveServiceInterface {
        <<interface>>
        +signIn() GoogleDriveResult
    }
    class GoogleDriveService {
        -GoogleDriveAuthenticator authenticator
        +signIn() GoogleDriveResult
    }
    class GoogleDriveAuthenticator {
        -GoogleSignInClient googleSignInClient
        +signIn() GoogleDriveResult
    }

    MenuFragment --> MenuViewModel : observes
    MenuFragment o-- GoogleDriveServiceInterface : uses
    GoogleDriveService ..|> GoogleDriveServiceInterface : implements
    GoogleDriveService o-- GoogleDriveAuthenticator : delegates to
```

## 🔄 Interaction Flow: Play Album (Updating Queue)

This sequence illustrates how selecting an album updates the playback queue and handles automatic transitions between tracks.

```mermaid
sequenceDiagram
    participant User
    participant Fragment as Presentation (TracksFragment)
    participant Activity as Presentation (MainActivity)
    participant VM as Presentation (PlayerViewModel)
    participant Manager as Domain (MusicPlayerManager)
    participant Engine as Infrastructure (Audio Engine)

    User->>Fragment: Tap "Play" on Album
    Fragment->>Activity: playAlbum(album)
    Activity->>Activity: Set currentAlbumTracks & index = 0
    Activity->>Manager: playTrack(firstTrack)
    Manager->>Engine: loadTrack(firstTrack, autoPlay=true)
    Engine-->>Manager: onPlaybackStarted()
    Manager-->>Activity: onPlaybackStateChanged()
    Activity-->>VM: Notify UI update
    
    Note over Engine: Track finishes...
    Engine-->>Manager: onPlaybackCompleted(track)
    Manager-->>Activity: onPlaybackCompletionListener(track)
    
    Activity->>Activity: Increment track index
    Activity->>Manager: playTrack(nextTrack)
    Manager->>Engine: loadTrack(nextTrack, autoPlay=true)
```

### Play Album Class Diagram

```mermaid
classDiagram
    class TracksFragment {
        +onAlbumSwiped(album)
    }
    class MainActivity {
        -Album currentPlayingAlbum
        -List currentAlbumTracks
        -Int currentTrackIndexInAlbum
        +playAlbum(album)
    }
    class MusicPlayerManager {
        +playTrack(track)
        +setOnPlaybackCompletionListener(listener)
    }
    class MusicPlayer {
        +loadTrack(track, autoPlay)
    }

    TracksFragment --> MainActivity : calls
    MainActivity --> MusicPlayerManager : uses
    MusicPlayerManager --> MusicPlayer : controls
    MusicPlayerManager ..> MainActivity : notifies completion
```

## 🔄 Interaction Flow: Play Track in Album

Selecting a specific track from within an album updates the current index while keeping the album context for future automatic transitions.

```mermaid
sequenceDiagram
    participant User
    participant Fragment as Presentation (PlayingFragment)
    participant Activity as Presentation (MainActivity)
    participant Manager as Domain (MusicPlayerManager)
    participant Engine as Infrastructure (Audio Engine)

    User->>Fragment: Tap Track in Album List
    Fragment->>Activity: playTrackInAlbum(track, index)
    Activity->>Activity: Set currentTrackIndexInAlbum = index
    Activity->>Manager: playTrack(track)
    Manager->>Engine: loadTrack(track, autoPlay=true)
    Engine-->>Manager: onPlaybackStarted()
    Manager-->>Activity: onPlaybackStateChanged()
    Activity-->>Fragment: onPlaybackStateChanged()
```

### Play Track in Album Class Diagram

```mermaid
classDiagram
    class PlayingFragment {
        -AlbumTrackAdapter albumTrackAdapter
        +onPlaybackStateChanged()
    }
    class MainActivity {
        -Int currentTrackIndexInAlbum
        +playTrackInAlbum(track, index)
        +notifyPlaybackStateChanged()
    }
    class MusicPlayerManager {
        +playTrack(track)
        +setOnPlaybackStateChangeListener(listener)
    }
    class MusicPlayer {
        +loadTrack(track, autoPlay)
    }

    PlayingFragment --> MainActivity : calls
    MainActivity --> MusicPlayerManager : uses
    MusicPlayerManager --> MusicPlayer : controls
    MusicPlayerManager ..> MainActivity : notifies state change
    MainActivity ..> PlayingFragment : notifies state change
```

## 🛡️ Security Boundaries

Security is handled at the outermost layers before reaching the domain:
1.  **UI**: Basic input validation.
2.  **Data Layer**: Mappers ensure raw data is sanitized into Domain Entities.
3.  **Infrastructure**: Binary signature validation (Magic Numbers) is performed before images are sent to the Presentation layer.
4.  **Domain Layer**: Enforces invariants (e.g., a `Track` object cannot be instantiated with an invalid path).
