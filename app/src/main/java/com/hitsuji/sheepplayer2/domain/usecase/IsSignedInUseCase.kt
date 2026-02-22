package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface

class IsSignedInUseCase(private val service: GoogleDriveServiceInterface) {
    operator fun invoke(): Boolean = service.isSignedIn()
}
