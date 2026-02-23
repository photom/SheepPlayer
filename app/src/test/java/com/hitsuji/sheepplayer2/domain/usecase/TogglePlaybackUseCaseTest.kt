package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.fakes.FakePlaybackManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TogglePlaybackUseCaseTest {

    private lateinit var fakePlaybackManager: FakePlaybackManager
    private lateinit var useCase: TogglePlaybackUseCase

    @Before
    fun setup() {
        fakePlaybackManager = FakePlaybackManager()
        useCase = TogglePlaybackUseCase(fakePlaybackManager)
    }

    @Test
    fun `invoke returns false when no track is loaded`() {
        assertFalse(useCase())
    }

    @Test
    fun `invoke toggles playback state when track is loaded`() {
        val track = Track(1, "Title", "Artist", "Album", 1000, "/path/song.mp3")
        fakePlaybackManager.playTrack(track)
        
        assertTrue(fakePlaybackManager.isPlaying)
        
        useCase()
        assertFalse(fakePlaybackManager.isPlaying)
        
        useCase()
        assertTrue(fakePlaybackManager.isPlaying)
    }
}
