package com.caceras.aibrief.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OwnerPostsValidatorTest {
    @Test
    fun sendsStoredEtagWhileDocumentIsCached() {
        assertEquals("\"abc\"", OwnerPostsValidator.ifNoneMatch("\"abc\"", "{\"posts\":[]}"))
        assertEquals("W/\"abc\"", OwnerPostsValidator.ifNoneMatch(" W/\"abc\" ", "{}"))
    }

    @Test
    fun fetchesInFullWithoutACachedDocument() {
        assertNull(OwnerPostsValidator.ifNoneMatch("\"abc\"", null))
        assertNull(OwnerPostsValidator.ifNoneMatch("\"abc\"", "  "))
    }

    @Test
    fun fetchesInFullWithoutAnEtag() {
        assertNull(OwnerPostsValidator.ifNoneMatch(null, "{}"))
        assertNull(OwnerPostsValidator.ifNoneMatch("", "{}"))
    }
}
