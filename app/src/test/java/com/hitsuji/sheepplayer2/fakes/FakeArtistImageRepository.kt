package com.hitsuji.sheepplayer2.fakes

import android.graphics.Bitmap
import com.hitsuji.sheepplayer2.interfaces.ArtistImageRepository

class FakeArtistImageRepository : ArtistImageRepository {

    private val searchResults = mutableMapOf<String, List<String>>()
    private val images = mutableMapOf<String, Bitmap>()
    private var placeholder: Bitmap? = null

    override suspend fun searchArtistImages(artistName: String, maxImages: Int): List<String> {
        return searchResults[artistName] ?: emptyList()
    }

    override suspend fun downloadImage(imageUrl: String): Bitmap? {
        return images[imageUrl]
    }

    override fun getLoadingPlaceholderBitmap(): Bitmap? {
        return placeholder
    }

    override fun cleanup() {
        // No-op
    }
    
    // Helper methods
    fun addSearchResult(artistName: String, urls: List<String>) {
        searchResults[artistName] = urls
    }
    
    fun addImage(url: String, bitmap: Bitmap) {
        images[url] = bitmap
    }
    
    fun setPlaceholder(bitmap: Bitmap) {
        placeholder = bitmap
    }
}
