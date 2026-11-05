package com.caceras.aibrief.ui.components

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleImageCacheTest {
    private fun cacheWith(vararg sizes: Int): Pair<File, List<File>> {
        val dir = Files.createTempDirectory("images").toFile()
        val files = sizes.mapIndexed { index, size ->
            File(dir, "img$index").apply {
                writeBytes(ByteArray(size))
                setLastModified(1_000_000L + index * 1_000L)
            }
        }
        return dir to files
    }

    @Test
    fun trimLeavesACacheUnderTheLimitAlone() {
        val (dir, files) = cacheWith(100, 100)
        ArticleImageCache.trimDiskCache(dir, 200)
        assertTrue(files.all { it.exists() })
        dir.deleteRecursively()
    }

    @Test
    fun trimEvictsLeastRecentlyUsedFirst() {
        val (dir, files) = cacheWith(100, 100, 100)
        files[0].setLastModified(9_000_000L) // recently viewed, so kept
        ArticleImageCache.trimDiskCache(dir, 200)
        assertTrue(files[0].exists())
        assertFalse(files[1].exists())
        assertTrue(files[2].exists())
        assertEquals(200L, dir.listFiles()!!.sumOf { it.length() })
        dir.deleteRecursively()
    }

    @Test
    fun trimKeepsPicturesWhileTheAppIsVisible() {
        // TRIM_MEMORY_RUNNING_LOW: still in the foreground, nothing to give back.
        assertNull(ArticleImageCache.trimTarget(10, 4096))
    }

    @Test
    fun trimHalvesPicturesWhenTheAppIsHidden() {
        assertEquals(2048, ArticleImageCache.trimTarget(20, 4096)) // TRIM_MEMORY_UI_HIDDEN
    }

    @Test
    fun trimDropsAllPicturesInTheBackground() {
        assertEquals(0, ArticleImageCache.trimTarget(40, 4096)) // TRIM_MEMORY_BACKGROUND
        assertEquals(0, ArticleImageCache.trimTarget(80, 4096)) // TRIM_MEMORY_COMPLETE
    }
}
