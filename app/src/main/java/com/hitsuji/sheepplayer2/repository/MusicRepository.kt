package com.hitsuji.sheepplayer2.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.hitsuji.sheepplayer2.Album
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.interfaces.MusicRepositoryInterface
import com.hitsuji.sheepplayer2.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) : MusicRepositoryInterface {

    override suspend fun loadMusicData(): List<Artist> = withContext(Dispatchers.IO) {
        val tracks = queryMusicFromMediaStore()
        return@withContext processAndStructureMusicData(tracks)
    }

    private suspend fun queryMusicFromMediaStore(): List<Track> = withContext(Dispatchers.IO) {
        val trackList = mutableListOf<Track>()
        val contentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder =
            "${MediaStore.Audio.Media.ARTIST} ASC, ${MediaStore.Audio.Media.ALBUM} ASC, ${MediaStore.Audio.Media.TRACK} ASC"

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: Constants.MediaStore.UNKNOWN_TITLE
                val artist = cursor.getString(artistColumn) ?: Constants.MediaStore.UNKNOWN_ARTIST
                val album = cursor.getString(albumColumn) ?: Constants.MediaStore.UNKNOWN_ALBUM
                val duration = cursor.getLong(durationColumn)
                val filePath = cursor.getString(dataColumn)

                // Validate file path for security
                if (filePath.isNullOrBlank() || !isValidAudioFile(filePath)) {
                    continue
                }
                val albumId = cursor.getLong(albumIdColumn)

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse(Constants.MediaStore.ALBUM_ART_URI_BASE),
                    albumId
                ).toString()

                trackList.add(
                    Track(id, title, artist, album, duration, filePath, albumArtUri)
                )
            }
        }
        return@withContext trackList
    }

    private fun processAndStructureMusicData(tracks: List<Track>): List<Artist> {
        val artistsMap = mutableMapOf<String, Artist>()
        val albumsMap = mutableMapOf<String, Album>()

        tracks.forEach { track ->
            val artist = artistsMap.getOrPut(track.artistName) {
                Artist(id = track.artistName.hashCode().toLong(), name = track.artistName)
            }

            val albumKey = "${track.artistName}|${track.albumName}"
            val album = albumsMap.getOrPut(albumKey) {
                Album(
                    id = track.albumName.hashCode().toLong(),
                    title = track.albumName,
                    artistName = track.artistName
                )
            }

            album.tracks.add(track)

            if (!artist.albums.contains(album)) {
                artist.albums.add(album)
            }
        }

        val allArtists = artistsMap.values.sortedBy { it.name }.toMutableList()
        allArtists.forEach { artist ->
            artist.albums.sortBy { it.title }
        }

        return allArtists
    }

    private fun isValidAudioFile(filePath: String): Boolean {
        return try {
            // Basic input validation
            if (filePath.isBlank() || filePath.length > 4096) return false

            // Security: Check for path traversal attacks
            if (filePath.contains("../") || filePath.contains("..\\") ||
                filePath.contains("//") || filePath.contains("\\\\")) {
                return false
            }

            // Security: Ensure path is within expected directories
            val normalizedPath = filePath.lowercase()
            if (!normalizedPath.startsWith("/storage/") &&
                !normalizedPath.startsWith("/sdcard/") &&
                !normalizedPath.startsWith("/data/media/")) {
                return false
            }

            // Validate file extension
            val validExtensions = setOf(".mp3", ".m4a", ".wav", ".flac", ".ogg", ".aac")
            val extension = filePath.substringAfterLast(".", "").lowercase()

            // Security: Additional character validation
            val allowedChars = Regex("[a-zA-Z0-9._/\\-\\s]+")
            return validExtensions.contains(".$extension") &&
                   allowedChars.matches(filePath)
        } catch (e: Exception) {
            android.util.Log.w("MusicRepository", "File validation error for: $filePath", e)
            false
        }
    }
}