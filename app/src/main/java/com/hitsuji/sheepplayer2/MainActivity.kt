package com.hitsuji.sheepplayer2

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hitsuji.sheepplayer2.databinding.ActivityMainBinding
import com.hitsuji.sheepplayer2.handlers.GoogleDriveAuthHandler
import com.hitsuji.sheepplayer2.handlers.MusicDataHandler
import com.hitsuji.sheepplayer2.handlers.PermissionHandler
import com.hitsuji.sheepplayer2.interfaces.FragmentNotifier
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.interfaces.MusicPlayerInterface
import com.hitsuji.sheepplayer2.interfaces.MusicRepositoryInterface
import com.hitsuji.sheepplayer2.interfaces.NavigationController
import com.hitsuji.sheepplayer2.interfaces.PlaybackManagerInterface
import com.hitsuji.sheepplayer2.interfaces.MusicLibraryRepository
import com.hitsuji.sheepplayer2.domain.model.LibraryUpdateEvent
import com.hitsuji.sheepplayer2.domain.usecase.GetMusicLibraryUseCase
import com.hitsuji.sheepplayer2.domain.usecase.PlayTrackUseCase
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationController, FragmentNotifier {

    private lateinit var binding: ActivityMainBinding

    // Core dependencies - injected via interfaces (DIP)
    private lateinit var musicRepository: MusicRepositoryInterface
    private lateinit var musicPlayer: MusicPlayerInterface
    private lateinit var musicPlayerManager: PlaybackManagerInterface
    private lateinit var googleDriveService: GoogleDriveServiceInterface
    private lateinit var playTrackUseCase: PlayTrackUseCase
    private lateinit var getMusicLibraryUseCase: GetMusicLibraryUseCase
    private lateinit var musicLibraryRepository: MusicLibraryRepository

    // Specialized handlers (SRP)
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var musicDataHandler: MusicDataHandler
    private lateinit var googleDriveAuthHandler: GoogleDriveAuthHandler

    // State management
    private var isGoogleDriveLoggedIn = false

    // Album playback state
    var currentPlayingAlbum: Album? = null
        private set
    var currentAlbumTracks: List<Track> = emptyList()
        private set
    var currentTrackIndexInAlbum: Int = 0

    // Public API for fragments
    val allArtists: List<Artist> get() = musicDataHandler.getAllArtists()
    val currentPlayingTrack: Track? get() = musicPlayerManager.currentPlayingTrack
    val isPlaying: Boolean get() = musicPlayerManager.isPlaying
    val currentPosition: Int get() = musicPlayerManager.getCurrentPosition()
    val duration: Int get() = musicPlayerManager.getDuration()

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            permissionHandler.handlePermissionResult(isGranted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependencies via AppContainer
        val appContainer = (application as SheepApplication).container
        musicRepository = appContainer.musicRepository
        googleDriveService = appContainer.googleDriveService
        musicPlayer = appContainer.musicPlayer
        musicPlayerManager = appContainer.musicPlayerManager
        playTrackUseCase = appContainer.playTrackUseCase
        getMusicLibraryUseCase = appContainer.getMusicLibraryUseCase
        musicLibraryRepository = appContainer.musicLibraryRepository

        initializeHandlers()
        setupCallbacks()
        collectLibraryUpdates()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.show()
        setupNavigation()

        permissionHandler.checkAndRequestPermission()
        googleDriveAuthHandler.checkExistingSignIn()
        verifyAppIntegrity()
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
    }

    private fun initializeHandlers() {
        permissionHandler = PermissionHandler(this, requestPermissionLauncher)
        
        musicDataHandler = MusicDataHandler(
            this, getMusicLibraryUseCase, lifecycleScope, this
        )
        googleDriveAuthHandler = GoogleDriveAuthHandler(
            (application as SheepApplication).container.signInUseCase,
            (application as SheepApplication).container.signOutUseCase,
            (application as SheepApplication).container.isSignedInUseCase,
            lifecycleScope
        )
    }

    private fun setupCallbacks() {
        permissionHandler.setCallback(object : PermissionHandler.PermissionCallback {
            override fun onPermissionGranted() {
                musicDataHandler.loadLocalMusicData()
            }

            override fun onPermissionDenied() {
                Toast.makeText(
                    this@MainActivity,
                    "Permission denied. Cannot load music.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        musicDataHandler.setCallback(object : MusicDataHandler.MusicDataCallback {
            override fun onLocalMusicLoaded(artists: List<Artist>) {}
            override fun onGoogleDriveMusicLoaded(artists: List<Artist>) {
                // Toast removed as it's redundant with the global sync indicator and final sync complete toast
            }
            override fun onMusicLoadError(message: String) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }
            override fun onNoMusicFound() {
                Toast.makeText(this@MainActivity, "No music files found.", Toast.LENGTH_SHORT).show()
            }
        })

        googleDriveAuthHandler.setCallback(object : GoogleDriveAuthHandler.AuthCallback {
            override fun onSignInSuccess() {
                isGoogleDriveLoggedIn = true
                invalidateOptionsMenu()
                musicDataHandler.refreshGoogleDriveMusic()
            }

            override fun onSignInError(message: String, exception: Throwable?) {
                isGoogleDriveLoggedIn = false
                invalidateOptionsMenu()
            }

            override fun onSignOutSuccess() {
                isGoogleDriveLoggedIn = false
                invalidateOptionsMenu()
                Toast.makeText(this@MainActivity, "Logged out from Google Drive", Toast.LENGTH_SHORT).show()
                musicDataHandler.loadLocalMusicData()
            }

            override fun onSignOutError(message: String, exception: Throwable?) {
                Toast.makeText(this@MainActivity, "Error logging out: $message", Toast.LENGTH_SHORT).show()
            }
        })

        musicPlayerManager.setOnPlaybackStateChangeListener {
            notifyPlaybackStateChanged()
        }

        musicPlayerManager.setOnPlaybackCompletionListener { completedTrack ->
            handleTrackCompletion(completedTrack)
        }
    }

    private fun collectLibraryUpdates() {
        lifecycleScope.launch {
            musicLibraryRepository.libraryUpdates.collect { event ->
                when (event) {
                    is LibraryUpdateEvent.Started -> {
                        showGlobalSyncIndicator(true)
                        musicDataHandler.showLoadingIndicator(true)
                    }
                    is LibraryUpdateEvent.Progress -> {
                        showGlobalSyncIndicator(true)
                        musicDataHandler.updateWithGoogleDriveData(showLoading = true)
                    }
                    is LibraryUpdateEvent.Success -> {
                        showGlobalSyncIndicator(false)
                        musicDataHandler.updateWithGoogleDriveData(showLoading = false)
                        // Toast removed as it's redundant with the global sync indicator state change
                    }
                    is LibraryUpdateEvent.Error -> {
                        showGlobalSyncIndicator(false)
                        musicDataHandler.showLoadingIndicator(false)
                        Toast.makeText(this@MainActivity, "Sync Error: ${event.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showGlobalSyncIndicator(show: Boolean) {
        binding.globalSyncIndicator.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    // NavigationController implementation (ISP)
    override fun switchToPlayingTab() {
        binding.navView.selectedItemId = R.id.navigation_playing
    }

    // FragmentNotifier implementation (ISP)
    override fun notifyDataLoaded(showLoading: Boolean) {
        notifyFragmentsDataLoaded(showLoading)
    }

    override fun notifyPlaybackStateChanged() {
        notifyPlayingFragmentStateChanged()
    }

    // Public API for track/album playback
    fun playTrack(track: Track) {
        currentPlayingAlbum = null
        currentAlbumTracks = emptyList()
        currentTrackIndexInAlbum = 0
        lifecycleScope.launch {
            playTrackUseCase(track)
        }
    }

    fun playAlbum(album: Album) {
        currentPlayingAlbum = album
        currentAlbumTracks = album.tracks.toList()
        currentTrackIndexInAlbum = 0
        if (currentAlbumTracks.isNotEmpty()) {
            lifecycleScope.launch {
                playTrackUseCase(currentAlbumTracks[0])
            }
        }
    }

    fun playTrackInAlbum(track: Track, trackIndex: Int) {
        if (currentPlayingAlbum != null && currentAlbumTracks.isNotEmpty()) {
            currentTrackIndexInAlbum = trackIndex
            lifecycleScope.launch {
                playTrackUseCase(track)
            }
        }
    }

    fun togglePlayback(): Boolean = musicPlayerManager.togglePlayback()
    fun stopPlayback() = musicPlayerManager.stopPlayback()
    fun syncPlaybackState() = musicPlayerManager.syncPlaybackState()
    fun getGoogleDriveService(): GoogleDriveServiceInterface = googleDriveService
    fun refreshGoogleDriveMusic() = musicDataHandler.refreshGoogleDriveMusic()

    // Menu handling
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        updateMenuItemsVisibility(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_google_drive_login -> {
                googleDriveAuthHandler.signIn(this)
                true
            }
            R.id.action_google_drive_logout -> {
                googleDriveAuthHandler.signOut()
                true
            }
            R.id.action_refresh_google_drive -> {
                musicDataHandler.refreshGoogleDriveMusic()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateMenuItemsVisibility(menu: Menu) {
        menu.findItem(R.id.action_google_drive_login).isVisible = !isGoogleDriveLoggedIn
        menu.findItem(R.id.action_google_drive_logout).isVisible = isGoogleDriveLoggedIn
        menu.findItem(R.id.action_refresh_google_drive).isVisible = isGoogleDriveLoggedIn
    }

    private fun handleTrackCompletion(completedTrack: Track) {
        if (currentPlayingAlbum != null && currentAlbumTracks.isNotEmpty()) {
            val nextIndex = currentTrackIndexInAlbum + 1
            if (nextIndex < currentAlbumTracks.size) {
                currentTrackIndexInAlbum = nextIndex
                lifecycleScope.launch {
                    playTrackUseCase(currentAlbumTracks[nextIndex])
                }
            } else {
                currentPlayingAlbum = null
                currentAlbumTracks = emptyList()
                currentTrackIndexInAlbum = 0
            }
        }
    }

    private fun notifyFragmentsDataLoaded(showLoading: Boolean) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        navHostFragment?.childFragmentManager?.fragments?.forEach { fragment ->
            if (fragment is com.hitsuji.sheepplayer2.ui.tracks.TracksFragment) {
                fragment.onMusicDataLoaded(showLoading)
            }
        }
    }

    private fun notifyPlayingFragmentStateChanged() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
        if (currentFragment is com.hitsuji.sheepplayer2.ui.playing.PlayingFragment) {
            currentFragment.onPlaybackStateChanged()
        }
    }

    private fun verifyAppIntegrity() {
        try {
            val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val buildTags = android.os.Build.TAGS
            if (buildTags != null && (buildTags.contains("test-keys") && !isDebuggable)) {
                android.util.Log.w("MainActivity", "App running on potentially compromised system")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error during integrity check", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayerManager.release()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        notifyLowMemoryToFragments()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                notifyLowMemoryToFragments()
            }
        }
    }

    private fun notifyLowMemoryToFragments() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
        if (currentFragment is com.hitsuji.sheepplayer2.ui.pictures.PicturesFragment) {
            currentFragment.onLowMemory()
        }
    }
}
