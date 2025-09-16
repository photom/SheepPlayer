package com.hitsuji.sheepplayer2.ui.tracks

import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.Album
import com.hitsuji.sheepplayer2.Track

sealed class TreeItem(
    val level: Int
) {
    data class ArtistItem(
        val artist: Artist,
        var isExpanded: Boolean = false
    ) : TreeItem(0)

    data class AlbumItem(
        val album: Album,
        var isExpanded: Boolean = false
    ) : TreeItem(1)

    data class TrackItem(
        val track: Track
    ) : TreeItem(2)
}