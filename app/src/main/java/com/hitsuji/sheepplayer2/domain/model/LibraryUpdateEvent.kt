package com.hitsuji.sheepplayer2.domain.model

import com.hitsuji.sheepplayer2.Artist

sealed class LibraryUpdateEvent {
    data class Progress(val artists: List<Artist>, val progress: Int, val total: Int) : LibraryUpdateEvent()
    data class Success(val artists: List<Artist>) : LibraryUpdateEvent()
    data class Error(val message: String) : LibraryUpdateEvent()
}
