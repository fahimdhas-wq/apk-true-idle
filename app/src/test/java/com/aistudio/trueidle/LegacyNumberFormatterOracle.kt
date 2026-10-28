package com.aistudio.trueidle

import java.util.Locale
import kotlin.math.abs

/**
 * Frozen copy of the formatter that shipped before the performance work.
 *
 * This is a **test oracle only** — it is never referenced from production code. `NumberFormatterTest`
 * compares the optimised implementation against this byte for byte over boundary data and a large
 * randomised sample, so "we kept the notation" is a measurement rather than a claim.
 */
object LegacyNumberFormatterOracle {

    fun short(x: Long): String = short(x.toDouble())

    fun short(x: Double): String {
        if (x.isNaN() || x.isInfinite()) return "—"
        val absVal = abs(x)
        val sign = if (x < 0) "-" else ""
        return when {
            absVal < 1000.0 -> {
                if (absVal == absVal.toLong().toDouble()) "${sign}${absVal.toLong()}"
                else String.format(Locale.ROOT, "%s%.1f", sign, absVal)
            }
            absVal < 1_000_000.0 -> {
                val v = absVal / 1000.0
                if (v >= 999.95) String.format(Locale.ROOT, "%s1.00M", sign)
                else if (v < 10.0) String.format(Locale.ROOT, "%s%.2fK", sign, v)
                else if (v < 100.0) String.format(Locale.ROOT, "%s%.1fK", sign, v)
                else String.format(Locale.ROOT, "%s%.0fK", sign, v)
            }
            absVal < 1_000_000_000.0 -> {
                val v = absVal / 1_000_000.0
                if (v >= 999.95) String.format(Locale.ROOT, "%s1.00B", sign)
                else if (v < 10.0) String.format(Locale.ROOT, "%s%.2fM", sign, v)
                else if (v < 100.0) String.format(Locale.ROOT, "%s%.1fM", sign, v)
                else String.format(Locale.ROOT, "%s%.0fM", sign, v)
            }
            absVal < 1_000_000_000_000.0 -> {
                val v = absVal / 1_000_000_000.0
                if (v >= 999.95) String.format(Locale.ROOT, "%s1.00T", sign)
                else if (v < 10.0) String.format(Locale.ROOT, "%s%.2fB", sign, v)
                else if (v < 100.0) String.format(Locale.ROOT, "%s%.1fB", sign, v)
                else String.format(Locale.ROOT, "%s%.0fB", sign, v)
            }
            absVal < 1_000_000_000_000_000.0 -> {
                val v = absVal / 1_000_000_000_000.0
                if (v < 10.0) String.format(Locale.ROOT, "%s%.2fT", sign, v)
                else if (v < 100.0) String.format(Locale.ROOT, "%s%.1fT", sign, v)
                else String.format(Locale.ROOT, "%s%.0fT", sign, v)
            }
            else -> String.format(Locale.ROOT, "%s%.2e", sign, absVal)
        }
    }
}
