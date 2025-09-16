package com.hitsuji.sheepplayer2.factory

import android.content.Context
import com.hitsuji.sheepplayer2.MusicPlayer
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.interfaces.MusicPlayerInterface
import com.hitsuji.sheepplayer2.interfaces.MusicRepositoryInterface
import com.hitsuji.sheepplayer2.repository.MusicRepository
import com.hitsuji.sheepplayer2.service.GoogleDriveService

/**
 * Factory class for creating dependencies following Dependency Inversion Principle.
 * This class centralizes object creation and ensures proper dependency injection.
 * 
 * Benefits:
 * - Single place to configure dependencies
 * - Easy to swap implementations for testing
 * - Follows the Factory pattern for object creation
 * - Supports dependency injection without a DI framework
 */
object DependencyFactory {
    
    fun createMusicRepository(context: Context): MusicRepositoryInterface {
        return MusicRepository(context)
    }
    
    fun createGoogleDriveService(context: Context): GoogleDriveServiceInterface {
        return GoogleDriveService(context)
    }
    
    fun createMusicPlayer(
        context: Context, 
        googleDriveService: GoogleDriveServiceInterface
    ): MusicPlayerInterface {
        val musicPlayer = MusicPlayer(context)
        musicPlayer.setGoogleDriveService(googleDriveService)
        return musicPlayer
    }
    
    /**
     * Creates a music player specifically for testing with mock dependencies
     */
    fun createTestMusicPlayer(
        context: Context,
        mockGoogleDriveService: GoogleDriveServiceInterface? = null
    ): MusicPlayerInterface {
        val musicPlayer = MusicPlayer(context)
        mockGoogleDriveService?.let { musicPlayer.setGoogleDriveService(it) }
        return musicPlayer
    }
    
    /**
     * Creates all core dependencies in the correct order
     */
    fun createCoreDependencies(context: Context): CoreDependencies {
        val musicRepository = createMusicRepository(context)
        val googleDriveService = createGoogleDriveService(context)
        val musicPlayer = createMusicPlayer(context, googleDriveService)
        
        return CoreDependencies(
            musicRepository = musicRepository,
            googleDriveService = googleDriveService,
            musicPlayer = musicPlayer
        )
    }
    
    data class CoreDependencies(
        val musicRepository: MusicRepositoryInterface,
        val googleDriveService: GoogleDriveServiceInterface,
        val musicPlayer: MusicPlayerInterface
    )
}