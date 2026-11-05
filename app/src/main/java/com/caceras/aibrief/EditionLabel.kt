package com.caceras.aibrief

import com.caceras.aibrief.data.FeedFreshness
import java.time.Duration
import java.time.Instant

/**
 * The short label beside the edition date. A copy kept on the device says how
 * old it is, so a reader offline on a train knows whether they are reading this
 * morning's edition or last week's. A timestamp in the future, such as after
 * the device clock was moved back, falls back to the plain label.
 */
internal fun editionLabel(freshness: FeedFreshness, lastUpdated: Instant?, now: Instant): String =
    when (freshness) {
        FeedFreshness.LIVE -> "Latest edition"
        FeedFreshness.OFFLINE -> "Offline edition"
        FeedFreshness.CACHED -> lastUpdated?.let { updatedAgo(Duration.between(it, now)) } ?: "Cached edition"
    }

private fun updatedAgo(age: Duration): String? {
    if (age.isNegative) return null
    val minutes = age.toMinutes()
    val hours = age.toHours()
    val days = age.toDays()
    return when {
        minutes < 1 -> "Updated just now"
        minutes < 60 -> "Updated $minutes min ago"
        hours < 24 -> "Updated $hours h ago"
        days == 1L -> "Updated yesterday"
        else -> "Updated $days days ago"
    }
}
