package com.caceras.aibrief.data

import java.time.Instant
import java.time.LocalDate
import org.json.JSONObject

/** One row of the model leaderboard. */
data class RankedModel(
    val rank: Int,
    val name: String,
    val maker: String,
    val score: Double,
    val openWeights: Boolean,
)

/** The top models by a published index, always shown with its source. */
data class ModelRankings(
    val title: String,
    val metric: String,
    val source: String,
    val sourceUrl: String,
    val checkedAt: Instant?,
    val entries: List<RankedModel>,
)

/** A newly listed model, or a stealth one whose maker has not been announced. */
data class RadarModel(
    val name: String,
    val maker: String,
    val listedOn: LocalDate?,
    val note: String,
    val url: String,
    val stealth: Boolean,
)

data class ModelRadar(
    val title: String,
    val source: String,
    val sourceUrl: String,
    val checkedAt: Instant?,
    val entries: List<RadarModel>,
)

/**
 * The front page's data modules. They travel inside the edition document
 * (`content/posts.json`, under `extras`), so they are cached, validated and
 * fetched with the stories in one request, and an older build that does not
 * know them simply ignores them.
 *
 * Each module is optional and parsed defensively: a malformed module is
 * dropped without touching the stories.
 */
data class EditionExtras(
    val rankings: ModelRankings? = null,
    val radar: ModelRadar? = null,
) {
    companion object {
        val EMPTY = EditionExtras()
        private const val MAX_ENTRIES = 20
        private const val MAX_TEXT = 200

        fun parse(raw: String?): EditionExtras {
            val extras = raw?.let { runCatching { JSONObject(it) }.getOrNull() }?.optJSONObject("extras")
                ?: return EMPTY
            return EditionExtras(
                rankings = runCatching { rankings(extras.optJSONObject("rankings")) }.getOrNull(),
                radar = runCatching { radar(extras.optJSONObject("radar")) }.getOrNull(),
            )
        }

        private fun rankings(json: JSONObject?): ModelRankings? {
            json ?: return null
            val rows = json.optJSONArray("entries") ?: return null
            val entries = (0 until minOf(rows.length(), MAX_ENTRIES)).mapNotNull { i ->
                val row = rows.optJSONObject(i) ?: return@mapNotNull null
                val score = row.optDouble("score").takeIf { it.isFinite() } ?: return@mapNotNull null
                RankedModel(
                    rank = row.optInt("rank", i + 1),
                    name = row.text("name") ?: return@mapNotNull null,
                    maker = row.text("maker").orEmpty(),
                    score = score,
                    openWeights = row.optBoolean("openWeights", false),
                )
            }.sortedBy { it.rank }
            if (entries.isEmpty()) return null
            return ModelRankings(
                title = json.text("title") ?: "Top AI models",
                metric = json.text("metric") ?: return null,
                source = json.text("source") ?: return null,
                sourceUrl = json.https("sourceUrl") ?: return null,
                checkedAt = json.instant("checkedAt"),
                entries = entries,
            )
        }

        private fun radar(json: JSONObject?): ModelRadar? {
            json ?: return null
            val rows = json.optJSONArray("entries") ?: return null
            val entries = (0 until minOf(rows.length(), MAX_ENTRIES)).mapNotNull { i ->
                val row = rows.optJSONObject(i) ?: return@mapNotNull null
                RadarModel(
                    name = row.text("name") ?: return@mapNotNull null,
                    maker = row.text("maker").orEmpty(),
                    listedOn = row.text("listedAt")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    note = row.text("note").orEmpty(),
                    url = row.https("url") ?: return@mapNotNull null,
                    stealth = row.optBoolean("stealth", false),
                )
            }
            if (entries.isEmpty()) return null
            return ModelRadar(
                title = json.text("title") ?: "On the radar",
                source = json.text("source") ?: return null,
                sourceUrl = json.https("sourceUrl") ?: return null,
                checkedAt = json.instant("checkedAt"),
                entries = entries,
            )
        }

        private fun JSONObject.text(key: String): String? =
            optString(key).trim().takeIf { it.isNotEmpty() && it != "null" && it.length <= MAX_TEXT }

        private fun JSONObject.https(key: String): String? = text(key)?.takeIf { it.startsWith("https://") }

        private fun JSONObject.instant(key: String): Instant? =
            text(key)?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }
}
