package com.hitsuji.sheepplayer2.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.hitsuji.sheepplayer2.MainActivity
import com.hitsuji.sheepplayer2.SheepApplication
import com.hitsuji.sheepplayer2.factory.ViewModelFactory
import com.hitsuji.sheepplayer2.databinding.FragmentMenuBinding
import com.hitsuji.sheepplayer2.service.GoogleDriveResult
import kotlinx.coroutines.launch

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private lateinit var menuViewModel: MenuViewModel

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireActivity().application as SheepApplication
        val factory = ViewModelFactory(application.container)
        menuViewModel = ViewModelProvider(this, factory).get(MenuViewModel::class.java)

        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupObservers()
        setupClickListeners()
        updateUIState()

        return root
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
        menuViewModel.updateMusicCount()
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
        menuViewModel.updateGoogleAccountStatus()
    }

    private fun signInToGoogleDrive() {
        lifecycleScope.launch {
            when (val result = menuViewModel.signIn(requireActivity() as AppCompatActivity)) {
                is GoogleDriveResult.Success -> {
                    Toast.makeText(context, "Successfully signed in to Google Drive", Toast.LENGTH_SHORT).show()
                    updateUIState()
                    (activity as? MainActivity)?.refreshGoogleDriveMusic()
                }
                is GoogleDriveResult.Error -> {
                    Toast.makeText(context, "Google Drive sign-in failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun signOutFromGoogleDrive() {
        lifecycleScope.launch {
            when (val result = menuViewModel.signOut()) {
                is GoogleDriveResult.Success -> {
                    Toast.makeText(context, "Signed out from Google Drive", Toast.LENGTH_SHORT).show()
                    updateUIState()
                    menuViewModel.updateMusicCount()
                }
                is GoogleDriveResult.Error -> {
                    Toast.makeText(context, "Error during sign out: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun selectDifferentAccount() {
        lifecycleScope.launch {
            when (val result = menuViewModel.signOut()) {
                is GoogleDriveResult.Success -> {
                    kotlinx.coroutines.delay(500)
                    signInToGoogleDrive()
                }
                is GoogleDriveResult.Error -> {
                    Toast.makeText(context, "Error selecting account: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun refreshGoogleDriveMusic() {
        if (menuViewModel.isSignedIn.value != true) {
            Toast.makeText(context, "Please sign in to Google Drive first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            Toast.makeText(context, "Refreshing Google Drive music...", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.refreshGoogleDriveMusic()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
