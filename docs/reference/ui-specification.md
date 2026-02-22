# UI and Feature Specification (Android Music Player)

This document specifies the user interface, interaction design, and functional requirements for SheepPlayer, a professional-grade Android music player built on **DDD**, **Clean Architecture**, and **Material 3**.

## 📱 User Experience Principles

-   **Uninterrupted Playback**: Music must continue playing seamlessly during navigation, configuration changes (rotation), and while the app is in the background.
-   **Material 3 & Dynamic Color**: Adheres to modern design standards, utilizing **Material You (Dynamic Color)** to harmonize the app's palette with the user's wallpaper.
-   **Edge-to-Edge Display**: The UI renders behind system bars (status and navigation) to provide a more immersive experience, using window insets for proper component spacing.
-   **Low Latency & Tactile Feedback**: All interactions (Play/Pause, Seek) provide immediate visual (Material Ripple) and haptic (vibration) feedback.
-   **Contextual Awareness**: The UI reflects the current playback state, track metadata, and data source (local vs. cloud) at all times.

## 🧭 Navigation & Information Architecture

The application uses a **Material 3 Bottom Navigation Bar**, providing high-reachability for ergonomic single-handed use.

```mermaid
graph TD
    subgraph BottomNav ["Navigation Hub"]
        T[Tracks - Music Library]
        P[Playing - Control Center]
        G[Pictures - Artist Gallery]
        S[Settings - Configuration]
    end

    T --> T_View[Artist > Album > Track Hierarchy]
    P --> P_View[Playback Controls & Metadata]
    G --> G_View[Dynamic Artist Imagery]
    S --> S_View[Preferences & Cloud Auth]

    style P stroke:#6750A4,stroke-width:4px
```

### 1. Tracks Fragment (Library Browser)
-   **Hierarchy**: An accordion-style list (Artist → Album → Track) to manage large collections efficiently.
-   **Search**: A persistent **Material 3 SearchBar** at the top for real-time filtering of the domain library.
-   **Empty State**: When no music is found, a clear "No Music Found" illustration with a "Scan Again" button is displayed.
-   **Interactions**:
    -   **Swipe-to-Play**: Swiping a track/album right triggers immediate playback and navigates to the "Playing" tab.
    -   **Long Press**: Opens a bottom sheet for "Add to Playlist" or "Track Info".

### 2. Playing Fragment (Now Playing)
-   **Immersive Visuals**: Large album artwork in an `ElevatedCard`.
-   **Playback Controls**: 
    -   Central FAB for Play/Pause.
    -   Next/Previous and Stop buttons.
    -   **Seek Bar**: A Material 3 `Slider` for precise time scrubbing.
-   **Queue View**: A swipeable bottom sheet showing the upcoming tracks in the current playback session.

### 3. Pictures Fragment (Artist Gallery)
-   **Dynamic Gallery**: A staggered grid of validated artist images.
-   **Loading state**: An animated "Sheep" GIF at the end of the list indicates an active search.
-   **Shimmer**: Individual image slots use shimmer effects during the validation phase.

## 🔄 Interaction Flow: Updating Playing List

This flow describes how the playback queue (Playing List) is updated when a user interacts with the library.

```mermaid
sequenceDiagram
    participant User
    participant Tracks as Tracks Fragment
    participant Activity as MainActivity
    participant Playing as Playing Fragment
    participant Manager as MusicPlayerManager

    User->>Tracks: Swipe Right on Album
    Tracks->>Activity: playAlbum(album)
    Activity->>Activity: Set currentAlbumTracks
    Activity->>Manager: playTrack(firstTrack)
    Activity->>Activity: switchToPlayingTab()
    
    rect rgb(240, 240, 240)
    Note over Playing: User switches to Playing tab
    User->>Playing: Tap track in Album List
    Playing->>Activity: playTrackInAlbum(track, index)
    Activity->>Activity: Update currentTrackIndex
    Activity->>Manager: playTrack(track)
    Manager-->>Activity: onPlaybackStateChanged()
    Activity-->>Playing: Update highlight in list
    end
```

## 🛠️ Feature Specifications

### 🔊 Audio Engine & System Integration
-   **Foreground Service**: Playback is managed by a service to ensure it survives activity destruction.
-   **MediaSession & Notifications**:
    -   Implements `MediaSessionCompat` for lock screen controls and system-level media integration.
    -   Displays a persistent **Media Notification** with album art, playback controls, and a "Close" action.
-   **Audio Focus**: Automatically handles transitions (pausing for calls, ducking for notifications).

### 📁 Data & Library Management
-   **Hybrid Discovery**: Merges `MediaStore` (local) and `GoogleDrive` (cloud) into a unified domain library.
-   **Persistence**: Uses a `LocalCacheDataSource` (Room) to store cloud metadata and user playlists.
-   **Playlist Synchronization**: A `LinearProgressIndicator` at the top of the library view indicates active synchronization or persistence tasks.

### 🔐 Security & Integrity
-   **Path Sanitization**: All file paths are validated before any `File` or `Uri` operation.
-   **Image Validation**: Binary signatures (Magic Numbers) are verified for all downloaded imagery (JPEG, PNG, GIF, WebP, AVIF, HEIF).

## ♿ Accessibility & Inclusivity
-   **Contrast**: All text and icons meet WCAG 2.1 AA contrast ratios.
-   **Touch Targets**: Minimum 48x48dp for all interactive elements.
-   **TalkBack**: Descriptive `contentDescription` for every UI state change (e.g., "Song paused", "Scanning library").

## 🔮 Roadmap

| Phase | Focus | Key Features |
| :--- | :--- | :--- |
| **Phase 1** | Foundation | DDD Core, MediaStore Scan, Basic Playback, Security. |
| **Phase 2** | UX Polish | Seek Bar, MediaSession (Notification), Material 3 Styling. |
| **Phase 3** | Personalization | Playlists, Global Search, Dynamic Color support. |
| **Phase 4** | Cloud & Sync | Google Drive integration, Metadata caching. |
| **Phase 5** | Advanced | Equalizer, Sleep Timer, Widgets. |
