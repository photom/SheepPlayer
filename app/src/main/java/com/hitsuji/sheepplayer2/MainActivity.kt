package com.hitsuji.sheepplayer2

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hitsuji.sheepplayer2.databinding.ActivityMainBinding
import com.hitsuji.sheepplayer2.manager.MusicPlayerManager
import com.hitsuji.sheepplayer2.repository.MusicRepository
import com.hitsuji.sheepplayer2.service.GoogleDriveService
import com.hitsuji.sheepplayer2.service.GoogleDriveResult
import com.hitsuji.sheepplayer2.service.MetadataLoadingService
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var allArtists: MutableList<Artist> = mutableListOf()
    private lateinit var musicRepository: MusicRepository
    private lateinit var musicPlayerManager: MusicPlayerManager
    private lateinit var googleDriveService: GoogleDriveService
    private var isGoogleDriveLoggedIn = false
    private var metadataLoadingReceiver: BroadcastReceiver? = null

    fun getGoogleDriveService(): GoogleDriveService = googleDriveService

    val currentPlayingTrack: Track? get() = musicPlayerManager.currentPlayingTrack
    val isPlaying: Boolean get() = musicPlayerManager.isPlaying
    val currentPosition: Int get() = musicPlayerManager.getCurrentPosition()
    val duration: Int get() = musicPlayerManager.getDuration()

    // Album playback state
    var currentPlayingAlbum: Album? = null
        private set
    var currentAlbumTracks: List<Track> = emptyList()
        private set
    var currentTrackIndexInAlbum: Int = 0

    // ActivityResultLauncher for permission requests
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("Permission", "Permission granted")
                loadMusicDataFromStorage()
            } else {
                Log.e("Permission", "Permission denied")
                Toast.makeText(this, "Permission denied. Cannot load music.", Toast.LENGTH_LONG)
                    .show()
                // Handle the case where permission is denied (e.g., show a message, disable functionality)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep action bar visible for menu
        supportActionBar?.show()

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        // Initialize components
        initializeComponents()

        // Check and request permission, then load music
        checkAndRequestPermission()

        // Check if already signed in to Google Drive
        checkExistingGoogleDriveSignIn()

        // Verify app integrity
        verifyAppIntegrity()
    }

    private fun checkAndRequestPermission() {
        val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permissionToRequest
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("Permission", "Permission already granted")
                loadMusicDataFromStorage()
            }

            shouldShowRequestPermissionRationale(permissionToRequest) -> {
                // Show an educational UI to the user explaining why you need the permission
                // For simplicity, we'll just request directly here, but in a real app, show a dialog.
                Log.w(
                    "Permission",
                    "Showing rationale (not implemented in this example, requesting directly)"
                )
                requestPermissionLauncher.launch(permissionToRequest)
            }

            else -> {
                // Directly request the permission
                Log.i("Permission", "Requesting permission")
                requestPermissionLauncher.launch(permissionToRequest)
            }
        }
    }

    private fun loadMusicDataFromStorage() {
        lifecycleScope.launch {
            try {
                val artists = musicRepository.loadMusicData()
                allArtists.clear()
                allArtists.addAll(artists)

                val trackCount = allArtists.sumOf { it.albums.sumOf { it.tracks.size } }
                Log.d("MusicLoader", "Found $trackCount tracks by ${allArtists.size} artists.")

                if (allArtists.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No music files found.", Toast.LENGTH_SHORT)
                        .show()
                }

                notifyFragmentsDataLoaded()
            } catch (e: Exception) {
                Log.e("MusicLoader", "Failed to load music data", e)
                Toast.makeText(this@MainActivity, "Failed to load music files.", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }


    fun switchToPlayingTab() {
        binding.navView.selectedItemId = R.id.navigation_playing
    }

    private fun initializeComponents() {
        musicRepository = MusicRepository(this)
        googleDriveService = GoogleDriveService(this)

        val musicPlayer = MusicPlayer(this)
        musicPlayer.setGoogleDriveService(googleDriveService)
        musicPlayerManager = MusicPlayerManager(musicPlayer)

        musicPlayerManager.setOnPlaybackStateChangeListener {
            notifyPlayingFragmentStateChanged()
        }

        // Set up album sequential playback
        musicPlayerManager.setOnPlaybackCompletionListener { completedTrack ->
            handleTrackCompletion(completedTrack)
        }
        
        // Set up metadata loading broadcast receiver
        setupMetadataLoadingReceiver()
    }
    
    private fun setupMetadataLoadingReceiver() {
        metadataLoadingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("MainActivity", "=== BROADCAST RECEIVED ===")
                Log.d("MainActivity", "Action: ${intent?.action}")
                
                when (intent?.action) {
                    MetadataLoadingService.BROADCAST_METADATA_UPDATE -> {
                        val artistsCount = intent.getIntExtra(MetadataLoadingService.EXTRA_ARTISTS_COUNT, 0)
                        val tracksCount = intent.getIntExtra(MetadataLoadingService.EXTRA_TRACKS_COUNT, 0)
                        val progress = intent.getIntExtra(MetadataLoadingService.EXTRA_PROGRESS, 0)
                        val total = intent.getIntExtra(MetadataLoadingService.EXTRA_TOTAL, 0)
                        
                        Log.d("MainActivity", "*** METADATA UPDATE: $artistsCount artists, $tracksCount tracks (Progress: $progress/$total) ***")
                    }
                    "com.hitsuji.sheepplayer2.RELOAD_GOOGLE_DRIVE_DATA" -> {
                        Log.d("MainActivity", "*** RELOAD_GOOGLE_DRIVE_DATA received - calling updateWithLatestGoogleDriveData() ***")
                        // Update UI with latest Google Drive data
                        updateWithLatestGoogleDriveData()
                    }
                    MetadataLoadingService.BROADCAST_LOADING_COMPLETE -> {
                        Log.d("MainActivity", "*** Google Drive metadata loading completed ***")
                        Toast.makeText(this@MainActivity, "Google Drive music loaded", Toast.LENGTH_SHORT).show()
                    }
                    MetadataLoadingService.BROADCAST_LOADING_ERROR -> {
                        val error = intent.getStringExtra(MetadataLoadingService.EXTRA_ERROR_MESSAGE) ?: "Unknown error"
                        Log.e("MainActivity", "*** Google Drive metadata loading error: $error ***")
                        Toast.makeText(this@MainActivity, "Error loading Google Drive music: $error", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Log.d("MainActivity", "Unknown broadcast action: ${intent?.action}")
                    }
                }
                Log.d("MainActivity", "=== BROADCAST PROCESSED ===")
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
    
    private fun updateWithLatestGoogleDriveData() {
        Log.d("MainActivity", "=== updateWithLatestGoogleDriveData() CALLED ===")
        
        try {
            // Get the latest cached data from GoogleDriveService
            Log.d("MainActivity", "*** About to call googleDriveService.getLatestGoogleDriveArtists() ***")
            val googleDriveArtists = googleDriveService.getLatestGoogleDriveArtists()
            Log.d("MainActivity", "Got latest cached data: ${googleDriveArtists.size} artists")
            
            if (googleDriveArtists.isNotEmpty()) {
                // Replace Google Drive portion of artists list
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
                Log.d("MainActivity", "*** CALLING notifyFragmentsDataLoaded() ***")
                notifyFragmentsDataLoaded()
                
                val totalTracks = allArtists.sumOf { it.albums.sumOf { album -> album.tracks.size } }
                val googleDriveTracks = googleDriveArtists.sumOf { it.albums.sumOf { album -> album.tracks.size } }
                Log.d("MainActivity", "*** UPDATED MUSIC LIBRARY: ${allArtists.size} artists, $totalTracks tracks ($googleDriveTracks from Google Drive) ***")
            } else {
                Log.d("MainActivity", "No Google Drive data available yet")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating with latest Google Drive data", e)
        }
    }

    fun playTrack(track: Track) {
        // Clear album context when playing individual track
        currentPlayingAlbum = null
        currentAlbumTracks = emptyList()
        currentTrackIndexInAlbum = 0

        musicPlayerManager.playTrack(track)
    }

    fun playAlbum(album: Album) {
        // Set album context
        currentPlayingAlbum = album
        currentAlbumTracks = album.tracks.toList()
        currentTrackIndexInAlbum = 0

        // Start playing first track
        if (currentAlbumTracks.isNotEmpty()) {
            musicPlayerManager.playTrack(currentAlbumTracks[0])
        }
    }

    fun playTrackInAlbum(track: Track, trackIndex: Int) {
        // Play track within existing album context without clearing it
        if (currentPlayingAlbum != null && currentAlbumTracks.isNotEmpty()) {
            currentTrackIndexInAlbum = trackIndex
            musicPlayerManager.playTrack(track)
        }
    }

    fun syncPlaybackState() {
        musicPlayerManager.syncPlaybackState()
    }

    fun togglePlayback(): Boolean {
        return musicPlayerManager.togglePlayback()
    }

    fun stopPlayback() {
        musicPlayerManager.stopPlayback()
    }

    private fun notifyPlayingFragmentStateChanged() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        if (currentFragment is com.hitsuji.sheepplayer2.ui.playing.PlayingFragment) {
            currentFragment.onPlaybackStateChanged()
        }
    }

    private fun handleTrackCompletion(completedTrack: Track) {
        // Check if we're playing an album and should continue to next track
        if (currentPlayingAlbum != null && currentAlbumTracks.isNotEmpty()) {
            val nextIndex = currentTrackIndexInAlbum + 1
            if (nextIndex < currentAlbumTracks.size) {
                // Play next track in album
                currentTrackIndexInAlbum = nextIndex
                musicPlayerManager.playTrack(currentAlbumTracks[nextIndex])
            } else {
                // Album finished - could restart or stop
                // For now, stop at end of album
                currentPlayingAlbum = null
                currentAlbumTracks = emptyList()
                currentTrackIndexInAlbum = 0
            }
        }
        // If not playing an album, just let the track end naturally
    }

    private fun notifyFragmentsDataLoaded() {
        Log.d("MainActivity", "=== notifyFragmentsDataLoaded() START ===")
        
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        
        Log.d("MainActivity", "NavHostFragment: $navHostFragment")
        
        // Notify all fragments, not just the currently visible one
        var tracksFragmentCount = 0
        navHostFragment?.childFragmentManager?.fragments?.forEach { fragment ->
            Log.d("MainActivity", "Found fragment: ${fragment::class.simpleName}")
            when (fragment) {
                is com.hitsuji.sheepplayer2.ui.tracks.TracksFragment -> {
                    tracksFragmentCount++
                    Log.d("MainActivity", "*** CALLING onMusicDataLoaded() on TracksFragment ***")
                    fragment.onMusicDataLoaded()
                }
                // Add other fragments here if needed
            }
        }
        
        // Also find and update any detached fragments that might exist
        supportFragmentManager.fragments.forEach { fragment ->
            Log.d("MainActivity", "Found root fragment: ${fragment::class.simpleName}")
            if (fragment is com.hitsuji.sheepplayer2.ui.tracks.TracksFragment) {
                tracksFragmentCount++
                Log.d("MainActivity", "*** CALLING onMusicDataLoaded() on detached TracksFragment ***")
                fragment.onMusicDataLoaded()
            }
        }
        
        Log.d("MainActivity", "Notified $tracksFragmentCount TracksFragment instances")
        Log.d("MainActivity", "=== notifyFragmentsDataLoaded() END ===")
    }

    private fun checkExistingGoogleDriveSignIn() {
        lifecycleScope.launch {
            try {
                // Give a small delay to ensure GoogleDriveService is fully initialized
                delay(100)
                
                val isSignedIn = googleDriveService.isSignedIn()
                val currentAccount = googleDriveService.getCurrentAccount()
                
                Log.d("MainActivity", "Checking Google Drive sign-in: isSignedIn=$isSignedIn, account=${currentAccount?.email}")
                
                if (isSignedIn) {
                    isGoogleDriveLoggedIn = true
                    invalidateOptionsMenu()
                    Log.d("MainActivity", "Already signed in to Google Drive, isGoogleDriveLoggedIn set to: $isGoogleDriveLoggedIn")
                    
                    // Automatically load Google Drive music with sequential loading
                    refreshGoogleDriveMusic()
                } else {
                    isGoogleDriveLoggedIn = false
                    invalidateOptionsMenu()
                    Log.d("MainActivity", "Not signed in to Google Drive, isGoogleDriveLoggedIn set to: $isGoogleDriveLoggedIn")
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Error checking Google Drive sign-in status", e)
                isGoogleDriveLoggedIn = false
                invalidateOptionsMenu()
            }
        }
    }

    private fun verifyAppIntegrity() {
        try {
            // Basic integrity check - verify we're running in expected environment
            val isDebuggable =
                (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebuggable) {
                Log.d("MainActivity", "Running in debug mode")
            }

            // Check for suspicious system properties that might indicate tampering
            val buildTags = android.os.Build.TAGS
            if (buildTags != null && (buildTags.contains("test-keys") && !isDebuggable)) {
                Log.w("MainActivity", "App running on potentially compromised system")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during integrity check", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        updateMenuItemsVisibility(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_google_drive_login -> {
                handleGoogleDriveSignIn()
                true
            }

            R.id.action_google_drive_logout -> {
                logoutFromGoogleDrive()
                true
            }

            R.id.action_refresh_google_drive -> {
                refreshGoogleDriveMusic()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateMenuItemsVisibility(menu: Menu) {
        val loginItem = menu.findItem(R.id.action_google_drive_login)
        val logoutItem = menu.findItem(R.id.action_google_drive_logout)
        val refreshItem = menu.findItem(R.id.action_refresh_google_drive)

        loginItem.isVisible = !isGoogleDriveLoggedIn
        logoutItem.isVisible = isGoogleDriveLoggedIn
        refreshItem.isVisible = isGoogleDriveLoggedIn
    }

    private fun handleGoogleDriveSignIn() {
        lifecycleScope.launch {
            when (val result = googleDriveService.signIn()) {
                is GoogleDriveResult.Success -> {
                    onGoogleDriveSignInSuccess()
                }
                is GoogleDriveResult.Error -> {
                    onGoogleDriveSignInError(result.message, result.exception)
                }
            }
        }
    }
    
    fun onGoogleDriveSignInSuccess() {
        isGoogleDriveLoggedIn = true
        invalidateOptionsMenu()
        Log.d("MainActivity", "Google Drive login successful, isGoogleDriveLoggedIn set to: $isGoogleDriveLoggedIn")
        refreshGoogleDriveMusic()
    }
    
    fun onGoogleDriveSignInError(message: String, exception: Throwable? = null) {
        isGoogleDriveLoggedIn = false
        invalidateOptionsMenu()
        Log.e("MainActivity", "Google Drive login failed: $message, isGoogleDriveLoggedIn set to: $isGoogleDriveLoggedIn", exception)
    }

    private fun logoutFromGoogleDrive() {
        lifecycleScope.launch {
            when (val result = googleDriveService.signOut()) {
                is GoogleDriveResult.Success -> {
                    isGoogleDriveLoggedIn = false
                    invalidateOptionsMenu()
                    Toast.makeText(
                        this@MainActivity,
                        "Logged out from Google Drive",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Clear Google Drive music and reload local music
                    loadMusicDataFromStorage()
                }
                is GoogleDriveResult.Error -> {
                    Log.e("MainActivity", "Google Drive logout failed: ${result.message}", result.exception)
                    Toast.makeText(
                        this@MainActivity,
                        "Error logging out: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun refreshGoogleDriveMusic() {
        if (!isGoogleDriveLoggedIn) {
            Toast.makeText(this, "Please login to Google Drive first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                Toast.makeText(
                    this@MainActivity,
                    "Starting Google Drive music loading...",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Load local music first
                allArtists.clear()
                val localArtists = musicRepository.loadMusicData()
                allArtists.addAll(localArtists)
                
                // Notify UI with local music first
                notifyFragmentsDataLoaded()
                
                // Start the metadata loading service
                Log.d("MainActivity", "*** STARTING METADATA LOADING SERVICE ***")
                MetadataLoadingService.startService(this@MainActivity)
                Log.d("MainActivity", "*** SERVICE START REQUESTED ***")

            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting Google Drive music loading", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error starting Google Drive music loading: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun loadLocalMusicOnly() {
        lifecycleScope.launch {
            try {
                allArtists.clear()
                val localArtists = musicRepository.loadMusicData()
                allArtists.addAll(localArtists)

                val totalTracks = allArtists.sumOf { it.albums.sumOf { it.tracks.size } }
                Log.d(
                    "MusicLoader",
                    "Loaded $totalTracks local tracks by ${allArtists.size} artists."
                )

                notifyFragmentsDataLoaded()

            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading local music", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error loading local music: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayerManager.release()
        
        // Unregister broadcast receiver
        metadataLoadingReceiver?.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Notify current fragment about low memory
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        if (currentFragment is com.hitsuji.sheepplayer2.ui.pictures.PicturesFragment) {
            currentFragment.onLowMemory()
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Handle different trim levels
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // App is in background, aggressively trim memory
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
                val currentFragment =
                    navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

                if (currentFragment is com.hitsuji.sheepplayer2.ui.pictures.PicturesFragment) {
                    currentFragment.onLowMemory()
                }
            }
        }
    }
}
