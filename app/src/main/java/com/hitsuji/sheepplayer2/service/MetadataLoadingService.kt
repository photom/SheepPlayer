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
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.MainActivity
import com.hitsuji.sheepplayer2.R
import com.hitsuji.sheepplayer2.SheepApplication
import com.hitsuji.sheepplayer2.domain.model.LibraryUpdateEvent
import com.hitsuji.sheepplayer2.interfaces.MusicLibraryRepository
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
 * the app is backgrounded. It provides progress updates via a shared repository
 * and maintains a persistent notification during operation.
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
        
        /**
         * Starts the metadata loading service.
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
    private lateinit var updateHelper: UpdateHelper
    private lateinit var musicLibraryRepository: MusicLibraryRepository
    
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
     * Manages update operations via repository.
     */
    private inner class UpdateHelper {
        fun sendMetadataUpdate(artists: List<Artist>, progress: Int, total: Int) {
            serviceScope.launch {
                musicLibraryRepository.emitUpdate(
                    LibraryUpdateEvent.Progress(artists, progress, total)
                )
            }
        }
        
        fun sendLoadingComplete(artists: List<Artist>) {
            serviceScope.launch {
                musicLibraryRepository.emitUpdate(LibraryUpdateEvent.Success(artists))
            }
        }
        
        fun sendLoadingError(message: String) {
            serviceScope.launch {
                musicLibraryRepository.emitUpdate(LibraryUpdateEvent.Error(message))
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MetadataLoadingService created")
        
        try {
            val app = application as SheepApplication
            musicLibraryRepository = app.container.musicLibraryRepository
            
            googleDriveService = GoogleDriveService(this)
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            progressTracker = LoadingProgressTracker()
            updateHelper = UpdateHelper()
            
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
            updateHelper.sendLoadingError("Service initialization failed: ${e.message}")
            stopSelf()
            START_NOT_STICKY
        }
    }
    
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
    
    private fun startForegroundService() {
        try {
            val notification = createNotification("Initializing metadata loading...", 0, 0)
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            updateHelper.sendLoadingError("Failed to start foreground service")
            stopSelf()
        }
    }
    
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
        
        if (total > 0) {
            builder.setProgress(total, progress, false)
            val percentage = (progress * 100) / total
            builder.setSubText("$percentage% complete")
        } else {
            builder.setProgress(0, 0, true)
        }
        
        return builder.build()
    }
    
    private fun updateNotification(text: String, progress: Int, total: Int) {
        try {
            val notification = createNotification(text, progress, total)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification", e)
        }
    }
    
    private fun startMetadataLoading() {
        Log.d(TAG, "Initiating metadata loading process")
        
        if (!googleDriveService.isSignedIn()) {
            Log.e(TAG, "Cannot start loading: not signed in to Google Drive")
            updateHelper.sendLoadingError("Please sign in to Google Drive first")
            stopSelf()
            return
        }
        
        progressTracker.reset()
        
        serviceScope.launch {
            try {
                processMetadataSequentially()
            } catch (e: Exception) {
                Log.e(TAG, "Critical error in metadata loading", e)
                updateHelper.sendLoadingError(e.message ?: "Failed to load metadata")
                stopSelf()
            }
        }
    }
    
    private suspend fun processMetadataSequentially() {
        Log.d(TAG, "Starting sequential metadata processing")
        
        googleDriveService.loadMusicFromGoogleDriveSequentially()
            .catch { exception ->
                Log.e(TAG, "Flow error during metadata loading", exception)
                updateHelper.sendLoadingError(exception.message ?: "Loading flow interrupted")
                stopSelf()
            }
            .collect { result ->
                when (result) {
                    is GoogleDriveResult.Success -> handleLoadingSuccess(result.data)
                    is GoogleDriveResult.Error -> handleLoadingError(result)
                }
            }
        
        Log.d(TAG, "Metadata loading completed successfully")
        updateNotification("Loading complete!", progressTracker.processedFiles, progressTracker.totalFiles)
        
        val finalArtists = googleDriveService.getLatestGoogleDriveArtists()
        updateHelper.sendLoadingComplete(finalArtists)
        
        kotlinx.coroutines.delay(1000)
        stopSelf()
    }
    
    private fun handleLoadingSuccess(artists: List<Artist>) {
        progressTracker.incrementProcessed()
        
        val progressMessage = progressTracker.getProgressMessage()
        updateNotification(progressMessage, progressTracker.processedFiles, progressTracker.totalFiles)
        
        if (progressTracker.shouldBroadcast()) {
            updateHelper.sendMetadataUpdate(artists, progressTracker.processedFiles, progressTracker.totalFiles)
        }
    }
    
    private fun handleLoadingError(result: GoogleDriveResult.Error<List<Artist>>) {
        Log.e(TAG, "Loading error: ${result.message}", result.exception)
        updateNotification("Loading failed", 0, 0)
        updateHelper.sendLoadingError(result.message)
        stopSelf()
    }
    
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
