package com.caceras.aibrief.data

import java.text.Normalizer
import java.util.Locale

/**
 * Matches what a reader types against the stories on the device.
 *
 * Every word must appear somewhere in the story, in any order, so "EU act"
 * finds "The EU AI Act's four risk levels". Case, accents and typographic
 * apostrophes or dashes are ignored, so "dont" or "don't" both find "don’t"
 * and "regle" finds "règle". Stories whose headline holds every word come
 * first; otherwise the edition order (newest first) is kept.
 */
object StorySearch {
    fun filter(articles: List<NewsArticle>, query: String): List<NewsArticle> {
        val words = wordsOf(query)
        if (words.isEmpty()) return articles
        val matches = articles.filter { article -> words.all { article.foldedText.contains(it) } }
        val (inTitle, elsewhere) = matches.partition { article -> words.all { article.foldedTitle.contains(it) } }
        return inTitle + elsewhere
    }

    internal fun wordsOf(query: String): List<String> =
        fold(query).split(' ').filter { it.isNotEmpty() }.distinct()

    /** Lower case, no accents, apostrophes dropped and other punctuation turned into spaces. */
    internal fun fold(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val out = StringBuilder(decomposed.length)
        for (ch in decomposed) {
            when {
                Character.getType(ch) == Character.NON_SPACING_MARK.toInt() -> Unit
                ch == '\'' || ch == '’' || ch == '‘' || ch == 'ʼ' -> Unit
                Character.isLetterOrDigit(ch) -> out.append(ch)
                else -> if (out.isNotEmpty() && out.last() != ' ') out.append(' ')
            }
        }
        return out.toString().trimEnd().lowercase(Locale.ROOT)
    }
}
