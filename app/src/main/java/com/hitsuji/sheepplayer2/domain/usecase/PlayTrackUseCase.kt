package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.interfaces.GoogleDriveServiceInterface
import com.hitsuji.sheepplayer2.interfaces.PlaybackManagerInterface
import com.hitsuji.sheepplayer2.service.GoogleDriveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PlayTrackUseCase(
    private val musicPlayerManager: PlaybackManagerInterface,
    private val googleDriveService: GoogleDriveServiceInterface,
    private val cacheDir: File
) {
    suspend operator fun invoke(track: Track) {
        if (track.filePath.startsWith("gdrive://")) {
            val fileId = track.filePath.removePrefix("gdrive://")
            val cacheFile = File(cacheDir, "gdrive_${fileId}.tmp")
            
            val localPath = withContext(Dispatchers.IO) {
                if (cacheFile.exists() && cacheFile.length() > 0) {
                     return@withContext cacheFile.absolutePath
                }
                
                when (val result = googleDriveService.downloadFile(fileId)) {
                    is GoogleDriveResult.Success -> {
                        try {
                            FileOutputStream(cacheFile).use { fos ->
                                fos.write(result.data)
                            }
                            cacheFile.absolutePath
                        } catch (e: Exception) {
                            null
                        }
                    }
                    is GoogleDriveResult.Error -> {
                        null
                    }
                }
            }
            
            if (localPath != null) {
                withContext(Dispatchers.Main) {
                    musicPlayerManager.playTrack(track.copy(filePath = localPath))
                }
            }
        } else {
             withContext(Dispatchers.Main) {
                musicPlayerManager.playTrack(track)
             }
        }
    }
}
