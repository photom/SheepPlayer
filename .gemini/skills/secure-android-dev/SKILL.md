---
name: secure-android-dev
description: Expert guidance on secure Android application development. Use when implementing file access, input validation, network security, or handling sensitive data.
---

# Secure Android Development

This skill provides procedural knowledge for implementing and maintaining a secure Android application using best practices for data protection, component hardening, and secure communication, aligned with OWASP Mobile Top 10.

## Core Principles

1.  **Defense in Depth**: Layered security controls so that if one fails, others provide protection.
2.  **Least Privilege**: Request only the minimum permissions and narrowest scope for file/data access.
3.  **Zero Trust Architecture**: Never trust external data (Intents, files, network responses) without validation.
4.  **Secure Defaults**: Use the most secure settings by default (e.g., `exported="false"`, `allowBackup="false"`).

## Security Focus Areas

### 1. Data at Rest (Secure Storage)
-   **Guideline**: Use **Jetpack Security (EncryptedSharedPreferences/EncryptedFile)** for sensitive data.
-   **Internal Storage**: Prefer internal storage over external storage for app-private data.
-   **No Hardcoded Secrets**: Use Android Keystore for managing cryptographic keys.

### 2. Data in Transit (Secure Communication)
-   **Enforce HTTPS**: Use `Network Security Configuration` to disable cleartext traffic.
-   **SSL Pinning**: For high-security apps, implement certificate pinning to prevent MitM attacks.
-   **Sensitive Data**: Avoid passing sensitive data via URL parameters.

### 3. Component Hardening
-   **Exported Components**: Explicitly set `android:exported="false"` for all Activities, Services, and Receivers unless they MUST be public.
-   **Intent Validation**: Validate all incoming Intent extras and actions.
-   **Permission Enforcement**: Use custom permissions for inter-app communication.

### 4. Input Validation & Path Security
-   **Path Traversal**: Always use `File.getCanonicalPath()` and check against a base directory.
-   **Content Providers**: Secure Content Providers with `readPermission` and `writePermission`.
-   **SQL Injection**: Use parameterized queries with Room/SQLite.

## Security Workflows

### Implementing Encrypted Storage
1.  Add `androidx.security:security-crypto` dependency.
2.  Initialize `MasterKey` using `MasterKey.Builder`.
3.  Create `EncryptedSharedPreferences` using the MasterKey.
4.  Store/Retrieve data using standard `SharedPreferences` API.

### Hardening Network Security
1.  Create `res/xml/network_security_config.xml`.
2.  Define `<base-config cleartextTrafficPermitted="false">`.
3.  (Optional) Add `<pin-set>` for specific domains.
4.  Link in `AndroidManifest.xml` via `android:networkSecurityConfig`.

### Secure File Access (Path Sanitization)
1.  Receive a path from an untrusted source.
2.  Convert to `File` object and call `canonicalPath`.
3.  Check if `canonicalPath.startsWith(expectedBaseDir)`.
4.  Verify the file extension against a strict whitelist.

## Security Checklist (OWASP Mobile Top 10)
-   [ ] **M1: Improper Platform Usage**: Are we using permissions and intents correctly?
-   [ ] **M2: Insecure Data Storage**: Is sensitive data encrypted?
-   [ ] **M3: Insecure Communication**: Is HTTPS enforced everywhere?
-   [ ] **M4: Insecure Authentication**: Are we using Biometrics/Keystore properly?
-   [ ] **M5: Insufficient Cryptography**: Are we using modern algorithms (AES-GCM, RSA-PSS)?
-   [ ] **M7: Client Code Quality**: Have we sanitized all inputs to prevent injection/traversal?

## References
-   See `docs/reference/test-plans/security.md` for specific security test cases.
-   Android Developers Security Best Practices: [https://developer.android.com/topic/security/best-practices](https://developer.android.com/topic/security/best-practices)
-   OWASP Mobile Top 10: [https://owasp.org/www-project-mobile-top-10/](https://owasp.org/www-project-mobile-top-10/)
