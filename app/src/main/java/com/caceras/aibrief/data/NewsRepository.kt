package com.caceras.aibrief.data

import android.content.Context
import android.text.Html
import android.util.Xml
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
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
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

enum class NewsCategory(val label: String) {
    ALL("All"),
    RESEARCH("Research"),
    PRODUCTS("Products"),
    POLICY("Policy"),
    BUILDERS("Builders"),
}

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
) {
    val readingTimeMinutes: Int
        get() = ((title.split(Regex("\\s+")).size + summary.split(Regex("\\s+")).size) / 45 + 1).coerceIn(1, 15)
}

enum class FeedFreshness {
    LIVE,
    CACHED,
    OFFLINE,
}

data class NewsLoad(
    val articles: List<NewsArticle>,
    val freshness: FeedFreshness,
    val updatedAt: Instant? = null,
)

private data class FeedSource(
    val name: String,
    val url: String,
    val category: NewsCategory,
)

/** Fetches public editorial feeds and keeps saved reads available without an account. */
class NewsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "ai_brief_preferences",
        Context.MODE_PRIVATE,
    )

    fun initialLoad(): NewsLoad {
        val cached = cachedArticles()
        return if (!cached.isNullOrEmpty()) {
            NewsLoad(
                articles = cached,
                freshness = FeedFreshness.CACHED,
                updatedAt = cachedAt(),
            )
        } else {
            NewsLoad(
                articles = SeedArticles.create(),
                freshness = FeedFreshness.OFFLINE,
            )
        }
    }

    suspend fun loadLatest(): NewsLoad = withContext(Dispatchers.IO) {
        val liveArticles = coroutineScope {
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
        }
            .distinctBy { it.url }
            .sortedByDescending { it.publishedAt }

        if (liveArticles.isNotEmpty()) {
            val cached = cachedArticles().orEmpty()
            val refreshedArticles = (liveArticles + cached.filter { cachedArticle ->
                liveArticles.none { liveArticle -> liveArticle.url == cachedArticle.url }
            })
                .distinctBy { it.url }
                .sortedByDescending { it.publishedAt }
                .take(MAX_ARTICLES)
            val fallback = SeedArticles.create().filter { seed ->
                refreshedArticles.none { article -> article.url == seed.url }
            }
            cacheLiveArticles(refreshedArticles)
            NewsLoad(
                articles = (refreshedArticles + fallback).take(MAX_ARTICLES),
                freshness = FeedFreshness.LIVE,
                updatedAt = Instant.now(),
            )
        } else {
            cachedArticles()?.let { cached ->
                NewsLoad(
                    articles = cached,
                    freshness = FeedFreshness.CACHED,
                    updatedAt = cachedAt(),
                )
            } ?: NewsLoad(
                articles = SeedArticles.create(),
                freshness = FeedFreshness.OFFLINE,
            )
        }
    }

    fun savedArticles(): List<NewsArticle> = restoredArticles(SAVED_ARTICLES_KEY)

    fun toggleSaved(article: NewsArticle): List<NewsArticle> {
        val saved = savedArticles().toMutableList()
        val existingIndex = saved.indexOfFirst { it.id == article.id }
        if (existingIndex >= 0) {
            saved.removeAt(existingIndex)
        } else {
            saved.add(0, article)
            if (saved.size > MAX_SAVED_ARTICLES) {
                saved.removeAt(saved.lastIndex)
            }
        }
        persistArticles(SAVED_ARTICLES_KEY, saved, MAX_SAVED_ARTICLES)
        return saved
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
            connection.setRequestProperty("User-Agent", "AI-Brief-Android/2.0")

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

        return NewsArticle(
            id = stableId(cleanLink),
            title = cleanTitle,
            source = feed.name,
            author = plainText(author).takeIf { it.isNotBlank() },
            url = cleanLink,
            imageUrl = imageUrl.trim().takeIf { it.startsWith("http") },
            publishedAt = parseDate(published),
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

    private fun restoredArticles(key: String): List<NewsArticle> {
        val rawArticles = preferences.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawArticles)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toArticle())
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistArticles(key: String, articles: List<NewsArticle>, maximum: Int) {
        val encoded = JSONArray().apply {
            articles.take(maximum).forEach { article ->
                put(article.toJson())
            }
        }
        preferences.edit().putString(key, encoded.toString()).apply()
    }

    private fun cacheLiveArticles(articles: List<NewsArticle>) {
        persistArticles(CACHED_ARTICLES_KEY, articles, MAX_ARTICLES)
        preferences.edit().putLong(CACHED_AT_KEY, Instant.now().toEpochMilli()).apply()
    }

    private fun cachedArticles(): List<NewsArticle>? = restoredArticles(CACHED_ARTICLES_KEY)
        .takeIf { it.isNotEmpty() }

    private fun cachedAt(): Instant? = preferences.getLong(CACHED_AT_KEY, 0)
        .takeIf { it > 0 }
        ?.let(Instant::ofEpochMilli)

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
    }

    private fun JSONObject.toArticle(): NewsArticle = NewsArticle(
        id = getString("id"),
        title = getString("title"),
        source = getString("source"),
        author = optString("author").takeIf { it.isNotBlank() },
        url = getString("url"),
        imageUrl = optString("imageUrl").takeIf { it.isNotBlank() },
        publishedAt = Instant.ofEpochMilli(getLong("publishedAt")),
        summary = getString("summary"),
        category = NewsCategory.valueOf(getString("category")),
        isOfflineBrief = optBoolean("isOfflineBrief", false),
    )

    private fun plainText(raw: String): String = Html.fromHtml(
        raw,
        Html.FROM_HTML_MODE_LEGACY,
    ).toString().replace(WHITESPACE, " ").trim()

    private fun parseDate(value: String): Instant {
        val date = value.trim()
        return listOf<() -> Instant>(
            { ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() },
            { OffsetDateTime.parse(date).toInstant() },
            { Instant.parse(date) },
        ).firstNotNullOfOrNull { parser -> runCatching(parser).getOrNull() } ?: Instant.now()
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
        const val SAVED_ARTICLES_KEY = "saved_articles"
        const val CACHED_ARTICLES_KEY = "cached_articles"
        const val CACHED_AT_KEY = "cached_at"
        const val NETWORK_TIMEOUT_MILLIS = 7_000
        const val ARTICLES_PER_SOURCE = 8
        const val MAX_ARTICLES = 24
        const val MAX_SAVED_ARTICLES = 80
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
            url = "https://www.aisi.gov/",
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
