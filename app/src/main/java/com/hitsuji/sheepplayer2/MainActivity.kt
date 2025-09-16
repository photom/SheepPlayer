package com.hitsuji.sheepplayer2

import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import com.hitsuji.sheepplayer2.interfaces.PlaybackStateListener
import com.hitsuji.sheepplayer2.manager.MusicPlayerManager
import com.hitsuji.sheepplayer2.repository.MusicRepository
import com.hitsuji.sheepplayer2.service.GoogleDriveService
import com.hitsuji.sheepplayer2.service.MetadataLoadingService

class MainActivity : AppCompatActivity(), NavigationController, FragmentNotifier {

    private lateinit var binding: ActivityMainBinding

    // Core dependencies - injected via interfaces (DIP)
    private lateinit var musicRepository: MusicRepositoryInterface
    private lateinit var musicPlayer: MusicPlayerInterface
    private lateinit var musicPlayerManager: MusicPlayerManager
    private lateinit var googleDriveService: GoogleDriveServiceInterface

    // Specialized handlers (SRP)
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var musicDataHandler: MusicDataHandler
    private lateinit var googleDriveAuthHandler: GoogleDriveAuthHandler

    // State management
    private var isGoogleDriveLoggedIn = false
    private var metadataLoadingReceiver: BroadcastReceiver? = null

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

        // Initialize dependencies first, before view inflation
        initializeDependencies()
        initializeHandlers()
        setupCallbacks()

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

    private fun initializeDependencies() {
        // Repository layer
        musicRepository = MusicRepository(this)
        googleDriveService = GoogleDriveService(this)

        // Player layer
        musicPlayer = MusicPlayer(this).apply {
            setGoogleDriveService(googleDriveService)
        }
        musicPlayerManager = MusicPlayerManager(musicPlayer as MusicPlayer)
    }

    private fun initializeHandlers() {
        permissionHandler = PermissionHandler(this, requestPermissionLauncher)
        musicDataHandler = MusicDataHandler(
            this, musicRepository, googleDriveService, lifecycleScope, this
        )
        googleDriveAuthHandler = GoogleDriveAuthHandler(googleDriveService, lifecycleScope)

        setupMetadataLoadingReceiver()
    }

    private fun setupCallbacks() {
        // Permission handler callbacks
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

        // Music data handler callbacks
        musicDataHandler.setCallback(object : MusicDataHandler.MusicDataCallback {
            override fun onLocalMusicLoaded(artists: List<Artist>) {
                // Handled by FragmentNotifier interface
            }

            override fun onGoogleDriveMusicLoaded(artists: List<Artist>) {
                Toast.makeText(
                    this@MainActivity,
                    "Google Drive music loaded",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onMusicLoadError(message: String) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }

            override fun onNoMusicFound() {
                Toast.makeText(
                    this@MainActivity,
                    "No music files found.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Google Drive auth callbacks
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
                Toast.makeText(
                    this@MainActivity,
                    "Logged out from Google Drive",
                    Toast.LENGTH_SHORT
                ).show()
                musicDataHandler.loadLocalMusicData()
            }

            override fun onSignOutError(message: String, exception: Throwable?) {
                Toast.makeText(
                    this@MainActivity,
                    "Error logging out: $message",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Music player callbacks
        musicPlayerManager.setOnPlaybackStateChangeListener {
            notifyPlaybackStateChanged()
        }

        musicPlayerManager.setOnPlaybackCompletionListener { completedTrack ->
            handleTrackCompletion(completedTrack)
        }
    }

    // NavigationController implementation (ISP)
    override fun switchToPlayingTab() {
        binding.navView.selectedItemId = R.id.navigation_playing
    }

    // FragmentNotifier implementation (ISP)
    override fun notifyDataLoaded() {
        notifyFragmentsDataLoaded()
    }

    override fun notifyPlaybackStateChanged() {
        notifyPlayingFragmentStateChanged()
    }

    // Public API for track/album playback
    fun playTrack(track: Track) {
        currentPlayingAlbum = null
        currentAlbumTracks = emptyList()
        currentTrackIndexInAlbum = 0
        musicPlayerManager.playTrack(track)
    }

    fun playAlbum(album: Album) {
        currentPlayingAlbum = album
        currentAlbumTracks = album.tracks.toList()
        currentTrackIndexInAlbum = 0
        if (currentAlbumTracks.isNotEmpty()) {
            musicPlayerManager.playTrack(currentAlbumTracks[0])
        }
    }

    fun playTrackInAlbum(track: Track, trackIndex: Int) {
        if (currentPlayingAlbum != null && currentAlbumTracks.isNotEmpty()) {
            currentTrackIndexInAlbum = trackIndex
            musicPlayerManager.playTrack(track)
        }
    }

    fun togglePlayback(): Boolean = musicPlayerManager.togglePlayback()
    fun stopPlayback() = musicPlayerManager.stopPlayback()
    fun syncPlaybackState() = musicPlayerManager.syncPlaybackState()
    fun getGoogleDriveService(): GoogleDriveServiceInterface = googleDriveService

    // Menu handling
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        updateMenuItemsVisibility(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_google_drive_login -> {
                googleDriveAuthHandler.signIn()
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

    private fun setupMetadataLoadingReceiver() {
        metadataLoadingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.hitsuji.sheepplayer2.RELOAD_GOOGLE_DRIVE_DATA" -> {
                        musicDataHandler.updateWithGoogleDriveData()
                    }
                    MetadataLoadingService.BROADCAST_LOADING_COMPLETE -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Google Drive music loaded",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    MetadataLoadingService.BROADCAST_LOADING_ERROR -> {
                        val error = intent.getStringExtra(MetadataLoadingService.EXTRA_ERROR_MESSAGE)
                            ?: "Unknown error"
                        Toast.makeText(
                            this@MainActivity,
                            "Error loading Google Drive music: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(MetadataLoadingService.BROADCAST_METADATA_UPDATE)
            addAction(MetadataLoadingService.BROADCAST_LOADING_COMPLETE)
            addAction(MetadataLoadingService.BROADCAST_LOADING_ERROR)
            addAction("com.hitsuji.sheepplayer2.RELOAD_GOOGLE_DRIVE_DATA")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(metadataLoadingReceiver!!, filter)
    }

    private fun handleTrackCompletion(completedTrack: Track) {
        if (currentPlayingAlbum != null && currentAlbumTracks.isNotEmpty()) {
            val nextIndex = currentTrackIndexInAlbum + 1
            if (nextIndex < currentAlbumTracks.size) {
                currentTrackIndexInAlbum = nextIndex
                musicPlayerManager.playTrack(currentAlbumTracks[nextIndex])
            } else {
                currentPlayingAlbum = null
                currentAlbumTracks = emptyList()
                currentTrackIndexInAlbum = 0
            }
        }
    }

    private fun notifyFragmentsDataLoaded() {
        Log.d("MainActivity", "*** notifyFragmentsDataLoaded() called ***")
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        var fragmentsNotified = 0
        navHostFragment?.childFragmentManager?.fragments?.forEach { fragment ->
            Log.d("MainActivity", "Found fragment: ${fragment::class.simpleName}")
            if (fragment is com.hitsuji.sheepplayer2.ui.tracks.TracksFragment) {
                Log.d("MainActivity", "*** Calling onMusicDataLoaded() on TracksFragment ***")
                fragment.onMusicDataLoaded()
                fragmentsNotified++
            }
        }
        Log.d("MainActivity", "*** Notified $fragmentsNotified TracksFragment(s) ***")
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
        metadataLoadingReceiver?.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
        }
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