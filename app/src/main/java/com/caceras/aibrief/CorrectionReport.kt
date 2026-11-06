package com.caceras.aibrief

import com.caceras.aibrief.data.NewsArticle

/** A prefilled email that reports an error in one story, so the editor knows exactly which copy was read. */
internal data class CorrectionEmail(val subject: String, val body: String)

internal fun correctionEmail(article: NewsArticle, appVersion: String): CorrectionEmail = CorrectionEmail(
    subject = "Correction: ${article.title}",
    body = buildString {
        append("What is wrong, and where did you find the correct information?\n\n\n")
        append("---\n")
        append("Story: ${article.title}\n")
        append("Story ID: ${article.id}\n")
        if (article.url.isNotBlank()) append("Link: ${article.url}\n")
        append("Published: ${article.publishedAt}\n")
        article.updatedAt?.let { append("Updated: $it\n") }
        append("App: AI Brief $appVersion\n")
    },
)
