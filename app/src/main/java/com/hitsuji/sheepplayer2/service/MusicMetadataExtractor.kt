package com.hitsuji.sheepplayer2.service

import android.util.Log
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

data class MusicMetadata(
    val artist: String?,
    val album: String?,
    val title: String?,
    val duration: Long = 0L,
    val trackNumber: Int? = null,
    val artwork: ByteArray? = null,
    val isComplete: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MusicMetadata

        if (artist != other.artist) return false
        if (album != other.album) return false
        if (title != other.title) return false
        if (duration != other.duration) return false
        if (trackNumber != other.trackNumber) return false
        if (artwork != null) {
            if (other.artwork == null) return false
            if (!artwork.contentEquals(other.artwork)) return false
        } else if (other.artwork != null) return false
        if (isComplete != other.isComplete) return false

        return true
    }

    override fun hashCode(): Int {
        var result = artist?.hashCode() ?: 0
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + duration.hashCode()
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (artwork?.contentHashCode() ?: 0)
        result = 31 * result + isComplete.hashCode()
        return result
    }
}

class MusicMetadataExtractor {

    companion object {
        private const val TAG = "MusicMetadataExtractor"
    }

    suspend fun extractMetadataFromDrive(
        driveService: Drive,
        fileId: String,
        fileName: String,
        mimeType: String
    ): MusicMetadata? {

        val chunkSize = when {
            mimeType.contains("mp3") || mimeType.contains("mpeg") -> 256 * 1024 // 256KB for MP3 (ID3v2 tags are at beginning)
            mimeType.contains("ogg") -> 1 * 1024 * 1024 // 1MB for OGG (Vorbis comments in early pages)
            mimeType.contains("mp4") || mimeType.contains("m4a") -> 5 * 1024 * 1024
            else -> 2 * 1024 * 1024 // 2MB default
        }

        return try {
            Log.d(TAG, "Start to download partial data for $fileName")
            val audioData = downloadPartialFile(driveService, fileId, chunkSize)
            Log.d(TAG, "Start to extract metadata for $fileName")
            extractMetadataFromBytes(audioData, fileName, mimeType)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract metadata for $fileName", e)
            null
        }
    }

    private fun extractMetadataFromBytes(
        audioData: ByteArray,
        fileName: String,
        mimeType: String
    ): MusicMetadata? {
        return try {
            val extension = when {
                mimeType.contains("ogg") -> ".ogg"
                mimeType.contains("mp3") || mimeType.contains("mpeg") -> ".mp3"
                mimeType.contains("mp4") -> ".mp4"
                mimeType.contains("m4a") -> ".m4a"
                else -> ".tmp"
            }

            val tempFile = java.io.File.createTempFile("audio_temp", extension)
            tempFile.writeBytes(audioData)

            val metadata = when {
                mimeType.contains("mp3") || mimeType.contains("mpeg") -> {
                    extractMP3Metadata(tempFile, fileName)
                }
                mimeType.contains("ogg") -> {
                    extractOGGMetadata(tempFile, fileName)
                }
                else -> {
                    extractGenericMetadata(tempFile, fileName)
                }
            }

            tempFile.delete()
            metadata

        } catch (e: Exception) {
            Log.d(TAG, "Failed to extract metadata for $fileName", e)
            null
        }
    }

    private fun extractMP3Metadata(tempFile: java.io.File, fileName: String): MusicMetadata? {
        return try {
            val mp3Metadata = PartialMP3MetadataReader.readPartialMP3Metadata(tempFile)
            if (mp3Metadata != null) {
                val trackNumber = try {
                    mp3Metadata.trackNumber?.split("/")?.firstOrNull()?.toIntOrNull()
                } catch (e: Exception) {
                    null
                }

                MusicMetadata(
                    artist = mp3Metadata.artist,
                    album = mp3Metadata.album,
                    title = mp3Metadata.title,
                    duration = 0L, // Duration not available from partial MP3
                    trackNumber = trackNumber,
                    artwork = mp3Metadata.artwork,
                    isComplete = mp3Metadata.artist != null || mp3Metadata.title != null
                )
            } else {
                // Fallback to AudioFileIO
                extractGenericMetadata(tempFile, fileName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract MP3 metadata for $fileName, falling back to generic", e)
            extractGenericMetadata(tempFile, fileName)
        }
    }

    private fun extractOGGMetadata(tempFile: java.io.File, fileName: String): MusicMetadata? {
        return try {
            val oggMetadata = PartialOGGMetadataReader.readPartialOGGMetadata(tempFile)
            if (oggMetadata != null) {
                val trackNumber = try {
                    oggMetadata.trackNumber?.split("/")?.firstOrNull()?.toIntOrNull()
                } catch (e: Exception) {
                    null
                }

                MusicMetadata(
                    artist = oggMetadata.artist,
                    album = oggMetadata.album,
                    title = oggMetadata.title,
                    duration = 0L, // Duration not available from partial OGG
                    trackNumber = trackNumber,
                    artwork = oggMetadata.artwork,
                    isComplete = oggMetadata.artist != null || oggMetadata.title != null
                )
            } else {
                // Fallback to AudioFileIO
                extractGenericMetadata(tempFile, fileName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract OGG metadata for $fileName, falling back to generic", e)
            extractGenericMetadata(tempFile, fileName)
        }
    }

    private fun extractGenericMetadata(tempFile: java.io.File, fileName: String): MusicMetadata? {
        return try {
            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tag
            val audioHeader = audioFile.audioHeader

            val durationInSeconds = audioHeader?.trackLength ?: 0
            val durationInMs = durationInSeconds * 1000L

            // Extract artwork if available
            val artwork = try {
                tag?.firstArtwork?.binaryData
            } catch (e: Exception) {
                Log.w(TAG, "Failed to extract artwork from $fileName", e)
                null
            }

            // Extract track number if available
            val trackNumber = try {
                tag?.getFirst(FieldKey.TRACK)?.toIntOrNull()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to extract track number from $fileName", e)
                null
            }

            MusicMetadata(
                artist = tag?.getFirst(FieldKey.ARTIST),
                album = tag?.getFirst(FieldKey.ALBUM),
                title = tag?.getFirst(FieldKey.TITLE),
                duration = durationInMs,
                trackNumber = trackNumber,
                artwork = artwork,
                isComplete = tag != null
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract generic metadata for $fileName", e)
            null
        }
    }

    private suspend fun downloadPartialFile(
        driveService: Drive,
        fileId: String,
        chunkSize: Int
    ): ByteArray {
        return withContext(Dispatchers.IO) {
            val request = driveService.files().get(fileId)
            request.requestHeaders.range = "bytes=0-${chunkSize - 1}"

            val response = request.executeMedia()
            val inputStream = response.content

            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var totalBytesRead = 0
            val maxBytes = chunkSize

            while (totalBytesRead < maxBytes) {
                val bytesToRead = minOf(buffer.size, maxBytes - totalBytesRead)
                val bytesRead = inputStream.read(buffer, 0, bytesToRead)
                if (bytesRead == -1) break

                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
            }

            inputStream.close()
            outputStream.close()

            outputStream.toByteArray()
        }
    }
}