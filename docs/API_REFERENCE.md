# Reference Documentation (DDD & Clean Architecture)

This reference provides detailed technical information about SheepPlayer's classes, methods, and APIs, organized by architectural layer.

## 🏛️ Domain Layer (`domain/`)

The core business logic, entities, and repository interfaces. Independent of Android.

### Entities (`domain/model/`)

#### `Artist`
Represents a musical artist in the domain.
-   `id: Long`: Unique identifier.
-   `name: String`: Artist's name.
-   `albums: List<Album>`: Collection of albums by this artist.

#### `Album`
Represents a musical album.
-   `id: Long`: Unique identifier.
-   `title: String`: Album title.
-   `artistName: String`: Name of the artist.
-   `tracks: List<Track>`: Ordered list of tracks.

#### `Track`
Represents an individual music track.
-   `id: Long`: Unique identifier.
-   `title: String`: Track title.
-   `duration: Long`: Length in milliseconds.
-   `filePath: String`: Physical file location.
-   `albumArtUri: String?`: URI for cover art.

### Repository Interfaces (`domain/repository/`)

#### `MusicRepository`
Defines the contract for accessing music data.
-   `getAllMusic(): List<Artist>`: Retrieves the entire library.
-   `getArtist(id: Long): Artist?`: Fetches a specific artist.
-   `search(query: String): List<Track>`: Searches for tracks matching the query.

#### `AuthRepository`
Defines the contract for user authentication (e.g., Google Drive).
-   `signIn(): Result<User>`
-   `signOut()`
-   `getCurrentUser(): User?`

### Use Cases (`domain/usecase/`)

#### `GetMusicLibraryUseCase`
Orchestrates the retrieval and organization of the music library.
-   `invoke(): Result<List<Artist>>`: Executes the logic to fetch and sort artists.

#### `PlayTrackUseCase`
Handles the business logic for starting playback.
-   `invoke(track: Track)`: Validates the track and instructs the player to start.

#### `SearchLibraryUseCase`
Encapsulates search logic.
-   `invoke(query: String): List<Track>`: Returns matching tracks.

## 💾 Data Layer (`data/`)

Implementations of domain interfaces and data sources.

### Repository Implementations (`data/repository/`)

#### `MusicRepositoryImpl`
Concrete implementation of `MusicRepository`.
-   **Dependencies**: `LocalMediaDataSource`, `RemoteDriveDataSource`.
-   **Responsibility**: Coordinates fetching from local and remote sources, mapping results to Domain Entities.

#### `AuthRepositoryImpl`
Concrete implementation of `AuthRepository`.
-   **Dependencies**: `GoogleSignInClient`.
-   **Responsibility**: Wraps Google Sign-In SDK calls.

### Data Sources (`data/datasource/`)

#### `LocalMediaDataSource`
Wrapper around Android `MediaStore`.
-   `queryAudioFiles(): List<MediaDto>`: Low-level query for audio files.

#### `RemoteDriveDataSource`
Wrapper around Google Drive API.
-   `listFiles(): List<DriveFileDto>`: Fetches files from Drive.

### Mappers (`data/mapper/`)

#### `TrackMapper`
Converts `MediaDto` (Data Layer) to `Track` (Domain Layer).
-   `mapToDomain(dto: MediaDto): Track`

## 🖥️ Presentation Layer (`presentation/`)

UI components and state holders.

### ViewModels (`presentation/viewmodel/`)

#### `LibraryViewModel`
State holder for the library screen.
-   **Dependencies**: `GetMusicLibraryUseCase`.
-   **State**: `LibraryUiState` (Loading, Success, Error).
-   `loadLibrary()`: Triggers the Use Case.

#### `PlayerViewModel`
State holder for the player screen.
-   **Dependencies**: `PlayTrackUseCase`, `ControlPlaybackUseCase`.
-   **State**: `PlayerUiState` (Playing, Paused, TrackInfo).
-   `play(track: Track)`: Triggers playback.

### UI Components (`presentation/ui/`)

#### `TracksFragment`
Displays the music library.
-   **Observes**: `LibraryViewModel.uiState`.
-   **Adapter**: `TreeAdapter` (renders Domain Entities).

#### `PlayingFragment`
Displays current track info and controls.
-   **Observes**: `PlayerViewModel.uiState`.

## 📦 Dependency Injection (`di/`)

Configuration for wiring layers together (e.g., Hilt/Koin modules).

-   **DomainModule**: Provides Use Cases.
-   **DataModule**: Binds Repository implementations to interfaces.
-   **AppModule**: Provides system services (Context, ContentResolver).
