package com.hitsuji.sheepplayer2.ui.tracks

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hitsuji.sheepplayer2.R
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.utils.Constants
import com.hitsuji.sheepplayer2.utils.TimeUtils

class TreeAdapter(
    private val onTrackClick: (Track) -> Unit = {},
    private val onTrackSwipe: (Track) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<TreeItem>()


    fun submitList(newItems: List<TreeItem>) {
        android.util.Log.d("TreeAdapter", "*** submitList called with ${newItems.size} items ***")
        items.clear()
        items.addAll(newItems)
        android.util.Log.d("TreeAdapter", "*** items list now has ${items.size} items, calling notifyDataSetChanged() ***")
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TreeItem.ArtistItem -> Constants.ViewTypes.ARTIST
            is TreeItem.AlbumItem -> Constants.ViewTypes.ALBUM
            is TreeItem.TrackItem -> Constants.ViewTypes.TRACK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            Constants.ViewTypes.ARTIST -> {
                val view = inflater.inflate(R.layout.item_artist, parent, false)
                ArtistViewHolder(view) { position ->
                    toggleArtistExpansion(position)
                }
            }

            Constants.ViewTypes.ALBUM -> {
                val view = inflater.inflate(R.layout.item_album, parent, false)
                AlbumViewHolder(view) { position ->
                    toggleAlbumExpansion(position)
                }
            }

            Constants.ViewTypes.TRACK -> {
                val view = inflater.inflate(R.layout.item_track_tree, parent, false)
                TrackViewHolder(view, onTrackClick)
            }

            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TreeItem.ArtistItem -> (holder as ArtistViewHolder).bind(item)
            is TreeItem.AlbumItem -> (holder as AlbumViewHolder).bind(item)
            is TreeItem.TrackItem -> (holder as TrackViewHolder).bind(item.track)
        }
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): TreeItem? {
        return if (position in 0 until items.size) items[position] else null
    }

    private fun toggleArtistExpansion(position: Int) {
        val artistItem = items[position] as TreeItem.ArtistItem
        artistItem.isExpanded = !artistItem.isExpanded

        if (artistItem.isExpanded) {
            // Add albums
            val albumsToAdd = artistItem.artist.albums.map { album ->
                TreeItem.AlbumItem(album, false)
            }
            items.addAll(position + 1, albumsToAdd)
            notifyItemRangeInserted(position + 1, albumsToAdd.size)
        } else {
            // Remove albums and tracks
            val toRemove = mutableListOf<Int>()
            for (i in position + 1 until items.size) {
                when (val item = items[i]) {
                    is TreeItem.AlbumItem -> {
                        if (item.album.artistName == artistItem.artist.name) {
                            toRemove.add(i)
                        } else {
                            break
                        }
                    }

                    is TreeItem.TrackItem -> {
                        if (item.track.artistName == artistItem.artist.name) {
                            toRemove.add(i)
                        } else {
                            break
                        }
                    }

                    else -> break
                }
            }

            // Remove in reverse order to maintain correct indices
            toRemove.reversed().forEach { index ->
                items.removeAt(index)
            }
            notifyItemRangeRemoved(position + 1, toRemove.size)
        }

        notifyItemChanged(position)
    }

    private fun toggleAlbumExpansion(position: Int) {
        val albumItem = items[position] as TreeItem.AlbumItem
        albumItem.isExpanded = !albumItem.isExpanded

        if (albumItem.isExpanded) {
            // Add tracks
            val tracksToAdd = albumItem.album.tracks.map { track ->
                TreeItem.TrackItem(track)
            }
            items.addAll(position + 1, tracksToAdd)
            notifyItemRangeInserted(position + 1, tracksToAdd.size)
        } else {
            // Remove tracks
            val toRemove = mutableListOf<Int>()
            for (i in position + 1 until items.size) {
                when (val item = items[i]) {
                    is TreeItem.TrackItem -> {
                        if (item.track.albumName == albumItem.album.title &&
                            item.track.artistName == albumItem.album.artistName
                        ) {
                            toRemove.add(i)
                        } else {
                            break
                        }
                    }

                    else -> break
                }
            }

            // Remove in reverse order to maintain correct indices
            toRemove.reversed().forEach { index ->
                items.removeAt(index)
            }
            notifyItemRangeRemoved(position + 1, toRemove.size)
        }

        notifyItemChanged(position)
    }

    class ArtistViewHolder(
        itemView: View,
        private val onExpandClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        private val artistName: TextView = itemView.findViewById(R.id.artistName)
        private val albumCount: TextView = itemView.findViewById(R.id.albumCount)

        init {
            itemView.setOnClickListener {
                onExpandClick(adapterPosition)
            }
        }

        fun bind(item: TreeItem.ArtistItem) {
            artistName.text = item.artist.name
            albumCount.text = "${item.artist.albums.size} albums"
            expandIcon.setImageResource(
                if (item.isExpanded) R.drawable.ic_close
                else R.drawable.ic_add
            )
        }
    }

    class AlbumViewHolder(
        itemView: View,
        private val onExpandClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        private val albumArt: ImageView = itemView.findViewById(R.id.albumArt)
        private val albumTitle: TextView = itemView.findViewById(R.id.albumTitle)
        private val trackCount: TextView = itemView.findViewById(R.id.trackCount)

        init {
            itemView.setOnClickListener {
                onExpandClick(adapterPosition)
            }
        }

        fun bind(item: TreeItem.AlbumItem) {
            albumTitle.text = item.album.title
            trackCount.text = "${item.album.tracks.size} tracks"
            expandIcon.setImageResource(
                if (item.isExpanded) R.drawable.ic_close
                else R.drawable.ic_add
            )

            // Load album art from first track if available
            loadAlbumArt(item.album.tracks.firstOrNull()?.albumArtUri, albumArt)
        }

        private fun loadAlbumArt(albumArtUri: String?, imageView: ImageView) {
            albumArtUri?.let { uriString ->
                try {
                    val uri = Uri.parse(uriString)
                    imageView.setImageURI(uri)
                } catch (e: Exception) {
                    android.util.Log.w("TreeAdapter", "Failed to load album art", e)
                    // If loading fails, keep the default background
                }
            }
        }
    }

    class TrackViewHolder(
        itemView: View,
        private val onTrackClick: (Track) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val trackTitle: TextView = itemView.findViewById(R.id.trackTitle)
        private val duration: TextView = itemView.findViewById(R.id.duration)

        fun bind(track: Track) {
            trackTitle.text = track.title
            duration.text = TimeUtils.formatDuration(track.duration)

            itemView.setOnClickListener {
                onTrackClick(track)
            }
        }

    }
}