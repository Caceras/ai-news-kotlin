package com.caceras.aibrief.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class StorySearchTest {
    private fun story(id: String, title: String, summary: String = "", sponsor: String? = null) = NewsArticle(
        id = id, title = title, source = "AI Brief", url = "", publishedAt = Instant.EPOCH,
        summary = summary, category = NewsCategory.ALL,
        format = if (sponsor != null) StoryFormat.SPONSORED else StoryFormat.NEWS, sponsor = sponsor)

    private val actStory = story("act", "The EU AI Act's four risk levels", "High-risk rules move to 2027.")
    private val chatStory = story("chat", "Why chatbots guess instead of saying “I don’t know”")
    private val mention = story("mention", "Model cards", "Read them before relying on the AI Act.")
    private val edition = listOf(mention, actStory, chatStory)

    private fun ids(query: String) = StorySearch.filter(edition, query).map { it.id }

    @Test
    fun `a blank query keeps the whole edition in order`() {
        assertEquals(listOf("mention", "act", "chat"), ids("   "))
    }

    @Test
    fun `every word must appear, in any order`() {
        assertEquals(listOf("act"), ids("EU act"))
        assertEquals(listOf("act"), ids("levels risk"))
        assertEquals(emptyList<String>(), ids("EU chatbots"))
    }

    @Test
    fun `headline matches come before matches in the body`() {
        assertEquals(listOf("act", "mention"), ids("AI act"))
    }

    @Test
    fun `case, accents and curly apostrophes are ignored`() {
        assertEquals(listOf("chat"), ids("DON'T KNOW"))
        assertEquals(listOf("chat"), ids("dont"))
        val accented = story("fr", "Une règle pour l’IA")
        assertEquals(listOf("fr"), StorySearch.filter(listOf(accented), "regle").map { it.id })
    }

    @Test
    fun `punctuation and hyphens split words`() {
        assertEquals(listOf("act"), ids("high risk"))
        assertEquals(listOf("act"), ids("high-risk"))
    }

    @Test
    fun `the sponsor's name finds sponsored content`() {
        val paid = story("paid", "Ten prompts for spreadsheets", sponsor = "Acme Tools")
        assertEquals(listOf("paid"), StorySearch.filter(listOf(paid, actStory), "acme").map { it.id })
    }
}
