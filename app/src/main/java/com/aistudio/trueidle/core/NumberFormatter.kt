package com.aistudio.trueidle.core

import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.LongAdder
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * The single canonical number formatter for gameplay values.
 *
 * ## Separation of concerns
 *
 * Gameplay stores numbers as `Long` (currencies, XP, damage totals) or `Double` (HP, DPS, stats).
 * This object only turns a value into display text; it never parses text back into a number and
 * never feeds a formatted string into arithmetic. Combat, rewards, offline progression and the save
 * format are untouched by anything here.
 *
 * ## Notation (unchanged)
 *
 * The established notation is preserved exactly, including the carry rule at `999.95`:
 *
 * | magnitude      | example output |
 * |----------------|----------------|
 * | `< 1,000`      | `999`, `12.3`  |
 * | `1e3 .. 1e6`   | `1.23K`        |
 * | `1e6 .. 1e9`   | `1.23M`        |
 * | `1e9 .. 1e12`  | `1.23B`        |
 * | `1e12 .. 1e15` | `1.23T`        |
 * | `>= 1e15`      | `1.23e+15`     |
 *
 * Decimals are picked per band (`< 10` -> 2, `< 100` -> 1, otherwise 0) and the `K`/`M`/`B` bands
 * promote to the next suffix once the scaled value reaches `999.95`. Values that are not finite
 * render as an em dash. Every one of those rules is locked down by `NumberFormatterTest`, which
 * compares this implementation against a frozen copy of the previous `String.format` version over
 * boundary data and a large randomised sample.
 *
 * ## Alphabetic notation (opt-in, not used by the shipping UI)
 *
 * [alphabeticShort] renders `A, B, ... Z, AA, AB, ... ZZ, AAA, ...` (bijective base-26). The
 * exponent attached to `A` and the exponent step per suffix are *explicit configuration*
 * ([SuffixMagnitudeConfig]); they are never inferred from the letters. This mode exists so a future
 * progression can adopt it without inventing thresholds at a call site. The shipping game keeps the
 * compact `K/M/B/T` notation, so nothing the player currently sees changes.
 *
 * ## Performance
 *
 * - One canonical path for every caller; no second implementation for "fast" call sites.
 * - Integral values below 1,000 (the majority of displayed numbers) skip floating point entirely.
 * - Other values are rendered from a rounded fixed-point digit string instead of `String.format`,
 *   which avoids format-string parsing, `Formatter` object creation and locale lookups per call.
 * - Rounds through the same double the previous implementation rounded, with an explicit guard that
 *   defers to `String.format` whenever the value sits within double-error distance of a rounding
 *   boundary. That keeps the output identical instead of "almost always" identical.
 * - A fixed-size, lock-free, direct-mapped cache absorbs repeated formatting of the same value. It
 *   is strictly bounded by [CACHE_CAPACITY] and reports hits/misses/evictions through [cacheStats].
 */
object NumberFormatter {

    /** Maximum number of cached formatting results. Fixed: the cache can never grow past this. */
    const val CACHE_CAPACITY: Int = 2048

    /** Bumped when a formatting rule changes, so memoised UI text can invalidate itself. */
    const val CONFIG_VERSION: Int = 2

    // ---------------------------------------------------------------- public API

    /**
     * Formats [x] for display. Values in `(-1000, 1000)` are rendered from the integer directly
     * (exact, no `Double` hop, no cache traffic); everything else goes through [short]'s `Double`
     * path, exactly as the previous implementation did.
     */
    fun short(x: Long): String {
        if (x > -1000L && x < 1000L) return x.toString()
        return short(x.toDouble())
    }

    /** Formats [x] for display using the compact `K/M/B/T` notation described in the class docs. */
    fun short(x: Double): String {
        if (x.isNaN() || x.isInfinite()) return NOT_AVAILABLE

        val key = x.toRawBits()
        val slot = spread(key) and CACHE_MASK
        val cached = cache.get(slot)
        if (cached != null && cached.bits == key) {
            hitCount.increment()
            return cached.text
        }
        missCount.increment()

        val text = formatCompact(x)
        store(slot, key, text)
        return text
    }

