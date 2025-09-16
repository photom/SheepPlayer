package com.hitsuji.sheepplayer2.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ArtistImageService(private val context: Context? = null) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun searchArtistImages(artistName: String, maxImages: Int = 10): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(
                    "ArtistImageService",
                    "Aggressively searching images for artist: \"$artistName\" (using quoted searches)"
                )

                val imageUrls = mutableSetOf<String>() // Use Set to avoid duplicates

                // Multiple search terms for better coverage with quoted artist names
                val searchTerms = listOf(
                    "\"$artistName\" musician artist photos",
                    "\"$artistName\" band music images",
                    "\"$artistName\" concert live photos",
                    "\"$artistName\" album cover photos",
                    "\"$artistName\" press photos",
                    "\"$artistName\""
                )

                // Strategy 1: Try multiple Google searches with different terms
                for (searchTerm in searchTerms) {
                    if (imageUrls.size >= maxImages * 2) break // Get extra URLs in case some fail

                    try {
                        val googleUrls = searchWithGoogleVariant(searchTerm, maxImages)
                        imageUrls.addAll(googleUrls)
                        Log.d(
                            "ArtistImageService",
                            "Found ${googleUrls.size} URLs from Google with term: $searchTerm"
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "ArtistImageService",
                            "Google search failed for '$searchTerm': ${e.message}"
                        )
                    }
                }

                // Strategy 2: Try multiple Bing searches
                for (searchTerm in searchTerms.take(3)) {
                    if (imageUrls.size >= maxImages * 2) break

                    try {
                        val bingUrls = searchWithBingVariant(searchTerm, maxImages)
                        imageUrls.addAll(bingUrls)
                        Log.d(
                            "ArtistImageService",
                            "Found ${bingUrls.size} URLs from Bing with term: $searchTerm"
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "ArtistImageService",
                            "Bing search failed for '$searchTerm': ${e.message}"
                        )
                    }
                }

                // Strategy 3: Try DuckDuckGo with different terms
                for (searchTerm in searchTerms.take(2)) {
                    if (imageUrls.size >= maxImages * 2) break

                    try {
                        val duckUrls = searchWithDuckDuckGoVariant(searchTerm, maxImages)
                        imageUrls.addAll(duckUrls)
                        Log.d(
                            "ArtistImageService",
                            "Found ${duckUrls.size} URLs from DuckDuckGo with term: $searchTerm"
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "ArtistImageService",
                            "DuckDuckGo search failed for '$searchTerm': ${e.message}"
                        )
                    }
                }

                val finalUrls = imageUrls.toList().shuffled()
                    .take(maxImages * 2) // Shuffle and take extra for redundancy
                Log.d(
                    "ArtistImageService",
                    "Total found ${finalUrls.size} unique image URLs for $artistName"
                )
                return@withContext finalUrls

            } catch (e: Exception) {
                Log.e("ArtistImageService", "Error searching images for $artistName", e)
                emptyList()
            }
        }
    }

    private suspend fun searchWithGoogleVariant(searchTerm: String, maxImages: Int): List<String> {
        return try {
            val encodedTerm = URLEncoder.encode(searchTerm, "UTF-8")
            val searchUrl =
                "https://www.google.com/search?q=${encodedTerm}&udm=2&safe=active&num=20"

            val request = Request.Builder()
                .url(searchUrl)
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
                )
                .addHeader(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
                )
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("DNT", "1")
                .addHeader("Connection", "keep-alive")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                Log.d("ArtistImageService", "Google search successful, HTML length: ${html.length}")
                val results = parseGoogleImageUrls(html, maxImages)
                Log.d("ArtistImageService", "Google parsing found ${results.size} URLs")
                results
            } else {
                Log.w(
                    "ArtistImageService",
                    "Google search failed for '$searchTerm': ${response.code}"
                )
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ArtistImageService", "Google search error for '$searchTerm'", e)
            emptyList()
        }
    }

    // Keep old method for compatibility
    private suspend fun searchWithGoogle(artistName: String, maxImages: Int): List<String> {
        return searchWithGoogleVariant("\"$artistName\" musician artist photos", maxImages)
    }

    private suspend fun searchWithBingVariant(searchTerm: String, maxImages: Int): List<String> {
        return try {
            val encodedTerm = URLEncoder.encode(searchTerm, "UTF-8")
            val searchUrl =
                "https://www.bing.com/images/search?q=${encodedTerm}&form=HDRSC2&first=1&count=20&tsc=ImageHoverTitle"

            val request = Request.Builder()
                .url(searchUrl)
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                .addHeader(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                )
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                parseBingImageUrls(html, maxImages)
            } else {
                Log.w(
                    "ArtistImageService",
                    "Bing search failed for '$searchTerm': ${response.code}"
                )
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ArtistImageService", "Bing search error for '$searchTerm'", e)
            emptyList()
        }
    }

    // Keep old method for compatibility
    private suspend fun searchWithBing(artistName: String, maxImages: Int): List<String> {
        return searchWithBingVariant("\"$artistName\" musician artist", maxImages)
    }

    private suspend fun searchWithDuckDuckGoVariant(
        searchTerm: String,
        maxImages: Int
    ): List<String> {
        return try {
            val encodedTerm = URLEncoder.encode(searchTerm, "UTF-8")
            val searchUrl =
                "https://duckduckgo.com/?q=${encodedTerm}&t=h_&iar=images&iax=images&ia=images"

            val request = Request.Builder()
                .url(searchUrl)
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
                )
                .addHeader(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                )
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                parseDuckDuckGoImageUrls(html, maxImages)
            } else {
                Log.w(
                    "ArtistImageService",
                    "DuckDuckGo search failed for '$searchTerm': ${response.code}"
                )
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ArtistImageService", "DuckDuckGo search error for '$searchTerm'", e)
            emptyList()
        }
    }

    // Keep old method for compatibility
    private suspend fun searchWithDuckDuckGo(artistName: String, maxImages: Int): List<String> {
        return searchWithDuckDuckGoVariant("\"$artistName\" musician photos", maxImages)
    }

    private fun parseGoogleImageUrls(html: String, maxImages: Int): List<String> {
        val urls = mutableListOf<String>()
        try {
            // Parse Google Images search results - look for various patterns more aggressively
            val patterns = listOf(
                """"ou":"([^"]+)"""".toRegex(), // Original URL pattern
                """"src":"([^"]+)"""".toRegex(), // Source URL pattern  
                """imgurl=([^&]+)""".toRegex(), // Image URL parameter
                """"tu":"([^"]+)"""".toRegex(), // Thumbnail URL pattern
                """"rg":"([^"]+)"""".toRegex(), // New Google pattern for image URLs
                """"isu":"([^"]+)"""".toRegex(), // Image source URL pattern
                """\"([^\"]*https?://[^\"]*\.(jpg|jpeg|png|webp|gif)[^\"]*)\"""".toRegex(), // Any quoted URL with image extension
                """src="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp|gif)[^"]*)" """.toRegex(), // Direct image URLs
                """url="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp|gif)[^"]*)" """.toRegex(), // URL attributes
                """href="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp|gif)[^"]*)" """.toRegex(), // HREF attributes
                """data-src="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp|gif)[^"]*)" """.toRegex(), // Data source
                """https?://[^\s"'<>]+\.(jpg|jpeg|png|webp|gif)(?:\?[^\s"'<>]*)?""".toRegex() // Any direct image URL
            )

            for ((index, pattern) in patterns.withIndex()) {
                val matches = pattern.findAll(html)
                val matchCount = matches.count()
                if (matchCount > 0) {
                    Log.d("ArtistImageService", "Pattern $index matched $matchCount times")
                }
                matches.forEach { match ->
                    val rawUrl = match.groups[1]?.value
                    if (rawUrl != null) {
                        try {
                            val url = URLDecoder.decode(
                                rawUrl.replace("\\u003d", "=").replace("\\u0026", "&"), "UTF-8"
                            )
                            if (isValidImageUrl(url) && !urls.contains(url) && urls.size < maxImages * 2) {
                                urls.add(url)
                                Log.d(
                                    "ArtistImageService",
                                    "Added valid URL from pattern $index: ${url.take(100)}..."
                                )
                            }
                        } catch (e: Exception) {
                            // Continue with next URL if this one fails
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ArtistImageService", "Error parsing Google image URLs", e)
        }
        return urls.take(maxImages * 2)
    }

    private fun parseBingImageUrls(html: String, maxImages: Int): List<String> {
        val urls = mutableListOf<String>()
        try {
            // Parse Bing image search results - more aggressive patterns
            val patterns = listOf(
                """"murl":"([^"]+)"""".toRegex(), // Main URL pattern
                """"imgurl":"([^"]+)"""".toRegex(), // Image URL pattern
                """mediaurl=([^&]+)""".toRegex(), // Media URL parameter
                """"src":"([^"]+)"""".toRegex(), // Source URL pattern
                """src="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp)[^"]*)" """.toRegex(), // Direct image URLs
                """data-src="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp)[^"]*)" """.toRegex(), // Data source
                """"thumbnail":"([^"]+)"""".toRegex(), // Thumbnail URLs
                """"url":"([^"]+)"""".toRegex() // Generic URL pattern
            )

            for (pattern in patterns) {
                val matches = pattern.findAll(html)
                matches.forEach { match ->
                    val rawUrl = match.groups[1]?.value
                    if (rawUrl != null) {
                        try {
                            val url = rawUrl
                                .replace("\\u002f", "/")
                                .replace("\\u003d", "=")
                                .replace("\\u0026", "&")
                                .replace("\\", "")
                                .let { URLDecoder.decode(it, "UTF-8") }

                            if (isValidImageUrl(url) && !urls.contains(url) && urls.size < maxImages * 2) {
                                urls.add(url)
                            }
                        } catch (e: Exception) {
                            // Continue with next URL if this one fails
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ArtistImageService", "Error parsing Bing image URLs", e)
        }
        return urls.take(maxImages * 2)
    }

    private fun parseDuckDuckGoImageUrls(html: String, maxImages: Int): List<String> {
        val urls = mutableListOf<String>()
        try {
            // Parse DuckDuckGo image search results - more aggressive patterns
            val patterns = listOf(
                """"image":"([^"]+)"""".toRegex(), // JSON image field
                """data-src="([^"]+\.(jpg|jpeg|png|webp)[^"]*)"""".toRegex(), // Data source
                """src="([^"]+\.(jpg|jpeg|png|webp)[^"]*)"""".toRegex(), // Direct source
                """"url":"([^"]+)"""".toRegex(), // URL field
                """"thumbnail":"([^"]+)"""".toRegex(), // Thumbnail field
                """href="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp)[^"]*)" """.toRegex(), // HREF attributes
                """background-image:\s*url\('([^']*https?://[^']*\.(jpg|jpeg|png|webp)[^']*)'\)""".toRegex() // CSS background images
            )

            for (pattern in patterns) {
                val matches = pattern.findAll(html)
                matches.forEach { match ->
                    val rawUrl = match.groups[1]?.value
                    if (rawUrl != null) {
                        try {
                            val url = URLDecoder.decode(rawUrl.replace("\\", ""), "UTF-8")
                            if (isValidImageUrl(url) && !urls.contains(url) && urls.size < maxImages * 2) {
                                urls.add(url)
                            }
                        } catch (e: Exception) {
                            // Continue with next URL if this one fails
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ArtistImageService", "Error parsing DuckDuckGo image URLs", e)
        }
        return urls.take(maxImages * 2)
    }

    suspend fun downloadImage(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (!isValidImageUrl(imageUrl)) {
                    Log.w("ArtistImageService", "Invalid image URL: $imageUrl")
                    return@withContext null
                }

                Log.d("ArtistImageService", "Downloading image from: $imageUrl")

                val request = Request.Builder()
                    .url(imageUrl)
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
                    )
                    .addHeader("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .addHeader("Referer", "https://www.google.com/")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    inputStream?.let { stream ->
                        // Read the entire response as bytes
                        val bytes = stream.readBytes()

                        // Validate magic number - check if it's actually an image file
                        if (!isValidImageMagicNumber(bytes)) {
                            Log.w(
                                "ArtistImageService",
                                "Invalid image magic number, excluding file from: $imageUrl"
                            )
                            return@withContext null
                        }

                        Log.d(
                            "ArtistImageService",
                            "Valid image magic number detected for: $imageUrl"
                        )

                        // Use BitmapFactory.Options to control memory usage
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }

                        // First decode to get dimensions
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                        // Skip images that are too small (likely thumbnails)
                        if (options.outWidth < 150 || options.outHeight < 150) {
                            Log.d(
                                "ArtistImageService",
                                "Skipping small image: ${options.outWidth}x${options.outHeight}"
                            )
                            return@withContext null
                        }

                        // Calculate sample size to reduce memory usage
                        options.inSampleSize = calculateInSampleSize(options, 400, 400)
                        options.inJustDecodeBounds = false
                        options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory

                        // Decode the actual bitmap with reduced size
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        if (bitmap != null) {
                            Log.d(
                                "ArtistImageService",
                                "Successfully downloaded and validated image: ${bitmap.width}x${bitmap.height}"
                            )
                        }
                        bitmap
                    }
                } else {
                    Log.w(
                        "ArtistImageService",
                        "Failed to download image: ${response.code} - $imageUrl"
                    )
                    null
                }
            } catch (e: Exception) {
                Log.e("ArtistImageService", "Error downloading image: $imageUrl", e)
                null
            }
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }


    private fun isValidImageUrl(url: String): Boolean {
        return try {
            // More permissive URL validation for better coverage
            val cleanUrl = url.trim().lowercase()

            (cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) &&
                    (cleanUrl.contains(".jpg") || cleanUrl.contains(".jpeg") || cleanUrl.contains(".png") ||
                            cleanUrl.contains(".webp") || cleanUrl.contains(".gif") ||
                            cleanUrl.contains("image") || cleanUrl.contains("photo") || cleanUrl.contains(
                        "pic"
                    ) ||
                            cleanUrl.contains("media") || cleanUrl.contains("upload") || cleanUrl.contains(
                        "cdn"
                    )) &&
                    !cleanUrl.contains("..") && // Prevent path traversal
                    !cleanUrl.contains("javascript:") && // Prevent XSS
                    !cleanUrl.contains("data:") && // Skip data URLs
                    !cleanUrl.contains("blob:") && // Skip blob URLs
                    cleanUrl.length < 2000 && // More generous URL length limit
                    cleanUrl.length > 15 && // Must have reasonable content
                    !cleanUrl.contains("favicon") && // Skip favicons
                    !cleanUrl.contains("logo") && // Skip small logos (usually)
                    !cleanUrl.contains("icon") // Skip icons
        } catch (e: Exception) {
            false
        }
    }

    fun getLoadingPlaceholderBitmap(): Bitmap? {
        return try {
            if (context != null) {
                // Try to load the GIF as a bitmap (first frame)
                val inputStream = context.resources.openRawResource(
                    context.resources.getIdentifier(
                        "sheep_loading_placeholder",
                        "drawable",
                        context.packageName
                    )
                )
                BitmapFactory.decodeStream(inputStream)
            } else {
                // Fallback: create a simple placeholder if context is not available
                createSimplePlaceholder()
            }
        } catch (e: Exception) {
            Log.w(
                "ArtistImageService",
                "Could not load sheep_loading_placeholder.gif, using fallback"
            )
            createSimplePlaceholder()
        }
    }

    private fun createSimplePlaceholder(): Bitmap {
        // Simple fallback placeholder
        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)
        val canvas = android.graphics.Canvas(bitmap)

        // Blue gradient background
        val gradient = android.graphics.LinearGradient(
            0f, 0f, 400f, 400f,
            android.graphics.Color.parseColor("#1E3A8A"),
            android.graphics.Color.parseColor("#3B82F6"),
            android.graphics.Shader.TileMode.CLAMP
        )
        val backgroundPaint = android.graphics.Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, 400f, 400f, backgroundPaint)

        // Simple text
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            textSize = 24f
        }
        canvas.drawText("SheepPlayer", 200f, 180f, textPaint)
        canvas.drawText("Loading...", 200f, 220f, textPaint)

        return bitmap
    }

    /**
     * Validates image magic numbers to ensure the downloaded file is actually an image.
     * Supports JPEG, PNG, GIF, WebP, and BMP formats.
     *
     * @param bytes The byte array to check
     * @return true if valid image magic number is found, false otherwise
     */
    private fun isValidImageMagicNumber(bytes: ByteArray): Boolean {
        if (bytes.size < 12) {
            Log.d("ArtistImageService", "File too small to contain valid image magic number")
            return false
        }

        return when {
            // JPEG magic numbers: FF D8 FF
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> {
                Log.d("ArtistImageService", "Detected JPEG image")
                true
            }

            // PNG magic numbers: 89 50 4E 47 0D 0A 1A 0A
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() &&
                    bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
                    bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte() -> {
                Log.d("ArtistImageService", "Detected PNG image")
                true
            }

            // GIF magic numbers: 47 49 46 38 (GIF8)
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() &&
                    bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte() -> {
                Log.d("ArtistImageService", "Detected GIF image")
                true
            }

            // WebP magic numbers: 52 49 46 46 ... 57 45 42 50 (RIFF...WEBP)
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
                    bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
                    bytes.size >= 12 && bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
                    bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte() -> {
                Log.d("ArtistImageService", "Detected WebP image")
                true
            }

            // BMP magic numbers: 42 4D (BM)
            bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte() -> {
                Log.d("ArtistImageService", "Detected BMP image")
                true
            }

            // ICO magic numbers: 00 00 01 00
            bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() &&
                    bytes[2] == 0x01.toByte() && bytes[3] == 0x00.toByte() -> {
                Log.d("ArtistImageService", "Detected ICO image")
                true
            }

            // TIFF magic numbers: 49 49 2A 00 (little-endian) or 4D 4D 00 2A (big-endian)
            (bytes[0] == 0x49.toByte() && bytes[1] == 0x49.toByte() &&
                    bytes[2] == 0x2A.toByte() && bytes[3] == 0x00.toByte()) ||
                    (bytes[0] == 0x4D.toByte() && bytes[1] == 0x4D.toByte() &&
                            bytes[2] == 0x00.toByte() && bytes[3] == 0x2A.toByte()) -> {
                Log.d("ArtistImageService", "Detected TIFF image")
                true
            }

            else -> {
                // Log the first few bytes for debugging
                val hexBytes = bytes.take(12).joinToString(" ") { "%02X".format(it) }
                Log.w("ArtistImageService", "Unknown file format with magic bytes: $hexBytes")
                false
            }
        }
    }

    fun cleanup() {
        try {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        } catch (e: Exception) {
            Log.e("ArtistImageService", "Error during cleanup", e)
        }
    }
}