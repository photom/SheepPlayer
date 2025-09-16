package com.hitsuji.sheepplayer2.ui.tracks

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.hitsuji.sheepplayer2.Album
import com.hitsuji.sheepplayer2.Track

class SwipeToPlayHelper(
    private val adapter: TreeAdapter,
    private val onTrackSwipe: (Track) -> Unit,
    private val onAlbumSwipe: (Album) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION && direction == ItemTouchHelper.RIGHT) {
            val item = adapter.getItem(position)
            when (item) {
                is TreeItem.TrackItem -> {
                    onTrackSwipe(item.track)
                    // Reset the view position
                    adapter.notifyItemChanged(position)
                }

                is TreeItem.AlbumItem -> {
                    onAlbumSwipe(item.album)
                    // Reset the view position
                    adapter.notifyItemChanged(position)
                }

                is TreeItem.ArtistItem -> {
                    // Artists are not swipeable for playback
                    adapter.notifyItemChanged(position)
                }

                null -> {
                    // Handle null case
                }
            }
        }
    }

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Allow swiping on both track and album items
        val position = viewHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            val item = adapter.getItem(position)
            if (item is TreeItem.TrackItem || item is TreeItem.AlbumItem) {
                return super.getSwipeDirs(recyclerView, viewHolder)
            }
        }
        return 0
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}