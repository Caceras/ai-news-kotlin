package com.caceras.aibrief

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/** A story the reader left for another part of its series, and where they were in it. */
data class TrailStop(val id: String, val index: Int = 0, val offset: Int = 0)

/**
 * Stories opened from inside a story, oldest first. Back walks this trail
 * before it returns to the front page, landing where the reader left off.
 */
@JvmInline
value class ReaderTrail(val stops: List<TrailStop> = emptyList()) {
    /** Leaves [stop] for another story. */
    fun push(stop: TrailStop) = ReaderTrail(stops + stop)

    /** The story to return to, or null for the front page, and the trail left after it. */
    fun pop(): Pair<TrailStop?, ReaderTrail> = stops.lastOrNull() to ReaderTrail(stops.dropLast(1))

    companion object {
        val Saver: Saver<ReaderTrail, Any> = listSaver(
            save = { trail -> trail.stops.flatMap { listOf(it.id, it.index, it.offset) } },
            restore = { flat ->
                ReaderTrail(flat.chunked(3).filter { it.size == 3 }.mapNotNull { (id, index, offset) ->
                    if (id is String && index is Int && offset is Int) TrailStop(id, index, offset) else null
                })
            },
        )
    }
}
