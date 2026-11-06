package com.caceras.aibrief.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.caceras.aibrief.data.CardTheme
import com.caceras.aibrief.ui.theme.BriefColors
import com.caceras.aibrief.ui.theme.DarkBrief
import com.caceras.aibrief.ui.theme.LightBrief
import com.caceras.aibrief.ui.theme.cardPalette
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every text colour must be readable on the surface it is set on: WCAG AA,
 * 4.5:1, the threshold Play's pre-launch accessibility check reports. The
 * pairs below are the ones the screens actually use; add a pair when a screen
 * sets a role as text on a new surface.
 */
class ContrastTest {
    private fun ratio(text: Color, surface: Color): Double {
        val a = text.compositeOver(surface).luminance() + 0.05
        val b = surface.luminance() + 0.05
        return (maxOf(a, b) / minOf(a, b)).toDouble()
    }

    private fun textPairs(c: BriefColors): Map<String, Pair<Color, Color>> = buildMap {
        for ((name, surface) in listOf("paper" to c.paper, "card" to c.card)) {
            put("ink on $name", c.ink to surface)
            put("inkMuted on $name", c.inkMuted to surface)
            put("accent on $name", c.accent to surface)
            put("breakingInk on $name", c.breakingInk to surface)
            put("opinionInk on $name", c.opinionInk to surface)
        }
        put("ink on opinionPaper", c.ink to c.opinionPaper)
        put("inkMuted on opinionPaper", c.inkMuted to c.opinionPaper)
        put("accent on opinionPaper", c.accent to c.opinionPaper)
        put("opinionInk on opinionPaper", c.opinionInk to c.opinionPaper)
        put("onMasthead on masthead", c.onMasthead to c.masthead)
        put("unselected topic on masthead", c.onMasthead.copy(alpha = 0.66f) to c.masthead)
        put("other series part on masthead", c.onMasthead.copy(alpha = 0.6f) to c.masthead)
        put("mastheadAction on masthead", c.mastheadAction to c.masthead)
        put("opinionAccent on masthead", c.opinionAccent to c.masthead)
        put("onMasthead on ribbonLabel", c.onMasthead to c.ribbonLabel)
        put("onAccent on accent", c.onAccent to c.accent)
        put("onBreaking on breaking", c.onBreaking to c.breaking)
        put("breaking ribbon title", c.onMasthead to c.masthead)
        // The scrim over the brightest possible photograph.
        put("onImage on scrim", c.onImage to c.imageScrim.compositeOver(Color.White))
    }

    @Test
    fun textMeetsAaContrastInBothThemes() {
        val failures = listOf("light" to LightBrief, "dark" to DarkBrief).flatMap { (theme, colors) ->
            textPairs(colors).mapNotNull { (name, pair) ->
                val r = ratio(pair.first, pair.second)
                if (r < 4.5) "$theme $name: %.2f:1".format(r) else null
            }
        }
        assertTrue("Text below 4.5:1 contrast:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun designedCardTextMeetsAaContrastAcrossTheGradient() {
        val failures = CardTheme.entries.flatMap { theme ->
            val p = cardPalette(theme)
            listOf("ink" to p.ink, "inkMuted" to p.inkMuted).mapNotNull { (name, ink) ->
                val r = ratio(ink, p.edge)
                if (r < 4.5) "$theme $name on edge: %.2f:1".format(r) else null
            }
        }
        assertTrue("Designed-card text below 4.5:1:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
