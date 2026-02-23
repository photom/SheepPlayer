package com.hitsuji.sheepplayer2

import android.content.Context
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.interfaces.MusicPlayerInterface
import com.hitsuji.sheepplayer2.interfaces.MusicRepositoryInterface
import com.hitsuji.sheepplayer2.interfaces.PlaybackManagerInterface
import com.hitsuji.sheepplayer2.interfaces.ArtistImageRepository
import com.hitsuji.sheepplayer2.interfaces.MusicLibraryRepository
import com.hitsuji.sheepplayer2.manager.MusicPlayerManager
import com.hitsuji.sheepplayer2.repository.MusicRepository
import com.hitsuji.sheepplayer2.repository.MusicLibraryRepositoryImpl
import com.hitsuji.sheepplayer2.service.GoogleDriveService
import com.hitsuji.sheepplayer2.service.ArtistImageService
import com.hitsuji.sheepplayer2.domain.service.BinarySignatureValidator
import com.hitsuji.sheepplayer2.domain.service.PathValidator
import com.hitsuji.sheepplayer2.domain.usecase.*

/**
 * Manual Dependency Injection Container.
 * Holds references to application-wide singletons.
 */
class AppContainer(private val context: Context) {

    val musicRepository: MusicRepositoryInterface by lazy {
        MusicRepository(context)
    }

    val musicLibraryRepository: MusicLibraryRepository by lazy {
        MusicLibraryRepositoryImpl()
    }

    val pathValidator: PathValidator by lazy {
        PathValidator(listOf(
            "/storage/",
            "/sdcard/",
            "/data/media/",
            "/android_asset/",
            context.cacheDir.absolutePath,
            context.filesDir.absolutePath
        ))
    }

    val binarySignatureValidator: BinarySignatureValidator by lazy {
        BinarySignatureValidator()
    }

    val googleDriveService: GoogleDriveServiceInterface by lazy {
        GoogleDriveService(context)
    }

    val artistImageRepository: ArtistImageRepository by lazy {
        ArtistImageService(context)
    }

    val musicPlayer: MusicPlayerInterface by lazy {
        MusicPlayer(context, pathValidator)
    }

    val musicPlayerManager: PlaybackManagerInterface by lazy {
        MusicPlayerManager(musicPlayer)
    }

    val getMusicLibraryUseCase: GetMusicLibraryUseCase by lazy {
        GetMusicLibraryUseCase(musicRepository, googleDriveService)
    }

    val playTrackUseCase: PlayTrackUseCase by lazy {
        PlayTrackUseCase(musicPlayerManager, googleDriveService, context.cacheDir)
    }

    val togglePlaybackUseCase: TogglePlaybackUseCase by lazy {
        TogglePlaybackUseCase(musicPlayerManager)
    }

    val searchArtistImagesUseCase: SearchArtistImagesUseCase by lazy {
        SearchArtistImagesUseCase(artistImageRepository)
    }

    val downloadArtistImageUseCase: DownloadArtistImageUseCase by lazy {
        DownloadArtistImageUseCase(artistImageRepository)
    }

    val getLoadingPlaceholderUseCase: GetLoadingPlaceholderUseCase by lazy {
        GetLoadingPlaceholderUseCase(artistImageRepository)
    }

    val signInUseCase: SignInUseCase by lazy {
        SignInUseCase(googleDriveService)
    }

    val signOutUseCase: SignOutUseCase by lazy {
        SignOutUseCase(googleDriveService)
    }

    val isSignedInUseCase: IsSignedInUseCase by lazy {
        IsSignedInUseCase(googleDriveService)
    }

    val getAccountEmailUseCase: GetAccountEmailUseCase by lazy {
        GetAccountEmailUseCase(googleDriveService)
    }
}
