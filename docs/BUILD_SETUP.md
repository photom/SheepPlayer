# Build & Setup Guide 🔧

This guide provides step-by-step instructions for setting up, building, and running the SheepPlayer
Android application.

## 📋 Prerequisites

### Required Software

- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: Java 11 or higher
- **Android SDK**: API level 33 (Android 13) or higher
- **Kotlin**: 2.0.21 or compatible version

### System Requirements

- **OS**: Windows 10/11, macOS 10.14+, or Ubuntu 18.04+
- **RAM**: Minimum 8GB (16GB recommended)
- **Storage**: 4GB free space for Android SDK and project files
- **Device**: Android device with API 33+ or Android emulator

## 🚀 Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/SheepPlayer.git
cd SheepPlayer
```

### 2. Android Studio Setup

1. **Open Android Studio**
2. **Import Project**:
    - Select "Open an existing Android Studio project"
    - Navigate to the cloned SheepPlayer directory
    - Click "OK"

3. **SDK Setup**:
    - Go to `File > Project Structure > SDK Location`
    - Ensure Android SDK path is set correctly
    - Verify API Level 33+ is installed

### 3. Gradle Sync

Android Studio should automatically sync the project. If not:

- Click "Sync Now" banner at the top
- Or go to `File > Sync Project with Gradle Files`

## 🔨 Build Instructions

### Debug Build

```bash
# Command line build
./gradlew assembleDebug

# Or in Android Studio
Build > Make Project (Ctrl+F9)
```

### Release Build

```bash
# Command line build
./gradlew assembleRelease

# Generate signed APK in Android Studio
Build > Generate Signed Bundle/APK
```

## 🏃‍♂️ Running the App

### On Physical Device

1. **Enable Developer Options**:
    - Go to Settings > About Phone
    - Tap "Build Number" 7 times
    - Enable "USB Debugging" in Developer Options

2. **Connect Device**:
    - Connect via USB cable
    - Accept USB debugging permission
    - Device should appear in Android Studio

3. **Run App**:
    - Click "Run" button (green play icon)
    - Or press `Shift + F10`

### On Emulator

1. **Create Virtual Device**:
    - Tools > AVD Manager
    - Click "Create Virtual Device"
    - Choose device (Pixel 4 recommended)
    - Select API Level 33+ system image
    - Configure and finish setup

2. **Run App**:
    - Start emulator
    - Click "Run" button in Android Studio

## ⚙️ Configuration

### Gradle Configuration

Current configuration in `app/build.gradle.kts`:

```kotlin
android {
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.hitsuji.sheepplayer"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
}
```

### Dependencies

Key dependencies are managed in `gradle/libs.versions.toml`:

- Core KTX: 1.10.1
- AppCompat: 1.6.1
- Material: 1.10.0
- Navigation: 2.6.0
- Lifecycle: 2.6.1

## 🧪 Build Variants

### Debug

- **Purpose**: Development and testing
- **Features**: Debugging enabled, logs visible
- **Signing**: Debug keystore (auto-generated)

### Release

- **Purpose**: Production deployment
- **Features**: Code optimization, no debug logs
- **Signing**: Requires production keystore

## 📱 Permissions Setup

The app requires the following permissions:

- `READ_MEDIA_AUDIO` (API 33+)
- `READ_EXTERNAL_STORAGE` (API 32 and below)
- `WAKE_LOCK` (for continuous playback)

These are automatically requested at runtime.

## 🔧 Development Tools

### Useful Gradle Commands

```bash
# Clean build
./gradlew clean

# Run tests
./gradlew test

# Run lint checks
./gradlew lint

# Generate APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### Android Studio Shortcuts

- **Build**: `Ctrl+F9` (Cmd+F9 on Mac)
- **Run**: `Shift+F10` (Ctrl+R on Mac)
- **Debug**: `Shift+F9` (Ctrl+D on Mac)
- **Clean**: `Build > Clean Project`

## 🐛 Troubleshooting Build Issues

### Common Problems

1. **Gradle Sync Failed**:
   ```bash
   # Clear Gradle cache
   ./gradlew clean
   # Invalidate caches in Android Studio
   File > Invalidate Caches and Restart
   ```

2. **SDK Not Found**:
    - Check SDK path in Project Structure
    - Ensure API Level 33+ is installed
    - Update SDK through SDK Manager

3. **Kotlin Version Conflicts**:
    - Check `libs.versions.toml` for version consistency
    - Update to compatible Kotlin version

4. **Build Tools Issues**:
   ```bash
   # Update build tools
   sdkmanager "build-tools;34.0.0"
   ```

### Performance Optimization

- **Parallel Builds**: Add to `gradle.properties`:
  ```
  org.gradle.parallel=true
  org.gradle.caching=true
  ```

- **Memory Settings**:
  ```
  org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
  ```

## 🚢 Deployment

### APK Generation

1. Build > Generate Signed Bundle/APK
2. Select APK
3. Choose/create keystore
4. Select release build variant
5. Generate APK

### Play Store Bundle

1. Build > Generate Signed Bundle/APK
2. Select Android App Bundle
3. Configure signing
4. Upload to Play Console

## 📚 Additional Resources

- [Android Studio User Guide](https://developer.android.com/studio)
- [Gradle Build Tool](https://gradle.org/guides/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Android Developer Guides](https://developer.android.com/guide)