    /**
     * Formats [x] with an alphabetic suffix sequence (`A, B, ... Z, AA, ...`). Not used by the
     * shipped UI; see [SuffixMagnitudeConfig] for the magnitude mapping and `NumberFormatterTest`
     * for the boundary tests that pin it down.
     */
    fun alphabeticShort(
        x: Double,
        config: SuffixMagnitudeConfig = SuffixMagnitudeConfig.DEFAULT
    ): String {
        if (x.isNaN() || x.isInfinite()) return NOT_AVAILABLE

        val negative = x < 0
        val magnitude = abs(x)
        val sb = StringBuilder(16)
        if (negative) sb.append('-')

        if (magnitude < 1000.0) {
            if (magnitude == magnitude.toLong().toDouble()) {
                sb.append(magnitude.toLong())
            } else {
                appendRounded(sb, magnitude, 1)
            }
            return sb.toString()
        }

        val magnitudeExponent = floor(log10(magnitude)).toInt()
        val rawIndex = (magnitudeExponent - config.firstSuffixExponent) / config.exponentStep
        var exponent = config.firstSuffixExponent + (if (rawIndex < 0) 0 else rawIndex) * config.exponentStep
        if (exponent < 0) exponent = 0

        // At most a couple of carries are possible, and the loop is bounded so a pathological input
        // can never spin here.
        var attempts = 0
        while (true) {
            if (exponent > MAX_SUFFIX_EXPONENT) return scientific(magnitude, negative)
            val unit = pow10(exponent)
            if (unit.isInfinite()) return scientific(magnitude, negative)
            val scaled = magnitude / unit
            val decimals = decimalsFor(scaled)
            val index = (exponent - config.firstSuffixExponent) / config.exponentStep
            if (index < 0) return scientific(magnitude, negative)
            // Explicit safe failure: no label of a sane length exists, so fall back to the
            // scientific notation the formatter already uses past 1e15.
            val label = suffixLabel(index.toLong(), config.maxSuffixLength)
                ?: return scientific(magnitude, negative)
            val rounded = roundedScaled(scaled, decimals)
            val limit = LIMIT_SCALED[decimals]
            if (rounded >= 0 && rounded >= limit && attempts < 4) {
                // 999.95+ rounds up to the next suffix instead of printing "1000A".
                exponent += config.exponentStep
                attempts++
                continue
            }
            appendRounded(sb, scaled, decimals)
            sb.append(label)
            return sb.toString()
        }
    }

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

    fun percent(r: Double): String {
        if (r.isNaN() || r.isInfinite()) return NOT_AVAILABLE
        return String.format(Locale.ROOT, "%.0f%%", r * 100.0)
    }

    // ------------------------------------------------------------- cache surface

    /** Snapshot of the format cache counters. Allocates; intended for tests and diagnostics. */
    fun cacheStats(): CacheStats = CacheStats(
        hits = hitCount.sum(),
        misses = missCount.sum(),
        evictions = evictionCount.sum(),
        entries = entryCount.get(),
        capacity = CACHE_CAPACITY
    )

    /** Drops every cached result and resets the counters. */
    fun clearCache() {
        for (i in 0 until CACHE_CAPACITY) cache.set(i, null)
        entryCount.set(0)
        hitCount.reset()
        missCount.reset()
        evictionCount.reset()
    }

    data class CacheStats(
        val hits: Long,
        val misses: Long,
        val evictions: Long,
        val entries: Int,
        val capacity: Int
    ) {
        val hitRate: Double get() = if (hits + misses == 0L) 0.0 else hits.toDouble() / (hits + misses)
        val isBounded: Boolean get() = entries <= capacity && capacity == CACHE_CAPACITY
    }

    // -------------------------------------------------------------- configuration

