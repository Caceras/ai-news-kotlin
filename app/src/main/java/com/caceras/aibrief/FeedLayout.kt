package com.caceras.aibrief

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.StoryFormat
import java.time.Duration
import java.time.Instant

/**
 * One slot of the front page. The feed is composed from these, in order, and
 * each slot has exactly one component that draws it.
 */
sealed interface FeedSlot {
    val key: String

    /** The top story, drawn in its format's hero style. */
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
}

/**
 * Lays out the front page. The rules are fixed so every edition looks
 * deliberate whatever was published:
 *
 * 1. The lead is the newest breaking story from the last [BREAKING_WINDOW], or
 *    else the newest news story. A guide, sponsored post or column never leads
 *    while there is news to lead with.
 * 2. News then runs in a repeating rhythm: one big headline, a pair, two rows.
 * 3. Columns are gathered into one Opinion rail after the first rhythm.
 * 4. At most one designed card follows each rhythm, so two never touch, and a
 *    sponsored card never sits directly under the lead.
 * 5. Anything left over closes the page under its own ribbon.
 */
object FeedLayout {
    val BREAKING_WINDOW: Duration = Duration.ofHours(24)

    fun compose(articles: List<NewsArticle>, now: Instant): List<FeedSlot> {
        if (articles.isEmpty()) return emptyList()
        val newest = articles.sortedByDescending { it.publishedAt }
        val lead = newest.firstOrNull {
            it.format == StoryFormat.BREAKING && Duration.between(it.publishedAt, now) <= BREAKING_WINDOW
        } ?: newest.firstOrNull { it.isNews } ?: newest.first()

        val rest = newest - lead
        val news = ArrayDeque(rest.filter { it.isNews })
        val opinions = rest.filter { it.format == StoryFormat.OPINION }
        val designed = ArrayDeque(rest.filter { it.isDesigned })

        val slots = mutableListOf<FeedSlot>(FeedSlot.Lead(lead))
        var opinionPlaced = opinions.isEmpty()
        var cycle = 0
        while (news.isNotEmpty()) {
            if (cycle == 0) slots += FeedSlot.Ribbon(if (lead.format == StoryFormat.BREAKING) "Latest" else "Headlines")
            slots += FeedSlot.Headline(news.removeFirst())
            if (news.size >= 2) slots += FeedSlot.Pair(news.removeFirst(), news.removeFirst())
            repeat(2) { news.removeFirstOrNull()?.let { slots += FeedSlot.Row(it) } }
            if (!opinionPlaced) {
                slots += FeedSlot.Ribbon("Opinion")
                slots += FeedSlot.OpinionRail(opinions)
                opinionPlaced = true
            }
            designed.removeFirstOrNull()?.let { slots += FeedSlot.Designed(it) }
            cycle++
        }
        if (!opinionPlaced) {
            slots += FeedSlot.Ribbon("Opinion")
            slots += FeedSlot.OpinionRail(opinions)
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
