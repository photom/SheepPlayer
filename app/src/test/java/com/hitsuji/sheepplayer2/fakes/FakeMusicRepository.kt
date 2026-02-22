package com.hitsuji.sheepplayer2.fakes

import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.interfaces.MusicRepositoryInterface

class FakeMusicRepository : MusicRepositoryInterface {

    private var artists: List<Artist> = emptyList()
    private var shouldThrowError: Boolean = false

    override suspend fun loadMusicData(): List<Artist> {
        if (shouldThrowError) {
            throw RuntimeException("Fake repository error")
        }
        return artists
    }

    fun setArtists(newArtists: List<Artist>) {
        this.artists = newArtists
    }

    fun setShouldThrowError(shouldThrow: Boolean) {
        this.shouldThrowError = shouldThrow
    }
}
