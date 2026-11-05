package com.caceras.aibrief

import com.caceras.aibrief.data.NewsCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoredCategoryTest {
    @Test fun restoresEverySavedTopic() {
        NewsCategory.entries.forEach { assertEquals(it, restoredCategory(it.name)) }
    }

    @Test fun fallsBackToAllWhenNothingOrSomethingUnknownWasSaved() {
        assertEquals(NewsCategory.ALL, restoredCategory(null))
        assertEquals(NewsCategory.ALL, restoredCategory("RETIRED_TOPIC"))
    }
}
