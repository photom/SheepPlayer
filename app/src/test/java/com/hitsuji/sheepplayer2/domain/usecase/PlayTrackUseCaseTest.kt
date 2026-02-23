package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.fakes.FakeGoogleDriveService
import com.hitsuji.sheepplayer2.fakes.FakePlaybackManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PlayTrackUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakePlaybackManager: FakePlaybackManager
    private lateinit var fakeGoogleDriveService: FakeGoogleDriveService
    private lateinit var cacheDir: File
    private lateinit var useCase: PlayTrackUseCase

    @Before
    fun setup() {
        fakePlaybackManager = FakePlaybackManager()
        fakeGoogleDriveService = FakeGoogleDriveService()
        cacheDir = tempFolder.newFolder("cache")
        useCase = PlayTrackUseCase(fakePlaybackManager, fakeGoogleDriveService, cacheDir)
    }

    @Test
    fun `invoke plays local track directly`() = runBlocking {
        val track = Track(1, "Local", "Artist", "Album", 1000, "/path/song.mp3")
        
        useCase(track)
        
        assertEquals(track, fakePlaybackManager.currentPlayingTrack)
        assertTrue(fakePlaybackManager.isPlaying)
    }

    @Test
    fun `invoke downloads and plays Google Drive track`() = runBlocking {
        val fileId = "file123"
        val track = Track(2, "Cloud", "Artist", "Album", 2000, "gdrive://$fileId")
        val fileData = "fake audio data".toByteArray()
        fakeGoogleDriveService.addFile(fileId, fileData)
        
        useCase(track)
        
        val expectedCacheFile = File(cacheDir, "gdrive_${fileId}.mp3")
        assertTrue("Cache file should exist", expectedCacheFile.exists())
        assertEquals("File content should match", String(fileData), expectedCacheFile.readText())
        
        val playingTrack = fakePlaybackManager.currentPlayingTrack
        assertEquals(track.id, playingTrack?.id)
        assertEquals(expectedCacheFile.absolutePath, playingTrack?.filePath)
        assertTrue(fakePlaybackManager.isPlaying)
    }

    @Test
    fun `invoke uses cached file if it exists`() = runBlocking {
        val fileId = "file456"
        val track = Track(3, "Cached", "Artist", "Album", 3000, "gdrive://$fileId")
        val expectedCacheFile = File(cacheDir, "gdrive_${fileId}.mp3")
        val cachedData = "pre-cached data"
        expectedCacheFile.writeText(cachedData)
        
        // Google Drive service would fail if it tried to download
        // (FakeGoogleDriveService returns error if file not found)
        
        useCase(track)
        
        assertEquals(expectedCacheFile.absolutePath, fakePlaybackManager.currentPlayingTrack?.filePath)
        assertEquals(cachedData, File(fakePlaybackManager.currentPlayingTrack!!.filePath).readText())
    }
}
