package com.hitsuji.sheepplayer2.ui.pictures

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hitsuji.sheepplayer2.MainActivity
import com.hitsuji.sheepplayer2.databinding.FragmentPicturesBinding

class PicturesFragment : Fragment() {

    private var _binding: FragmentPicturesBinding? = null
    private lateinit var picturesViewModel: PicturesViewModel
    private lateinit var imageAdapter: ArtistImageAdapter
    private var isUpdatingFromPlayingTrack = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        picturesViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(PicturesViewModel::class.java)

        _binding = FragmentPicturesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupObservers()
        setupArtistNameInput()

        return root
    }

    override fun onResume() {
        super.onResume()
        checkForPlayingTrack()
    }

    private fun setupRecyclerView() {
        imageAdapter = ArtistImageAdapter()
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        binding.recyclerArtistImages.apply {
            this.layoutManager = layoutManager
            adapter = imageAdapter
            setHasFixedSize(true) // Performance optimization
            isVerticalScrollBarEnabled = true

            // Add scroll listener for circular buffer
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // Check if scrolled to bottom
                    if (dy > 0) { // Scrolling down
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                        // If we're at the bottom and have exactly 10 images (no placeholder)
                        if (totalItemCount == 10 &&
                            firstVisibleItemPosition + visibleItemCount >= totalItemCount
                        ) {

                            android.util.Log.d(
                                "PicturesFragment",
                                "Reached bottom with full buffer, triggering circular buffer"
                            )
                            picturesViewModel.loadNextImageForCircularBuffer()
                        }
                    }
                }
            })
        }
    }

    private fun setupObservers() {
        val textView: TextView = binding.textPictures

        picturesViewModel.text.observe(viewLifecycleOwner) { text ->
            if (text.isBlank()) {
                // Hide the text view when there's no text
                textView.visibility = View.GONE
            } else {
                textView.visibility = View.VISIBLE
                textView.text = text
            }
        }

        // Observe when we get a complete new set of images (when changing artists)
        picturesViewModel.images.observe(viewLifecycleOwner) { images ->
            if (images.isEmpty()) {
                // Clear all images when starting fresh
                imageAdapter.clearImages()
            }
        }

        // Observe individual new images as they download
        picturesViewModel.newImage.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                // Add each new image as it downloads (sequential display)
                imageAdapter.addImage(bitmap)
            }
        }

        // Observe animated placeholder addition (added at bottom)
        picturesViewModel.addAnimatedPlaceholder.observe(viewLifecycleOwner) { shouldAdd ->
            if (shouldAdd == true) {
                // Add animated GIF placeholder at bottom of existing images
                imageAdapter.addAnimatedPlaceholderAtBottom()
            }
        }

        // Observe placeholder updates (update existing placeholder)
        picturesViewModel.updatePlaceholder.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                // Update the last image (placeholder) with new content
                imageAdapter.updateLastImage(bitmap)
            }
        }

        // Observe placeholder replacement (replace placeholder with real image)
        picturesViewModel.replacePlaceholder.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                // Replace placeholder with actual downloaded image
                android.util.Log.d("PicturesFragment", "replacePlaceholder observer triggered")
                imageAdapter.replacePlaceholderWithImage(bitmap)
            }
        }

        // Observe circular buffer update
        picturesViewModel.circularBufferUpdate.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                // Perform atomic circular buffer operation
                android.util.Log.d("PicturesFragment", "circularBufferUpdate observer triggered")
                imageAdapter.performCircularBufferUpdate(bitmap)
            }
        }
    }

    private fun setupArtistNameInput() {
        binding.editArtistName.apply {
            // Handle search when user presses Enter or Done
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {

                    val searchTerm = text.toString().trim()
                    if (searchTerm.isNotEmpty()) {
                        android.util.Log.d(
                            "PicturesFragment",
                            "User initiated search for: $searchTerm"
                        )
                        picturesViewModel.searchCustomArtist(searchTerm)

                        // Hide keyboard
                        val imm =
                            context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                        imm?.hideSoftInputFromWindow(windowToken, 0)
                    }
                    true
                } else {
                    false
                }
            }

            // Optional: Add TextWatcher for real-time search (commented out to avoid too many requests)
            /*
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // Could implement debounced search here if desired
                }
            })
            */
        }
    }

    private fun checkForPlayingTrack() {
        val mainActivity = activity as? MainActivity
        val currentTrack = mainActivity?.currentPlayingTrack

        if (currentTrack != null && mainActivity.isPlaying) {
            // Update EditText with current artist name (prevent triggering search)
            isUpdatingFromPlayingTrack = true
            binding.editArtistName.setText(currentTrack.artistName)
            isUpdatingFromPlayingTrack = false

            picturesViewModel.loadArtistImages(currentTrack.artistName)
        } else {
            // Clear EditText when no track is playing
            isUpdatingFromPlayingTrack = true
            binding.editArtistName.setText("")
            isUpdatingFromPlayingTrack = false

            picturesViewModel.clearImages()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear images (without recycling to avoid crashes)
        imageAdapter.clearImages()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        // Trim memory when fragment is not visible
        System.gc() // Suggest garbage collection
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Clear images on low memory (let GC handle bitmap cleanup)
        imageAdapter.clearImages()
        picturesViewModel.clearImages()
    }
}