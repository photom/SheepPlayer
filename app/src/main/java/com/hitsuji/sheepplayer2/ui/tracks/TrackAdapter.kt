package com.hitsuji.sheepplayer2.ui.tracks

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hitsuji.sheepplayer2.R
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.utils.TimeUtils

class TrackAdapter(
    private val onTrackClick: (Track) -> Unit = {}
) : ListAdapter<Track, TrackAdapter.TrackViewHolder>(TrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view, onTrackClick)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TrackViewHolder(
        itemView: View,
        private val onTrackClick: (Track) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val albumArt: ImageView = itemView.findViewById(R.id.albumArt)
        private val trackTitle: TextView = itemView.findViewById(R.id.trackTitle)
        private val artistName: TextView = itemView.findViewById(R.id.artistName)
        private val duration: TextView = itemView.findViewById(R.id.duration)

        fun bind(track: Track) {
            trackTitle.text = track.title
            artistName.text = track.artistName
            duration.text = TimeUtils.formatDuration(track.duration)

            // Load album art if available
            track.albumArtUri?.let { uriString ->
                try {
                    val uri = Uri.parse(uriString)
                    albumArt.setImageURI(uri)
                } catch (e: Exception) {
                    // If loading fails, keep the default background
                }
            }

            itemView.setOnClickListener {
                onTrackClick(track)
            }
        }

    }
}

class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
    override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem == newItem
    }
}