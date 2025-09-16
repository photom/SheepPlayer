# Google Drive Integration Setup

## Overview

The SheepPlayer app includes Google Drive integration to load and play music files directly from
Google Drive. However, this requires proper Google API configuration.

## Setup Requirements

### 1. Google Cloud Console Setup

1. **Create a Google Cloud Project**:
    - Go to [Google Cloud Console](https://console.cloud.google.com/)
    - Create a new project or select an existing one
    - Enable the following APIs:
        - Google Drive API
        - Google Sign-In API

2. **Configure OAuth 2.0**:
    - Go to "Credentials" in the Google Cloud Console
    - Create OAuth 2.0 Client IDs for Android
    - Add your app's SHA-1 fingerprints (see below)

### 2. Get SHA-1 Fingerprints

Run these commands in your project directory:

```bash
# For debug builds
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# For release builds (if you have a release keystore)
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias_name
```

### 3. Configure Firebase (Recommended)

1. **Create Firebase Project**:
    - Go to [Firebase Console](https://console.firebase.google.com/)
    - Create a new project or use existing Google Cloud project
    - Add Android app with package name: `com.hitsuji.sheepplayer`

2. **Add SHA-1 Fingerprints**:
    - In Firebase Console, go to Project Settings
    - Add SHA-1 fingerprints from step 2

3. **Download Configuration**:
    - Download `google-services.json`
    - Place it in `app/` directory

### 4. Add Google Services Plugin

Add to `build.gradle.kts` (Module: app):

```kotlin
plugins {
    // ... existing plugins
    id("com.google.gms.google-services")
}
```

Add to `build.gradle.kts` (Project level):

```kotlin
plugins {
    // ... existing plugins
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

## Current Status

The app currently includes:

- ✅ Google Drive API dependencies
- ✅ Google Sign-In integration
- ✅ Drive file discovery and metadata extraction
- ✅ Music playback from Google Drive
- ✅ Background metadata loading service
- ✅ SQLite metadata caching system
- ✅ Authentication flow with menu integration
- ✅ Progress tracking and user feedback
- ❌ `google-services.json` configuration file
- ❌ SHA-1 fingerprint registration

## Error Messages

If you see these errors:

- `"Sign in cancelled or failed"` - Missing Google services configuration
- `"GoogleSignIn not properly configured"` - Need `google-services.json`
- Authentication failures - Check SHA-1 fingerprints

## Manual Testing (Alternative)

If you don't want to set up Google services right now, you can:

1. Comment out Google Drive functionality
2. Use only local music files
3. The app will work normally for local storage music

## File Structure

```
app/
├── src/main/java/.../service/
│   ├── GoogleDriveService.kt                # Google Drive integration
│   ├── GoogleDriveRepository.kt             # Google Drive data operations  
│   ├── GoogleDriveFileDiscovery.kt          # File discovery service
│   ├── GoogleDriveMetadataService.kt        # Metadata extraction
│   ├── MetadataLoadingService.kt            # Background loading service
│   ├── MetadataCache.kt                     # Caching system
│   ├── MetadataCacheDbHelper.kt             # SQLite database
│   ├── MusicMetadataExtractor.kt            # Music file metadata
│   └── auth/
│       └── GoogleDriveAuthenticator.kt      # Authentication logic
├── src/main/java/.../ui/menu/
│   ├── MenuFragment.kt                      # Settings UI
│   └── MenuViewModel.kt                     # Settings logic
├── google-services.json                     # ❌ MISSING - Need to add
└── build.gradle.kts                         # Dependencies configured ✅
```

## Troubleshooting

1. **Check Logs**: Look for "GoogleDriveService" logs in Android Studio
2. **Verify Package Name**: Must match `com.hitsuji.sheepplayer`
3. **Check SHA-1**: Ensure fingerprints are added to Firebase/Google Console
4. **Restart App**: After adding `google-services.json`, clean and rebuild

## Support

For Google Sign-In issues, refer to:

- [Google Sign-In Android Documentation](https://developers.google.com/identity/sign-in/android)
- [Firebase Authentication Setup](https://firebase.google.com/docs/auth/android/google-signin)