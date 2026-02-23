package com.hitsuji.sheepplayer2.domain.service

/**
 * Domain service for validating file formats using magic numbers (binary signatures).
 */
class BinarySignatureValidator {

    fun isImage(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false

        return when {
            // JPEG: FF D8 FF
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> true
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> true
            // GIF: 47 49 46 38 (GIF8)
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte() -> true
            // WebP: 52 49 46 46 (RIFF) ... 57 45 42 50 (WEBP)
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
                    bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() && bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte() -> true
            else -> false
        }
    }
}
