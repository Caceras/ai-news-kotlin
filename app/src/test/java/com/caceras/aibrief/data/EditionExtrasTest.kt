package com.caceras.aibrief.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditionExtrasTest {
    private val document = """
        {"version": 1, "posts": [], "extras": {
          "rankings": {"title": "Top AI models", "metric": "Intelligence Index", "source": "Artificial Analysis",
            "sourceUrl": "https://artificialanalysis.ai/leaderboards/models", "checkedAt": "2026-11-05T20:00:00Z",
            "entries": [{"rank": 2, "name": "B", "maker": "Lab B", "score": 50.5},
                        {"rank": 1, "name": "A", "maker": "Lab A", "score": 60, "openWeights": true},
                        {"rank": 3, "name": "No score"}]},
          "radar": {"title": "On the radar", "source": "OpenRouter", "sourceUrl": "https://openrouter.ai/models",
            "entries": [{"name": "Crouton Alpha", "maker": "Unannounced", "listedAt": "2026-11-03",
                         "note": "A stealth model.", "url": "https://openrouter.ai/stealth/crouton-alpha",
                         "stealth": true},
                        {"name": "Plain link", "url": "http://insecure.example"}]}
        }}
    """.trimIndent()

    @Test
    fun `rankings are read in rank order and rows without a score are dropped`() {
        val rankings = EditionExtras.parse(document).rankings!!
        assertEquals(listOf("A", "B"), rankings.entries.map { it.name })
        assertTrue(rankings.entries.first().openWeights)
        assertNotNull(rankings.checkedAt)
    }

    @Test
    fun `radar entries need an https link`() {
        val radar = EditionExtras.parse(document).radar!!
        assertEquals(listOf("Crouton Alpha"), radar.entries.map { it.name })
        assertTrue(radar.entries.single().stealth)
    }

    @Test
    fun `a module without its source is dropped, never shown unattributed`() {
        val extras = EditionExtras.parse(document.replace("\"source\": \"Artificial Analysis\",", ""))
        assertNull(extras.rankings)
        assertNotNull(extras.radar)
    }

    @Test
    fun `a document without extras or a broken one has none`() {
        assertEquals(EditionExtras.EMPTY, EditionExtras.parse("""{"version": 1, "posts": []}"""))
        assertEquals(EditionExtras.EMPTY, EditionExtras.parse("not json"))
    }

    @Test
    fun `the published edition carries attributed modules`() {
        val file = listOf(File("../content/posts.json"), File("content/posts.json")).first { it.exists() }
        val extras = EditionExtras.parse(file.readText())
        assertNotNull(extras.rankings)
        assertNotNull(extras.radar)
    }

    @Test
    fun `series references are validated`() {
        fun parse(json: String) = OwnerPosts.parseSeries(org.json.JSONObject(json))
        assertEquals(SeriesInfo("ai-and-philosophy", "AI and philosophy", 2, 3),
            parse("""{"id": "ai-and-philosophy", "title": "AI and philosophy", "part": 2, "of": 3}"""))
        assertNull(parse("""{"id": "x", "title": "T", "part": 4, "of": 3}"""))
        assertNull(parse("""{"id": "Bad Id", "title": "T", "part": 1, "of": 3}"""))
    }
}
