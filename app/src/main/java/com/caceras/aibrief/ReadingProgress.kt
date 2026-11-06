package com.caceras.aibrief

/**
 * How far through a story's text the reader is, from 0 to 1.
 *
 * [proseOffset] is where the text's top sits relative to the top of the
 * viewport (positive while the header is still on screen), [proseSize] its
 * height and [viewport] the visible height, all in pixels. The bar starts at
 * 0 while the text's top is on screen and fills when the text's last line
 * reaches the bottom of the screen; a story shorter than the screen counts as
 * read once it is all in view.
 */
internal fun readingProgress(proseOffset: Int, proseSize: Int, viewport: Int): Float {
    val scrollable = proseSize - viewport
    if (scrollable <= 0) return if (proseOffset + proseSize <= viewport) 1f else 0f
    return (-proseOffset / scrollable.toFloat()).coerceIn(0f, 1f)
}
