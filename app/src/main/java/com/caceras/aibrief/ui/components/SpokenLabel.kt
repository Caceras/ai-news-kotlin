package com.caceras.aibrief.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * What TalkBack reads for a link or card whose visible text carries a
 * decorative arrow (→ ← ↗). Without it the reader hears "right arrow" or
 * "north east arrow" after every label.
 */
fun Modifier.spokenAs(label: String): Modifier = semantics { contentDescription = label }
