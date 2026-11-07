package com.caceras.aibrief

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.PostBlock
import com.caceras.aibrief.data.StoryFormat

/**
 * The text a story is shared with. Paid content keeps its disclosure when it
 * leaves the app, so whoever receives it still sees who paid. Empty parts and
 * repeated links are left out.
 */
internal fun shareText(article: NewsArticle): String {
    val parts = mutableListOf(article.title.trim())
    if (article.format == StoryFormat.SPONSORED && !article.sponsor.isNullOrBlank()) {
        parts += "Sponsored by ${article.sponsor.trim()}"
    }
    article.summary.trim().takeIf { it.isNotEmpty() }?.let { parts += it }
    val url = article.url.trim()
    if (url.isNotEmpty()) parts += url
    if (article.isOriginalPost) {
        article.blocks.filterIsInstance<PostBlock.Link>()
            .filter { it.url.isNotBlank() && it.url.trim() != url }
            .distinctBy { it.url.trim() }
            .forEach { parts += if (it.text.isBlank()) it.url.trim() else "${it.text.trim()}: ${it.url.trim()}" }
        parts += "From AI Brief"
    }
    return parts.filter { it.isNotEmpty() }.joinToString("\n\n")
}
