package com.caceras.aibrief

/**
 * The stories this reader has opened, kept only on the device.
 *
 * The front page sets their headlines in muted ink, and Up Next offers an
 * unread story before one already read. Oldest first; only the latest
 * [MAX_READ] are kept, which is many editions' worth.
 */
object ReadHistory {
    const val MAX_READ = 300

    /** [ids] with [id] added as the latest, or [ids] itself when it is already there. */
    fun marked(ids: Set<String>, id: String): Set<String> =
        if (id in ids) ids else LinkedHashSet((ids.toList() + id).takeLast(MAX_READ))

    fun encode(ids: Set<String>): String = ids.toList().takeLast(MAX_READ).joinToString("\n")

    fun decode(raw: String?): Set<String> =
        LinkedHashSet(raw.orEmpty().lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
            .takeLast(MAX_READ))
}
