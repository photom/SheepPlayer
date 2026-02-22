package com.hitsuji.sheepplayer2.interfaces

import android.graphics.Bitmap

interface ArtistImageRepository {
    suspend fun searchArtistImages(artistName: String, maxImages: Int = 10): List<String>
    suspend fun downloadImage(imageUrl: String): Bitmap?
    fun getLoadingPlaceholderBitmap(): Bitmap?
    fun cleanup()
}
