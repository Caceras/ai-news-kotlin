package com.caceras.aibrief.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.caceras.aibrief.ui.theme.Brief

/**
 * Opens web links in a browser tab that sits over AI Brief (a Custom Tab), so
 * Back from a source returns to the story instead of leaving the reader in a
 * separate browser. Speaks the Custom Tabs protocol through intent extras
 * rather than the androidx.browser library, so the app stays lean. A browser
 * without Custom Tabs ignores the extras and opens the page as before, and a
 * verified app link (a publisher's own app) still opens that app.
 */
internal object BrowserTab {
    const val EXTRA_SESSION = "android.support.customtabs.extra.SESSION"
    const val EXTRA_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR"
    const val EXTRA_TITLE_VISIBILITY = "android.support.customtabs.extra.TITLE_VISIBILITY"
    const val SHOW_PAGE_TITLE = 1

    /** Only web pages open in a tab; anything else is handed to the app that owns it. */
    fun opensInTab(url: String): Boolean =
        url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
}

/** Opens [url], in a browser tab coloured [toolbarColor] when it is a web page; says so when nothing can. */
fun Context.openLink(url: String, toolbarColor: Int) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    if (BrowserTab.opensInTab(url)) {
        intent.putExtras(Bundle().apply { putBinder(BrowserTab.EXTRA_SESSION, null) })
        intent.putExtra(BrowserTab.EXTRA_TOOLBAR_COLOR, toolbarColor)
        intent.putExtra(BrowserTab.EXTRA_TITLE_VISIBILITY, BrowserTab.SHOW_PAGE_TITLE)
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No browser is available to open this link.", Toast.LENGTH_LONG).show()
    }
}

/** Opens links in a browser tab with the masthead's colour as its toolbar. */
@Composable
fun rememberLinkOpener(): (String) -> Unit {
    val context = LocalContext.current
    val toolbar = Brief.colors.masthead.toArgb()
    return remember(context, toolbar) { { url: String -> context.openLink(url, toolbar) } }
}
