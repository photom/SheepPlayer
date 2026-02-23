package com.hitsuji.sheepplayer2

data class Artist(
    val id: Long, // MediaStore ID for the artist (optional but useful)
    val name: String,
    val albums: MutableList<Album> = mutableListOf() // Use MutableList to build it up
)

data class Album(
    val id: Long, // MediaStore ID for the album
    val title: String,
    val artistName: String,
    val tracks: MutableList<Track> = mutableListOf() // Use MutableList
)

data class Track(
    val id: Long, // MediaStore ID for the track
    val title: String,
    val artistName: String,
    val albumName: String,
    val duration: Long, // Duration in milliseconds
    val filePath: String, // URI path to the file
    val albumArtUri: String? = null, // Optional: URI for album art
    val trackNumber: Int? = null, // Track number for ordering within albums
    val googleDriveFileId: String? = null, // Google Drive file ID for caching
    val isMetadataLoaded: Boolean = true // Track if metadata is fully loaded
) {
    init {
        require(duration >= 0) { "Track duration cannot be negative" }
        require(title.isNotBlank()) { "Track title cannot be blank" }
    }
}

data class CachedMetadata(
    val fileId: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val duration: Long,
    val trackNumber: Int? = null,
    val artwork: ByteArray? = null,
    val cacheTime: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedMetadata

        if (fileId != other.fileId) return false
        if (title != other.title) return false
        if (artistName != other.artistName) return false
        if (albumName != other.albumName) return false
        if (duration != other.duration) return false
        if (trackNumber != other.trackNumber) return false
        if (artwork != null) {
            if (other.artwork == null) return false
            if (!artwork.contentEquals(other.artwork)) return false
        } else if (other.artwork != null) return false
        if (cacheTime != other.cacheTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artistName.hashCode()
        result = 31 * result + albumName.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (artwork?.contentHashCode() ?: 0)
        result = 31 * result + cacheTime.hashCode()
        return result
    }
}
