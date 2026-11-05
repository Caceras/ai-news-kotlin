package com.caceras.aibrief.data

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * The largest edition document the app will download. Today's file is about
 * 36 KB; this leaves decades of growth while keeping a broken or hostile
 * response from exhausting memory or bloating the stored copy on the device.
 */
const val MAX_EDITION_BYTES = 2 * 1024 * 1024

/**
 * Reads [stream] as UTF-8 text, or returns null as soon as it is longer than
 * [maxBytes]. Stops reading at the limit rather than buffering the whole body,
 * so an oversized response costs at most [maxBytes] of memory.
 */
fun readBoundedText(stream: InputStream, maxBytes: Int = MAX_EDITION_BYTES): String? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (true) {
        val read = stream.read(buffer)
        if (read < 0) break
        if (out.size() + read > maxBytes) return null
        out.write(buffer, 0, read)
    }
    return out.toString(Charsets.UTF_8.name())
}
