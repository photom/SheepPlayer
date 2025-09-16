package com.hitsuji.sheepplayer2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.MainActivity
import com.hitsuji.sheepplayer2.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * Foreground service for loading Google Drive music metadata with real-time UI updates.
 * 
 * This service runs in the foreground to ensure metadata loading continues even when
 * the app is backgrounded. It provides progress updates via local broadcasts and
 * maintains a persistent notification during operation.
 * 
 * Key Features:
 * - Foreground service with persistent notification
 * - Real-time progress updates via LocalBroadcastManager
 * - Throttled broadcasts to prevent UI flooding
 * - Automatic service termination on completion/error
 * - Coroutine-based asynchronous processing
 * 
 * @author SheepPlayer Team
 * @version 2.0
 * @since 1.0
 */
class MetadataLoadingService : Service() {
    
    companion object {
        private const val TAG = "MetadataLoadingService"
        
        // Service configuration
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "metadata_loading_channel"
        private const val BROADCAST_THROTTLE_MS = 2000L
        
        // Service actions
        const val ACTION_START_LOADING = "com.hitsuji.sheepplayer2.START_LOADING"
        const val ACTION_STOP_LOADING = "com.hitsuji.sheepplayer2.STOP_LOADING"
        
        // Broadcast actions for UI communication
        const val BROADCAST_METADATA_UPDATE = "com.hitsuji.sheepplayer2.METADATA_UPDATE"
        const val BROADCAST_LOADING_COMPLETE = "com.hitsuji.sheepplayer2.LOADING_COMPLETE"
        const val BROADCAST_LOADING_ERROR = "com.hitsuji.sheepplayer2.LOADING_ERROR"
        
        // Broadcast extra keys
        const val EXTRA_ARTISTS_COUNT = "artists_count"
        const val EXTRA_TRACKS_COUNT = "tracks_count"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_ARTISTS_DATA = "artists_data"
        
        /**
         * Starts the metadata loading service.
         * 
         * This method properly handles foreground service startup for Android O+
         * and ensures the service runs in the foreground to prevent termination.
         * 
         * @param context Android context for service operations
         */
        fun startService(context: Context) {
            val intent = Intent(context, MetadataLoadingService::class.java).apply {
                action = ACTION_START_LOADING
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Metadata loading service start requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start metadata loading service", e)
            }
        }
        
        /**
         * Stops the metadata loading service.
         * 
         * @param context Android context for service operations
         */
        fun stopService(context: Context) {
            val intent = Intent(context, MetadataLoadingService::class.java).apply {
                action = ACTION_STOP_LOADING
            }
            
            try {
                context.stopService(intent)
                Log.d(TAG, "Metadata loading service stop requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop metadata loading service", e)
            }
        }
    }
    
    // Service dependencies
    private lateinit var googleDriveService: GoogleDriveService
    private lateinit var notificationManager: NotificationManager
    private lateinit var progressTracker: LoadingProgressTracker
    private lateinit var broadcastHelper: BroadcastHelper
    
    // Coroutine management
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isLoading = AtomicBoolean(false)
    
    /**
     * Tracks loading progress and manages state.
     */
    private inner class LoadingProgressTracker {
        @Volatile
        var processedFiles = 0
            private set
        
        @Volatile
        var totalFiles = 0
            private set
        
        @Volatile
        var lastBroadcastTime = 0L
            private set
        
        fun incrementProcessed() {
            processedFiles++
        }
        
        fun setTotal(total: Int) {
            totalFiles = max(totalFiles, total)
        }
        
        fun shouldBroadcast(): Boolean {
            val currentTime = System.currentTimeMillis()
            val shouldBroadcast = currentTime - lastBroadcastTime > BROADCAST_THROTTLE_MS || processedFiles == 1
            if (shouldBroadcast) {
                lastBroadcastTime = currentTime
            }
            return shouldBroadcast
        }
        
        fun getProgressMessage(): String {
            return if (totalFiles > 0) {
                "Loaded $processedFiles/$totalFiles files"
            } else {
                "Loading music files... ($processedFiles loaded)"
            }
        }
        
        fun reset() {
            processedFiles = 0
            totalFiles = 0
            lastBroadcastTime = 0L
        }
    }
    
