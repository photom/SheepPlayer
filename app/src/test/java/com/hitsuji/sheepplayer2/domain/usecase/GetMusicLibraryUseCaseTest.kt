package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.Album
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.fakes.FakeGoogleDriveService
import com.hitsuji.sheepplayer2.fakes.FakeMusicRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMusicLibraryUseCaseTest {

    @Test
    fun `invoke merges local and remote artists by name`() = runBlocking {
        // Arrange
        val fakeRepo = FakeMusicRepository()
        val fakeDrive = FakeGoogleDriveService()

        fakeRepo.setArtists(listOf(
            Artist(1, "Artist A", mutableListOf(
                Album(1, "Album 1", "Artist A", mutableListOf(
                    Track(1, "Track 1", "Artist A", "Album 1", 100, "/local/1")
                ))
            ))
        ))

        fakeDrive.setArtists(listOf(
            Artist(2, "Artist A", mutableListOf(
                Album(2, "Album 1", "Artist A", mutableListOf(
                    Track(2, "Track 2", "Artist A", "Album 1", 200, "gdrive://2")
                )),
                Album(3, "Album 2", "Artist A", mutableListOf(
                    Track(3, "Track 3", "Artist A", "Album 2", 300, "gdrive://3")
                ))
            )),
            Artist(4, "Artist B", mutableListOf(
                Album(4, "Album 3", "Artist B", mutableListOf(
                    Track(4, "Track 4", "Artist B", "Album 3", 400, "gdrive://4")
                ))
            ))
        ))

        val useCase = GetMusicLibraryUseCase(fakeRepo, fakeDrive)

        // Act
        val result = useCase()

        // Assert
        assertEquals(2, result.size)
        
        val artistA = result.find { it.name == "Artist A" }!!
        assertEquals(2, artistA.albums.size)
        
        val album1 = artistA.albums.find { it.title == "Album 1" }!!
        assertEquals(2, album1.tracks.size)
        assertEquals("/local/1", album1.tracks[0].filePath)
        assertEquals("gdrive://2", album1.tracks[1].filePath)
        
        val album2 = artistA.albums.find { it.title == "Album 2" }!!
        assertEquals(1, album2.tracks.size)
        
        val artistB = result.find { it.name == "Artist B" }!!
        assertEquals(1, artistB.albums.size)
    }
}
