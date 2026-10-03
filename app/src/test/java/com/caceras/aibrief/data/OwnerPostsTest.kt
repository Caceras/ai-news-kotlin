package com.caceras.aibrief.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerPostsTest {

    @Test
    fun `incomplete remote editions cannot replace the bundled publication`() {
        assertFalse(OwnerPosts.isEditorialDocument("not json"))
        assertFalse(OwnerPosts.isEditorialDocument(document()))
        assertFalse(OwnerPosts.isEditorialDocument(document(post(extra =
            ""","summary":"Intro","author":"Editor","blocks":[{"type":"paragraph","text":"Unsourced body"}]"""))))
    }

    @Test
    fun `complete sourced scheduled editions are accepted but duplicate ids are not`() {
        val complete = post(publishedAt = "2027-01-01T00:00:00Z", extra =
            ""","summary":"Intro","author":"Editor","blocks":[
                {"type":"paragraph","text":"Original writing"},
                {"type":"link","text":"Evidence","url":"https://www.nist.gov/"}]""")
        assertTrue(OwnerPosts.isEditorialDocument(document(complete)))
        assertFalse(OwnerPosts.isEditorialDocument(document(complete, complete)))
        assertFalse(OwnerPosts.isEditorialDocument(document(complete).replace("\"version\":1", "\"version\":2")))
    }

    private val now = Instant.parse("2026-08-30T12:00:00Z")

    private fun document(vararg posts: String) =
        """{"version":1,"posts":[${posts.joinToString(",")}]}"""

    private fun post(
        id: String = "a",
        title: String = "A title",
        publishedAt: String = "2026-08-30T10:00:00Z",
        extra: String = "",
    ) = """{"id":"$id","title":"$title","publishedAt":"$publishedAt"$extra}"""

    @Test
    fun `reads a complete post`() {
        val posts = OwnerPosts.published(
            document(
                post(
                    extra = ""","summary":"Standfirst","author":"Rikard","category":"POLICY",
                        "blocks":[{"type":"paragraph","text":"Body"}]""",
                ),
            ),
            now,
        )

        assertEquals(1, posts.size)
        val only = posts.single()
        assertEquals("a", only.id)
        assertEquals("Standfirst", only.summary)
        assertEquals("Rikard", only.author)
        assertEquals(NewsCategory.POLICY, only.category)
        assertEquals(listOf(PostBlock.Paragraph("Body")), only.blocks)
    }

    @Test
    fun `withholds a post dated in the future`() {
        val raw = document(
            post(id = "due", publishedAt = "2026-08-30T11:59:00Z"),
            post(id = "scheduled", publishedAt = "2026-08-30T12:00:01Z"),
        )

        assertEquals(listOf("due"), OwnerPosts.published(raw, now).map { it.id })
        // The scheduled post is still in the file; it is only being withheld.
        assertEquals(setOf("due", "scheduled"), OwnerPosts.parse(raw).map { it.id }.toSet())
    }

    @Test
    fun `releases a scheduled post once its moment passes`() {
        val raw = document(post(id = "later", publishedAt = "2026-08-30T12:00:01Z"))

        assertTrue(OwnerPosts.published(raw, now).isEmpty())
        assertEquals(1, OwnerPosts.published(raw, Instant.parse("2026-08-30T12:00:01Z")).size)
    }

    @Test
    fun `a post exactly due is published`() {
        val raw = document(post(publishedAt = "2026-08-30T12:00:00Z"))
        assertEquals(1, OwnerPosts.published(raw, now).size)
    }

    @Test
    fun `orders newest first`() {
        val raw = document(
            post(id = "older", publishedAt = "2026-08-28T10:00:00Z"),
            post(id = "newer", publishedAt = "2026-08-29T10:00:00Z"),
        )
        assertEquals(listOf("newer", "older"), OwnerPosts.published(raw, now).map { it.id })
    }

    @Test
    fun `skips posts missing anything required`() {
        val raw = document(
            """{"title":"No id","publishedAt":"2026-08-30T10:00:00Z"}""",
            """{"id":"no-title","publishedAt":"2026-08-30T10:00:00Z"}""",
            """{"id":"no-date","title":"T"}""",
            """{"id":"bad-date","title":"T","publishedAt":"the third of May"}""",
            post(id = "good"),
        )
        assertEquals(listOf("good"), OwnerPosts.published(raw, now).map { it.id })
    }

    @Test
    fun `malformed json yields no posts rather than failing`() {
        assertTrue(OwnerPosts.published("not json at all", now).isEmpty())
        assertTrue(OwnerPosts.published("", now).isEmpty())
        assertTrue(OwnerPosts.published("""{"posts":"not an array"}""", now).isEmpty())
        assertTrue(OwnerPosts.published("""{"version":1}""", now).isEmpty())
    }

    @Test
    fun `keeps only the first post for a repeated id`() {
        val raw = document(
            post(id = "same", title = "First"),
            post(id = "same", title = "Second"),
        )
        assertEquals(listOf("First"), OwnerPosts.published(raw, now).map { it.title })
    }

    @Test
    fun `reads every supported block type`() {
        val raw = document(
            post(
                extra = ""","blocks":[
                    {"type":"paragraph","text":"P"},
                    {"type":"heading","text":"H"},
                    {"type":"quote","text":"Q","attribution":"Someone"},
                    {"type":"bullets","items":["one","two"]},
                    {"type":"image","url":"https://example.com/a.png","caption":"Cap"}
                ]""",
            ),
        )

        assertEquals(
            listOf(
                PostBlock.Paragraph("P"),
                PostBlock.Heading("H"),
                PostBlock.Quote("Q", "Someone"),
                PostBlock.Bullets(listOf("one", "two")),
                PostBlock.Image("https://example.com/a.png", "Cap"),
            ),
            OwnerPosts.published(raw, now).single().blocks,
        )
    }

    @Test
    fun `skips a block type this build does not know, keeping the rest`() {
        // Posts are fetched at runtime, so an older build can be handed a block
        // type it predates. It must render the rest of the post regardless.
        val raw = document(
            post(
                extra = ""","blocks":[
                    {"type":"paragraph","text":"Before"},
                    {"type":"interpretive-dance","text":"???"},
                    {"type":"paragraph","text":"After"}
                ]""",
            ),
        )

        assertEquals(
            listOf(PostBlock.Paragraph("Before"), PostBlock.Paragraph("After")),
            OwnerPosts.published(raw, now).single().blocks,
        )
    }

    @Test
    fun `refuses an image that is not served over https`() {
        val raw = document(
            post(
                extra = ""","blocks":[
                    {"type":"image","url":"http://example.com/insecure.png"},
                    {"type":"image","url":"https://example.com/fine.png"}
                ]""",
            ),
        )

        assertEquals(
            listOf(PostBlock.Image("https://example.com/fine.png", null)),
            OwnerPosts.published(raw, now).single().blocks,
        )
    }

    @Test
    fun `refuses a post image that is not served over https`() {
        val raw = document(post(extra = ""","imageUrl":"http://example.com/x.png""""))
        assertNull(OwnerPosts.published(raw, now).single().imageUrl)
    }

    @Test
    fun `drops empty blocks rather than rendering blank space`() {
        val raw = document(
            post(
                extra = ""","blocks":[
                    {"type":"paragraph","text":"   "},
                    {"type":"bullets","items":["","  "]},
                    {"type":"paragraph","text":"Kept"}
                ]""",
            ),
        )

        assertEquals(
            listOf(PostBlock.Paragraph("Kept")),
            OwnerPosts.published(raw, now).single().blocks,
        )
    }

    @Test
    fun `an unknown category falls back rather than dropping the post`() {
        val raw = document(post(extra = ""","category":"ASTROLOGY""""))
        assertEquals(NewsCategory.ALL, OwnerPosts.published(raw, now).single().category)
    }

    @Test
    fun `category matching ignores case`() {
        val raw = document(post(extra = ""","category":"research""""))
        assertEquals(NewsCategory.RESEARCH, OwnerPosts.published(raw, now).single().category)
    }

    @Test
    fun `blocks survive the round trip through the on-device cache`() {
        val blocks = listOf(
            PostBlock.Paragraph("P"),
            PostBlock.Heading("H"),
            PostBlock.Quote("Q", "Someone"),
            PostBlock.Quote("No attribution", null),
            PostBlock.Bullets(listOf("one", "two")),
            PostBlock.Image("https://example.com/a.png", "Cap"),
            PostBlock.Image("https://example.com/b.png", null),
            PostBlock.Link("Primary source", "https://example.com/source"),
        )

        assertEquals(blocks, OwnerPosts.decodeBlocks(OwnerPosts.encodeBlocks(blocks)))
    }

    @Test
    fun `encoding no blocks decodes back to none`() {
        assertEquals(emptyList<PostBlock>(), OwnerPosts.decodeBlocks(OwnerPosts.encodeBlocks(emptyList())))
    }

    @Test
    fun `source links reject plaintext urls and empty labels`() {
        val raw = document(post(extra = ""","blocks":[
            {"type":"link","text":"Insecure","url":"http://example.com"},
            {"type":"link","text":" ","url":"https://example.com"},
            {"type":"link","text":"Primary source","url":"https://example.com/source"}
        ]"""))
        assertEquals(listOf(PostBlock.Link("Primary source", "https://example.com/source")),
            OwnerPosts.published(raw, now).single().blocks)
    }
}
