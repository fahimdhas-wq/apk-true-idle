package com.aistudio.trueidle.core

import java.util.Locale

/**
 * Compatibility facade over [BigNumberFormatter].
 *
 * The whole game — screens, logs, offline summaries, tooltips — calls `NumberFormatter.short(...)`.
 * Keeping those call sites working is the point of this object: it owns *no* formatting rules of its
 * own, it only forwards to the canonical engine so there is exactly one place that decides notation,
 * precision, grouping and suffixes.
 *
 * Durations ([duration]) and percentages ([percent]) deliberately live here and are **not** routed
 * through the large-number engine: a timer is not an idle-game quantity, and formatting "3m 12s" or
 * "35%" through a suffix engine would be wrong. They still contain no notation state.
 */
object NumberFormatter {

    /** Maximum cached formatting results; owned by [BigNumberFormatter]. */
    const val CACHE_CAPACITY: Int = BigNumberFormatter.CACHE_CAPACITY

    /** Version of the active notation rules. */
    const val CONFIG_VERSION: Int = BigNumberFormatter.CONFIG_VERSION

    /** Placeholder used for values the numeric model cannot represent (NaN / infinity). */
    const val NOT_AVAILABLE: String = BigNumberFormatter.NOT_AVAILABLE

    /** Formats [x] with the active notation (see [BigNumberFormatter.config]). */
    fun short(x: Long): String = BigNumberFormatter.format(x)

    /** Formats [x] with the active notation; non-finite values render as an em dash. */
    fun short(x: Double): String = BigNumberFormatter.format(x)

    /** Snapshot of the shared format cache. Allocates; use from tests and diagnostics only. */
    fun cacheStats(): BigNumberFormatter.CacheStats = BigNumberFormatter.cacheStats()

    /** Drops cached formatting results. */
    fun clearCache(): Unit = BigNumberFormatter.clearCache()

    /**
     * Compact duration: `42s`, `3m 12s`, `2h 05m`, `1d 04h`.
     *
     * Not a large-number quantity: it never uses suffixes or grouping, so it stays independent of the
     * notation settings.
     */
    fun duration(seconds: Long): String {
        if (seconds < 0) return "0s"
        val d = seconds / 86400
        val h = (seconds % 86400) / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            d > 0 -> "${d}d ${String.format(Locale.ROOT, "%02d", h)}h"
            h > 0 -> "${h}h ${String.format(Locale.ROOT, "%02d", m)}m"
            m > 0 -> "${m}m ${String.format(Locale.ROOT, "%02d", s)}s"
            else -> "${s}s"
        }
    }

    /** Whole-percent rendering (`0.35` -> `35%`); never suffixed, never grouped. */
    fun percent(r: Double): String {
        if (r.isNaN() || r.isInfinite()) return NOT_AVAILABLE
        return String.format(Locale.ROOT, "%.0f%%", r * 100.0)
    }
}
