package com.caceras.aibrief.data

import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

/**
 * The on-device format for the saved shelf and the cached edition.
 *
 * Decoding is tolerant per entry. The shelf used to be read with an
 * all-or-nothing parse, so a single entry this build could not read (for
 * example a category renamed in a later release) made every saved post
 * disappear, and the next save then wrote that empty shelf over the reader's
 * list. Now an unreadable entry is skipped, an unknown category falls back to
 * the general one, and everything else survives.
 */
object StoredArticles {

    fun encode(articles: List<NewsArticle>, maximum: Int): String = JSONArray().apply {
        articles.take(maximum).forEach { put(it.toJson()) }
    }.toString()

    fun decode(raw: String?): List<NewsArticle> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length())
            .mapNotNull { index -> array.optJSONObject(index)?.let(::decodeArticle) }
            .distinctBy { it.id }
    }

    private fun NewsArticle.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("source", source)
        put("author", author)
        put("url", url)
        put("imageUrl", imageUrl)
        put("publishedAt", publishedAt.toEpochMilli())
        put("summary", summary)
        put("category", category.name)
        put("isOfflineBrief", isOfflineBrief)
        put("blocks", OwnerPosts.encodeBlocks(blocks))
    }

    private fun decodeArticle(json: JSONObject): NewsArticle? = runCatching {
        val id = json.optString("id").trim().ifBlank { return null }
        val title = json.optString("title").trim().ifBlank { return null }
        if (!json.has("publishedAt")) return null
        NewsArticle(
            id = id,
            title = title,
            source = json.optString("source").trim().ifBlank { "AI Brief" },
            author = json.optString("author").takeIf { it.isNotBlank() },
            url = json.optString("url").trim(),
            imageUrl = json.optString("imageUrl").takeIf { it.isNotBlank() },
            publishedAt = Instant.ofEpochMilli(json.getLong("publishedAt")),
            summary = json.optString("summary"),
            category = NewsCategory.entries.firstOrNull { it.name == json.optString("category") }
                ?: NewsCategory.ALL,
            isOfflineBrief = json.optBoolean("isOfflineBrief", false),
            blocks = OwnerPosts.decodeBlocks(json.optJSONArray("blocks")),
        )
    }.getOrNull()
}
