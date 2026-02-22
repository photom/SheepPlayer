package com.hitsuji.sheepplayer2.repository

import com.hitsuji.sheepplayer2.domain.model.LibraryUpdateEvent
import com.hitsuji.sheepplayer2.interfaces.MusicLibraryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MusicLibraryRepositoryImpl : MusicLibraryRepository {
    private val _libraryUpdates = MutableSharedFlow<LibraryUpdateEvent>(replay = 1)
    override val libraryUpdates: SharedFlow<LibraryUpdateEvent> = _libraryUpdates.asSharedFlow()

    override suspend fun emitUpdate(event: LibraryUpdateEvent) {
        _libraryUpdates.emit(event)
    }
}
