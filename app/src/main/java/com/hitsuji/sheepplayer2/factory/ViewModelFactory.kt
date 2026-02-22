package com.hitsuji.sheepplayer2.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hitsuji.sheepplayer2.AppContainer
import com.hitsuji.sheepplayer2.ui.pictures.PicturesViewModel
import com.hitsuji.sheepplayer2.ui.tracks.TracksViewModel
import com.hitsuji.sheepplayer2.ui.playing.PlayingViewModel
import com.hitsuji.sheepplayer2.ui.menu.MenuViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PicturesViewModel::class.java) -> {
                PicturesViewModel(
                    container.searchArtistImagesUseCase,
                    container.downloadArtistImageUseCase,
                    container.getLoadingPlaceholderUseCase
                ) as T
            }
            modelClass.isAssignableFrom(TracksViewModel::class.java) -> {
                TracksViewModel(container.getMusicLibraryUseCase) as T
            }
            modelClass.isAssignableFrom(PlayingViewModel::class.java) -> {
                PlayingViewModel(
                    container.togglePlaybackUseCase,
                    container.playTrackUseCase,
                    container.musicPlayerManager
                ) as T
            }
            modelClass.isAssignableFrom(MenuViewModel::class.java) -> {
                MenuViewModel(
                    container.signInUseCase,
                    container.signOutUseCase,
                    container.isSignedInUseCase,
                    container.getAccountEmailUseCase,
                    container.getMusicLibraryUseCase
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
