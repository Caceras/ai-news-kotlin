package com.caceras.aibrief

import androidx.activity.BackEventCompat
import com.caceras.aibrief.ui.theme.Motion

/**
 * How the open story yields to a back swipe before the reader lets go, so
 * the gesture shows where Back leads (Android 13+ predictive back).
 */
object BackPreview {
    /** The story's scale at [progress] (0 to 1) of the swipe. */
    fun scale(progress: Float): Float = 1f - (1f - Motion.backPreviewScale) * progress.coerceIn(0f, 1f)

    /** Which way the story slides: away from the edge the swipe started at, not at all for a button. */
    fun direction(swipeEdge: Int): Float = when (swipeEdge) {
        BackEventCompat.EDGE_LEFT -> 1f
        BackEventCompat.EDGE_RIGHT -> -1f
        else -> 0f
    }
}
