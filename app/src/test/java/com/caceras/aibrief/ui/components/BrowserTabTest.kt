package com.caceras.aibrief.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserTabTest {
    @Test
    fun webPagesOpenInATab() {
        assertTrue(BrowserTab.opensInTab("https://github.com/Caceras/ai-news-kotlin/blob/main/PRIVACY.md"))
        assertTrue(BrowserTab.opensInTab("HTTPS://example.org/"))
        assertTrue(BrowserTab.opensInTab("http://example.org/"))
    }

    @Test
    fun otherLinksGoToTheAppThatOwnsThem() {
        assertFalse(BrowserTab.opensInTab("mailto:editor@example.org"))
        assertFalse(BrowserTab.opensInTab("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(BrowserTab.opensInTab(""))
    }
}
