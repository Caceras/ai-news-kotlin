package com.caceras.aibrief.data

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the real `content/posts.json`, the file the app fetches at runtime.
 *
 * Publishing a post is a change to that file alone, with no build in between,
 * so nothing else would catch a malformed edit before a phone tried to read it.
 * These tests are the safety net for that: they parse the shipped file with the
 * same code the app uses.
 */
class PublishedPostsFileTest {

    private val postsFile: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "content/posts.json") }
        .firstOrNull { it.isFile }
        ?: error("content/posts.json was not found in any parent directory")

    private val raw: String get() = postsFile.readText()

    @Test
    fun `the published file parses`() {
        assertTrue(
            "content/posts.json contains no readable posts — check it is valid JSON " +
                "and that every post has id, title and publishedAt.",
            OwnerPosts.parse(raw).isNotEmpty(),
        )
    }

    @Test
    fun `every post in the published file survives parsing`() {
        // A post silently dropped for a missing field would simply never appear
        // on the phone, with nothing to explain why. Compare what the file
        // declares against what actually parses.
        val declared = Regex("\"id\"\\s*:").findAll(raw).count()
        val parsed = OwnerPosts.parse(raw).size
        assertTrue(
            "content/posts.json declares $declared posts but only $parsed parsed. " +
                "One is missing a required field, or has a malformed publishedAt.",
            declared == parsed,
        )
    }

    @Test
    fun `every post has a body and a standfirst`() {
        OwnerPosts.parse(raw).forEach { post ->
            assertTrue(
                "Post '${post.id}' has no blocks, so it would open as a blank page.",
                post.blocks.isNotEmpty(),
            )
            assertTrue(
                "Post '${post.id}' has no summary, so it would show as a bare " +
                    "headline with no standfirst in the feed.",
                post.summary.isNotBlank(),
            )
        }
    }
}
