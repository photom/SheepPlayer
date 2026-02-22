package com.hitsuji.sheepplayer2.interfaces

import com.hitsuji.sheepplayer2.domain.model.LibraryUpdateEvent
import kotlinx.coroutines.flow.SharedFlow

interface MusicLibraryRepository {
    val libraryUpdates: SharedFlow<LibraryUpdateEvent>
    suspend fun emitUpdate(event: LibraryUpdateEvent)
}
