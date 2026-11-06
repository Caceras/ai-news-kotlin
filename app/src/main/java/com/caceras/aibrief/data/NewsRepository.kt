package com.caceras.aibrief.data

import android.content.Context
import android.text.Html
import android.util.Xml
import androidx.core.content.edit
import com.caceras.aibrief.BuildConfig
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

enum class NewsCategory(val label: String) {
    ALL("All"),
    RESEARCH("Research"),
    PRODUCTS("Products"),
    POLICY("Policy"),
    BUILDERS("Builders"),
}

/** How long a story keeps its Breaking flag, and may lead the front page as breaking news. */
val BREAKING_WINDOW: Duration = Duration.ofHours(24)

data class NewsArticle(
    val id: String,
    val title: String,
    val source: String,
    val author: String? = null,
    val url: String,
    val imageUrl: String? = null,
    val publishedAt: Instant,
    val summary: String,
    val category: NewsCategory,
    val isOfflineBrief: Boolean = false,
    /**
     * Set only on original posts written by the owner. RSS stories carry an
     * empty list and are rendered from [summary] as before.
     */
    val blocks: List<PostBlock> = emptyList(),
    val format: StoryFormat = StoryFormat.NEWS,
    val kicker: String? = null,
    val gallery: List<String> = emptyList(),
    val authorImageUrl: String? = null,
    val authorRole: String? = null,
    val theme: CardTheme? = null,
    val sponsor: String? = null,
    val updatedAt: Instant? = null,
    val imageCredit: String? = null,
    val series: SeriesInfo? = null,
    val rating: Int? = null,
) {
    /** True for an original post, which has no external article to link out to. */
    val isOriginalPost: Boolean get() = blocks.isNotEmpty()

    /**
     * Breaking news is breaking for [BREAKING_WINDOW] after it was published or
     * last updated. After that it reads as an ordinary news story everywhere.
     */
    fun isBreakingAt(now: Instant): Boolean =
        format == StoryFormat.BREAKING && Duration.between(updatedAt ?: publishedAt, now) <= BREAKING_WINDOW

    /** This story as a reader should see it at [now]: stale breaking news loses its flag. */
    fun asOf(now: Instant): NewsArticle =
        if (format == StoryFormat.BREAKING && !isBreakingAt(now)) copy(format = StoryFormat.NEWS) else this

    /** Guides and sponsored posts share the designed card. */
    val isDesigned: Boolean get() = format == StoryFormat.GUIDE || format == StoryFormat.SPONSORED

    /** The theme a designed card uses: the named one, or a stable one picked from the ID. */
    val cardTheme: CardTheme get() = theme ?: CardTheme.forId(id)

    /** The cover first, then the extra collage pictures, without repeats. */
    val pictures: List<String> get() = (listOfNotNull(imageUrl?.takeIf { it.isNotBlank() }) + gallery).distinct()

    /** [searchableText] plus sponsor and kicker, folded by [StorySearch] once per article. */
    internal val foldedText: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        StorySearch.fold(listOfNotNull(searchableText, sponsor, kicker).joinToString(" "))
    }

    internal val foldedTitle: String by lazy(LazyThreadSafetyMode.PUBLICATION) { StorySearch.fold(title) }

    /**
     * Computed once per article. Story rows read [readingTimeMinutes] on every
     * recomposition and search scans every article on each keystroke, so
     * rebuilding a long post's full text each time made scrolling and typing
     * do needless work on the main thread.
     */
    val searchableText: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        listOf(title, summary, source, author.orEmpty()).joinToString(" ") +
            " " + blocks.joinToString(" ") { block ->
                when (block) {
                    is PostBlock.Paragraph -> block.text
                    is PostBlock.Heading -> block.text
                    is PostBlock.Quote -> block.text
                    is PostBlock.Bullets -> block.items.joinToString(" ")
                    is PostBlock.Image -> block.caption.orEmpty()
                    is PostBlock.Link -> block.text
                    is PostBlock.Video -> block.title
                }
            }
    }

    val readingTimeMinutes: Int by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ((searchableText.trim().split(WORD_BREAK).size + 199) / 200).coerceIn(1, 30)
    }
}

private val WORD_BREAK = Regex("\\s+")

