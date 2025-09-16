package com.hitsuji.sheepplayer2.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.hitsuji.sheepplayer2.MainActivity
import com.hitsuji.sheepplayer2.databinding.FragmentMenuBinding
import com.hitsuji.sheepplayer2.service.GoogleDriveService
import com.hitsuji.sheepplayer2.service.GoogleDriveResult
import kotlinx.coroutines.launch

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private lateinit var menuViewModel: MenuViewModel
    private lateinit var googleDriveService: GoogleDriveService

    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        menuViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(MenuViewModel::class.java)

        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Get GoogleDriveService from MainActivity
        val mainActivity = activity as? MainActivity
        googleDriveService =
            mainActivity?.getGoogleDriveService() ?: GoogleDriveService(requireContext())

        setupObservers()
        setupClickListeners()
        updateUIState()

        return root
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
        updateMusicCount()
    }

    private fun setupObservers() {
        menuViewModel.googleAccountStatus.observe(viewLifecycleOwner) { status ->
            binding.textGoogleAccountStatus.text = status
        }

        menuViewModel.currentAccountEmail.observe(viewLifecycleOwner) { email ->
            if (email.isNotEmpty()) {
                binding.textCurrentAccount.text = email
                binding.textCurrentAccount.visibility = View.VISIBLE
            } else {
                binding.textCurrentAccount.visibility = View.GONE
            }
        }

        menuViewModel.isSignedIn.observe(viewLifecycleOwner) { isSignedIn ->
            updateButtonVisibility(isSignedIn)
        }

        menuViewModel.musicCount.observe(viewLifecycleOwner) { count ->
            binding.textMusicCount.text = count
        }
    }

    private fun setupClickListeners() {
        binding.btnSignInGoogle.setOnClickListener {
            signInToGoogleDrive()
        }

        binding.btnSignOutGoogle.setOnClickListener {
            signOutFromGoogleDrive()
        }

        binding.btnSelectAccount.setOnClickListener {
            selectDifferentAccount()
        }

        binding.btnRefreshMusic.setOnClickListener {
            refreshGoogleDriveMusic()
        }
    }

    private fun updateButtonVisibility(isSignedIn: Boolean) {
        binding.btnSignInGoogle.visibility = if (isSignedIn) View.GONE else View.VISIBLE
        binding.btnSignOutGoogle.visibility = if (isSignedIn) View.VISIBLE else View.GONE
        binding.btnSelectAccount.visibility = if (isSignedIn) View.VISIBLE else View.GONE
        binding.btnRefreshMusic.visibility = if (isSignedIn) View.VISIBLE else View.GONE
    }

    private fun updateUIState() {
        menuViewModel.updateGoogleAccountStatus(googleDriveService)
    }

    private fun updateMusicCount() {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            val allArtists = mainActivity.allArtists
            val totalTracks = allArtists.sumOf { it.albums.sumOf { album -> album.tracks.size } }

            // For now, count all as local. In a more sophisticated implementation,
            // you could track which tracks are from Google Drive
            val googleDriveTracks = allArtists.sumOf { artist ->
                artist.albums.sumOf { album ->
                    album.tracks.count { it.filePath.startsWith("gdrive://") }
                }
            }
            val localTracks = totalTracks - googleDriveTracks

            menuViewModel.updateMusicCount(localTracks, googleDriveTracks)
        }
    }

    private fun signInToGoogleDrive() {
        lifecycleScope.launch {
            when (val result = googleDriveService.signIn()) {
                is GoogleDriveResult.Success -> {
                    Toast.makeText(
                        context,
                        "Successfully signed in to Google Drive",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUIState()

                    // Notify MainActivity to update its state
                    val mainActivity = activity as? MainActivity
                    mainActivity?.onGoogleDriveSignInSuccess()
                }
                is GoogleDriveResult.Error -> {
                    Toast.makeText(
                        context,
                        "Google Drive sign-in failed: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Notify MainActivity to update its state
                    val mainActivity = activity as? MainActivity
                    mainActivity?.onGoogleDriveSignInError(result.message, result.exception)
                }
            }
        }
    }

    private fun signOutFromGoogleDrive() {
        lifecycleScope.launch {
            when (val result = googleDriveService.signOut()) {
                is GoogleDriveResult.Success -> {
                    Toast.makeText(context, "Signed out from Google Drive", Toast.LENGTH_SHORT).show()
                    updateUIState()

                    // Notify MainActivity to reload local music only
                    val mainActivity = activity as? MainActivity
                    mainActivity?.loadLocalMusicOnly()

                    updateMusicCount()
                }
                is GoogleDriveResult.Error -> {
                    Toast.makeText(
                        context,
                        "Error during sign out: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun selectDifferentAccount() {
        // Sign out first, then sign in again to show account picker
        lifecycleScope.launch {
            when (val result = googleDriveService.signOut()) {
                is GoogleDriveResult.Success -> {
                    // Small delay to ensure sign out is complete
                    kotlinx.coroutines.delay(500)
                    signInToGoogleDrive()
                }
                is GoogleDriveResult.Error -> {
                    Toast.makeText(
                        context,
                        "Error selecting account: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun refreshGoogleDriveMusic() {
        if (!googleDriveService.isSignedIn()) {
            Toast.makeText(context, "Please sign in to Google Drive first", Toast.LENGTH_SHORT)
                .show()
            return
        }

        lifecycleScope.launch {
            try {
                Toast.makeText(context, "Refreshing Google Drive music...", Toast.LENGTH_SHORT)
                    .show()
                val mainActivity = activity as? MainActivity
                mainActivity?.refreshGoogleDriveMusic()
                updateMusicCount()
            } catch (e: Exception) {
                Toast.makeText(context, "Error refreshing music: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}