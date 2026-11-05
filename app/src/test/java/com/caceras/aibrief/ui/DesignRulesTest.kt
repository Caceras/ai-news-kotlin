package com.caceras.aibrief.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Enforces the design system (docs/DESIGN_SYSTEM.md) on every Kotlin file.
 *
 * Colours, fonts, type sizes, weights, corner shapes and spacing are defined
 * once in ui/theme/Theme.kt. Anywhere else, a raw value fails the build, so
 * the app cannot drift one screen at a time.
 */
class DesignRulesTest {
    private val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }

    private val sources: List<File> = sourceRoot.walkTopDown()
        .filter { it.isFile && it.extension == "kt" && !it.invariantSeparatorsPath.contains("/ui/theme/") }
        .toList()

    private data class Rule(val name: String, val pattern: Regex, val fix: String)

    private val rules = listOf(
        Rule("raw colour", Regex("""\bColor\(\s*0x"""), "use a role from Brief.colors"),
        Rule("raw type size", Regex("""\d+(\.\d+)?\.sp\b"""), "use a style from Brief.type"),
        Rule("font weight override", Regex("""\bFontWeight\."""), "use a style from Brief.type"),
        Rule("font family", Regex("""\b(FontFamily|Font)\("""), "fonts live in the theme"),
        Rule("Material typography", Regex("""MaterialTheme\.typography"""), "use Brief.type"),
        Rule("Material colours", Regex("""MaterialTheme\.colorScheme"""), "use Brief.colors"),
        Rule("ad-hoc corner shape", Regex("""\b(RoundedCornerShape|CutCornerShape)\("""), "use BriefShapes"),
        Rule("raw padding", Regex("""\bpadding\([^)]*\d+(\.\d+)?\.dp"""), "use a step from Space"),
        Rule("raw gap", Regex("""\bspacedBy\(\s*\d+(\.\d+)?\.dp"""), "use a step from Space"),
        Rule("raw spacer", Regex("""Spacer\(Modifier\.(height|width)\(\s*\d+(\.\d+)?\.dp"""), "use a step from Space"),
    )

    /** File name to rule names it may break, each with a reason. Keep this empty if you can. */
    private val allowed = emptyMap<String, Set<String>>()

    @Test
    fun theSourceTreeIsChecked() {
        assertTrue("No Kotlin sources found under $sourceRoot", sources.size > 5)
    }

    @Test
    fun screensUseOnlyDesignTokens() {
        val violations = sources.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if (line.trimStart().startsWith("//") || line.trimStart().startsWith("*")) return@mapIndexedNotNull null
                rules.firstOrNull { it.pattern.containsMatchIn(line) && it.name !in allowed[file.name].orEmpty() }
                    ?.let { "${file.name}:${index + 1}: ${it.name} — ${it.fix}\n    ${line.trim()}" }
            }
        }
        assertTrue("Design rules broken (see docs/DESIGN_SYSTEM.md):\n" + violations.joinToString("\n"),
            violations.isEmpty())
    }
}
