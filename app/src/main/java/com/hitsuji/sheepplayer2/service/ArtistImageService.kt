package com.hitsuji.sheepplayer2.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.hitsuji.sheepplayer2.domain.service.BinarySignatureValidator
import com.hitsuji.sheepplayer2.interfaces.ArtistImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ArtistImageService(
    private val context: Context? = null,
    private val binarySignatureValidator: BinarySignatureValidator
) : ArtistImageRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun searchArtistImages(artistName: String, maxImages: Int): List<String> {
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

    private fun parseGoogleImageUrls(html: String, maxImages: Int): List<String> {
        val urls = mutableListOf<String>()
        try {
            val patterns = listOf(
                """"ou":"([^"]+)"""".toRegex(),
                """"src":"([^"]+)"""".toRegex(),
                """imgurl=([^&]+)""".toRegex(),
                """"tu":"([^"]+)"""".toRegex(),
                """"rg":"([^"]+)"""".toRegex(),
                """"isu":"([^"]+)"""".toRegex(),
                """\"([^\"]*https?://[^\"]*\.(jpg|jpeg|png|webp|gif)[^\"]*)\"""".toRegex(),
                """src="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp|gif)[^"]*)" """.toRegex(),
                """url="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp|gif)[^"]*)" """.toRegex(),
                """href="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp|gif)[^"]*)" """.toRegex(),
                """data-src="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp|gif)[^"]*)" """.toRegex(),
                """https?://[^\s"'<>]+\.(jpg|jpeg|png|webp|gif)(?:\?[^\s"'<>]*)?""".toRegex()
            )

            for ((index, pattern) in patterns.withIndex()) {
                val matches = pattern.findAll(html)
                matches.forEach { match ->
                    val rawUrl = match.groups[1]?.value
                    if (rawUrl != null) {
                        try {
                            val url = URLDecoder.decode(
                                rawUrl.replace("\\u003d", "=").replace("\\u0026", "&"), "UTF-8"
                            )
                            if (isValidImageUrl(url) && !urls.contains(url) && urls.size < maxImages * 2) {
                                urls.add(url)
                            }
                        } catch (e: Exception) {
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
            val patterns = listOf(
                """"murl":"([^"]+)"""".toRegex(),
                """"imgurl":"([^"]+)"""".toRegex(),
                """mediaurl=([^&]+)""".toRegex(),
                """"src":"([^"]+)"""".toRegex(),
                """src="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp)[^"]*)" """.toRegex(),
                """data-src="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp)[^"]*)" """.toRegex(),
                """"thumbnail":"([^"]+)"""".toRegex(),
                """"url":"([^"]+)"""".toRegex()
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
            val patterns = listOf(
                """"image":"([^"]+)"""".toRegex(),
                """data-src="([^"]+\.(jpg|jpeg|png|webp)[^"]*)"""".toRegex(),
                """src="([^"]+\.(jpg|jpeg|png|webp)[^"]*)"""".toRegex(),
                """"url":"([^"]+)"""".toRegex(),
                """"thumbnail":"([^"]+)"""".toRegex(),
                """href="([^"]*https?://[^"]*\.(jpg|jpeg|png|webp)[^"]*)" """.toRegex(),
                """background-image:\s*url\('([^']*https?://[^']*\.(jpg|jpeg|png|webp)[^']*)'\)""".toRegex()
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
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ArtistImageService", "Error parsing DuckDuckGo image URLs", e)
        }
        return urls.take(maxImages * 2)
    }

    override suspend fun downloadImage(imageUrl: String): Bitmap? {
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
                        val bytes = stream.readBytes()

                        if (!binarySignatureValidator.isImage(bytes)) {
                            Log.w(
                                "ArtistImageService",
                                "Invalid image magic number, excluding file from: $imageUrl"
                            )
                            return@withContext null
                        }

                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }

                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                        if (options.outWidth < 150 || options.outHeight < 150) {
                            return@withContext null
                        }

                        options.inSampleSize = calculateInSampleSize(options, 400, 400)
                        options.inJustDecodeBounds = false
                        options.inPreferredConfig = Bitmap.Config.RGB_565

                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        bitmap
                    }
                } else {
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
                    !cleanUrl.contains("..") &&
                    !cleanUrl.contains("javascript:") &&
                    !cleanUrl.contains("data:") &&
                    !cleanUrl.contains("blob:") &&
                    cleanUrl.length < 2000 &&
                    cleanUrl.length > 15 &&
                    !cleanUrl.contains("favicon") &&
                    !cleanUrl.contains("logo") &&
                    !cleanUrl.contains("icon")
        } catch (e: Exception) {
            false
        }
    }

    override fun getLoadingPlaceholderBitmap(): Bitmap? {
        return try {
            if (context != null) {
                val inputStream = context.resources.openRawResource(
                    context.resources.getIdentifier(
                        "sheep_loading_placeholder",
                        "drawable",
                        context.packageName
                    )
                )
                BitmapFactory.decodeStream(inputStream)
            } else {
                createSimplePlaceholder()
            }
        } catch (e: Exception) {
            createSimplePlaceholder()
        }
    }

    private fun createSimplePlaceholder(): Bitmap {
        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)
        val canvas = android.graphics.Canvas(bitmap)

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

    override fun cleanup() {
        try {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        } catch (e: Exception) {
            Log.e("ArtistImageService", "Error during cleanup", e)
        }
    }
}
