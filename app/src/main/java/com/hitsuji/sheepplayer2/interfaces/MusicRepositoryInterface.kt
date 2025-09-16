package com.hitsuji.sheepplayer2.interfaces

import com.hitsuji.sheepplayer2.Artist

interface MusicRepositoryInterface {
    suspend fun loadMusicData(): List<Artist>
}