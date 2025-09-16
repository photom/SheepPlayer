package com.hitsuji.sheepplayer2.ui.pictures

import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hitsuji.sheepplayer2.R

class ArtistImageAdapter : RecyclerView.Adapter<ArtistImageAdapter.ImageViewHolder>() {

    private val images = mutableListOf<Any>() // Can hold Bitmap or special placeholder marker
    private var isPlaceholderActive = false

    companion object {
        private const val PLACEHOLDER_GIF = "PLACEHOLDER_GIF"
    }

    fun updateImages(newImages: List<Bitmap>) {
        // Don't clear existing images, just add new ones up to max limit
        val oldSize = images.size

        newImages.forEach { newBitmap ->
            if (images.size < 10 && !images.contains(newBitmap)) {
                images.add(newBitmap)
            }
        }

        // Notify about the new items added
        if (images.size > oldSize) {
            notifyItemRangeInserted(oldSize, images.size - oldSize)
        }
    }

    fun replaceAllImages(newImages: List<Bitmap>) {
        // This method replaces all images (used when changing artists)
        // Don't recycle here - let GC handle it to avoid crashes
        images.clear()
        images.addAll(newImages.take(10))
        isPlaceholderActive = false
        notifyDataSetChanged()
    }

    fun addImage(image: Bitmap) {
        if (images.size < 10 && !images.contains(image)) { // Limit to max 10 images and avoid duplicates
            images.add(image)
            notifyItemInserted(images.size - 1)
        }
    }

    fun updateLastImage(newImage: Bitmap) {
        if (images.isNotEmpty()) {
            // Update the last image (which should be the placeholder at bottom)
            val lastIndex = images.size - 1
            // Don't recycle the old bitmap - let GC handle it to avoid crashes
            // The UI might still be using it for drawing
            images[lastIndex] = newImage
            notifyItemChanged(lastIndex)
        } else {
            // If no images, add as first image
            addImage(newImage)
        }
    }

    fun addAnimatedPlaceholderAtBottom() {
        if (images.size < 10 && !isPlaceholderActive) { // Ensure we don't exceed max limit and no duplicate placeholder
            images.add(PLACEHOLDER_GIF)
            isPlaceholderActive = true
            notifyItemInserted(images.size - 1)
            Log.d("ArtistImageAdapter", "Added animated GIF placeholder at bottom")
        }
    }

    fun replacePlaceholderWithImage(realImage: Bitmap) {
        Log.d(
            "ArtistImageAdapter",
            "replacePlaceholderWithImage called, current images: ${images.size}, isPlaceholderActive: $isPlaceholderActive"
        )
        if (images.isNotEmpty() && isPlaceholderActive) {
            // Replace the last image (placeholder) with actual downloaded image
            val lastIndex = images.size - 1
            if (images[lastIndex] == PLACEHOLDER_GIF) {
                Log.d(
                    "ArtistImageAdapter",
                    "Replacing animated GIF placeholder at index $lastIndex with downloaded image"
                )
                images[lastIndex] = realImage
                isPlaceholderActive = false
                notifyItemChanged(lastIndex)
            } else {
                Log.w("ArtistImageAdapter", "Last item is not a placeholder, adding as new image")
                addImage(realImage)
            }
        } else {
            // Fallback: add as new image if no placeholder exists
            Log.d("ArtistImageAdapter", "No placeholder found, adding as new image")
            addImage(realImage)
        }
    }

    fun performCircularBufferUpdate(newBottomImage: Bitmap) {
        if (images.size == 10 && !isPlaceholderActive) {
            Log.d("ArtistImageAdapter", "Performing circular buffer update")

            // Remove top image and add new image at bottom in one operation
            images.removeAt(0)
            images.add(newBottomImage)

            // Notify about the range of changes to prevent full rebind
            notifyItemRemoved(0)
            notifyItemInserted(images.size - 1)

            Log.d("ArtistImageAdapter", "Circular buffer update completed")
        }
    }

    fun clearImages() {
        // Don't recycle bitmaps here - let GC handle it to avoid crashes
        // The UI might still be referencing these bitmaps for drawing
        images.clear()
        isPlaceholderActive = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val imageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_artist_image, parent, false) as ImageView
        return ImageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = images[position]
        if (item == PLACEHOLDER_GIF) {
            holder.bindAnimatedPlaceholder()
        } else if (item is Bitmap) {
            holder.bind(item)
        }
    }

    override fun getItemCount(): Int = images.size

    override fun onViewRecycled(holder: ImageViewHolder) {
        super.onViewRecycled(holder)
        holder.clearImage()
    }

    class ImageViewHolder(private val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
        fun bind(bitmap: Bitmap) {
            try {
                if (!bitmap.isRecycled) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    // If bitmap is recycled, clear the image to avoid crashes
                    imageView.setImageDrawable(null)
                }
            } catch (e: Exception) {
                // If any error occurs (like recycled bitmap), clear the image
                imageView.setImageDrawable(null)
            }
        }

        fun bindAnimatedPlaceholder() {
            try {
                // Use Glide to load and animate the GIF
                Glide.with(imageView.context)
                    .asGif()
                    .load(R.drawable.noddingsheep2)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache the GIF resource
                    .into(imageView)
                Log.d("ImageViewHolder", "Loading animated GIF placeholder with Glide")
            } catch (e: Exception) {
                Log.e("ImageViewHolder", "Failed to load animated GIF placeholder", e)
                imageView.setImageDrawable(null)
            }
        }

        fun clearImage() {
            // Clear any Glide loading and set to null
            try {
                Glide.with(imageView.context).clear(imageView)
                imageView.setImageDrawable(null)
            } catch (e: Exception) {
                imageView.setImageDrawable(null)
            }
        }
    }
}