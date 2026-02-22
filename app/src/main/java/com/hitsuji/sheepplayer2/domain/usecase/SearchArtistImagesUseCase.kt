package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.interfaces.ArtistImageRepository

class SearchArtistImagesUseCase(private val repository: ArtistImageRepository) {
    suspend operator fun invoke(artistName: String, maxImages: Int = 10): List<String> {
        return repository.searchArtistImages(artistName, maxImages)
    }
}
