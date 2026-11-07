package com.caceras.aibrief

import com.caceras.aibrief.data.NewsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmptyFeedTest {
    @Test fun searchAsksForOtherWords() {
        val copy = emptyFeedCopy("quantum", NewsCategory.ALL)
        assertEquals("No stories found.", copy.title)
        assertFalse(copy.showAllTopics)
    }

    @Test fun emptyTopicOffersAllTopics() {
        val copy = emptyFeedCopy("", NewsCategory.POLICY)
        assertEquals("No Policy stories yet.", copy.title)
        assertTrue(copy.showAllTopics)
    }

    @Test fun emptyEditionDoesNotMentionTopicsOrSearch() {
        val copy = emptyFeedCopy(" ", NewsCategory.ALL)
        assertFalse(copy.showAllTopics)
        assertFalse(copy.hint.contains("topic") || copy.hint.contains("search"))
    }
}
