package com.hitsuji.sheepplayer2.ui.playing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hitsuji.sheepplayer2.R
import com.hitsuji.sheepplayer2.Track
import com.hitsuji.sheepplayer2.utils.TimeUtils

class AlbumTrackAdapter(
    private val onTrackClick: (Track, Int) -> Unit
) : RecyclerView.Adapter<AlbumTrackAdapter.TrackViewHolder>() {

    private val tracks = mutableListOf<Track>()
    private var currentPlayingIndex = -1

    fun submitTracks(newTracks: List<Track>, playingIndex: Int = -1) {
        tracks.clear()
        tracks.addAll(newTracks)
        currentPlayingIndex = playingIndex
        notifyDataSetChanged()
    }

    fun updatePlayingIndex(playingIndex: Int) {
        val oldIndex = currentPlayingIndex
        currentPlayingIndex = playingIndex
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (currentPlayingIndex != -1) notifyItemChanged(currentPlayingIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track_simple, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(tracks[position], position + 1, position == currentPlayingIndex)
    }

    override fun getItemCount(): Int = tracks.size

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val trackNumber: TextView = itemView.findViewById(R.id.trackNumber)
        private val trackTitle: TextView = itemView.findViewById(R.id.trackTitle)
        private val trackDuration: TextView = itemView.findViewById(R.id.trackDuration)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTrackClick(tracks[position], position)
                }
            }
        }

        fun bind(track: Track, number: Int, isPlaying: Boolean) {
            trackNumber.text = number.toString()
            trackTitle.text = track.title
            trackDuration.text = TimeUtils.formatDuration(track.duration)

            // Highlight currently playing track
            val textColor = if (isPlaying) {
                itemView.context.getColor(android.R.color.holo_blue_bright)
            } else {
                val typedArray = itemView.context.theme.obtainStyledAttributes(
                    intArrayOf(android.R.attr.textColorPrimary)
                )
                val color = typedArray.getColor(0, 0)
                typedArray.recycle()
                color
            }
            trackTitle.setTextColor(textColor)
            trackNumber.setTextColor(
                if (isPlaying) textColor else {
                    val typedArray = itemView.context.theme.obtainStyledAttributes(
                        intArrayOf(android.R.attr.textColorSecondary)
                    )
                    val color = typedArray.getColor(0, 0)
                    typedArray.recycle()
                    color
                }
            )
        }
    }
}