enum class FeedFreshness {
    LIVE,
    CACHED,
    OFFLINE,
}

data class NewsLoad(
    val articles: List<NewsArticle>,
    val freshness: FeedFreshness,
    val updatedAt: Instant? = null,
    /** The front page's data modules carried by the same edition document. */
    val extras: EditionExtras = EditionExtras.EMPTY,
)

private data class FeedSource(
    val name: String,
    val url: String,
    val category: NewsCategory,
)

/** Decides when a refresh may ask the server for changes only. */
internal object OwnerPostsValidator {
    /**
     * The `If-None-Match` value to send, or null to fetch in full. A stored
     * ETag is only usable while the document it describes is still cached;
     * otherwise a 304 would leave nothing to show.
     */
    fun ifNoneMatch(etag: String?, cachedDocument: String?): String? =
        etag?.trim()?.takeIf { it.isNotEmpty() && !cachedDocument.isNullOrBlank() }
}

/** Fetches public editorial feeds and keeps saved reads available without an account. */
class NewsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "ai_brief_preferences",
        Context.MODE_PRIVATE,
    )

    // Read on first use, which is always off the main thread (see NewsViewModel).
    private val bundledPosts by lazy {
        context.applicationContext.assets.open("posts.json").bufferedReader().use { it.readText() }
    }

    private fun localPostsRaw(): String =
        preferences.getString(OWNER_POSTS_KEY, null) ?: bundledPosts

    private fun visiblePosts(raw: String): List<NewsArticle> =
        OwnerPosts.published(raw).map { it.toArticle() }

    /** Reads assets and preferences from disk, so callers run it off the main thread. */
    fun initialLoad(): NewsLoad {
        val raw = localPostsRaw()
        val posts = visiblePosts(raw)
        val feedCache = if (BuildConfig.EXTERNAL_FEEDS_ENABLED) {
            cachedArticles().orEmpty().filterNot { it.id.startsWith("post-") }
        } else emptyList()
        val articles = (posts + feedCache).distinctBy { it.id }
            .sortedByDescending { it.publishedAt }
        val hasCache = preferences.contains(OWNER_POSTS_KEY) || feedCache.isNotEmpty()
        return NewsLoad(
            articles = articles.ifEmpty { visiblePosts(bundledPosts).ifEmpty { SeedArticles.create() } },
            freshness = if (hasCache && articles.isNotEmpty()) FeedFreshness.CACHED else FeedFreshness.OFFLINE,
            updatedAt = cachedAt(),
            extras = EditionExtras.parse(raw),
        )
    }

    suspend fun loadLatest(): NewsLoad = withContext(Dispatchers.IO) {
        val (liveArticles, fetched) = coroutineScope {
            val feedJob = async {
                if (BuildConfig.EXTERNAL_FEEDS_ENABLED) {
                    feeds.map { feed ->
                        async {
                            try {
                                fetchFeed(feed)
                            } catch (error: Exception) {
                                if (error is CancellationException) throw error
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten()
                } else emptyList()
            }
            val postJob = async {
                try {
                    fetchOwnerPostDocument()
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    null
                }
            }
            feedJob.await() to postJob.await()
        }

        val postDocument = fetched?.body
        if (fetched != null) {
            // Cache the document, not only today's visible posts. Scheduled
            // writing becomes visible on a later refresh even when offline.
            // The ETag is stored with it, or cleared, so the two never disagree.
            preferences.edit {
                putString(OWNER_POSTS_KEY, fetched.body)
                if (fetched.etag != null) putString(OWNER_POSTS_ETAG_KEY, fetched.etag)
                else remove(OWNER_POSTS_ETAG_KEY)
            }
        }
        val raw = postDocument ?: localPostsRaw()
        val posts = visiblePosts(raw)
        val feedArticles = if (BuildConfig.EXTERNAL_FEEDS_ENABLED) {
            (liveArticles + cachedArticles().orEmpty().filterNot { it.id.startsWith("post-") })
                .distinctBy { it.id }.sortedByDescending { it.publishedAt }.take(MAX_ARTICLES)
        } else emptyList()
        val refreshed = postDocument != null || liveArticles.isNotEmpty()
        if (refreshed) cacheLiveArticles(feedArticles)
        val articles = (posts + feedArticles).distinctBy { it.id }
            .sortedByDescending { it.publishedAt }
        NewsLoad(
            articles = articles.ifEmpty { visiblePosts(bundledPosts).ifEmpty { SeedArticles.create() } },
            freshness = when {
                articles.isEmpty() -> FeedFreshness.OFFLINE
                refreshed -> FeedFreshness.LIVE
                preferences.contains(OWNER_POSTS_KEY) || feedArticles.isNotEmpty() -> FeedFreshness.CACHED
                else -> FeedFreshness.OFFLINE
            },
            updatedAt = cachedAt(),
            extras = EditionExtras.parse(raw),
        )
    }

    /**
     * A malformed update keeps the last good document and bundled edition.
     *
     * Sends the stored ETag so an unchanged edition costs a 304 with no body
     * instead of the whole document. Returns the fetched document (and its
     * ETag), or the cached one when the server confirms it is still current.
     */
    private fun fetchOwnerPostDocument(): FetchedDocument? {
        val cached = preferences.getString(OWNER_POSTS_KEY, null)
        val validator = OwnerPostsValidator.ifNoneMatch(preferences.getString(OWNER_POSTS_ETAG_KEY, null), cached)
        val connection = URL(OWNER_POSTS_URL).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = NETWORK_TIMEOUT_MILLIS
            connection.readTimeout = NETWORK_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "AI-Brief-Android/${BuildConfig.VERSION_NAME}")
            if (validator != null) connection.setRequestProperty("If-None-Match", validator)
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return if (validator != null && cached != null) FetchedDocument(cached, validator) else null
            }
            if (code !in 200..299) return null
            if (connection.contentLengthLong > MAX_EDITION_BYTES) return null
            // An oversized body is refused, keeping the last good document.
            val body = connection.inputStream.use { readBoundedText(it) } ?: return null
            if (!OwnerPosts.isEditorialDocument(body)) return null
            FetchedDocument(body, connection.getHeaderField("ETag"))
        } finally {
            connection.disconnect()
        }
    }

    private class FetchedDocument(val body: String, val etag: String?)

    private fun OwnerPost.toArticle(): NewsArticle = NewsArticle(
        id = "post-$id",
        title = title,
        source = OWNER_SOURCE,
        author = author,
        url = "",
        imageUrl = imageUrl,
        publishedAt = publishedAt,
        summary = summary,
        category = category,
        blocks = blocks,
        format = format,
        kicker = kicker,
        gallery = gallery,
        authorImageUrl = authorImageUrl,
        authorRole = authorRole,
        theme = theme,
        sponsor = sponsor,
        updatedAt = updatedAt,
        imageCredit = imageCredit,
        series = series,
        rating = rating,
    )

    fun savedArticles(): List<NewsArticle> = restoredArticles(SAVED_ARTICLES_KEY)

    /** Writes the saved shelf to disk. Encodes every saved post, so callers run it off the main thread. */
    fun persistSaved(saved: List<NewsArticle>) {
        persistArticles(SAVED_ARTICLES_KEY, saved, MAX_SAVED_ARTICLES)
    }

    /** The ids of stories this reader has opened; see [com.caceras.aibrief.ReadHistory]. */
    fun readStoryIds(): Set<String> =
        com.caceras.aibrief.ReadHistory.decode(preferences.getString(READ_STORIES_KEY, null))

    /** Writes the read list to disk, so callers run it off the main thread. */
    fun persistReadStoryIds(ids: Set<String>) {
        preferences.edit { putString(READ_STORIES_KEY, com.caceras.aibrief.ReadHistory.encode(ids)) }
    }

    /** The story text size picked in the reader; see [com.caceras.aibrief.ReaderTextSize]. */
    fun readerTextSize(): com.caceras.aibrief.ReaderTextSize =
        com.caceras.aibrief.ReaderTextSize.decode(preferences.getString(READER_TEXT_SIZE_KEY, null))

    /** Writes the text size to disk, so callers run it off the main thread. */
    fun persistReaderTextSize(size: com.caceras.aibrief.ReaderTextSize) {
        preferences.edit { putString(READER_TEXT_SIZE_KEY, size.name) }
    }

    private fun fetchFeed(feed: FeedSource): List<NewsArticle> {
        val connection = URL(feed.url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = NETWORK_TIMEOUT_MILLIS
            connection.readTimeout = NETWORK_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty(
                "Accept",
                "application/rss+xml, application/atom+xml, application/xml, text/xml",
            )
            connection.setRequestProperty("User-Agent", "AI-Brief-Android/${BuildConfig.VERSION_NAME}")

            if (connection.responseCode !in 200..299) return emptyList()
            connection.inputStream.use { stream -> parseFeed(stream, feed) }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseFeed(stream: InputStream, feed: FeedSource): List<NewsArticle> {
        val parser = Xml.newPullParser().apply { setInput(stream, null) }
        val articles = mutableListOf<NewsArticle>()
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT && articles.size < ARTICLES_PER_SOURCE) {
            if (event == XmlPullParser.START_TAG && parser.name.lowercase() in ENTRY_TAGS) {
                parseEntry(parser, feed)?.let(articles::add)
            }
            event = parser.next()
        }
        return articles
    }

    private fun parseEntry(parser: XmlPullParser, feed: FeedSource): NewsArticle? {
        val entryDepth = parser.depth
        var title = ""
        var link = ""
        var summary = ""
        var published = ""
        var tags = ""
        var author = ""
        var imageUrl = ""

        while (true) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> if (parser.depth == entryDepth) break
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "title" -> title = parser.readText()
                        "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            link = href ?: parser.readText()
                        }
                        "description", "summary", "encoded" -> {
                            val candidate = parser.readText()
                            if (candidate.length > summary.length) summary = candidate
                        }
                        "content" -> {
                            val mediaUrl = parser.getAttributeValue(null, "url")
                            if (mediaUrl.isImageReference(parser.getAttributeValue(null, "type"))) {
                                imageUrl = mediaUrl.orEmpty()
                                parser.skipCurrentTag()
                            } else {
                                val candidate = parser.readText()
                                if (candidate.length > summary.length) summary = candidate
                            }
                        }
                        "thumbnail", "enclosure" -> {
                            val mediaUrl = parser.getAttributeValue(null, "url")
                            if (imageUrl.isBlank() && mediaUrl.isImageReference(parser.getAttributeValue(null, "type"))) {
                                imageUrl = mediaUrl.orEmpty()
                            }
                            parser.skipCurrentTag()
                        }
                        "pubdate", "published", "updated", "date" -> published = parser.readText()
                        "creator", "author" -> if (author.isBlank()) author = parser.readText()
                        "category" -> {
                            tags += " " + (parser.getAttributeValue(null, "term") ?: parser.readText())
                        }
                    }
                }
                XmlPullParser.END_DOCUMENT -> break
            }
        }

        val cleanTitle = plainText(title)
        val cleanLink = link.trim()
        if (cleanTitle.isBlank() || !cleanLink.startsWith("http")) return null

        val parsedDate = parseDate(published) ?: return null

        return NewsArticle(
            id = stableId(cleanLink),
            title = cleanTitle,
            source = feed.name,
            author = plainText(author).takeIf { it.isNotBlank() },
            url = cleanLink,
            imageUrl = imageUrl.trim().takeIf { it.startsWith("http") },
            publishedAt = parsedDate,
            summary = plainText(summary).ifBlank { "Open the original story for the full report." }.take(MAX_SUMMARY_LENGTH),
            category = inferCategory("$tags $cleanTitle $summary", feed.category),
        )
    }

    private fun XmlPullParser.readText(): String = runCatching { nextText() }.getOrDefault("")

    private fun XmlPullParser.skipCurrentTag() {
        var depth = 1
        while (depth > 0) {
            when (next()) {
                XmlPullParser.START_TAG -> depth += 1
                XmlPullParser.END_TAG -> depth -= 1
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }

    private fun String?.isImageReference(type: String?): Boolean {
        val url = this?.trim().orEmpty()
        return url.startsWith("http") && (
            type.orEmpty().startsWith("image/") ||
                url.substringBefore('?').endsWith(".jpg", true) ||
                url.substringBefore('?').endsWith(".jpeg", true) ||
                url.substringBefore('?').endsWith(".png", true) ||
                url.substringBefore('?').endsWith(".webp", true)
            )
    }

    private fun restoredArticles(key: String): List<NewsArticle> =
        StoredArticles.decode(preferences.getString(key, null))

    private fun persistArticles(key: String, articles: List<NewsArticle>, maximum: Int) {
        preferences.edit { putString(key, StoredArticles.encode(articles, maximum)) }
    }

    private fun cacheLiveArticles(articles: List<NewsArticle>) {
        persistArticles(CACHED_ARTICLES_KEY, articles, MAX_ARTICLES)
        preferences.edit { putLong(CACHED_AT_KEY, Instant.now().toEpochMilli()) }
    }

    private fun cachedArticles(): List<NewsArticle>? = restoredArticles(CACHED_ARTICLES_KEY)
        .takeIf { it.isNotEmpty() }

    private fun cachedAt(): Instant? = preferences.getLong(CACHED_AT_KEY, 0)
        .takeIf { it > 0 }
        ?.let(Instant::ofEpochMilli)

    private fun plainText(raw: String): String = Html.fromHtml(
        raw,
        Html.FROM_HTML_MODE_LEGACY,
    ).toString().replace(WHITESPACE, " ").trim()

    private fun parseDate(value: String): Instant? {
        val date = value.trim()
        return listOf<() -> Instant>(
            { ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() },
            { OffsetDateTime.parse(date).toInstant() },
            { Instant.parse(date) },
        ).firstNotNullOfOrNull { parser -> runCatching(parser).getOrNull() }
    }

    private fun inferCategory(text: String, fallback: NewsCategory): NewsCategory {
        val normalized = text.lowercase()
        return when {
            normalized.contains("policy") || normalized.contains("regulation") || normalized.contains("law") || normalized.contains("safety") -> NewsCategory.POLICY
            normalized.contains("research") || normalized.contains("benchmark") || normalized.contains("paper") || normalized.contains("science") -> NewsCategory.RESEARCH
            normalized.contains("developer") || normalized.contains("open source") || normalized.contains("code") || normalized.contains("hugging face") -> NewsCategory.BUILDERS
            normalized.contains("product") || normalized.contains("launch") || normalized.contains("model") || normalized.contains("company") -> NewsCategory.PRODUCTS
            else -> fallback
        }
    }

    private fun stableId(url: String): String = "article-${url.hashCode().toUInt().toString(16)}"

    private companion object {
        const val OWNER_POSTS_KEY = "owner_posts_document"
        const val OWNER_POSTS_ETAG_KEY = "owner_posts_etag"
        const val SAVED_ARTICLES_KEY = "saved_articles"
        const val READ_STORIES_KEY = "read_story_ids"
        const val READER_TEXT_SIZE_KEY = "reader_text_size"
        const val CACHED_ARTICLES_KEY = "cached_articles"
        const val CACHED_AT_KEY = "cached_at"
        const val NETWORK_TIMEOUT_MILLIS = 7_000

        /**
         * Where original posts are read from. This points at the repository's
         * own content file, so a commit publishes a post immediately: the app
         * re-reads this on every refresh and never needs rebuilding for it.
         */
        const val OWNER_POSTS_URL =
            "https://raw.githubusercontent.com/Caceras/ai-news-kotlin/main/content/posts.json"

        /** Shown as the source line on original posts. */
        const val OWNER_SOURCE = "AI Brief"
        const val ARTICLES_PER_SOURCE = 8
        const val MAX_ARTICLES = 24
        const val MAX_SUMMARY_LENGTH = 320
        val ENTRY_TAGS = setOf("item", "entry")
        val WHITESPACE = Regex("\\s+")

        val feeds = listOf(
            FeedSource(
                name = "MIT News",
                url = "https://news.mit.edu/rss/topic/artificial-intelligence2",
                category = NewsCategory.RESEARCH,
            ),
            FeedSource(
                name = "Google AI",
                url = "https://research.google/blog/rss/",
                category = NewsCategory.RESEARCH,
            ),
            FeedSource(
                name = "Hugging Face",
                url = "https://huggingface.co/blog/feed.xml",
                category = NewsCategory.BUILDERS,
            ),
            FeedSource(
                name = "VentureBeat",
                url = "https://venturebeat.com/category/ai/feed/",
                category = NewsCategory.PRODUCTS,
            ),
        )
    }
}

/** A concise offline edition keeps the app useful when a source is unavailable. */
object SeedArticles {
    private val offlineEditionPublishedAt = Instant.parse("2026-08-17T12:00:00Z")

    fun create(now: Instant = offlineEditionPublishedAt): List<NewsArticle> = listOf(
        article(
            title = "The practical question for AI teams is no longer whether to use models, but where judgment stays human.",
            source = "AI Brief",
            url = "https://www.nist.gov/itl/ai-risk-management-framework",
            publishedAt = now.minusSeconds(60 * 42),
            summary = "The strongest workflows pair a clear human decision with a narrow, observable model task. Start with the decision, then make the automation earn its place.",
            category = NewsCategory.POLICY,
        ),
        article(
            title = "Small models are becoming serious tools for private, local work.",
            source = "AI Brief",
            url = "https://huggingface.co/blog",
            publishedAt = now.minusSeconds(60 * 60 * 3),
            summary = "Efficiency gains are bringing capable language and multimodal systems closer to the device, changing the tradeoff between privacy, cost, and speed.",
            category = NewsCategory.BUILDERS,
        ),
        article(
            title = "What an AI evaluation should reveal before a product reaches real people.",
            source = "AI Brief",
            url = "https://hai.stanford.edu/ai-index",
            publishedAt = now.minusSeconds(60 * 60 * 6),
            summary = "A useful evaluation reflects the actual work, names its failure modes, and gives people a way to recover when the model is wrong.",
            category = NewsCategory.RESEARCH,
        ),
        article(
            title = "The next AI product race will be decided by trust, not just capability.",
            source = "AI Brief",
            url = "https://news.mit.edu/topic/artificial-intelligence2",
            publishedAt = now.minusSeconds(60 * 60 * 9),
            summary = "As baseline model quality converges, clear controls, reliable behavior, and respectful product design become meaningful differentiators.",
            category = NewsCategory.PRODUCTS,
        ),
        article(
            title = "Open weights are expanding the set of people who can build with modern AI.",
            source = "AI Brief",
            url = "https://huggingface.co/docs/hub/model-cards",
            publishedAt = now.minusSeconds(60 * 60 * 13),
            summary = "The opportunity is broader experimentation. The responsibility is making provenance, evaluation, and deployment discipline easier to practice.",
            category = NewsCategory.BUILDERS,
        ),
        article(
            title = "AI policy is moving from broad principles toward concrete obligations.",
            source = "AI Brief",
            url = "https://oecd.ai/en/",
            publishedAt = now.minusSeconds(60 * 60 * 18),
            summary = "The emerging rules focus on accountability, documentation, risk, and the situations where people need meaningful recourse.",
            category = NewsCategory.POLICY,
        ),
        article(
            title = "The best AI interfaces make uncertainty visible without making work harder.",
            source = "AI Brief",
            url = "https://ai.google/",
            publishedAt = now.minusSeconds(60 * 60 * 24),
            summary = "A good interface offers context, lightweight verification, and a graceful handoff when an answer should be checked rather than accepted.",
            category = NewsCategory.PRODUCTS,
        ),
        article(
            title = "The most valuable AI research questions are increasingly about use, not only scale.",
            source = "AI Brief",
            url = "https://www.ai.gov/",
            publishedAt = now.minusSeconds(60 * 60 * 31),
            summary = "Researchers and practitioners are focusing more closely on reliability, measurement, and the social systems surrounding a model.",
            category = NewsCategory.RESEARCH,
        ),
    )

    private fun article(
        title: String,
        source: String,
        url: String,
        publishedAt: Instant,
        summary: String,
        category: NewsCategory,
    ) = NewsArticle(
        id = "seed-${url.hashCode().toUInt().toString(16)}",
        title = title,
        source = source,
        author = "AI Brief",
        url = url,
        publishedAt = publishedAt,
        summary = summary,
        category = category,
        isOfflineBrief = true,
    )
}