    /**
     * Manages broadcast operations with consistent logging.
     */
    private inner class BroadcastHelper {
        fun sendMetadataUpdate(artists: List<Artist>, progress: Int, total: Int) {
            val tracksCount = artists.sumOf { it.albums.sumOf { album -> album.tracks.size } }
            
            Log.d(TAG, "Broadcasting update: ${artists.size} artists, $tracksCount tracks, $progress/$total")
            
            val intent = Intent(BROADCAST_METADATA_UPDATE).apply {
                putExtra(EXTRA_ARTISTS_COUNT, artists.size)
                putExtra(EXTRA_TRACKS_COUNT, tracksCount)
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_TOTAL, total)
            }
            
            LocalBroadcastManager.getInstance(this@MetadataLoadingService).sendBroadcast(intent)
            
            // Trigger MainActivity to refresh with latest data
            val refreshIntent = Intent("com.hitsuji.sheepplayer2.RELOAD_GOOGLE_DRIVE_DATA")
            LocalBroadcastManager.getInstance(this@MetadataLoadingService).sendBroadcast(refreshIntent)
            
            Log.d(TAG, "Metadata update broadcast sent successfully")
        }
        
        fun sendLoadingComplete() {
            Log.d(TAG, "Broadcasting loading complete")
            val intent = Intent(BROADCAST_LOADING_COMPLETE)
            LocalBroadcastManager.getInstance(this@MetadataLoadingService).sendBroadcast(intent)
        }
        
