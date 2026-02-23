package com.hitsuji.sheepplayer2.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BinarySignatureValidatorTest {

    private val validator = BinarySignatureValidator()

    @Test
    fun `isImage returns true for valid JPEG`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xDB.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)
        assertTrue(validator.isImage(jpeg))
    }

    @Test
    fun `isImage returns true for valid PNG`() {
        val png = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(), 0, 0, 0, 0)
        assertTrue(validator.isImage(png))
    }

    @Test
    fun `isImage returns true for valid GIF`() {
        val gif = byteArrayOf(0x47.toByte(), 0x49.toByte(), 0x46.toByte(), 0x38.toByte(), 0x39.toByte(), 0x61.toByte(), 0, 0, 0, 0, 0, 0)
        assertTrue(validator.isImage(gif))
    }

    @Test
    fun `isImage returns false for malicious payload`() {
        // HTML file renamed to .jpg
        val html = "<html><body></body></html>".toByteArray()
        assertFalse(validator.isImage(html))

        // Shell script renamed to .png
        val shell = "#!/bin/bash
rm -rf /".toByteArray()
        assertFalse(validator.isImage(shell))
    }

    @Test
    fun `isImage returns false for short byte array`() {
        val short = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        assertFalse(validator.isImage(short))
    }
}
