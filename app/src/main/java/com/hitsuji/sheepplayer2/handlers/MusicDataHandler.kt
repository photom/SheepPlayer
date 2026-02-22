package com.hitsuji.sheepplayer2.handlers

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.domain.usecase.GetMusicLibraryUseCase
import com.hitsuji.sheepplayer2.interfaces.FragmentNotifier
import com.hitsuji.sheepplayer2.service.MetadataLoadingService
import kotlinx.coroutines.launch

class MusicDataHandler(
    private val context: Context,
    private val getMusicLibraryUseCase: GetMusicLibraryUseCase,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val fragmentNotifier: FragmentNotifier
) {
    
    interface MusicDataCallback {
        fun onLocalMusicLoaded(artists: List<Artist>)
        fun onGoogleDriveMusicLoaded(artists: List<Artist>)
        fun onMusicLoadError(message: String)
        fun onNoMusicFound()
    }
    
    private var callback: MusicDataCallback? = null
    private val allArtists = mutableListOf<Artist>()
    
    fun setCallback(callback: MusicDataCallback) {
        this.callback = callback
    }
    
    fun getAllArtists(): List<Artist> = allArtists.toList()
    
    fun loadLocalMusicData() {
        lifecycleScope.launch {
            try {
                // GetMusicLibraryUseCase handles merging local and cached remote data
                val artists = getMusicLibraryUseCase()
                allArtists.clear()
                allArtists.addAll(artists)
                
                val trackCount = allArtists.sumOf { it.albums.sumOf { it.tracks.size } }
                Log.d("MusicDataHandler", "Found $trackCount tracks by ${allArtists.size} artists.")
                
                if (allArtists.isEmpty()) {
                    callback?.onNoMusicFound()
                } else {
                    callback?.onLocalMusicLoaded(allArtists.toList())
                    fragmentNotifier.notifyDataLoaded()
                }
            } catch (e: Exception) {
                Log.e("MusicDataHandler", "Failed to load music data", e)
                callback?.onMusicLoadError("Failed to load music files: ${e.message}")
            }
        }
    }
    
    fun refreshGoogleDriveMusic() {
        Log.d("MusicDataHandler", "*** refreshGoogleDriveMusic() called ***")
        lifecycleScope.launch {
            try {
                // Initial load from whatever is currently available (use case handles this)
                val artists = getMusicLibraryUseCase()
                allArtists.clear()
                allArtists.addAll(artists)

                Log.d("MusicDataHandler", "Current library has ${allArtists.size} artists")

                // Notify UI with current music first
                fragmentNotifier.notifyDataLoaded()

                // Start the metadata loading service to fetch fresh remote data
                Log.d("MusicDataHandler", "*** Starting metadata loading service ***")
                MetadataLoadingService.startService(context)

            } catch (e: Exception) {
                Log.e("MusicDataHandler", "Error starting Google Drive music loading", e)
                callback?.onMusicLoadError("Error starting Google Drive music loading: ${e.message}")
            }
        }
    }
    
    fun updateWithGoogleDriveData() {
        lifecycleScope.launch {
            try {
                // Refresh data using the use case (it will pick up newly cached remote data)
                val updatedArtists = getMusicLibraryUseCase()
                
                allArtists.clear()
                allArtists.addAll(updatedArtists)

                // Notify fragments about data update
                fragmentNotifier.notifyDataLoaded()
                
                val totalTracks = allArtists.sumOf { it.albums.sumOf { album -> album.tracks.size } }
                Log.d("MusicDataHandler", "Updated music library via Use Case: ${allArtists.size} artists, $totalTracks tracks")
                
                // We don't have the specific Google Drive count easily here without filtering, 
                // but the Use Case has done the heavy lifting of merging.
                callback?.onGoogleDriveMusicLoaded(updatedArtists.filter { it.albums.any { it.tracks.any { it.googleDriveFileId != null } } })

            } catch (e: Exception) {
                Log.e("MusicDataHandler", "Error updating with latest Google Drive data", e)
                callback?.onMusicLoadError("Error updating Google Drive data: ${e.message}")
            }
        }
    }
}
