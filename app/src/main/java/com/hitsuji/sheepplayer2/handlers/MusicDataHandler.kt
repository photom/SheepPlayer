package com.hitsuji.sheepplayer2.handlers

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.interfaces.FragmentNotifier
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.interfaces.MusicRepositoryInterface
import com.hitsuji.sheepplayer2.service.MetadataLoadingService
import kotlinx.coroutines.launch

class MusicDataHandler(
    private val context: Context,
    private val musicRepository: MusicRepositoryInterface,
    private val googleDriveService: GoogleDriveServiceInterface,
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
                val artists = musicRepository.loadMusicData()
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
        lifecycleScope.launch {
            try {
                // Load local music first
                allArtists.clear()
                val localArtists = musicRepository.loadMusicData()
                allArtists.addAll(localArtists)
                
                // Notify UI with local music first
                fragmentNotifier.notifyDataLoaded()
                
                // Start the metadata loading service
                Log.d("MusicDataHandler", "Starting metadata loading service")
                MetadataLoadingService.startService(context)
                
            } catch (e: Exception) {
                Log.e("MusicDataHandler", "Error starting Google Drive music loading", e)
                callback?.onMusicLoadError("Error starting Google Drive music loading: ${e.message}")
            }
        }
    }
    
    fun updateWithGoogleDriveData() {
        try {
            val googleDriveArtists = googleDriveService.getLatestGoogleDriveArtists()
            Log.d("MusicDataHandler", "Got latest cached data: ${googleDriveArtists.size} artists")
            
            if (googleDriveArtists.isNotEmpty()) {
                // Remove existing Google Drive tracks (identified by googleDriveFileId)
                val localOnlyArtists = allArtists.map { artist ->
                    artist.copy(
                        albums = artist.albums.map { album ->
                            album.copy(
                                tracks = album.tracks.filter { track -> 
                                    track.googleDriveFileId == null 
                                }.toMutableList()
                            )
                        }.filter { it.tracks.isNotEmpty() }.toMutableList()
                    )
                }.filter { it.albums.isNotEmpty() }.toMutableList()
                
                // Merge local and updated Google Drive music
                allArtists.clear()
                allArtists.addAll(localOnlyArtists)
                allArtists.addAll(googleDriveArtists)
                
                // Notify fragments about data update
                fragmentNotifier.notifyDataLoaded()
                callback?.onGoogleDriveMusicLoaded(googleDriveArtists)
                
                val totalTracks = allArtists.sumOf { it.albums.sumOf { album -> album.tracks.size } }
                val googleDriveTracks = googleDriveArtists.sumOf { it.albums.sumOf { album -> album.tracks.size } }
                Log.d("MusicDataHandler", "Updated music library: ${allArtists.size} artists, $totalTracks tracks ($googleDriveTracks from Google Drive)")
            }
        } catch (e: Exception) {
            Log.e("MusicDataHandler", "Error updating with latest Google Drive data", e)
            callback?.onMusicLoadError("Error updating Google Drive data: ${e.message}")
        }
    }
}