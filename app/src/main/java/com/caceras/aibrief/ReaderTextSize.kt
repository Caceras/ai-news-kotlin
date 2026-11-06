package com.caceras.aibrief

import com.caceras.aibrief.ui.theme.ReaderScale

/**
 * The story text size the reader picked with the Aa button, kept on the device.
 *
 * It scales story text on top of the system font size, so a reader who
 * wants larger stories does not have to enlarge every app on the phone.
 */
enum class ReaderTextSize(val scale: Float, val label: String) {
    STANDARD(ReaderScale.standard, "Standard"),
    LARGE(ReaderScale.large, "Large"),
    LARGEST(ReaderScale.largest, "Largest");

    /** The size the Aa button moves to: larger each tap, then back to standard. */
    fun next(): ReaderTextSize = entries[(ordinal + 1) % entries.size]

    companion object {
        /** A stored name, or [STANDARD] when nothing (or something unknown) is stored. */
        fun decode(raw: String?): ReaderTextSize = entries.firstOrNull { it.name == raw } ?: STANDARD
    }
}
