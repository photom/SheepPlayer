package com.hitsuji.sheepplayer2.ui.pictures

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuji.sheepplayer2.domain.usecase.DownloadArtistImageUseCase
import com.hitsuji.sheepplayer2.domain.usecase.GetLoadingPlaceholderUseCase
import com.hitsuji.sheepplayer2.domain.usecase.SearchArtistImagesUseCase
import kotlinx.coroutines.launch

class PicturesViewModel(
    private val searchArtistImagesUseCase: SearchArtistImagesUseCase,
    private val downloadArtistImageUseCase: DownloadArtistImageUseCase,
    private val getLoadingPlaceholderUseCase: GetLoadingPlaceholderUseCase
) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = ""
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

    private var currentArtistName: String? = null

    fun loadArtistImages(artistName: String, forceReload: Boolean = false) {
        if (currentArtistName == artistName && !forceReload) {
            return
        }

        currentArtistName = artistName
        _text.value = ""
        _isLoading.value = true

        downloadedImages.clear()
        _images.value = emptyList()

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _addAnimatedPlaceholder.value = true
                placeholderActive = true

                val imageUrls = searchArtistImagesUseCase(artistName, 50)
                allImageUrls = imageUrls
                currentUrlIndex = 0

                if (imageUrls.isEmpty()) {
                    _isLoading.value = false
                    return@launch
                }

                kotlinx.coroutines.delay(500)

                var urlIndex = 0
                while (downloadedImages.size < maxDisplayImages && urlIndex < imageUrls.size) {
                    val url = imageUrls[urlIndex]
                    try {
                        val bitmap = downloadArtistImageUseCase(url)
                        if (bitmap != null) {
                            downloadedImages.add(bitmap)
                            currentUrlIndex = urlIndex + 1
                            
                            if (placeholderActive) {
                                _replacePlaceholder.value = bitmap
                                placeholderActive = false
                            } else {
                                _newImage.value = bitmap
                            }
                            _images.value = downloadedImages.toList()
                            kotlinx.coroutines.delay(300)
                        }
                    } catch (e: Exception) {
                        Log.e("PicturesViewModel", "Failed to download image: $url", e)
                    }
                    urlIndex++
                }
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("PicturesViewModel", "Failed to load artist images for $artistName", e)
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
        _text.value = ""
        _isLoading.value = false
        allImageUrls = emptyList()
        currentUrlIndex = 0
        isLoadingCircularBuffer = false
    }

    fun searchCustomArtist(customArtistName: String) {
        if (customArtistName.isBlank()) return
        clearImages()
        loadArtistImages(customArtistName.trim(), forceReload = true)
    }

    fun loadNextImageForCircularBuffer() {
        if (currentUrlIndex >= allImageUrls.size || downloadedImages.size < maxDisplayImages || isLoadingCircularBuffer) {
            return
        }

        isLoadingCircularBuffer = true
        viewModelScope.launch {
            try {
                val nextUrl = allImageUrls[currentUrlIndex]
                val bitmap = downloadArtistImageUseCase(nextUrl)
                if (bitmap != null) {
                    if (downloadedImages.isNotEmpty()) {
                        downloadedImages.removeAt(0)
                    }
                    downloadedImages.add(bitmap)
                    _circularBufferUpdate.value = bitmap
                    _images.value = downloadedImages.toList()
                    currentUrlIndex++
                } else {
                    currentUrlIndex++
                }
            } catch (e: Exception) {
                Log.e("PicturesViewModel", "Error in circular buffer loading", e)
                currentUrlIndex++
            } finally {
                isLoadingCircularBuffer = false
            }
        }
    }
}