        fun sendLoadingError(message: String) {
            Log.e(TAG, "Broadcasting loading error: $message")
            val intent = Intent(BROADCAST_LOADING_ERROR).apply {
                putExtra(EXTRA_ERROR_MESSAGE, message)
            }
            LocalBroadcastManager.getInstance(this@MetadataLoadingService).sendBroadcast(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MetadataLoadingService created")
        
        try {
            // Initialize service dependencies
            googleDriveService = GoogleDriveService(this)
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            progressTracker = LoadingProgressTracker()
            broadcastHelper = BroadcastHelper()
            
            createNotificationChannel()
            Log.d(TAG, "Service dependencies initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize service dependencies", e)
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service command received: ${intent?.action}")
        
        return try {
            when (intent?.action) {
                ACTION_START_LOADING -> handleStartLoading()
                ACTION_STOP_LOADING -> handleStopLoading()
                else -> {
                    Log.w(TAG, "Unknown action received: ${intent?.action}")
                    START_NOT_STICKY
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling service command", e)
            broadcastHelper.sendLoadingError("Service initialization failed: ${e.message}")
            stopSelf()
            START_NOT_STICKY
        }
    }
    
    /**
     * Handles the start loading command.
     */
    private fun handleStartLoading(): Int {
        return if (!isLoading.getAndSet(true)) {
            Log.d(TAG, "Starting metadata loading service")
            startForegroundService()
            startMetadataLoading()
            START_NOT_STICKY
        } else {
            Log.d(TAG, "Metadata loading already in progress")
            START_NOT_STICKY
        }
    }
    
    /**
     * Handles the stop loading command.
     */
    private fun handleStopLoading(): Int {
        Log.d(TAG, "Stopping metadata loading service")
        stopMetadataLoading()
        stopSelf()
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "MetadataLoadingService destroyed")
        try {
            stopMetadataLoading()
            googleDriveService.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error during service cleanup", e)
        } finally {
            super.onDestroy()
        }
    }
    
    /**
     * Creates the notification channel for metadata loading (Android O+).
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Metadata Loading",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while loading music metadata from Google Drive"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            try {
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create notification channel", e)
            }
        }
    }
    
    /**
     * Starts the service in foreground mode with initial notification.
     */
    private fun startForegroundService() {
        try {
            val notification = createNotification("Initializing metadata loading...", 0, 0)
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            broadcastHelper.sendLoadingError("Failed to start foreground service")
            stopSelf()
        }
    }
    
    /**
     * Creates a notification with progress information.
     * 
     * @param text The notification text to display
     * @param progress Current progress value
     * @param total Total expected items (0 for indeterminate)
     * @return Configured notification
     */
    private fun createNotification(text: String, progress: Int, total: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SheepPlayer - Loading Music")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        
        // Configure progress bar
        if (total > 0) {
            builder.setProgress(total, progress, false)
            val percentage = if (total > 0) (progress * 100) / total else 0
            builder.setSubText("$percentage% complete")
        } else {
            builder.setProgress(0, 0, true)
        }
        
        return builder.build()
    }
    
    /**
     * Updates the existing notification with new progress information.
     */
    private fun updateNotification(text: String, progress: Int, total: Int) {
        try {
            val notification = createNotification(text, progress, total)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification", e)
        }
    }
    
    /**
     * Starts the metadata loading process in a coroutine.
     * 
     * This method handles the complete metadata loading workflow:
     * 1. Initiates sequential loading from GoogleDriveService
     * 2. Processes progress updates with throttling
     * 3. Updates notification and broadcasts progress
     * 4. Handles errors and completion
     */
    private fun startMetadataLoading() {
        Log.d(TAG, "Initiating metadata loading process")
        
        if (!googleDriveService.isSignedIn()) {
            Log.e(TAG, "Cannot start loading: not signed in to Google Drive")
            broadcastHelper.sendLoadingError("Please sign in to Google Drive first")
            stopSelf()
            return
        }
        
        progressTracker.reset()
        
        serviceScope.launch {
            try {
                processMetadataSequentially()
            } catch (e: Exception) {
                Log.e(TAG, "Critical error in metadata loading", e)
                broadcastHelper.sendLoadingError(e.message ?: "Failed to load metadata")
                stopSelf()
            }
        }
    }
    
    /**
     * Processes metadata loading with sequential updates.
     */
    private suspend fun processMetadataSequentially() {
        Log.d(TAG, "Starting sequential metadata processing")
        
        googleDriveService.loadMusicFromGoogleDriveSequentially()
            .catch { exception ->
                Log.e(TAG, "Flow error during metadata loading", exception)
                broadcastHelper.sendLoadingError(exception.message ?: "Loading flow interrupted")
                stopSelf()
            }
            .collect { result ->
                when (result) {
                    is GoogleDriveResult.Success -> handleLoadingSuccess(result.data)
                    is GoogleDriveResult.Error -> handleLoadingError(result)
                }
            }
        
        // Loading completed successfully
        Log.d(TAG, "Metadata loading completed successfully")
        updateNotification("Loading complete!", progressTracker.processedFiles, progressTracker.totalFiles)
        
        // Send final metadata update with complete artist list (bypass throttling)
        val finalArtists = googleDriveService.getLatestGoogleDriveArtists()
        Log.d(TAG, "Broadcasting final complete data: ${finalArtists.size} artists")
        broadcastHelper.sendMetadataUpdate(finalArtists, progressTracker.processedFiles, progressTracker.totalFiles)
        
        broadcastHelper.sendLoadingComplete()
        
        // Auto-stop after a short delay to show completion
        kotlinx.coroutines.delay(1000)
        stopSelf()
    }
    
    /**
     * Handles successful loading progress update.
     */
    private fun handleLoadingSuccess(artists: List<Artist>) {
        progressTracker.incrementProcessed()
        
        // Always update notification with latest progress
        val progressMessage = progressTracker.getProgressMessage()
        updateNotification(progressMessage, progressTracker.processedFiles, progressTracker.totalFiles)
        
        // Throttle broadcasts to prevent UI flooding
        if (progressTracker.shouldBroadcast()) {
            Log.d(TAG, "Broadcasting progress: ${artists.size} artists, ${progressTracker.processedFiles} processed")
            broadcastHelper.sendMetadataUpdate(artists, progressTracker.processedFiles, progressTracker.totalFiles)
        } else {
            Log.v(TAG, "Progress throttled: ${progressTracker.processedFiles} processed")
        }
    }
    
    /**
     * Handles loading errors.
     */
    private fun handleLoadingError(result: GoogleDriveResult.Error<List<Artist>>) {
        Log.e(TAG, "Loading error: ${result.message}", result.exception)
        updateNotification("Loading failed", 0, 0)
        broadcastHelper.sendLoadingError(result.message)
        stopSelf()
    }
    
    /**
     * Stops the metadata loading process and cleans up resources.
     */
    private fun stopMetadataLoading() {
        Log.d(TAG, "Stopping metadata loading process")
        isLoading.set(false)
        
        try {
            serviceScope.cancel()
            progressTracker.reset()
            Log.d(TAG, "Metadata loading stopped successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Error during metadata loading cleanup", e)
        }
    }
    
}