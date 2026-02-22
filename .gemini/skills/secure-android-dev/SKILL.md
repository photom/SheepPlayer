---
name: secure-android-dev
description: Expert guidance on secure Android application development. Use when implementing file access, input validation, network security, or handling sensitive data.
---

# Secure Android Development

This skill provides procedural knowledge for implementing and maintaining a secure Android application using best practices for input validation, file security, and network safety.

## Core Principles

1.  **Least Privilege**: Request only the minimum permissions needed.
2.  **Input Validation**: Treat all external data (files, intents, network) as untrusted.
3.  **Path Sanitization**: Prevent path traversal attacks by validating and sanitizing file paths.
4.  **Secure Storage**: Protect sensitive data using encrypted storage if necessary.
5.  **Component Security**: Minimize exported components (activities, services, providers).

## Security Layers

### 1. Input Validation (`repository/`, `utils/`)
-   **Components**: File path validation, intent filtering, data sanitization.
-   **Responsibility**: Ensuring only valid and safe data enters the system.
-   **Guideline**: Use white-listing for file extensions and path structures.

### 2. File Security (`MusicPlayer.kt`, `MusicRepository.kt`)
-   **Components**: File system access, storage permissions.
-   **Responsibility**: Protecting the user's files and system integrity.
-   **Guideline**: Always check `exists()`, `canRead()`, and `isFile()` before processing files.

### 3. Network Security (`res/xml/network_security_config.xml`)
-   **Components**: API connections, SSL/TLS configuration.
-   **Responsibility**: Protecting data in transit.
-   **Guideline**: Enforce HTTPS and pin certificates for critical services.

### 4. Data Protection
-   **Components**: `AndroidManifest.xml` backup configuration.
-   **Responsibility**: Preventing unauthorized data leakage.
-   **Guideline**: Set `android:allowBackup="false"` for apps with sensitive data.

## Security Workflows

### Implementing Secure File Access
1.  **Validate Extension**: Use a white-list (e.g., `.mp3`, `.flac`).
2.  **Check Path Traversal**: Reject paths containing `..` or leading `/etc/`.
3.  **Sanitize Path**: Use `File.getCanonicalPath()` to resolve symbolic links and relative paths.
4.  **Verify Permissions**: Ensure the app has the necessary storage permissions (e.g., `READ_EXTERNAL_STORAGE`).

### Hardening Components
1.  **MainActivity**: Only export if it is the entry point.
2.  **Services/Receivers**: Set `android:exported="false"` by default.
3.  **Intents**: Use explicit intents for internal communication.
4.  **Intent Filters**: Be as specific as possible to prevent intent hijacking.

## Security Testing
-   Always include **malicious path tests** (e.g., `../../../etc/passwd`).
-   Test **extension validation** (e.g., rejecting `.txt`, `.jpg` as audio).
-   Verify **null/empty path handling**.
-   Test **permission denial scenarios**.

## References
-   See `docs/PROJECT_STRUCTURE.md` for the security architecture overview.
-   See `docs/TESTING_GUIDE.md` for specific security test examples.
-   See `docs/SOLID_COMPLIANCE.md` for the secure design principles.
