package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface

class GetAccountEmailUseCase(private val service: GoogleDriveServiceInterface) {
    operator fun invoke(): String? = service.getCurrentAccount()?.email
}
