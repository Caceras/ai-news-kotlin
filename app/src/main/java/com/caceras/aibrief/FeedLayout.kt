package com.caceras.aibrief

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.StoryFormat
import java.time.Instant

/**
 * One slot of the front page. The feed is composed from these, in order, and
 * each slot has exactly one component that draws it.
 */
sealed interface FeedSlot {
    val key: String

    /** The top story: a full-bleed picture with its headline set on it. */
    data class Lead(val article: NewsArticle) : FeedSlot {
        override val key = "lead-${article.id}"
    }

    /** A full-width ribbon that opens a section. */
    data class Ribbon(val title: String, val breaking: Boolean = false) : FeedSlot {
        override val key = "ribbon-$title"
    }

    /** A full-width photo with a big headline. */
    data class Headline(val article: NewsArticle) : FeedSlot {
        override val key = "headline-${article.id}"
    }

    /** Two headlines side by side. */
    data class Pair(val left: NewsArticle, val right: NewsArticle) : FeedSlot {
        override val key = "pair-${left.id}-${right.id}"
    }

    /** Headline on the left, small picture on the right. */
    data class Row(val article: NewsArticle) : FeedSlot {
        override val key = "row-${article.id}"
    }

    /** Signed columns in a sideways rail. */
    data class OpinionRail(val articles: List<NewsArticle>) : FeedSlot {
        override val key = "opinion-${articles.first().id}"
    }

    /** A guide or sponsored post on its designed card. */
    data class Designed(val article: NewsArticle) : FeedSlot {
        override val key = "designed-${article.id}"
    }

    /** Every part of one series, in order, in a sideways rail. */
    data class SeriesRail(val title: String, val parts: List<NewsArticle>) : FeedSlot {
        override val key = "series-${parts.first().series?.id}"
    }

    /** The model leaderboard from the edition's data. */
    data object Rankings : FeedSlot {
        override val key = "rankings"
    }

    /** Newly listed and stealth models from the edition's data. */
    data object Radar : FeedSlot {
        override val key = "radar"
    }
}

/** Which data modules the edition carries, so the layout can make room for them. */
data class FeedModules(val rankings: Boolean = false, val radar: Boolean = false) {
    companion object {
        val NONE = FeedModules()
    }
}

/**
 * Lays out the front page like a newspaper. The rules are fixed so every
 * edition looks deliberate whatever was published:
 *
 * 1. The lead is the newest breaking story from the last [com.caceras.aibrief.data.BREAKING_WINDOW], or
 *    else the newest news story. A guide, sponsored post or column never leads
 *    while there is news to lead with.
 * 2. Two stories sit side by side under the lead.
 * 3. "Latest": the next four stories, newest first, as a list.
 * 4. One designed card (a guide or sponsored post), then the data modules:
 *    the model rankings, each series in its own rail, and the radar.
 * 5. Reviews gather under their own ribbon, then columns in one Opinion rail.
 * 6. Remaining news runs in a rhythm of one big headline, a pair and two rows,
 *    with at most one designed card after each, so two never touch.
 * 7. Leftover guides close the page under their own ribbon.
 *
 * Parts of a series appear only in their series rail, except a part that
 * leads the page.
 */
object FeedLayout {
    const val LATEST_COUNT = 4

    fun compose(articles: List<NewsArticle>, now: Instant, modules: FeedModules = FeedModules.NONE): List<FeedSlot> {
        if (articles.isEmpty()) return emptyList()
        val newest = articles.sortedByDescending { it.publishedAt }
        val lead = newest.firstOrNull { it.isBreakingAt(now) } ?: newest.firstOrNull { it.isNews } ?: newest.first()

        val rest = newest - lead
        val series = newest.filter { it.series != null }
            .groupBy { it.series!!.id }
            .values.filter { it.size > 1 || it.first() != lead }
            .map { parts -> parts.sortedBy { it.series!!.part } }
        val inRails = series.flatten().toSet()
        val news = ArrayDeque(rest.filter { it.isNews && it !in inRails })
        val opinions = rest.filter { it.format == StoryFormat.OPINION }
        val designed = ArrayDeque(rest.filter { it.isDesigned })
        val reviews = rest.filter { it.format == StoryFormat.REVIEW }

        val slots = mutableListOf<FeedSlot>(FeedSlot.Lead(lead))
        if (news.size >= 2) slots += FeedSlot.Pair(news.removeFirst(), news.removeFirst())

        val latest = List(minOf(LATEST_COUNT, news.size)) { news.removeFirst() }
        if (latest.isNotEmpty()) {
            slots += FeedSlot.Ribbon("Latest", breaking = lead.format == StoryFormat.BREAKING)
            latest.forEach { slots += FeedSlot.Row(it) }
        }
        // A designed card, sponsored or not, never sits directly under the lead.
        if (slots.last() !is FeedSlot.Lead) designed.removeFirstOrNull()?.let { slots += FeedSlot.Designed(it) }

        if (modules.rankings) slots += FeedSlot.Rankings
        series.forEach { parts ->
            slots += FeedSlot.Ribbon("Series: ${parts.first().series!!.title}")
            slots += FeedSlot.SeriesRail(parts.first().series!!.title, parts)
        }
        if (modules.radar) slots += FeedSlot.Radar
        if (reviews.isNotEmpty()) {
            slots += FeedSlot.Ribbon("Reviews")
            reviews.forEach { slots += FeedSlot.Row(it) }
        }
        if (opinions.isNotEmpty()) {
            slots += FeedSlot.Ribbon("Opinion")
            slots += FeedSlot.OpinionRail(opinions)
        }

        var cycle = 0
        while (news.isNotEmpty()) {
            if (cycle == 0) slots += FeedSlot.Ribbon("More stories")
            slots += FeedSlot.Headline(news.removeFirst())
            if (news.size >= 2) slots += FeedSlot.Pair(news.removeFirst(), news.removeFirst())
            repeat(2) { news.removeFirstOrNull()?.let { slots += FeedSlot.Row(it) } }
            designed.removeFirstOrNull()?.let { slots += FeedSlot.Designed(it) }
            cycle++
        }
        if (designed.isNotEmpty()) {
            slots += FeedSlot.Ribbon("Guides")
            // Sponsored content is never first in the closing run.
            designed.sortedBy { it.format == StoryFormat.SPONSORED }.forEach { slots += FeedSlot.Designed(it) }
        }
        return slots
    }

    private val NewsArticle.isNews: Boolean
        get() = format == StoryFormat.NEWS || format == StoryFormat.BREAKING || format == StoryFormat.EXPLAINER
}
