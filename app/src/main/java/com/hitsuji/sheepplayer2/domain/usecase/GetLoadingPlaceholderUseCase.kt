package com.hitsuji.sheepplayer2.domain.usecase

import android.graphics.Bitmap
import com.hitsuji.sheepplayer2.interfaces.ArtistImageRepository

class GetLoadingPlaceholderUseCase(private val repository: ArtistImageRepository) {
    operator fun invoke(): Bitmap? {
        return repository.getLoadingPlaceholderBitmap()
    }
}