    /**
     * Magnitude mapping for the alphabetic suffix sequence.
     *
     * The suffix letters only say *where in the sequence* a value sits. What each position is worth
     * is declared here, explicitly: suffix index `0` ("A") covers `10^firstSuffixExponent` and each
     * following suffix is `10^exponentStep` times larger.
     *
     * The defaults (`A` = thousands, step 3) match the density of the existing `K/M/B/T` notation:
     * `1.00A` is `1.00K`, `1.00B` (second suffix) is `1.00M`. They are a documented choice, not an
     * inference from the letters and not a claim that the old formatter defined them.
     */
    data class SuffixMagnitudeConfig(
        val firstSuffixExponent: Int = 3,
        val exponentStep: Int = 3,
        val maxSuffixLength: Int = 10
    ) {
        init {
            require(firstSuffixExponent >= 0) { "firstSuffixExponent must be >= 0" }
            require(exponentStep > 0) { "exponentStep must be > 0" }
            require(maxSuffixLength in 1..MAX_SUFFIX_LENGTH_HARD_CAP) {
                "maxSuffixLength must be in 1..$MAX_SUFFIX_LENGTH_HARD_CAP"
            }
        }

        companion object {
            val DEFAULT = SuffixMagnitudeConfig()

            /** Hard ceiling so a bad config cannot ask for a megabyte-long label. */
            const val MAX_SUFFIX_LENGTH_HARD_CAP: Int = 32
        }
    }

    /** Largest exponent the alphabetic notation will attempt before falling back to scientific. */
    const val MAX_SUFFIX_EXPONENT: Int = 308

    /**
     * Bijective base-26 label for a zero-based [index]: `0 -> A`, `25 -> Z`, `26 -> AA`,
     * `51 -> AZ`, `52 -> BA`, `701 -> ZZ`, `702 -> AAA`.
     *
     * Returns `null` when the label would exceed [maxLength] characters, which is the documented
     * safe-failure path for absurd indices (callers fall back to scientific notation). Iterative, no
     * recursion, no lookahead table, no unbounded string collection.
     */
    fun suffixLabel(index: Long, maxLength: Int = 10): String? {
        require(index >= 0) { "suffix index must be >= 0" }
        require(maxLength in 1..SuffixMagnitudeConfig.MAX_SUFFIX_LENGTH_HARD_CAP) {
            "maxLength must be in 1..${SuffixMagnitudeConfig.MAX_SUFFIX_LENGTH_HARD_CAP}"
        }
        // Number of letters needed: the smallest n with 26 + 26^2 + ... + 26^n > index.
        var length = 1
        var span = 26L // number of labels with `length` letters
        var total = span
        while (index >= total) {
            length++
            if (length > maxLength) return null
            if (span > Long.MAX_VALUE / 26L) return null
            span *= 26L
            if (total > Long.MAX_VALUE - span) return null
            total += span
        }
        val chars = CharArray(length)
        var remaining = index
        for (position in length - 1 downTo 0) {
            chars[position] = ('A' + (remaining % 26L).toInt())
            remaining = remaining / 26L - 1L
        }
        return String(chars)
    }

    // -------------------------------------------------------------- implementation

    private const val NOT_AVAILABLE = "—"

    private const val CACHE_MASK = CACHE_CAPACITY - 1

    private val cache = AtomicReferenceArray<Entry>(CACHE_CAPACITY)
    private val entryCount = AtomicInteger(0)
    private val hitCount = LongAdder()
    private val missCount = LongAdder()
    private val evictionCount = LongAdder()

    private class Entry(val bits: Long, val text: String)

    private val FMT_0 = "%.0f"
    private val FMT_1 = "%.1f"
    private val FMT_2 = "%.2f"
    private val FMT_SCI = "%.2e"

    private val POW10 = doubleArrayOf(
        1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15,
        1e16, 1e17, 1e18, 1e19, 1e20, 1e21, 1e22
    )

    /** Scaled value that means "carry into the next suffix" for each decimal count. */
    private val LIMIT_SCALED = longArrayOf(1000L, 10_000L, 100_000L)

    private fun formatCompact(x: Double): String {
        val negative = x < 0
        val magnitude = abs(x)
        val sb = StringBuilder(12)
        if (negative) sb.append('-')

        when {
            magnitude < 1_000.0 -> {
                if (magnitude == magnitude.toLong().toDouble()) {
                    sb.append(magnitude.toLong())
                } else {
                    appendRounded(sb, magnitude, 1)
                }
            }

            magnitude < 1_000_000.0 -> {
                val v = magnitude / 1_000.0
                if (v >= 999.95) sb.append("1.00M") else appendSuffixed(sb, v, 'K')
            }

            magnitude < 1_000_000_000.0 -> {
                val v = magnitude / 1_000_000.0
                if (v >= 999.95) sb.append("1.00B") else appendSuffixed(sb, v, 'M')
            }

            magnitude < 1_000_000_000_000.0 -> {
                val v = magnitude / 1_000_000_000.0
                if (v >= 999.95) sb.append("1.00T") else appendSuffixed(sb, v, 'B')
            }

            magnitude < 1_000_000_000_000_000.0 -> {
                val v = magnitude / 1_000_000_000_000.0
                appendSuffixed(sb, v, 'T')
            }

            else -> sb.append(
                String.format(Locale.ROOT, FMT_SCI, magnitude)
            )
        }
        return sb.toString()
    }

