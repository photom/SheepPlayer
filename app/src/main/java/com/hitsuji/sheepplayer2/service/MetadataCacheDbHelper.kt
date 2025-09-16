package com.hitsuji.sheepplayer2.service

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MetadataCacheDbHelper(context: Context) : SQLiteOpenHelper(
    context, 
    DATABASE_NAME, 
    null, 
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "metadata_cache.db"
        private const val DATABASE_VERSION = 2
        
        const val TABLE_METADATA = "metadata_cache"
        const val COLUMN_FILE_ID = "file_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_ARTIST_NAME = "artist_name"
        const val COLUMN_ALBUM_NAME = "album_name"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_TRACK_NUMBER = "track_number"
        const val COLUMN_ARTWORK = "artwork"
        const val COLUMN_CACHE_TIME = "cache_time"
        
        private const val SQL_CREATE_TABLE = """
            CREATE TABLE $TABLE_METADATA (
                $COLUMN_FILE_ID TEXT PRIMARY KEY,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_ARTIST_NAME TEXT NOT NULL,
                $COLUMN_ALBUM_NAME TEXT NOT NULL,
                $COLUMN_DURATION INTEGER NOT NULL,
                $COLUMN_TRACK_NUMBER INTEGER,
                $COLUMN_ARTWORK BLOB,
                $COLUMN_CACHE_TIME INTEGER NOT NULL
            )
        """
        
        private const val SQL_CREATE_INDEX_CACHE_TIME = """
            CREATE INDEX idx_cache_time ON $TABLE_METADATA($COLUMN_CACHE_TIME)
        """
        
        private const val SQL_DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_METADATA"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE)
        db.execSQL(SQL_CREATE_INDEX_CACHE_TIME)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DROP_TABLE)
        onCreate(db)
    }
    
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}