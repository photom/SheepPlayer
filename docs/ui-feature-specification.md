# UI and Feature Specification

User interface design and feature specification for SheepPlayer Android music player.

## Application Overview

SheepPlayer is a local music player that organizes audio files in a hierarchical structure. Users
browse their music library by artists and albums, then play tracks using swipe gestures.

## User Interface Specification

### Navigation Structure

**Bottom Navigation Tabs**:

- **Tracks**: Primary music library browser
- **Playing**: Current track display and controls
- **Pictures**: Artist image gallery with dynamic image downloading

### Screen Layouts

#### Tracks Fragment

**Primary View**: Scrollable RecyclerView with hierarchical music display

**Item Types**:

- **Artist Items**: Artist name with album count, expand/collapse icon
- **Album Items**: Album artwork, title, track count, artist name
- **Track Items**: Track title, duration, artist context

**Interaction Model**:

- **Tap Artist**: Expand/collapse to show/hide albums
- **Tap Album**: Expand/collapse to show/hide tracks
- **Swipe Track Right**: Start playback and switch to Playing tab
- **Swipe Album Right**: Start album playback (sequential) and switch to Playing tab
- **Expand Icons**: Visual indication of expandable state
- **Cloud Indicators**: Visual indication for Google Drive tracks vs local tracks

**Visual Hierarchy**:

- Artists at top level with largest text
- Albums indented under expanded artists
- Tracks indented under expanded albums
- Progressive text size reduction by level

**Scrolling Behavior**:

- Vertical scroll through entire music collection
- Smooth scrolling with visual scroll indicators
- Maintains scroll position during expand/collapse operations
- Supports over-scroll effects for natural feel
- Dynamic bottom padding based on system UI (navigation bars)
- Window insets handling ensures last track is never hidden behind bottom navigation
- ConstraintLayout provides proper boundaries for long lists
- Clip-to-padding disabled for natural scroll behavior

#### Playing Fragment

**State: No Track Selected**

- Centered message: "No track selected"
- Instruction: "Select a song from the Tracks tab"

**State: Track Playing**

- **Album Artwork**: Large centered image (256x256dp)
- **Track Information**: Title, artist, album name vertically stacked
- **Time Display**: Current position and total duration with delimiter (MM:SS / MM:SS or H:MM:SS /
  H:MM:SS format)
- **Play/Stop Button**: Large circular button below track info
- **Album Track List**: When playing an album, shows scrollable list of all tracks in the album with current track highlighted

**Button States**:

- **Play State**: Shows "Stop" with stop icon
- **Stop State**: Shows "Play" with play icon

**Album Playback Features**:

- **Sequential Playback**: Automatically plays next track in album when current track ends
- **Track Navigation**: Tap any track in the album list to jump to that track
- **Album Context**: Maintains album playback context until user plays individual track
- **Current Track Highlighting**: Currently playing track is visually highlighted in the list

#### Pictures Fragment

**Dynamic Artist Images**: When a track is playing and user switches to Pictures tab:

- **Animated Placeholder**: Sheep character GIF shows searching status at bottom of list
- **Image Search**: Automatically searches for artist images from multiple sources (Google, Bing,
  DuckDuckGo)
- **Sequential Display**: Downloads and displays up to 10 artist images as they become available
- **Replacement Logic**: Animated placeholder gets replaced by first downloaded image
- **Scrollable Gallery**: Vertical RecyclerView for browsing downloaded images
- **Image Validation**: Magic number validation ensures only valid image files are processed

**Security Features**:

- **Format Validation**: Supports JPEG, PNG, GIF, WebP, BMP, ICO, TIFF formats
- **Magic Number Check**: Validates file signatures to prevent malicious content
- **Size Filtering**: Excludes thumbnails smaller than 150x150 pixels
- **Memory Management**: Efficient bitmap handling with automatic garbage collection

**User Experience**:

