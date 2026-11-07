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

/**
 * A `mailto:` address that carries its own subject and body (RFC 6068).
 * Several mail apps ignore `EXTRA_SUBJECT`/`EXTRA_TEXT` on `ACTION_SENDTO`
 * and open an empty draft, losing the story ID; the query keeps them.
 * Spaces are `%20`, never `+`, which mail apps would show literally.
 */
internal fun mailtoUri(address: String, subject: String, body: String? = null): String = buildString {
    append("mailto:").append(address).append("?subject=").append(mailtoEncode(subject))
    if (body != null) append("&body=").append(mailtoEncode(body))
}

private fun mailtoEncode(text: String): String =
    java.net.URLEncoder.encode(text, Charsets.UTF_8.name()).replace("+", "%20")
