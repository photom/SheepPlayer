package com.hitsuji.sheepplayer2.domain.usecase

import android.graphics.Bitmap
import com.hitsuji.sheepplayer2.interfaces.ArtistImageRepository

class DownloadArtistImageUseCase(private val repository: ArtistImageRepository) {
    suspend operator fun invoke(imageUrl: String): Bitmap? {
        return repository.downloadImage(imageUrl)
    }
}
