package com.caceras.aibrief

import com.caceras.aibrief.data.NewsCategory

/** What the front page says when nothing matches, and the way out it offers. */
internal data class EmptyFeedCopy(val title: String, val hint: String, val showAllTopics: Boolean = false)

/**
 * A search with no results asks for other words; an empty topic offers the
 * whole edition with one tap; an empty edition says to pull for new stories.
 * Advice to try "a different topic or search term" is wrong when the reader
 * has neither picked a topic nor typed anything.
 */
internal fun emptyFeedCopy(query: String, category: NewsCategory): EmptyFeedCopy = when {
    query.isNotBlank() -> EmptyFeedCopy("No stories found.", "Try another word, or fewer words.")
    category != NewsCategory.ALL ->
        EmptyFeedCopy("No ${category.label} stories yet.", "Other topics have stories today.", showAllTopics = true)
    else -> EmptyFeedCopy("More stories are on their way.", "Pull down to check for new stories.")
}
