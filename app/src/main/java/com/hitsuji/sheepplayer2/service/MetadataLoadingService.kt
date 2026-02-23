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
 */
class MetadataLoadingService : Service() {
    
    companion object {
        private const val TAG = "MetadataLoadingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "metadata_loading_channel"
        private const val BROADCAST_THROTTLE_MS = 2000L
        
        const val ACTION_START_LOADING = "com.hitsuji.sheepplayer2.START_LOADING"
        const val ACTION_STOP_LOADING = "com.hitsuji.sheepplayer2.STOP_LOADING"
        
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service", e)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, MetadataLoadingService::class.java).apply {
                action = ACTION_STOP_LOADING
            }
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop service", e)
            }
        }
    }
    
    private lateinit var googleDriveService: GoogleDriveService
    private lateinit var notificationManager: NotificationManager
    private lateinit var progressTracker: LoadingProgressTracker
    private lateinit var updateHelper: UpdateHelper
    private lateinit var musicLibraryRepository: MusicLibraryRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isLoading = AtomicBoolean(false)
    
    private inner class LoadingProgressTracker {
        @Volatile var processedFiles = 0
        @Volatile var totalFiles = 0
        @Volatile var lastBroadcastTime = 0L
        
        fun incrementProcessed() { processedFiles++ }
        fun setTotal(total: Int) { totalFiles = max(totalFiles, total) }
        fun shouldBroadcast(): Boolean {
            val currentTime = System.currentTimeMillis()
            val shouldBroadcast = currentTime - lastBroadcastTime > BROADCAST_THROTTLE_MS || processedFiles == 1
            if (shouldBroadcast) lastBroadcastTime = currentTime
            return shouldBroadcast
        }
        fun getProgressMessage(): String = if (totalFiles > 0) "Loaded $processedFiles/$totalFiles files" else "Loading music files... ($processedFiles loaded)"
        fun reset() { processedFiles = 0; totalFiles = 0; lastBroadcastTime = 0L }
    }
    
    private inner class UpdateHelper {
        fun sendMetadataUpdate(artists: List<Artist>, progress: Int, total: Int) {
            serviceScope.launch { musicLibraryRepository.emitUpdate(LibraryUpdateEvent.Progress(artists, progress, total)) }
        }
        fun sendLoadingComplete(artists: List<Artist>) {
            serviceScope.launch { musicLibraryRepository.emitUpdate(LibraryUpdateEvent.Success(artists)) }
        }
        fun sendLoadingError(message: String) {
            serviceScope.launch { musicLibraryRepository.emitUpdate(LibraryUpdateEvent.Error(message)) }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        try {
            val app = application as SheepApplication
            musicLibraryRepository = app.container.musicLibraryRepository
            googleDriveService = app.container.googleDriveService as GoogleDriveService
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            progressTracker = LoadingProgressTracker()
            updateHelper = UpdateHelper()
            createNotificationChannel()
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOADING -> handleStartLoading()
            ACTION_STOP_LOADING -> handleStopLoading()
        }
        return START_NOT_STICKY
    }
    
    private fun handleStartLoading() {
        if (!isLoading.getAndSet(true)) {
            startForegroundService()
            startMetadataLoading()
        }
    }
    
    private fun handleStopLoading() {
        stopMetadataLoading()
        stopSelf()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        try {
            stopMetadataLoading()
        } finally {
            super.onDestroy()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Music Metadata Loading", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundService() {
        val notification = createNotification("Initializing...", 0, 0)
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun createNotification(text: String, progress: Int, total: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SheepPlayer - Loading Music")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        
        if (total > 0) {
            builder.setProgress(total, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }
    
    private fun updateNotification(text: String, progress: Int, total: Int) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(text, progress, total))
    }
    
    private fun startMetadataLoading() {
        if (!googleDriveService.isSignedIn()) {
            updateHelper.sendLoadingError("Please sign in first")
            stopSelf()
            return
        }
        progressTracker.reset()
        serviceScope.launch {
            try {
                processMetadataSequentially()
            } catch (e: Exception) {
                updateHelper.sendLoadingError(e.message ?: "Failed")
                stopSelf()
            }
        }
    }
    
    private suspend fun processMetadataSequentially() {
        googleDriveService.loadMusicFromGoogleDriveSequentially()
            .catch { exception ->
                updateHelper.sendLoadingError(exception.message ?: "Interrupted")
                stopSelf()
            }
            .collect { result ->
                when (result) {
                    is GoogleDriveResult.Success -> {
                        progressTracker.incrementProcessed()
                        updateNotification(progressTracker.getProgressMessage(), progressTracker.processedFiles, progressTracker.totalFiles)
                        if (progressTracker.shouldBroadcast()) {
                            updateHelper.sendMetadataUpdate(result.data, progressTracker.processedFiles, progressTracker.totalFiles)
                        }
                    }
                    is GoogleDriveResult.Error -> {
                        updateHelper.sendLoadingError(result.message)
                        stopSelf()
                    }
                }
            }
        updateHelper.sendLoadingComplete(googleDriveService.getLatestGoogleDriveArtists())
        stopSelf()
    }
    
    private fun stopMetadataLoading() {
        isLoading.set(false)
        serviceScope.cancel()
    }
}
