package com.caceras.aibrief.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The update manifest is the only thing standing between the app and
 * downloading an arbitrary APK, so its parsing and validation are covered
 * directly rather than through the UI.
 */
class UpdateManifestParserTest {

    @Test
    fun `parses a complete manifest`() {
        val manifest = UpdateManifestParser.parse(
            """
            {
              "versionCode": 118,
              "versionName": "2.1.0",
              "buildNumber": 18,
              "apkUrl": "https://github.com/Caceras/ai-news-kotlin/releases/download/v118/ai-brief.apk",
              "notes": "Quieter category row"
            }
            """.trimIndent(),
        )

        assertNotNull(manifest)
        requireNotNull(manifest)
        assertEquals(118, manifest.versionCode)
        assertEquals("2.1.0", manifest.versionName)
        assertEquals(18, manifest.buildNumber)
        assertEquals("Quieter category row", manifest.notes)
        assertEquals("2.1.0 (18)", manifest.displayVersion)
    }

    @Test
    fun `rejects a non-https apk url so the app cannot be fed an insecure download`() {
        assertNull(
            UpdateManifestParser.parse(
                """{"versionCode": 118, "apkUrl": "http://example.com/ai-brief.apk"}""",
            ),
        )
    }

    @Test
    fun `rejects a manifest with no apk url`() {
        assertNull(UpdateManifestParser.parse("""{"versionCode": 118}"""))
    }

    @Test
    fun `rejects a manifest with no usable version code`() {
        assertNull(
            UpdateManifestParser.parse("""{"apkUrl": "https://example.com/ai-brief.apk"}"""),
        )
    }

    @Test
    fun `rejects malformed json rather than throwing`() {
        assertNull(UpdateManifestParser.parse("not json at all"))
        assertNull(UpdateManifestParser.parse(""))
    }

    @Test
    fun `falls back to placeholders when optional fields are absent`() {
        val manifest = UpdateManifestParser.parse(
            """{"versionCode": 101, "apkUrl": "https://example.com/ai-brief.apk"}""",
        )

        requireNotNull(manifest)
        assertEquals("unknown", manifest.versionName)
        assertEquals(0, manifest.buildNumber)
        assertEquals("", manifest.notes)
    }

    @Test
    fun `offers an update only when the published build is strictly newer`() {
        val manifest = manifest(versionCode = 110)

        assertTrue(manifest.isNewerThan(109))
        assertFalse("an identical build must not be offered again", manifest.isNewerThan(110))
        assertFalse("Android cannot install a downgrade", manifest.isNewerThan(111))
    }

    private fun manifest(versionCode: Int) = UpdateManifest(
        versionCode = versionCode,
        versionName = "2.1.0",
        buildNumber = versionCode - 100,
        apkUrl = "https://example.com/ai-brief.apk",
        notes = "",
    )
}
