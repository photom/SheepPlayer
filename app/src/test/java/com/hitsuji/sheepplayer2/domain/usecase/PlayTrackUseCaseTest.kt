package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.fakes.FakeGoogleDriveService
import com.hitsuji.sheepplayer2.fakes.FakePlaybackManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PlayTrackUseCaseTest {

    private lateinit var playTrackUseCase: PlayTrackUseCase
    private lateinit var fakePlaybackManager: FakePlaybackManager
    private lateinit var fakeGoogleDriveService: FakeGoogleDriveService
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        fakePlaybackManager = FakePlaybackManager()
        fakeGoogleDriveService = FakeGoogleDriveService()
        // Create a temp directory for cache
        cacheDir = Files.createTempDirectory("sheepplayer_test_cache").toFile()
        cacheDir.deleteOnExit()
        
        // Setup fake google drive service
        fakeGoogleDriveService.addFile("valid_id", "fake_data".toByteArray())
        
        playTrackUseCase = PlayTrackUseCase(
            fakePlaybackManager,
            fakeGoogleDriveService,
            cacheDir
        )
    }

    @Test
    fun `invoke plays local track directly`() = runBlocking {
        val localTrack = Track(
            id = 1,
            title = "Local Song",
            artistName = "Artist",
            albumName = "Album",
            duration = 1000,
            filePath = "/storage/emulated/0/Music/song.mp3"
        )

        playTrackUseCase(localTrack)

        assertEquals(localTrack, fakePlaybackManager.currentPlayingTrack)
    }

    @Test
    fun `invoke downloads and plays google drive track`() = runBlocking {
        val fileId = "valid_id"
        val remoteTrack = Track(
            id = 2,
            title = "Remote Song",
            artistName = "Artist",
            albumName = "Album",
            duration = 1000,
            filePath = "gdrive://$fileId"
        )

        playTrackUseCase(remoteTrack)

        val playedTrack = fakePlaybackManager.currentPlayingTrack
        // Track should be played
        assertTrue("Track was not played", playedTrack != null)
        // Path should be local cache path
        val expectedPath = File(cacheDir, "gdrive_$fileId.tmp").absolutePath
        assertEquals(expectedPath, playedTrack?.filePath)
        // File should exist and have content
        val cachedFile = File(expectedPath)
        assertTrue("Cached file does not exist", cachedFile.exists())
        assertEquals("fake_data", cachedFile.readText())
    }
}
