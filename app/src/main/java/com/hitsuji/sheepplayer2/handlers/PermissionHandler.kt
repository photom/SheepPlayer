package com.hitsuji.sheepplayer2.handlers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionHandler(
    private val context: Context,
    private val permissionLauncher: ActivityResultLauncher<String>
) {
    
    interface PermissionCallback {
        fun onPermissionGranted()
        fun onPermissionDenied()
    }
    
    private var callback: PermissionCallback? = null
    
    fun setCallback(callback: PermissionCallback) {
        this.callback = callback
    }
    
    fun checkAndRequestPermission() {
        val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                context,
                permissionToRequest
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("Permission", "Permission already granted")
                callback?.onPermissionGranted()
            }
            else -> {
                Log.i("Permission", "Requesting permission")
                permissionLauncher.launch(permissionToRequest)
            }
        }
    }
    
    fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            Log.i("Permission", "Permission granted")
            callback?.onPermissionGranted()
        } else {
            Log.e("Permission", "Permission denied")
            callback?.onPermissionDenied()
        }
    }
}