    private fun appendSuffixed(sb: StringBuilder, scaled: Double, suffix: Char) {
        appendRounded(sb, scaled, decimalsFor(scaled))
        sb.append(suffix)
    }

    private fun decimalsFor(scaled: Double): Int = when {
        scaled < 10.0 -> 2
        scaled < 100.0 -> 1
        else -> 0
    }

    /**
     * Appends [value] (non-negative, finite) with [decimals] fractional digits, rounding half-up
     * exactly like `String.format("%.<decimals>f", value)`.
     *
     * The product `value * 10^decimals` is a double, so it carries up to ~1e-16 relative error. If
     * the product lands within `1e-9 * product` of a `.5` boundary, the true value could round
     * either way and the reference formatter decides instead. That threshold is ~7 orders of
     * magnitude above the actual error, so the decision can never be wrong; it just costs a slow
     * path on a rare input (probability ~1e-6).
     */
    private fun appendRounded(sb: StringBuilder, value: Double, decimals: Int) {
        val scaled = roundedScaled(value, decimals)
        if (scaled < 0L) {
            val format = when (decimals) {
                0 -> FMT_0
                1 -> FMT_1
                else -> FMT_2
            }
            sb.append(String.format(Locale.ROOT, format, value))
            return
        }
        appendFixed(sb, scaled, decimals)
    }

    /** Returns `round-half-up(value * 10^decimals)` or `-1` when the reference path must decide. */
    private fun roundedScaled(value: Double, decimals: Int): Long {
        val product = value * when (decimals) {
            0 -> 1.0
            1 -> 10.0
            else -> 100.0
        }
        // Beyond this magnitude a double cannot represent every integer, so fixed-point digits
        // would be invented rather than rounded.
        if (!(product < 9.007199254740992E15)) return -1L
        val whole = floor(product)
        val fraction = product - whole
        if (abs(fraction - 0.5) <= 1.0E-9 * product + 1.0E-12) return -1L
        val base = whole.toLong()
        return if (fraction > 0.5) base + 1L else base
    }

    /** Appends an already-rounded fixed-point value such as `1234` with [decimals] = 2 -> `12.34`. */
    private fun appendFixed(sb: StringBuilder, scaled: Long, decimals: Int) {
        if (decimals == 0) {
            sb.append(scaled)
            return
        }
        val start = sb.length
        sb.append(scaled)
        val digits = sb.length - start
        if (digits <= decimals) {
            repeat(decimals - digits + 1) { sb.insert(start, '0') }
        }
        sb.insert(sb.length - decimals, '.')
    }

    private fun scientific(magnitude: Double, negative: Boolean): String {
        val digits = String.format(Locale.ROOT, FMT_SCI, magnitude)
        return if (negative) "-$digits" else digits
    }

    private fun pow10(exponent: Int): Double =
        if (exponent in 0..22) POW10[exponent] else 10.0.pow(exponent)

    private fun store(slot: Int, bits: Long, text: String) {
        val previous = cache.get(slot)
        if (previous == null) {
            bumpEntryCount()
        } else {
            evictionCount.increment()
        }
        cache.set(slot, Entry(bits, text))
    }

    /** Keeps [entryCount] honest under races: it can never exceed the fixed capacity. */
    private fun bumpEntryCount() {
        while (true) {
            val current = entryCount.get()
            if (current >= CACHE_CAPACITY) return
            if (entryCount.compareAndSet(current, current + 1)) return
        }
    }

    /** Cheap bit spread so consecutive doubles do not cluster in the same slot. */
    private fun spread(bits: Long): Int {
        var x = bits * -0x61c8864680b583ebL
        x = x xor (x ushr 32)
        return x.toInt()
    }
}
