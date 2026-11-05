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

    /** A primary source or further reading; no embedded browser or scripts. */
    data class Link(val text: String, val url: String) : PostBlock
}

/**
 * How a story is presented. Each format has exactly one card and one reader
 * header, so an editor chooses the kind of story and the app chooses the look.
 * Posts never carry their own colours, fonts or layout.
 */
enum class StoryFormat(val label: String) {
    /** Reported news: photo on top, heavy headline, a one-line deck. */
    NEWS("News"),

    /** Fast-moving news: a split collage of up to three pictures and a red kicker. */
    BREAKING("Breaking"),

    /** Background writing that explains a topic. Reads like news, labelled as an explainer. */
    EXPLAINER("Explainer"),

    /** A signed column: portrait, serif headline, and a views-are-the-author's note. */
    OPINION("Opinion"),

    /** An AI Brief playbook or how-to, set on a designed gradient card. */
    GUIDE("Guide"),

    /** Paid content. Same card as a guide, always labelled with its sponsor. */
    SPONSORED("Sponsored"),
    ;

    companion object {
        /** Unknown or missing formats read as news, so a newer post never breaks an older build. */
        fun parse(raw: String?): StoryFormat =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: NEWS
    }
}

/**
 * A story's place in a numbered series, such as part 2 of 3 of "AI and
 * philosophy". Parts of one series share a look on the front page and link to
 * each other in the reader.
 */
data class SeriesInfo(val id: String, val title: String, val part: Int, val count: Int) {
    /** "AI and philosophy · Part 2 of 3" */
    val label: String get() = "$title · Part $part of $count"
}

/**
 * The only backgrounds a designed card (guide or sponsored) may use. A post
 * names one; it cannot supply colours of its own.
 */
enum class CardTheme {
    SKY, LILAC, MINT, PEACH, OCEAN, INDIGO, BLUSH, NAVY;

    /** Dark themes carry white type; light ones carry ink. */
    val isDark: Boolean get() = this == OCEAN || this == INDIGO || this == NAVY

    companion object {
        fun parse(raw: String?): CardTheme? =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }

        /** A stable theme for a post that did not name one, so it never changes between refreshes. */
        fun forId(id: String): CardTheme = entries[Math.floorMod(id.hashCode(), entries.size)]
    }
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
    val format: StoryFormat = StoryFormat.NEWS,
    /** A short red lead-in set before the headline, such as "Revealed". */
    val kicker: String? = null,
    /** Extra cover pictures, used by breaking news as a split collage. */
    val gallery: List<String> = emptyList(),
    val authorImageUrl: String? = null,
    /** The author's title, such as "Columnist". */
    val authorRole: String? = null,
    val theme: CardTheme? = null,
    /** Who paid for a sponsored post. Sponsored posts without one are refused. */
    val sponsor: String? = null,
    /** Set when a published story is revised, and shown beside the date. */
    val updatedAt: Instant? = null,
    /** Who made the cover, shown under it in the reader, such as "Illustration: AI Brief". */
    val imageCredit: String? = null,
    val series: SeriesInfo? = null,
)

/** Reads `content/posts.json`, the document that carries every original post. */
object OwnerPosts {

    /** A remote edition must keep the reader's complete, sourced publication intact. */
    fun isEditorialDocument(raw: String): Boolean {
        val document = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        if (document.optInt("version") != 1) return false
        val declared = document.optJSONArray("posts") ?: return false
        if (declared.length() !in 1..200) return false
        val posts = parse(raw)
        return posts.size == declared.length() && posts.all { post ->
            post.summary.isNotBlank() && !post.author.isNullOrBlank() &&
                post.blocks.any { it is PostBlock.Link } &&
                post.blocks.any { it is PostBlock.Paragraph || it is PostBlock.Quote || it is PostBlock.Bullets }
        }
    }

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
        val format = StoryFormat.parse(json.optString("format"))
        val sponsor = json.cleanText("sponsor", MAX_NAME)
        // Paid content that does not say who paid is never shown.
        if (format == StoryFormat.SPONSORED && sponsor == null) return null

        return OwnerPost(
            id = id,
            title = title,
            summary = json.optString("summary").trim(),
            author = json.optString("author").trim().takeIf { it.isNotBlank() },
            category = parseCategory(json.optString("category")),
            imageUrl = json.optString("imageUrl").trim().takeIf { it.startsWith("https://") },
            publishedAt = publishedAt,
            blocks = parseBlocks(json.optJSONArray("blocks")),
            format = format,
            kicker = json.cleanText("kicker", MAX_KICKER),
            gallery = httpsList(json.optJSONArray("gallery")).take(MAX_GALLERY),
            authorImageUrl = json.optString("authorImageUrl").trim().takeIf { it.startsWith("https://") },
            authorRole = json.cleanText("authorRole", MAX_NAME),
            theme = CardTheme.parse(json.optString("theme")),
            sponsor = sponsor,
            updatedAt = parseInstant(json.optString("updatedAt"))?.takeIf { it.isAfter(publishedAt) },
            imageCredit = json.cleanText("imageCredit", MAX_CREDIT),
            series = parseSeries(json.optJSONObject("series")),
        )
    }

    /** A well-formed series reference, or null: a broken one never hides the story itself. */
    fun parseSeries(json: JSONObject?): SeriesInfo? {
        if (json == null) return null
        val id = json.optString("id").trim().takeIf { SERIES_ID.matches(it) } ?: return null
        val title = json.cleanText("title", MAX_SERIES_TITLE) ?: return null
        val count = json.optInt("of", 0).takeIf { it in 2..MAX_SERIES_PARTS } ?: return null
        val part = json.optInt("part", 0).takeIf { it in 1..count } ?: return null
        return SeriesInfo(id, title, part, count)
    }

    /** Writes [series] in the shape [parseSeries] reads. */
    fun encodeSeries(series: SeriesInfo): JSONObject = JSONObject()
        .put("id", series.id).put("title", series.title).put("part", series.part).put("of", series.count)

    private fun JSONObject.cleanText(key: String, limit: Int): String? =
        optString(key).trim().takeIf { it.isNotEmpty() && it.length <= limit }

    /** HTTPS addresses only, in order, without repeats. */
    fun httpsList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length())
            .map { array.optString(it).trim() }
            .filter { it.startsWith("https://") }
            .distinct()
    }

    /** Breaking news shows the cover plus at most two more pictures. */
    const val MAX_GALLERY = 2
    private const val MAX_KICKER = 24
    private const val MAX_NAME = 80
    private const val MAX_CREDIT = 240
    private const val MAX_SERIES_TITLE = 60
    private const val MAX_SERIES_PARTS = 12
    private val SERIES_ID = Regex("[a-z0-9][a-z0-9-]{0,59}")

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

            "link" -> json.optString("url").trim()
                .takeIf { it.startsWith("https://") }
                ?.let { url -> json.text()?.let { PostBlock.Link(it, url) } }

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

                    is PostBlock.Link -> JSONObject()
                        .put("type", "link")
                        .put("text", block.text)
                        .put("url", block.url)
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
