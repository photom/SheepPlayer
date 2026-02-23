package com.hitsuji.sheepplayer2.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PathValidatorTest {

    private val allowedPrefixes = listOf("/storage/emulated/0/Music/", "/data/user/0/com.hitsuji.sheepplayer2/cache/")
    private val validator = PathValidator(allowedPrefixes)

    @Test
    fun `isValidPath returns true for valid music path`() {
        assertTrue(validator.isValidPath("/storage/emulated/0/Music/song.mp3"))
        assertTrue(validator.isValidPath("/data/user/0/com.hitsuji.sheepplayer2/cache/download.wav"))
    }

    @Test
    fun `isValidPath returns false for traversal attempt`() {
        assertFalse(validator.isValidPath("/storage/emulated/0/Music/../../../etc/passwd"))
        assertFalse(validator.isValidPath("/storage/emulated/0/Music/..%2f..%2f..%2fetc/passwd"))
    }

    @Test
    fun `isValidPath returns false for disallowed prefix`() {
        assertFalse(validator.isValidPath("/system/etc/hosts"))
        assertFalse(validator.isValidPath("/data/local/tmp/malicious.sh"))
    }

    @Test
    fun `isValidPath returns false for disallowed extension`() {
        assertFalse(validator.isValidPath("/storage/emulated/0/Music/playlist.txt"))
        assertFalse(validator.isValidPath("/storage/emulated/0/Music/app.exe"))
    }

    @Test
    fun `isValidPath returns false for blank path`() {
        assertFalse(validator.isValidPath(""))
        assertFalse(validator.isValidPath("   "))
    }
}
