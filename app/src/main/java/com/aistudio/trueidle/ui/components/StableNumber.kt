package com.aistudio.trueidle.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aistudio.trueidle.core.StableNumberText

/**
 * Formats [value] once per distinct value for the lifetime of this composition slot.
 *
 * Recomposition caused by an unrelated field (a combat tick, a log line) reuses the memoised string
 * instead of re-running the formatter, and an unchanged number never reaches `NumberFormatter` at
 * all. The holder is plain Kotlin with no view or context reference, so it cannot leak the screen.
 */
@Composable
fun rememberStableShort(value: Long): String {
    val holder = remember { StableNumberText() }
    return holder.text(value)
}

/** [rememberStableShort] for `Double`-backed values such as DPS and rates. */
@Composable
fun rememberStableShort(value: Double): String {
    val holder = remember { StableNumberText() }
    return holder.text(value)
}
