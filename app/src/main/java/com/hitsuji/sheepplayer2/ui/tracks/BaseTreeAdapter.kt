package com.hitsuji.sheepplayer2.ui.tracks

import androidx.recyclerview.widget.RecyclerView
import com.hitsuji.sheepplayer2.Album
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.interfaces.SortType
import com.hitsuji.sheepplayer2.interfaces.TreeAdapterInterface
import com.hitsuji.sheepplayer2.interfaces.TreeDataFilter
import com.hitsuji.sheepplayer2.interfaces.TreeItemInteractionHandler

/**
 * Base adapter class following Open/Closed Principle.
 * 
 * This class is:
 * - Open for extension: Can be subclassed to add new functionality
 * - Closed for modification: Core functionality doesn't need to change
 * 
 * Features:
 * - Template method pattern for customizable behavior
 * - Strategy pattern for filtering and sorting
 * - Observer pattern for interaction handling
 */
abstract class BaseTreeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), TreeAdapterInterface {
    
    protected val items = mutableListOf<TreeItem>()
    protected var interactionHandler: TreeItemInteractionHandler? = null
    protected var dataFilter: TreeDataFilter? = null
    
    override fun submitList(newItems: List<TreeItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    override fun getItem(position: Int): TreeItem? {
        return items.getOrNull(position)
    }
    
    override fun getItemCount(): Int = items.size
    
    // Template method - subclasses can override specific behavior
    protected open fun onTrackClicked(track: Track) {
        interactionHandler?.onTrackClick(track)
    }
    
    protected open fun onTrackSwiped(track: Track) {
        interactionHandler?.onTrackSwipe(track)
    }
    
    protected open fun onAlbumSwiped(album: Album) {
        interactionHandler?.onAlbumSwipe(album)
    }
    
    protected open fun onArtistExpanded(artistId: Long, isExpanded: Boolean) {
        interactionHandler?.onArtistExpand(artistId, isExpanded)
    }
    
    protected open fun onAlbumExpanded(albumId: Long, isExpanded: Boolean) {
        interactionHandler?.onAlbumExpand(albumId, isExpanded)
    }
    
    // Strategy method for filtering
    fun filterItems(query: String) {
        val filteredItems = dataFilter?.filter(query, items.toList()) ?: items.toList()
        submitList(filteredItems)
    }
    
    // Strategy method for sorting
    fun sortItems(sortType: SortType) {
        val sortedItems = dataFilter?.sort(items.toList(), sortType) ?: items.toList()
        submitList(sortedItems)
    }
    
    // Abstract methods that must be implemented by subclasses
    protected abstract fun createTreeViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder
    protected abstract fun bindTreeViewHolder(holder: RecyclerView.ViewHolder, position: Int)
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return createTreeViewHolder(parent, viewType)
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        bindTreeViewHolder(holder, position)
    }
}