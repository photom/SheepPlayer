package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.service.GoogleDriveResult

class SignOutUseCase(private val service: GoogleDriveServiceInterface) {
    suspend operator fun invoke(): GoogleDriveResult<Unit> = service.signOut()
}
