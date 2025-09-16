# Tutorial: Your First Music Player

In this tutorial, we'll build a music player app together from the ground up. You'll create an
Android app that can find music on your device and play it back.

By the end of this tutorial, you will have:

- A working music player app
- Understanding of Android permissions
- Experience with MediaStore API
- A foundation for building more features

We'll guide you through each step. You don't need to understand everything immediately - we'll
explain as we go.

## What You Need

Before we start, make sure you have:

- Android Studio installed
- An Android device or emulator (Android 13+)
- Some music files on your device
- 30 minutes of focused time

## What We're Building

We're creating a music player that:

1. Asks permission to access your music
2. Finds all music files on your device
3. Shows you how many songs it found
4. Organizes music by artist and album

This forms the foundation of any music player app.

## Step 1: Create Your Project

Let's start by creating a new Android project.

Open Android Studio and follow these steps:

1. Click "Create New Project"
2. Select "Empty Views Activity"
3. Click "Next"
4. Set **Name** to "MusicPlayer"
5. Set **Package name** to "com.example.musicplayer"
6. Set **Language** to "Kotlin"
7. Set **Minimum SDK** to "API 33 (Android 13.0)"
8. Click "Finish"

Android Studio creates your project and opens it. You'll see files appear in the Project panel on
the left.

**What just happened?**
You created an Android app with a single screen (Activity). The app doesn't do anything yet, but it
can run on your device.

## Step 2: Request Music Access

Now we'll tell Android that our app needs to access music files.

1. In the Project panel, find and open `app/src/main/AndroidManifest.xml`
2. Find the line that starts with `<manifest`
3. After the `<manifest>` line, add these two lines:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
```

Your file should now look like this:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
        android:maxSdkVersion="32" />

    <application
        android:allowBackup="true"
```

Save the file (Ctrl+S or Cmd+S).

## Step 3: Create Music Data Structures

We'll create simple structures to hold music information.

1. Right-click on `com.example.musicplayer` (your package name)
2. Select "New" → "Kotlin Class/File"
3. Choose "File" and name it "Data"
4. Click "OK"
5. Replace everything in the file with this code:

```kotlin
package com.example.musicplayer

data class Artist(
    val id: Long,
    val name: String,
    val albums: MutableList<Album> = mutableListOf()
)

data class Album(
    val id: Long,
    val title: String,
    val artistName: String,
    val tracks: MutableList<Track> = mutableListOf()
)

data class Track(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val duration: Long,
    val filePath: String,
    val albumArtUri: String? = null
)
```

Save the file.

## Step 4: Ask Users for Permission

1. Open `MainActivity.kt`
2. Delete everything in the file
3. Copy and paste this code:

```kotlin
package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Great! We can access your music.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "We need permission to find your music.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        askForPermission()
    }

    private fun askForPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(this, "Ready to find your music!", Toast.LENGTH_SHORT).show()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}
```

## Step 5: Test Your App

1. Connect your Android device via USB (or start an emulator)
2. Click the green "Run" button (▶️) in the toolbar
3. Select your device and click "OK"

Your app will launch and ask for permission to access music files. Tap "Allow" when prompted.

You should see a message saying "Ready to find your music!"

Congratulations! Your app can now access music files.

## Step 6: Find Music Files

Now we'll make the app find music files on your device.

1. Right-click your package name → "New" → "Kotlin Class/File"
2. Choose "File" and name it "MusicRepository"
3. Replace everything in the file with this code:

```kotlin
package com.example.musicplayer

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class MusicRepository(private val context: Context) {

    fun loadMusicData(): List<Artist> {
        val trackList = mutableListOf<Track>()
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        
        context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val filePath = cursor.getString(dataColumn) ?: ""
                val albumId = cursor.getLong(albumIdColumn)

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                ).toString()

                trackList.add(Track(id, title, artist, album, duration, filePath, albumArtUri))
            }
        }
        
        return organizeMusicData(trackList)
    }

    private fun organizeMusicData(tracks: List<Track>): List<Artist> {
        val artistsMap = mutableMapOf<String, Artist>()
        val albumsMap = mutableMapOf<String, Album>()

        tracks.forEach { track ->
            val artist = artistsMap.getOrPut(track.artistName) {
                Artist(id = track.artistName.hashCode().toLong(), name = track.artistName)
            }

            val albumKey = "${track.artistName}|${track.albumName}"
            val album = albumsMap.getOrPut(albumKey) {
                Album(
                    id = track.albumName.hashCode().toLong(),
                    title = track.albumName,
                    artistName = track.artistName
                )
            }

            album.tracks.add(track)
            if (!artist.albums.contains(album)) {
                artist.albums.add(album)
            }
        }
        
        return artistsMap.values.sortedBy { it.name }
    }
}
```

## Step 7: Connect Music Loading

Now we'll update MainActivity to find and count your music files.

Replace everything in `MainActivity.kt` with:

```kotlin
package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var musicRepository: MusicRepository
    private val allArtists = mutableListOf<Artist>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                findMusic()
            } else {
                Toast.makeText(this, "We need permission to find your music.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        musicRepository = MusicRepository(this)
        askForPermission()
    }

    private fun findMusic() {
        lifecycleScope.launch {
            try {
                val artists = withContext(Dispatchers.IO) {
                    musicRepository.loadMusicData()
                }
                
                allArtists.clear()
                allArtists.addAll(artists)
                
                val trackCount = allArtists.sumOf { it.albums.sumOf { it.tracks.size } }
                
                Toast.makeText(
                    this@MainActivity, 
                    "Found ${allArtists.size} artists with $trackCount songs!", 
                    Toast.LENGTH_LONG
                ).show()
                
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Couldn't find music files", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun askForPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                findMusic()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}
```

## Step 8: Test Your Music Player

1. Make sure you have music files on your device
2. Click the green "Run" button (▶️)
3. Grant permission when asked
4. Wait for the message

You should see something like "Found 12 artists with 156 songs!"

## What You Built

Congratulations! You now have a working music player foundation that:

✅ Asks for permission properly  
✅ Finds all music on your device  
✅ Organizes it by artists and albums  
✅ Works in the background smoothly

## What You Learned

- How to request Android permissions
- How to access device music files
- How to organize data with Kotlin classes
- How to run code in the background

Your music player foundation is complete. You could now add a user interface, playback controls, or
other features on top of this base.

Well done! 🎵