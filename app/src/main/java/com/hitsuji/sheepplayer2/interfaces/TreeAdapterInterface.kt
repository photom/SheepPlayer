package com.hitsuji.sheepplayer2.interfaces

import com.hitsuji.sheepplayer2.ui.tracks.TreeItem

/**
 * Interface for tree adapters following Interface Segregation Principle.
 * Allows different implementations without changing client code.
 */
interface TreeAdapterInterface {
    fun submitList(newItems: List<TreeItem>)
    fun getItem(position: Int): TreeItem?
    fun getItemCount(): Int
}

/**
 * Interface for handling tree item interactions.
 * Follows Single Responsibility Principle by separating interaction logic.
 */
interface TreeItemInteractionHandler {
    fun onTrackClick(track: com.hitsuji.sheepplayer2.Track)
    fun onTrackSwipe(track: com.hitsuji.sheepplayer2.Track)
    fun onAlbumSwipe(album: com.hitsuji.sheepplayer2.Album)
    fun onArtistExpand(artistId: Long, isExpanded: Boolean)
    fun onAlbumExpand(albumId: Long, isExpanded: Boolean)
}

/**
 * Interface for data filtering and search functionality.
 * Can be extended without modifying existing adapter code.
 */
interface TreeDataFilter {
    fun filter(query: String, items: List<TreeItem>): List<TreeItem>
    fun sort(items: List<TreeItem>, sortType: SortType): List<TreeItem>
}

enum class SortType {
    ALPHABETICAL,
    RECENTLY_ADDED,
    MOST_PLAYED,
    DURATION
}