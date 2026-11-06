package com.caceras.aibrief

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.StoryFormat

/**
 * The story offered at the end of a story, so a reader can keep reading
 * without going back to the front page.
 *
 * Walks the edition (newest first) from the story being read, wrapping round
 * once. Skips sponsored posts (paid content is never pushed at a reader),
 * other parts of the same series (the series box already lists them) and
 * stories on the reader's trail (offering the story they came from would
 * send them in circles). Prefers a story the reader has not opened yet
 * ([readIds]), and offers a read one only when every other is read. Null
 * when nothing is left.
 */
object UpNext {
    fun after(
        article: NewsArticle,
        edition: List<NewsArticle>,
        trailIds: Set<String> = emptySet(),
        readIds: Set<String> = emptySet(),
    ): NewsArticle? {
        val start = edition.indexOfFirst { it.id == article.id } + 1
        val candidates = (edition.drop(start) + edition.take(start)).filter { candidate ->
            candidate.id != article.id &&
                candidate.id !in trailIds &&
                candidate.format != StoryFormat.SPONSORED &&
                (article.series == null || candidate.series?.id != article.series.id)
        }
        return candidates.firstOrNull { it.id !in readIds } ?: candidates.firstOrNull()
    }
}
