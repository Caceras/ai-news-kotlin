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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.caceras.aibrief.BuildConfig
import java.io.File
import java.io.ByteArrayOutputStream
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

    /**
     * The bitmap already decoded in memory, if any. Read during composition so
     * a story scrolled back into view, or opened from the feed, draws its
     * picture in the first frame instead of flashing the placeholder.
     */
    fun cached(url: String): Bitmap? = if (url.isBlank()) null else memoryCache.get(url)

    suspend fun load(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank() || !url.startsWith("http")) return@withContext null

        // 1. Check memory cache (instant 0ms return)
        memoryCache.get(url)?.let { return@withContext it }

        // 2. Check disk cache
        val diskCacheDir = File(context.cacheDir, "article_images").apply { if (!exists()) mkdirs() }
        val diskKey = hashKey(url)
        val diskFile = File(diskCacheDir, diskKey)

        if (diskFile.exists() && diskFile.length() in 1..MAX_IMAGE_BYTES) {
            val decoded = runCatching { decodeFileSafely(diskFile) }.getOrNull()
            if (decoded != null) {
                diskFile.setLastModified(System.currentTimeMillis())
                memoryCache.put(url, decoded)
                return@withContext decoded
            } else {
                runCatching { diskFile.delete() }
            }
        }

        // 3. Network fetch
        return@withContext runCatching {
            val bytes = download(url) ?: return@runCatching null
            val bitmap = decodeBytesSafely(bytes) ?: return@runCatching null

            storeOnDisk(diskCacheDir, diskFile, bytes)
            memoryCache.put(url, bitmap)
            bitmap
        }.getOrNull()
    }

    /**
     * Reads at most [MAX_IMAGE_BYTES] from [url]. The connection is released on
     * every path, including oversized responses and read errors, so scrolling a
     * feed of failing images never leaks sockets.
     */
    private fun download(url: String): ByteArray? {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 7_000
            connection.readTimeout = 7_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "AI-Brief-Android/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode !in 200..299) return null
            if (connection.contentLengthLong > MAX_IMAGE_BYTES) return null

            return connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_IMAGE_BYTES) return null
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Writes through a temporary file and renames it into place, so a process
     * killed mid-write never leaves a truncated image that would later decode
     * as a half-grey picture. Then evicts the least recently used images.
     */
    private fun storeOnDisk(dir: File, target: File, bytes: ByteArray) {
        runCatching {
            val temp = File.createTempFile(target.name, ".part", dir)
            try {
                FileOutputStream(temp).use { it.write(bytes) }
                if (!temp.renameTo(target)) return@runCatching
            } finally {
                if (temp.exists()) temp.delete()
            }
            trimDiskCache(dir, MAX_DISK_CACHE_BYTES)
        }
    }

    /** Keeps the image cache under [maxBytes], deleting the least recently used files first. */
    internal fun trimDiskCache(dir: File, maxBytes: Long) {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= maxBytes) break
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    private fun decodeBytesSafely(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun decodeFileSafely(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > MAX_DECODE_DIMENSION || height / sample > MAX_DECODE_DIMENSION) {
            sample *= 2
        }
        return sample
    }

    private const val MAX_IMAGE_BYTES = 12L * 1024L * 1024L
    private const val MAX_DECODE_DIMENSION = 1600
    private const val MAX_DISK_CACHE_BYTES = 48L * 1024L * 1024L

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
    fallbackResource: Int? = null,
) {
    if (imageUrl.isNullOrBlank()) return

    val context = LocalContext.current
    var bitmap by remember(imageUrl) { mutableStateOf(ArticleImageCache.cached(imageUrl)) }

    LaunchedEffect(imageUrl) {
        if (bitmap == null) bitmap = ArticleImageCache.load(context, imageUrl)
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
        if (fallbackResource != null) {
            Image(painterResource(fallbackResource), contentDescription,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
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
