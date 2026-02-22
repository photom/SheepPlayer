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
import com.hitsuji.sheepplayer2.SheepApplication
import com.hitsuji.sheepplayer2.factory.ViewModelFactory
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
        val application = requireActivity().application as SheepApplication
        val factory = ViewModelFactory(application.container)
        picturesViewModel = ViewModelProvider(this, factory).get(PicturesViewModel::class.java)

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
            setHasFixedSize(true)
            isVerticalScrollBarEnabled = true

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (dy > 0) {
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                        if (totalItemCount == 10 &&
                            firstVisibleItemPosition + visibleItemCount >= totalItemCount
                        ) {
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
                textView.visibility = View.GONE
            } else {
                textView.visibility = View.VISIBLE
                textView.text = text
            }
        }

        picturesViewModel.images.observe(viewLifecycleOwner) { images ->
            if (images.isEmpty()) {
                imageAdapter.clearImages()
            }
        }

        picturesViewModel.newImage.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                imageAdapter.addImage(bitmap)
            }
        }

        picturesViewModel.addAnimatedPlaceholder.observe(viewLifecycleOwner) { shouldAdd ->
            if (shouldAdd == true) {
                imageAdapter.addAnimatedPlaceholderAtBottom()
            }
        }

        picturesViewModel.replacePlaceholder.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                imageAdapter.replacePlaceholderWithImage(bitmap)
            }
        }

        picturesViewModel.circularBufferUpdate.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                imageAdapter.performCircularBufferUpdate(bitmap)
            }
        }
    }

    private fun setupArtistNameInput() {
        binding.editArtistName.apply {
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    val searchTerm = text.toString().trim()
                    if (searchTerm.isNotEmpty()) {
                        picturesViewModel.searchCustomArtist(searchTerm)
                        val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                        imm?.hideSoftInputFromWindow(windowToken, 0)
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun checkForPlayingTrack() {
        val mainActivity = activity as? MainActivity
        val currentTrack = mainActivity?.currentPlayingTrack

        if (currentTrack != null && mainActivity.isPlaying) {
            isUpdatingFromPlayingTrack = true
            binding.editArtistName.setText(currentTrack.artistName)
            isUpdatingFromPlayingTrack = false
            picturesViewModel.loadArtistImages(currentTrack.artistName)
        } else {
            isUpdatingFromPlayingTrack = true
            binding.editArtistName.setText("")
            isUpdatingFromPlayingTrack = false
            picturesViewModel.clearImages()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageAdapter.clearImages()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        System.gc()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        imageAdapter.clearImages()
        picturesViewModel.clearImages()
    }
}
