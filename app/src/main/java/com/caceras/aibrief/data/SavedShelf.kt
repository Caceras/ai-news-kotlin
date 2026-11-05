package com.caceras.aibrief.data

/** The most posts the saved shelf keeps; the oldest save drops off beyond this. */
const val MAX_SAVED_ARTICLES = 80

/**
 * Returns [saved] with [article] removed if it was saved, or added to the top
 * if it was not. Pure, so the UI can update the shelf instantly from memory
 * and leave the disk write to a background thread.
 */
fun toggledSaved(
    saved: List<NewsArticle>,
    article: NewsArticle,
    maximum: Int = MAX_SAVED_ARTICLES,
): List<NewsArticle> =
    if (saved.any { it.id == article.id }) {
        saved.filterNot { it.id == article.id }
    } else {
        (listOf(article) + saved).take(maximum)
    }
