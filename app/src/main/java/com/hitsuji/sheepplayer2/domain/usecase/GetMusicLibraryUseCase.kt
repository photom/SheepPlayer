package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.Album
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.interfaces.MusicRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetMusicLibraryUseCase(
    private val musicRepository: MusicRepositoryInterface,
    private val googleDriveService: GoogleDriveServiceInterface
) {
    suspend operator fun invoke(): List<Artist> = withContext(Dispatchers.IO) {
        val localArtists = musicRepository.loadMusicData()
        val googleDriveArtists = try {
             googleDriveService.getLatestGoogleDriveArtists()
        } catch (e: Exception) {
            emptyList()
        }
        
        if (googleDriveArtists.isEmpty()) return@withContext localArtists

        val artistMap = mutableMapOf<String, Artist>()

        // Process local artists first
        localArtists.forEach { artist ->
            artistMap[artist.name.lowercase()] = artist
        }

        // Merge Google Drive artists
        googleDriveArtists.forEach { gdArtist ->
            val key = gdArtist.name.lowercase()
            val existingArtist = artistMap[key]

            if (existingArtist != null) {
                // Merge albums into existing artist
                gdArtist.albums.forEach { gdAlbum ->
                    val existingAlbum = existingArtist.albums.find { 
                        it.title.lowercase() == gdAlbum.title.lowercase() 
                    }

                    if (existingAlbum != null) {
                        // Merge tracks into existing album
                        gdAlbum.tracks.forEach { gdTrack ->
                            if (existingAlbum.tracks.none { 
                                it.title.lowercase() == gdTrack.title.lowercase() 
                            }) {
                                existingAlbum.tracks.add(gdTrack)
                            }
                        }
                        // Sort tracks by track number if available
                        existingAlbum.tracks.sortBy { it.trackNumber ?: 0 }
                    } else {
                        // Add new album to existing artist
                        existingArtist.albums.add(gdAlbum)
                    }
                }
                // Sort albums by title
                existingArtist.albums.sortBy { it.title.lowercase() }
            } else {
                // Add new artist from Google Drive
                artistMap[key] = gdArtist
            }
        }

        artistMap.values.sortedBy { it.name.lowercase() }
    }
}
