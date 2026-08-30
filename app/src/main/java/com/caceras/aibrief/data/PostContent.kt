package com.caceras.aibrief.data

import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

/**
 * One element of an original AI Brief post.
 *
 * Posts are authored as a short list of typed blocks rather than as markup.
 * Every block maps onto type the app already defines, so an authored post can
 * introduce new content but never new styling, and the reader stays visually
 * identical to the rest of the app.
 */
sealed interface PostBlock {
    data class Paragraph(val text: String) : PostBlock

    data class Heading(val text: String) : PostBlock

    data class Quote(val text: String, val attribution: String? = null) : PostBlock

    data class Bullets(val items: List<String>) : PostBlock

    data class Image(val url: String, val caption: String? = null) : PostBlock
}

/**
 * An original post written by the owner of the app.
 *
 * These are published by committing `content/posts.json`, which the app reads at
 * runtime. Publishing a post therefore needs no new build and no app update.
 *
 * [publishedAt] doubles as the schedule: a post dated in the future is carried
 * in the file but withheld by [OwnerPosts.published] until that moment passes,
 * so a post can be written now and appear on its own later.
 */
data class OwnerPost(
    val id: String,
    val title: String,
    val summary: String,
    val author: String?,
    val category: NewsCategory,
    val imageUrl: String?,
    val publishedAt: Instant,
    val blocks: List<PostBlock>,
)

/** Reads `content/posts.json`, the document that carries every original post. */
object OwnerPosts {

    /**
     * Returns every post in [raw] that is due at [now], newest first.
     *
     * A malformed document yields an empty list rather than an error: the feed
     * is still full of live reporting, and a broken post file should not be
     * able to take the app down.
     */
    fun published(raw: String, now: Instant = Instant.now()): List<OwnerPost> =
        parse(raw)
            .filter { !it.publishedAt.isAfter(now) }
            .sortedByDescending { it.publishedAt }

    /** Parses every well-formed post in [raw], including ones not yet due. */
    fun parse(raw: String): List<OwnerPost> {
        val document = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        val posts = document.optJSONArray("posts") ?: return emptyList()
        return (0 until posts.length())
            .mapNotNull { index -> posts.optJSONObject(index) }
            .mapNotNull(::parsePost)
            .distinctBy { it.id }
    }

    private fun parsePost(json: JSONObject): OwnerPost? {
        val id = json.optString("id").trim().ifBlank { return null }
        val title = json.optString("title").trim().ifBlank { return null }
        val publishedAt = parseInstant(json.optString("publishedAt")) ?: return null

        return OwnerPost(
            id = id,
            title = title,
            summary = json.optString("summary").trim(),
            author = json.optString("author").trim().takeIf { it.isNotBlank() },
            category = parseCategory(json.optString("category")),
            imageUrl = json.optString("imageUrl").trim().takeIf { it.startsWith("https://") },
            publishedAt = publishedAt,
            blocks = parseBlocks(json.optJSONArray("blocks")),
        )
    }

    private fun parseBlocks(array: JSONArray?): List<PostBlock> {
        if (array == null) return emptyList()
        return (0 until array.length())
            .mapNotNull { index -> array.optJSONObject(index) }
            .mapNotNull(::parseBlock)
    }

    /**
     * Returns the block [json] describes, or `null` if this build does not know
     * the type.
     *
     * Posts are fetched at runtime, so a phone running an older build can be
     * handed a post using a block type that build predates. Skipping the unknown
     * block renders the rest of the post rather than failing the whole thing.
     */
    private fun parseBlock(json: JSONObject): PostBlock? =
        when (json.optString("type").trim().lowercase()) {
            "paragraph" -> json.text()?.let(PostBlock::Paragraph)

            "heading" -> json.text()?.let(PostBlock::Heading)

            "quote" -> json.text()?.let { text ->
                PostBlock.Quote(
                    text = text,
                    attribution = json.optString("attribution").trim().takeIf { it.isNotBlank() },
                )
            }

            "bullets" -> json.optJSONArray("items")
                ?.let { items ->
                    (0 until items.length())
                        .map { items.optString(it).trim() }
                        .filter { it.isNotEmpty() }
                }
                ?.takeIf { it.isNotEmpty() }
                ?.let(PostBlock::Bullets)

            // Images are downloaded and shown, so a plaintext URL is refused for
            // the same reason the updater refuses one.
            "image" -> json.optString("url").trim()
                .takeIf { it.startsWith("https://") }
                ?.let { url ->
                    PostBlock.Image(
                        url = url,
                        caption = json.optString("caption").trim().takeIf { it.isNotBlank() },
                    )
                }

            else -> null
        }

    /**
     * Writes [blocks] back out in the same shape [parseBlocks] reads.
     *
     * Cached and saved articles are stored as JSON on the device, so a post has
     * to survive that round trip to still read correctly offline or from the
     * saved shelf.
     */
    fun encodeBlocks(blocks: List<PostBlock>): JSONArray {
        val array = JSONArray()
        blocks.forEach { block ->
            array.put(
                when (block) {
                    is PostBlock.Paragraph -> JSONObject()
                        .put("type", "paragraph")
                        .put("text", block.text)

                    is PostBlock.Heading -> JSONObject()
                        .put("type", "heading")
                        .put("text", block.text)

                    is PostBlock.Quote -> JSONObject()
                        .put("type", "quote")
                        .put("text", block.text)
                        .put("attribution", block.attribution)

                    is PostBlock.Bullets -> JSONObject()
                        .put("type", "bullets")
                        .put("items", JSONArray(block.items))

                    is PostBlock.Image -> JSONObject()
                        .put("type", "image")
                        .put("url", block.url)
                        .put("caption", block.caption)
                },
            )
        }
        return array
    }

    /** Reads back what [encodeBlocks] wrote. */
    fun decodeBlocks(array: JSONArray?): List<PostBlock> = parseBlocks(array)

    private fun JSONObject.text(): String? = optString("text").trim().takeIf { it.isNotEmpty() }

    /** Unknown or missing categories fall back to the general bucket. */
    private fun parseCategory(raw: String): NewsCategory =
        NewsCategory.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
            ?: NewsCategory.ALL

    private fun parseInstant(raw: String): Instant? =
        runCatching { Instant.parse(raw.trim()) }.getOrNull()
}
