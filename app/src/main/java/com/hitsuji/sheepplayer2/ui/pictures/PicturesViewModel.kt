package com.hitsuji.sheepplayer2.ui.pictures

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hitsuji.sheepplayer2.service.ArtistImageService
import kotlinx.coroutines.launch

class PicturesViewModel(application: Application) : AndroidViewModel(application) {

    private val _text = MutableLiveData<String>().apply {
        value = ""  // Empty by default - no top description text
    }
    val text: LiveData<String> = _text

    private val _images = MutableLiveData<List<Bitmap>>().apply {
        value = emptyList()
    }
    val images: LiveData<List<Bitmap>> = _images

    private val _newImage = MutableLiveData<Bitmap?>()
    val newImage: LiveData<Bitmap?> = _newImage

    private val _updatePlaceholder = MutableLiveData<Bitmap?>()
    val updatePlaceholder: LiveData<Bitmap?> = _updatePlaceholder

    private val _addAnimatedPlaceholder = MutableLiveData<Boolean>()
    val addAnimatedPlaceholder: LiveData<Boolean> = _addAnimatedPlaceholder

    private val _replacePlaceholder = MutableLiveData<Bitmap?>()
    val replacePlaceholder: LiveData<Bitmap?> = _replacePlaceholder

    private var placeholderActive = false

    private val downloadedImages = mutableListOf<Bitmap>()
    private var allImageUrls = listOf<String>()
    private var currentUrlIndex = 0
    private val maxDisplayImages = 10
    private var isLoadingCircularBuffer = false

    private val _isLoading = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isLoading: LiveData<Boolean> = _isLoading

    private val _circularBufferUpdate = MutableLiveData<Bitmap?>()
    val circularBufferUpdate: LiveData<Bitmap?> = _circularBufferUpdate

    private val artistImageService = ArtistImageService(getApplication())
    private var currentArtistName: String? = null

    fun loadArtistImages(artistName: String, forceReload: Boolean = false) {
        if (currentArtistName == artistName && !forceReload) {
            return // Already loaded for this artist
        }

        currentArtistName = artistName
        _text.value = ""  // Clear top text
        _isLoading.value = true

        // Clear previous images when changing artists
        downloadedImages.clear()
        _images.value = emptyList()

        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Add single animated GIF placeholder at bottom
                _addAnimatedPlaceholder.value = true
                placeholderActive = true

                val imageUrls = artistImageService.searchArtistImages(
                    artistName,
                    50
                ) // Get more URLs for circular buffer
                allImageUrls = imageUrls
                currentUrlIndex = 0

                if (imageUrls.isEmpty()) {
                    // Keep the sheep placeholder visible (no updates needed)
                    _isLoading.value = false
                    return@launch
                }

                // Keep the sheep placeholder as-is (no text updates needed)
                kotlinx.coroutines.delay(500) // Brief delay before starting downloads

                var successCount = 0
                var failCount = 0
                var urlIndex = 0

                // Download aggressively until we get 10 images or exhaust all URLs
                while (downloadedImages.size < maxDisplayImages && urlIndex < imageUrls.size) {
                    val url = imageUrls[urlIndex]

                    // Keep the sheep placeholder unchanged during download

                    try {
                        Log.d("PicturesViewModel", "Attempting to download: $url")

                        val bitmap = artistImageService.downloadImage(url)
                        if (bitmap != null) {
                            downloadedImages.add(bitmap)
                            currentUrlIndex = urlIndex + 1 // Track position for circular buffer
                            successCount++

                            if (placeholderActive) {
                                // First image - replace the placeholder with real image
                                Log.d(
                                    "PicturesViewModel",
                                    "Replacing placeholder with first downloaded image"
                                )
                                _replacePlaceholder.value = bitmap
                                placeholderActive = false
                            } else {
                                // Subsequent images - add at bottom
                                Log.d("PicturesViewModel", "Adding subsequent image at bottom")
                                _newImage.value = bitmap
                            }

                            // Also update the full list
                            _images.value = downloadedImages.toList()

                            // Short delay to show sequential loading effect
                            kotlinx.coroutines.delay(300)
                        } else {
                            failCount++
                            Log.w(
                                "PicturesViewModel",
                                "Failed to download image from URL ${urlIndex + 1}: null bitmap"
                            )
                        }
                    } catch (e: Exception) {
                        failCount++
                        Log.e(
                            "PicturesViewModel",
                            "Failed to download image from URL ${urlIndex + 1}: $url",
                            e
                        )
                    }
                    urlIndex++
                }

                _isLoading.value = false

                // If no images downloaded, keep the sheep placeholder visible
                if (downloadedImages.isEmpty()) {
                    // Placeholder remains showing the sheep GIF
                }

            } catch (e: Exception) {
                Log.e("PicturesViewModel", "Failed to load artist images for $artistName", e)
                // Keep the sheep placeholder visible on error
                _isLoading.value = false
            }
        }
    }

    fun clearImages() {
        currentArtistName = null
        downloadedImages.clear()
        _images.value = emptyList()
        _newImage.value = null
        _updatePlaceholder.value = null
        _addAnimatedPlaceholder.value = false
        _replacePlaceholder.value = null
        _circularBufferUpdate.value = null
        placeholderActive = false
        _text.value = ""  // Keep text empty
        _isLoading.value = false
        allImageUrls = emptyList()
        currentUrlIndex = 0
        isLoadingCircularBuffer = false
    }

    /**
     * Searches for images with custom artist name from user input
     * Resets the current image list and starts fresh search
     */
    fun searchCustomArtist(customArtistName: String) {
        if (customArtistName.isBlank()) {
            return
        }

        Log.d("PicturesViewModel", "Starting custom search for: $customArtistName")

        // Clear current images and reset state
        clearImages()

        // Load images for the custom artist name (force reload)
        loadArtistImages(customArtistName.trim(), forceReload = true)
    }

    /**
     * Loads next image for circular buffer when user scrolls to bottom
     * Removes top image and adds new image at bottom
     */
    fun loadNextImageForCircularBuffer() {
        if (currentUrlIndex >= allImageUrls.size || downloadedImages.size < maxDisplayImages || isLoadingCircularBuffer) {
            Log.d(
                "PicturesViewModel",
                "No more URLs available, buffer not full, or already loading"
            )
            return
        }

        isLoadingCircularBuffer = true

        viewModelScope.launch {
            try {
                val nextUrl = allImageUrls[currentUrlIndex]
                Log.d("PicturesViewModel", "Loading next image for circular buffer: $nextUrl")

                val bitmap = artistImageService.downloadImage(nextUrl)
                if (bitmap != null) {
                    // Perform circular buffer operation: remove top, add bottom
                    if (downloadedImages.isNotEmpty()) {
                        downloadedImages.removeAt(0)
                    }
                    downloadedImages.add(bitmap)

                    // Trigger single update event
                    _circularBufferUpdate.value = bitmap

                    // Update full list and increment index
                    _images.value = downloadedImages.toList()
                    currentUrlIndex++

                    Log.d(
                        "PicturesViewModel",
                        "Circular buffer updated. Current index: $currentUrlIndex"
                    )
                } else {
                    Log.w("PicturesViewModel", "Failed to download next image for circular buffer")
                    currentUrlIndex++ // Skip failed URL
                }
            } catch (e: Exception) {
                Log.e("PicturesViewModel", "Error in circular buffer loading", e)
                currentUrlIndex++ // Skip failed URL
            } finally {
                isLoadingCircularBuffer = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        artistImageService.cleanup()
    }
}