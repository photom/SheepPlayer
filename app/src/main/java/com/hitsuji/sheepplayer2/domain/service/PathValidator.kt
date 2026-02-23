package com.hitsuji.sheepplayer2.domain.service

/**
 * Domain service for validating file paths to prevent traversal and unauthorized access.
 */
class PathValidator(
    private val allowedPrefixes: List<String>
) {
    private val suspiciousPatterns = listOf("../", "..\\", "//", "\\\\", "%2e%2e", "..%2f", "..%5c")
    private val validExtensions = setOf("mp3", "m4a", "wav", "flac", "ogg", "aac", "tmp")

    fun isValidPath(path: String): Boolean {
        if (path.isBlank() || path.length > 4096) return false

        val lowercasePath = path.lowercase()

        // 1. Check for suspicious patterns (Traversal)
        // We check for patterns that actually allow going up a directory
        if (suspiciousPatterns.any { lowercasePath.contains(it) }) return false

        // 2. Check for allowed prefixes
        if (!allowedPrefixes.any { lowercasePath.startsWith(it.lowercase()) }) return false

        // 3. Check extension
        val extension = path.substringAfterLast(".", "").lowercase()
        if (extension !in validExtensions) return false

        return true
    }
}
