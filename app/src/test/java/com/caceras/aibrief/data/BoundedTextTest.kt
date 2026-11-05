package com.caceras.aibrief.data

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedTextTest {
    @Test
    fun readsTextWithinTheLimit() {
        val text = "{\"posts\":[\"Café — naïve\"]}"
        assertEquals(text, readBoundedText(ByteArrayInputStream(text.toByteArray()), maxBytes = 1024))
    }

    @Test
    fun acceptsTextExactlyAtTheLimit() {
        val bytes = ByteArray(20_000) { 'a'.code.toByte() }
        assertEquals(20_000, readBoundedText(ByteArrayInputStream(bytes), maxBytes = 20_000)?.length)
    }

    @Test
    fun refusesTextOverTheLimit() {
        val bytes = ByteArray(20_001) { 'a'.code.toByte() }
        assertNull(readBoundedText(ByteArrayInputStream(bytes), maxBytes = 20_000))
    }

    @Test
    fun stopsReadingAnEndlessBodyAtTheLimit() {
        var served = 0L
        val endless = object : InputStream() {
            override fun read(): Int { served++; return 'a'.code }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                b.fill('a'.code.toByte(), off, off + len); served += len; return len
            }
        }
        assertNull(readBoundedText(endless, maxBytes = 64 * 1024))
        assertTrue("read $served bytes", served <= 64 * 1024 + 8 * 1024)
    }

    @Test
    fun todaysEditionFitsTheDefaultLimit() {
        val file = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "content/posts.json") }.first { it.exists() }
        assertTrue(file.length() < MAX_EDITION_BYTES / 10)
    }
}
