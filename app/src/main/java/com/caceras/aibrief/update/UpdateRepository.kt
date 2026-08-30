package com.caceras.aibrief.update

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Fetches the direct-install update manifest and downloads the APK it points at.
 *
 * Every failure is reported as a `null` result rather than an exception: a
 * missed update check is a normal, uninteresting outcome that should never
 * disturb reading the news.
 */
class UpdateRepository(private val manifestUrl: String) {

    /** Returns the newest published manifest, or `null` if it cannot be read. */
    suspend fun latestManifest(): UpdateManifest? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = open(manifestUrl, "application/json")
            if (connection.responseCode !in HTTP_SUCCESS) return@withContext null
            UpdateManifestParser.parse(connection.inputStream.bufferedReader().use { it.readText() })
        } catch (_: IOException) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Downloads the APK described by [manifest] into [directory], reporting
     * completion between `0f` and `1f` through [onProgress].
     *
     * Returns the downloaded file, or `null` if the download failed. A partial
     * file is deleted rather than left behind for the installer to choke on.
     */
    suspend fun downloadApk(
        manifest: UpdateManifest,
        directory: File,
        onProgress: (Float) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        val target = File(directory, "ai-brief-${manifest.versionCode}.apk")
        var connection: HttpURLConnection? = null
        try {
            directory.mkdirs()
            connection = open(manifest.apkUrl, APK_MEDIA_TYPE)
            if (connection.responseCode !in HTTP_SUCCESS) return@withContext null

            val expectedBytes = connection.contentLengthLong
            var writtenBytes = 0L
            connection.inputStream.use { source ->
                target.outputStream().use { sink ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                    while (true) {
                        // Abandon the transfer promptly if the screen goes away.
                        ensureActive()
                        val read = source.read(buffer)
                        if (read < 0) break
                        sink.write(buffer, 0, read)
                        writtenBytes += read
                        if (expectedBytes > 0) {
                            onProgress((writtenBytes.toFloat() / expectedBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            onProgress(1f)
            target
        } catch (_: IOException) {
            target.delete()
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** Removes previously downloaded APKs so the cache holds at most one build. */
    suspend fun clearDownloads(directory: File) = withContext(Dispatchers.IO) {
        directory.listFiles()?.forEach(File::delete)
        Unit
    }

    private fun open(url: String, accept: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            instanceFollowRedirects = true
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", USER_AGENT)
        }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
        const val DOWNLOAD_BUFFER_BYTES = 16 * 1024
        const val APK_MEDIA_TYPE = "application/vnd.android.package-archive"
        const val USER_AGENT = "AI-Brief-Android"
        val HTTP_SUCCESS = 200..299
    }
}
