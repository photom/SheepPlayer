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
-   **Release**: Optimized for production; excludes debug logs and requires a secure production keystore for signing.

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

## 🚢 Deployment
For production deployment, generate a signed APK or Android App Bundle via the "Generate Signed Bundle/APK" wizard in Android Studio, following the prompts to configure your production keystore.

## 📚 Additional Resources
Consult the official Android Studio, Gradle, Kotlin, and Android developer documentation for more detailed information on advanced build configurations and deployment strategies.