- **Auto-Refresh**: Clears and reloads when different artist starts playing
- **Empty State**: Shows placeholder when no track is playing
- **Network Resilience**: Handles failed downloads gracefully
- **Progress Feedback**: Animated sheep indicates active search and download

### Visual Design

#### Color Scheme

- **Primary**: Purple tones for branding and emphasis
- **Secondary**: Teal accents for interactive elements
- **Background**: Standard Material Design surface colors
- **Text**: High contrast ratios for readability

#### Typography

- **Artist Names**: Largest text size, medium weight
- **Album Titles**: Medium text size, regular weight
- **Track Titles**: Standard text size, regular weight
- **Secondary Info**: Smaller text size, secondary color

#### Spacing

- **Item Padding**: Consistent horizontal and vertical spacing
- **List Dividers**: Subtle separation between items
- **Content Margins**: Standard Material Design spacing
- **Touch Targets**: Minimum 48dp for accessibility

### Interaction Specifications

#### Touch Interactions

**Swipe Gestures**:

- **Direction**: Right swipe only on track items
- **Threshold**: Minimum distance required to trigger
- **Feedback**: Visual indication during swipe
- **Action**: Start playback and navigate to Playing tab

**Tap Interactions**:

- **Expandable Items**: Toggle expand/collapse state
- **Visual Feedback**: Material ripple effect on all touchable items
- **State Persistence**: Remember expanded items during navigation

**Button Interactions**:

- **Play/Stop Toggle**: Single button with state-dependent label
- **Immediate Feedback**: Button state changes instantly
- **Audio Control**: Direct MediaPlayer start/stop commands

#### Navigation Behavior

**Tab Switching**:

- **Selection Persistence**: Remember last selected tab
- **State Preservation**: Maintain scroll position and expansion states
- **Smooth Transitions**: Fade animations between fragments

**Auto-Navigation**:

- **Track Selection**: Automatically switch to Playing tab on track start
- **Back Navigation**: Standard Android back button behavior

## Feature Specifications

### Core Features

#### Google Drive Integration

**Cloud Music Access**:

- **Authentication**: Sign in/out via Google account through options menu
- **Automatic Discovery**: Finds music files across Google Drive folders
- **Background Loading**: Metadata extraction runs in background service
- **Progress Feedback**: Toast messages and broadcast updates for loading progress
- **Caching System**: SQLite-based metadata cache for improved performance
- **Hybrid Library**: Seamlessly mixes local and cloud music in unified interface

**Menu Options**:

- **Sign In**: Authenticate with Google Drive (visible when not signed in)
- **Sign Out**: Disconnect from Google Drive (visible when signed in)  
- **Refresh**: Reload Google Drive music library (visible when signed in)

**Data Synchronization**:

- **Incremental Loading**: Loads local music first, then adds Google Drive content
- **Real-time Updates**: UI updates as Google Drive tracks become available
- **Error Handling**: Graceful fallback when Google Drive access fails
- **Metadata Extraction**: Full metadata support for cloud files including artwork

#### Music Library Management

**Audio File Discovery**:

- **Source**: Android MediaStore API
- **Formats**: MP3, M4A, WAV, FLAC, OGG, AAC
- **Location**: All accessible storage locations
- **Filtering**: Music files only, excluding system sounds

**Data Organization**:

- **Structure**: Three-level hierarchy (Artist → Album → Track)
- **Sorting**: Alphabetical at all levels
- **Grouping**: Automatic by metadata fields
- **Counts**: Display track counts for albums, album counts for artists

#### Audio Playback

**Playback Engine**:

- **Framework**: Android MediaPlayer
- **States**: Idle, Loading, Playing, Stopped, Error
- **Audio Focus**: Proper audio session management
- **Interruption Handling**: Pause for calls and notifications

**Track Management**:

- **Loading**: Asynchronous track preparation
- **Validation**: File path security checks before loading
- **Error Handling**: User-friendly error messages
- **Completion**: Automatic stop at track end

#### Security Features

**File Access Protection**:

