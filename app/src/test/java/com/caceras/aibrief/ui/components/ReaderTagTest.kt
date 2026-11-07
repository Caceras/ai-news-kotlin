package com.caceras.aibrief.ui.components

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.StoryFormat
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderTagTest {
    private fun story(format: StoryFormat, category: NewsCategory) = NewsArticle(
        id = "a", title = "T", source = "AI Brief", url = "", publishedAt = Instant.EPOCH,
        summary = "S", category = category, format = format,
    )

    @Test fun breakingNamesOnlyTheTopicBecauseTheBandSaysBreaking() {
        assertEquals("POLICY", story(StoryFormat.BREAKING, NewsCategory.POLICY).readerTagText())
    }

    @Test fun breakingWithoutATopicHasNoLabel() {
        assertNull(story(StoryFormat.BREAKING, NewsCategory.ALL).readerTagText())
    }

    @Test fun otherFormatsKeepTheirFullLabel() {
        val explainer = story(StoryFormat.EXPLAINER, NewsCategory.POLICY)
        assertEquals(explainer.tagText(), explainer.readerTagText())
    }
}
