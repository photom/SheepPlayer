package com.hitsuji.sheepplayer2.service

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.hitsuji.sheepplayer2.CachedMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MetadataCache(context: Context) {
    private val dbHelper = MetadataCacheDbHelper(context)
    
    companion object {
        private const val TAG = "MetadataCache"
        private const val CACHE_EXPIRY_DAYS = 7
        private const val MAX_CACHE_SIZE = 100000
        private const val CACHE_EXPIRY_MS = CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L
    }
    
    init {
        // Clean up expired entries on initialization
        cleanupExpiredEntries()
    }
    
    suspend fun getCachedMetadata(fileId: String): CachedMetadata? = withContext(Dispatchers.IO) {
        var cursor: Cursor? = null
        try {
            val db = dbHelper.readableDatabase
            cursor = db.query(
                MetadataCacheDbHelper.TABLE_METADATA,
                arrayOf(
                    MetadataCacheDbHelper.COLUMN_FILE_ID,
                    MetadataCacheDbHelper.COLUMN_TITLE,
                    MetadataCacheDbHelper.COLUMN_ARTIST_NAME,
                    MetadataCacheDbHelper.COLUMN_ALBUM_NAME,
                    MetadataCacheDbHelper.COLUMN_DURATION,
                    MetadataCacheDbHelper.COLUMN_TRACK_NUMBER,
                    MetadataCacheDbHelper.COLUMN_ARTWORK,
                    MetadataCacheDbHelper.COLUMN_CACHE_TIME
                ),
                "${MetadataCacheDbHelper.COLUMN_FILE_ID} = ?",
                arrayOf(fileId),
                null,
                null,
                null
            )
            
            if (cursor.moveToFirst()) {
                val cacheTime = cursor.getLong(cursor.getColumnIndexOrThrow(MetadataCacheDbHelper.COLUMN_CACHE_TIME))
                val now = System.currentTimeMillis()
                
                // Check if entry is still valid
                if ((now - cacheTime) < CACHE_EXPIRY_MS) {
                    val artwork = cursor.getBlob(cursor.getColumnIndexOrThrow(MetadataCacheDbHelper.COLUMN_ARTWORK))
                    val trackNumber = cursor.getInt(cursor.getColumnIndexOrThrow(MetadataCacheDbHelper.COLUMN_TRACK_NUMBER))
                    val metadata = CachedMetadata(
                        fileId = cursor.getString(cursor.getColumnIndexOrThrow(MetadataCacheDbHelper.COLUMN_FILE_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(MetadataCacheDbHelper.COLUMN_TITLE)),
                        artistName = cursor.getString(cursor.getColumnIndexOrThrow(MetadataCacheDbHelper.COLUMN_ARTIST_NAME)),
                        albumName = cursor.getString(cursor.getColumnIndexOrThrow(MetadataCacheDbHelper.COLUMN_ALBUM_NAME)),
                        duration = cursor.getLong(cursor.getColumnIndexOrThrow(MetadataCacheDbHelper.COLUMN_DURATION)),
                        trackNumber = if (cursor.isNull(cursor.getColumnIndexOrThrow(MetadataCacheDbHelper.COLUMN_TRACK_NUMBER))) null else trackNumber,
                        artwork = artwork,
                        cacheTime = cacheTime
                    )
                    return@withContext metadata
                } else {
                    // Remove expired entry
                    removeExpiredEntry(fileId)
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached metadata for fileId: $fileId", e)
            null
        } finally {
            cursor?.close()
        }
    }
    
    suspend fun cacheMetadata(metadata: CachedMetadata) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            
            // Check cache size and cleanup if necessary
            maintainCacheSize(db)
            
            val values = ContentValues().apply {
                put(MetadataCacheDbHelper.COLUMN_FILE_ID, metadata.fileId)
                put(MetadataCacheDbHelper.COLUMN_TITLE, metadata.title)
                put(MetadataCacheDbHelper.COLUMN_ARTIST_NAME, metadata.artistName)
                put(MetadataCacheDbHelper.COLUMN_ALBUM_NAME, metadata.albumName)
                put(MetadataCacheDbHelper.COLUMN_DURATION, metadata.duration)
                put(MetadataCacheDbHelper.COLUMN_TRACK_NUMBER, metadata.trackNumber)
                put(MetadataCacheDbHelper.COLUMN_ARTWORK, metadata.artwork)
                put(MetadataCacheDbHelper.COLUMN_CACHE_TIME, metadata.cacheTime)
            }
            
            // Insert or replace the metadata
            val result = db.insertWithOnConflict(
                MetadataCacheDbHelper.TABLE_METADATA,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            
            if (result == -1L) {
                Log.w(TAG, "Failed to cache metadata for fileId: ${metadata.fileId}")
            } else {
                Log.d(TAG, "Successfully cached metadata for fileId: ${metadata.fileId}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error caching metadata for fileId: ${metadata.fileId}", e)
        }
    }
    
    private fun maintainCacheSize(db: SQLiteDatabase) {
        try {
            val countCursor = db.rawQuery("SELECT COUNT(*) FROM ${MetadataCacheDbHelper.TABLE_METADATA}", null)
            countCursor.use { cursor ->
                if (cursor.moveToFirst()) {
                    val currentSize = cursor.getInt(0)
                    
                    if (currentSize >= MAX_CACHE_SIZE) {
                        // Remove oldest 25% of entries
                        val entriesToRemove = MAX_CACHE_SIZE / 4
                        val deleteQuery = """
                            DELETE FROM ${MetadataCacheDbHelper.TABLE_METADATA} 
                            WHERE ${MetadataCacheDbHelper.COLUMN_FILE_ID} IN (
                                SELECT ${MetadataCacheDbHelper.COLUMN_FILE_ID} 
                                FROM ${MetadataCacheDbHelper.TABLE_METADATA} 
                                ORDER BY ${MetadataCacheDbHelper.COLUMN_CACHE_TIME} ASC 
                                LIMIT $entriesToRemove
                            )
                        """.trimIndent()
                        
                        db.execSQL(deleteQuery)
                        Log.d(TAG, "Removed $entriesToRemove old cache entries to maintain size limit")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error maintaining cache size", e)
        }
    }
    
    private fun removeExpiredEntry(fileId: String) {
        try {
            val db = dbHelper.writableDatabase
            val deletedRows = db.delete(
                MetadataCacheDbHelper.TABLE_METADATA,
                "${MetadataCacheDbHelper.COLUMN_FILE_ID} = ?",
                arrayOf(fileId)
            )
            
            if (deletedRows > 0) {
                Log.d(TAG, "Removed expired cache entry for fileId: $fileId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing expired entry for fileId: $fileId", e)
        }
    }
    
    private fun cleanupExpiredEntries() {
        try {
            val db = dbHelper.writableDatabase
            val expiryTime = System.currentTimeMillis() - CACHE_EXPIRY_MS
            
            val deletedRows = db.delete(
                MetadataCacheDbHelper.TABLE_METADATA,
                "${MetadataCacheDbHelper.COLUMN_CACHE_TIME} < ?",
                arrayOf(expiryTime.toString())
            )
            
            if (deletedRows > 0) {
                Log.d(TAG, "Cleaned up $deletedRows expired cache entries")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up expired entries", e)
        }
    }
    
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val deletedRows = db.delete(MetadataCacheDbHelper.TABLE_METADATA, null, null)
            Log.d(TAG, "Cleared $deletedRows cache entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }
    
    suspend fun getCacheSize(): Int = withContext(Dispatchers.IO) {
        var cursor: Cursor? = null
        try {
            val db = dbHelper.readableDatabase
            cursor = db.rawQuery("SELECT COUNT(*) FROM ${MetadataCacheDbHelper.TABLE_METADATA}", null)
            
            if (cursor.moveToFirst()) {
                return@withContext cursor.getInt(0)
            }
            
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache size", e)
            0
        } finally {
            cursor?.close()
        }
    }
    
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        var cursor: Cursor? = null
        try {
            val db = dbHelper.readableDatabase
            
            // Get total count and oldest/newest timestamps
            cursor = db.rawQuery("""
                SELECT 
                    COUNT(*) as total_count,
                    MIN(${MetadataCacheDbHelper.COLUMN_CACHE_TIME}) as oldest_time,
                    MAX(${MetadataCacheDbHelper.COLUMN_CACHE_TIME}) as newest_time
                FROM ${MetadataCacheDbHelper.TABLE_METADATA}
            """.trimIndent(), null)
            
            if (cursor.moveToFirst()) {
                val totalCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_count"))
                val oldestTime = cursor.getLong(cursor.getColumnIndexOrThrow("oldest_time"))
                val newestTime = cursor.getLong(cursor.getColumnIndexOrThrow("newest_time"))
                
                return@withContext CacheStats(
                    totalEntries = totalCount,
                    oldestEntryTime = if (totalCount > 0) oldestTime else 0L,
                    newestEntryTime = if (totalCount > 0) newestTime else 0L
                )
            }
            
            CacheStats(0, 0L, 0L)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache stats", e)
            CacheStats(0, 0L, 0L)
        } finally {
            cursor?.close()
        }
    }
    
    fun close() {
        dbHelper.close()
    }
}

data class CacheStats(
    val totalEntries: Int,
    val oldestEntryTime: Long,
    val newestEntryTime: Long
)