- **Path Validation**: Prevent directory traversal attacks
- **Extension Filtering**: Only allow supported audio formats
- **Permission Management**: Dynamic runtime permission requests
- **Input Sanitization**: Clean all metadata from MediaStore
- **Image Validation**: Magic number verification for downloaded image files
- **Content Filtering**: Excludes non-image files masquerading as images

### User Experience Features

#### Intuitive Navigation

**Gesture-Based Control**:

- **Primary Action**: Swipe-to-play for quick track access
- **Secondary Actions**: Tap-to-expand for exploration
- **Visual Cues**: Clear indication of interactive elements

**State Management**:

- **Expansion Memory**: Persist which items are expanded
- **Playback State**: Remember current track across app lifecycle
- **Navigation State**: Maintain tab selection and scroll positions

#### Information Display

**Metadata Presentation**:

- **Track Details**: Title, artist, album, duration
- **Visual Elements**: Album artwork when available
- **Status Information**: Track counts and library statistics
- **Time Formatting**: Consistent MM:SS or H:MM:SS duration display (shows hours when ≥ 60 minutes)
- **Progress Display**: Current playback position with total duration (MM:SS / MM:SS or H:MM:SS / H:
  MM:SS)

**System Communication**:

- **Loading Feedback**: Progress indication for long operations
- **Error Messages**: Clear, actionable error descriptions
- **Empty States**: Helpful guidance when no music found
- **Permission Requests**: Educational permission explanations

### Performance Features

#### Efficient Operations

**Background Processing**:

- **Music Loading**: Non-blocking library discovery
- **UI Responsiveness**: Main thread reserved for interface updates
- **Memory Management**: Efficient RecyclerView with view recycling
- **Resource Cleanup**: Proper MediaPlayer resource release

**Optimized Display**:

- **Lazy Loading**: Load music data asynchronously
- **Smooth Scrolling**: Optimized list performance for large libraries with vertical scroll
  indicators
- **Quick Response**: Fast expand/collapse animations
- **Minimal Lag**: Immediate feedback for user interactions
- **Dynamic Sizing**: RecyclerView adjusts height dynamically for expand/collapse operations
- **Scroll Memory**: Preserves scroll position during navigation and configuration changes

## Accessibility Features

### Content Accessibility

**Screen Reader Support**:

- **Content Descriptions**: Meaningful labels for all interactive elements
- **Navigation Aid**: Logical focus order through interface
- **State Announcements**: Audio feedback for expand/collapse actions

**Visual Accessibility**:

- **Text Scaling**: Support for dynamic text size adjustment
- **High Contrast**: Compatible with system accessibility modes
- **Color Independence**: No critical information conveyed by color alone

### Interaction Accessibility

**Touch Accessibility**:

- **Target Size**: Minimum 48dp touch targets
- **Gesture Alternatives**: Tap alternatives to swipe gestures
- **Clear Feedback**: Visual and audio confirmation of actions

## Future Enhancements

### Phase 2 Features

**Enhanced Playback**:

- **Pause/Resume**: Replace stop-only with pause capability
- **Track Navigation**: Previous/next track controls
- **Playback Modes**: Shuffle and repeat functionality

**Library Features**:

- **Search**: Real-time filtering across all metadata
- **Playlists**: Custom track collections
- **Sorting Options**: Alternative organization methods

### Phase 3 Features

**Advanced Controls**:

- **Equalizer**: Multi-band audio adjustment
- **Sleep Timer**: Automatic playback cessation
- **Crossfade**: Smooth track transitions

**Visual Enhancements**:

- **Theme Options**: Light/dark mode switching
- **Album Art Gallery**: Visual music browsing interface
- **Enhanced Animations**: More sophisticated UI transitions

**System Integration**:

- **Media Controls**: Lock screen and notification controls
- **Android Auto**: Vehicle integration
- **Widget Support**: Home screen playback controls

---

**Document Version**: 1.1  
**Last Updated**: 2025-08-11  
**Changes**: Added Pictures fragment functionality, image validation features