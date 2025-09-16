package com.hitsuji.sheepplayer2.ui.tracks

import com.hitsuji.sheepplayer2.Album
import com.hitsuji.sheepplayer2.Artist
import com.hitsuji.sheepplayer2.interfaces.SortType
import com.hitsuji.sheepplayer2.interfaces.TreeDataFilter

/**
 * Default implementation of TreeDataFilter following Strategy Pattern.
 * 
 * This class can be easily extended or replaced without modifying
 * the adapter code, demonstrating Open/Closed Principle.
 */
class DefaultTreeDataFilter : TreeDataFilter {
    
    override fun filter(query: String, items: List<TreeItem>): List<TreeItem> {
        if (query.isBlank()) return items
        
        val filteredItems = mutableListOf<TreeItem>()
        val queryLower = query.lowercase()
        
        items.forEach { item ->
            when (item) {
                is TreeItem.ArtistItem -> {
                    val matchingAlbums = filterArtistAlbums(item.artist, queryLower)
                    if (matchingAlbums.isNotEmpty()) {
                        val filteredArtist = item.artist.copy(
                            albums = matchingAlbums.toMutableList()
                        )
                        filteredItems.add(TreeItem.ArtistItem(filteredArtist, item.isExpanded))
                        
                        // Add matching albums and tracks if artist is expanded
                        if (item.isExpanded) {
                            matchingAlbums.forEach { album ->
                                filteredItems.add(TreeItem.AlbumItem(album, false))
                                // Add tracks if album matches or contains matching tracks
                                album.tracks.forEach { track ->
                                    filteredItems.add(TreeItem.TrackItem(track))
                                }
                            }
                        }
                    }
                }
                
                is TreeItem.AlbumItem -> {
                    if (albumMatches(item.album, queryLower)) {
                        filteredItems.add(item)
                    }
                }
                
                is TreeItem.TrackItem -> {
                    if (trackMatches(item.track, queryLower)) {
                        filteredItems.add(item)
                    }
                }
            }
        }
        
        return filteredItems
    }
    
    override fun sort(items: List<TreeItem>, sortType: SortType): List<TreeItem> {
        return when (sortType) {
            SortType.ALPHABETICAL -> sortAlphabetically(items)
            SortType.RECENTLY_ADDED -> sortByRecentlyAdded(items)
            SortType.MOST_PLAYED -> sortByMostPlayed(items)
            SortType.DURATION -> sortByDuration(items)
        }
    }
    
    private fun filterArtistAlbums(artist: Artist, queryLower: String): List<Album> {
        // Check if artist name matches
        if (artist.name.lowercase().contains(queryLower)) {
            return artist.albums
        }
        
        // Filter albums by query
        return artist.albums.mapNotNull { album ->
            val matchingTracks = album.tracks.filter { track ->
                trackMatches(track, queryLower)
            }
            
            if (albumMatches(album, queryLower) || matchingTracks.isNotEmpty()) {
                album.copy(tracks = matchingTracks.toMutableList())
            } else null
        }
    }
    
    private fun albumMatches(album: Album, queryLower: String): Boolean {
        return album.title.lowercase().contains(queryLower) ||
               album.artistName.lowercase().contains(queryLower)
    }
    
    private fun trackMatches(track: com.hitsuji.sheepplayer2.Track, queryLower: String): Boolean {
        return track.title.lowercase().contains(queryLower) ||
               track.artistName.lowercase().contains(queryLower) ||
               track.albumName.lowercase().contains(queryLower)
    }
    
    private fun sortAlphabetically(items: List<TreeItem>): List<TreeItem> {
        return items.sortedWith(compareBy { item ->
            when (item) {
                is TreeItem.ArtistItem -> item.artist.name
                is TreeItem.AlbumItem -> item.album.title
                is TreeItem.TrackItem -> item.track.title
            }
        })
    }
    
    private fun sortByRecentlyAdded(items: List<TreeItem>): List<TreeItem> {
        // For now, return as-is since we don't have timestamps
        // This could be extended to use file creation dates
        return items
    }
    
    private fun sortByMostPlayed(items: List<TreeItem>): List<TreeItem> {
        // For now, return as-is since we don't track play counts
        // This could be extended with a play count tracking system
        return items
    }
    
    private fun sortByDuration(items: List<TreeItem>): List<TreeItem> {
        return items.sortedWith(compareBy { item ->
            when (item) {
                is TreeItem.ArtistItem -> {
                    item.artist.albums.sumOf { album ->
                        album.tracks.sumOf { it.duration }
                    }
                }
                is TreeItem.AlbumItem -> {
                    item.album.tracks.sumOf { it.duration }
                }
                is TreeItem.TrackItem -> item.track.duration
            }
        }).reversed() // Longest first
    }
}