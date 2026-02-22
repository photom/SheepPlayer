package com.hitsuji.sheepplayer2.domain.usecase

import androidx.appcompat.app.AppCompatActivity
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.service.GoogleDriveResult

class SignInUseCase(private val service: GoogleDriveServiceInterface) {
    suspend operator fun invoke(activity: AppCompatActivity): GoogleDriveResult<Unit> = service.signIn(activity)
}
