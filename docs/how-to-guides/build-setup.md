# Build & Setup Guide 🔧

This guide provides step-by-step instructions for setting up, building, and running the SheepPlayer Android application.

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

### 1. Obtain the Source Code
Download or clone the SheepPlayer repository from the project's source control hosting service and navigate to the project root directory.

### 2. Android Studio Setup
1.  **Open Android Studio** and select the option to import or open an existing project.
2.  **Navigate to the SheepPlayer directory** and confirm the selection.
3.  **Configure the SDK**: Access the Project Structure settings and ensure the Android SDK path is correctly set, verifying that API Level 33 or higher is installed.

### 3. Gradle Synchronization
Android Studio will typically initiate a synchronization process upon opening the project. If it does not, manually trigger a sync via the "Sync Project with Gradle Files" option in the File menu.

## 🔨 Build Instructions

The project can be built using either the Android Studio interface or the command line.

### Common Build Tasks

| Target | Command Line Task | Android Studio Action |
| :--- | :--- | :--- |
| **Debug Build** | assembleDebug | Build > Make Project |
| **Release Build** | assembleRelease | Build > Generate Signed APK |
| **Clean Project** | clean | Build > Clean Project |
| **Run Tests** | test | Run > Run 'Tests' |

## 🏃‍♂️ Running the App

### On a Physical Device
1.  **Enable Developer Options**: In the device settings, find the "Build Number" and tap it seven times. Enable "USB Debugging" in the now-visible Developer Options.
2.  **Connect the Device**: Connect via USB and accept any debugging permission prompts.
3.  **Execute**: Use the "Run" button in Android Studio or the appropriate shortcut.

### On an Emulator
1.  **Create a Virtual Device**: Use the AVD Manager to set up a new device (e.g., Pixel 4) using an API Level 33 or higher system image.
2.  **Execute**: Start the emulator and click the "Run" button in Android Studio.

## ⚙️ Configuration

### Build Settings
The application's core build parameters are managed within the Gradle configuration:

| Setting | Value |
| :--- | :--- |
| **Application ID** | com.hitsuji.sheepplayer |
| **Minimum SDK** | 33 |
| **Target/Compile SDK** | 36 |
| **Java Compatibility** | Version 11 |
| **Kotlin JVM Target** | 11 |

### Dependencies
Key libraries include Core KTX, AppCompat, Material Components, Navigation, and Lifecycle components, with versions managed in the centralized library catalog.

## 🧪 Build Variants

-   **Debug**: Intended for development and testing; includes full logging and is signed with an automatically generated debug key.
-   **Release**: Optimized for production; excludes debug logs. It is signed only when a production keystore is configured (see [Release Signing](#-release-signing)); otherwise the release APK is built unsigned. It is never signed with the debug key.

## 📱 Permissions Setup
SheepPlayer automatically requests necessary permissions at runtime, including media access for audio files and wake locks for continuous background playback.

## 🔧 Development Tools

### Common Keyboard Shortcuts
-   **Build Project**: Ctrl+F9 (Windows/Linux) or Cmd+F9 (Mac)
-   **Run Application**: Shift+F10 (Windows/Linux) or Ctrl+R (Mac)
-   **Debug Application**: Shift+F9 (Windows/Linux) or Ctrl+D (Mac)

## 🐛 Troubleshooting Build Issues

### Common Solutions
-   **Sync Failures**: If Gradle synchronization fails, try clearing the Gradle cache or using the "Invalidate Caches and Restart" option in Android Studio.
-   **SDK Errors**: Verify the SDK path in the Project Structure and ensure all required API levels are installed via the SDK Manager.
-   **Version Conflicts**: Check the library catalog for consistency and ensure the Kotlin version is compatible with the Gradle plugin.

### Performance Optimization
To speed up builds, enable parallel execution and build caching in the project's Gradle properties. Additionally, ensure the JVM is allocated sufficient memory (at least 4GB) for the build process.

## 🔐 Release Signing
Release signing credentials are read from Gradle properties or environment variables and are never committed to the repository. Set all four values to sign release builds:

| Gradle property | Environment variable | Meaning |
| :--- | :--- | :--- |
| `sheepplayer.release.storeFile` | `SHEEPPLAYER_RELEASE_STORE_FILE` | Path to the production keystore |
| `sheepplayer.release.storePassword` | `SHEEPPLAYER_RELEASE_STORE_PASSWORD` | Keystore password |
| `sheepplayer.release.keyAlias` | `SHEEPPLAYER_RELEASE_KEY_ALIAS` | Key alias |
| `sheepplayer.release.keyPassword` | `SHEEPPLAYER_RELEASE_KEY_PASSWORD` | Key password |

Put the Gradle properties in the `gradle.properties` file of your Gradle user home, not in the project's `gradle.properties`. A relative keystore path is resolved against the project root. Blank values count as unset.

-   **None set**: `./gradlew build` still succeeds and produces an unsigned release APK, which is what CI does. An unsigned APK cannot be installed on a device, so `installRelease` requires signing.
-   **Some set**: the build fails immediately and names the missing properties.

## 🚢 Deployment
For production deployment, configure [Release Signing](#-release-signing) and run `./gradlew assembleRelease` or `./gradlew bundleRelease`, or generate a signed APK or Android App Bundle via the "Generate Signed Bundle/APK" wizard in Android Studio.

## 📚 Additional Resources
Consult the official Android Studio, Gradle, Kotlin, and Android developer documentation for more detailed information on advanced build configurations and deployment strategies.
