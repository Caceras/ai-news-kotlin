package com.caceras.aibrief.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Zero-layout-shift image loader with two-tier memory & disk caching and crossfade animation. */
object ArticleImageCache {
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(1024 * 4)
    private val memoryCache = object : LruCache<String, Bitmap>(maxMemoryKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    suspend fun load(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank() || !url.startsWith("http")) return@withContext null

        // 1. Check memory cache (instant 0ms return)
        memoryCache.get(url)?.let { return@withContext it }

        // 2. Check disk cache
        val diskCacheDir = File(context.cacheDir, "article_images").apply { if (!exists()) mkdirs() }
        val diskKey = hashKey(url)
        val diskFile = File(diskCacheDir, diskKey)

        if (diskFile.exists() && diskFile.length() > 0) {
            val decoded = runCatching { BitmapFactory.decodeFile(diskFile.absolutePath) }.getOrNull()
            if (decoded != null) {
                memoryCache.put(url, decoded)
                return@withContext decoded
            }
        }

        // 3. Network fetch
        return@withContext runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 7_000
            connection.readTimeout = 7_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "AI-Brief-Android/2.0")
            if (connection.responseCode !in 200..299) return@runCatching null

            val bytes = connection.inputStream.use { it.readBytes() }
            connection.disconnect()

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null

            // Save to disk cache asynchronously
            runCatching {
                FileOutputStream(diskFile).use { fos -> fos.write(bytes) }
            }
            memoryCache.put(url, bitmap)
            bitmap
        }.getOrNull()
    }

    private fun hashKey(url: String): String {
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(url.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        }.getOrDefault(url.hashCode().toString())
    }
}

@Composable
fun AsyncArticleImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
) {
    if (imageUrl.isNullOrBlank()) return

    val context = LocalContext.current
    var bitmap by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }
    var hasLoaded by remember(imageUrl) { mutableStateOf(false) }

    LaunchedEffect(imageUrl) {
        val loaded = ArticleImageCache.load(context, imageUrl)
        bitmap = loaded
        hasLoaded = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (bitmap != null) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "imageCrossfade",
    )

    // The Box always reserves the exact aspect ratio to prevent ANY layout shift during loading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha),
            )
        }
    }
}
