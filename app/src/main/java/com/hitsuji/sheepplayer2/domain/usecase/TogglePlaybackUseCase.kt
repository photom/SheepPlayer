package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.interfaces.PlaybackManagerInterface

class TogglePlaybackUseCase(
    private val musicPlayerManager: PlaybackManagerInterface
) {
    operator fun invoke(): Boolean {
        return musicPlayerManager.togglePlayback()
    }
}
