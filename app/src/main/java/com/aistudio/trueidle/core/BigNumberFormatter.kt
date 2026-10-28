package com.aistudio.trueidle.core

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.LongAdder

/**
 * The one large-number notation engine for the whole game.
 *
 * ## Why this exists
 *
 * Everything that shows a number goes through here: currencies, HP/DPS, XP, crafting costs, offline
 * summaries, logs, tooltips. [NumberFormatter] is a thin facade over this object so the old call
 * sites keep working, but there is exactly one implementation and one configuration.
 *
 * ## Notation modes
 *
 * | mode | `999` | `1_234` | `1.5e6` | `1e12` | `1e15` |
 * |---|---|---|---|---|---|
 * | [Mode.ALPHABETIC] (default) | `999` | `1,234` | `1,500,000` | `1.00A` | `1.00B` |
 * | [Mode.COMPACT] | `999` | `1.23K` | `1.50M` | `1.00T` | `1.00e+15` |
 * | [Mode.GROUPED] | `999` | `1,234` | `1,500,000` | `1,000,000,000,000` | `1,000,000,000,000,000` |
 * | [Mode.SCIENTIFIC] | `999` | `1,234` | `1.50e+6` | `1.00e+12` | `1.00e+15` |
 *
 * `ALPHABETIC` is the progression requested for this game: grouped digits up to the first suffix,
 * then `A, B, C, ... Z, AA, AB, ... ZZ, AAA, ...`.
 *
 * ## The magnitude convention (explicit, not inferred)
 *
 * Alphabetic letters only say *where in the sequence* a value sits; they do not carry a magnitude.
 * The magnitude is [Config.firstSuffixExponent] (which exponent `A` covers) and
 * [Config.exponentStep] (how many exponents each further suffix adds). The shipped default is
 * `firstSuffixExponent = 12`, `exponentStep = 3`:
 *
 * - `A` covers `[10^12, 10^15)`, `B` covers `[10^15, 10^18)`, … `Z` covers `[10^87, 10^90)`,
 * - `AA` starts at `10^90`, `ZZ` at `10^(12 + 701*3)`, `AAA` right after `ZZ`.
 *
 * Both numbers live in one [Config] value, so switching to "each letter is one power of ten" is
 * `exponentStep = 1` — no formatter rewrite, and [Config.bYName] exposes it to settings.
 *
 * ## Numerical model
 *
 * The authoritative gameplay numbers stay `Long`/`Double` in `GameState` (see the save model); this
 * engine never stores or mutates them. Formatting takes an exact [BigDecimal] view of the input and
 * rounds with an explicit [RoundingMode], so:
 *
 * - no `Float`/`Double` is used as a scratch representation for large values,
 * - values beyond `Long.MAX_VALUE` are supported through [format]`(BigDecimal)` /
 *   [formatInteger]`(BigInteger)`,
 * - `Double` inputs are converted with [BigDecimal.valueOf] (the shortest decimal that round-trips),
 *   which is what the old `String.format("%.Nf", …)` produced, so output stays compatible while
 *   becoming platform- and locale-independent.
 *
 * ## Cost
 *
 * Repeated formatting of the same value is served from a fixed-size, lock-free cache (see
 * [CACHE_CAPACITY]); changing the configuration bumps [epoch] and empties the cache so a stale
 * notation can never be displayed.
 */
object BigNumberFormatter {

    /** Maximum number of cached results. The cache is a fixed array and can never grow past this. */
    const val CACHE_CAPACITY: Int = 2048

    /** Incremented when a rendering rule changes, so memoised UI text invalidates itself. */
    const val CONFIG_VERSION: Int = 3

    /** Placeholder for values the numeric model cannot represent (NaN / infinity). */
    const val NOT_AVAILABLE: String = "—"

    // ------------------------------------------------------------------- modes

    enum class Mode {
        /** Grouped digits below the first suffix, then `A, B, ... ZZ, AAA, ...`. */
        ALPHABETIC,

        /** The original `K/M/B/T` notation, then scientific (`1.23e+15`). */
        COMPACT,

        /** Grouped digits at every magnitude (`1,000,000,000,000`). */
        GROUPED,

        /** Grouped digits for small values, then `1.23e+12`. */
        SCIENTIFIC
    }

    enum class Rounding {
        HALF_UP, DOWN, UP, HALF_EVEN;

        internal fun toJava(): RoundingMode = when (this) {
            HALF_UP -> RoundingMode.HALF_UP
            DOWN -> RoundingMode.DOWN
            UP -> RoundingMode.UP
            HALF_EVEN -> RoundingMode.HALF_EVEN
        }
    }

    /**
     * Notation configuration. One instance describes the complete output format, so nothing about
     * precision, separators or suffix magnitudes is scattered across screens.
     */
    data class Config(
        val mode: Mode = Mode.ALPHABETIC,
        /** Exponent covered by the first suffix (`A`). Default `12` => `1.00A` starts at 10^12. */
        val firstSuffixExponent: Int = 12,
        /** Exponents added by each further suffix. Default `3` => `A`=10^12, `B`=10^15, … */
        val exponentStep: Int = 3,
        /** Alphabet for [Mode.ALPHABETIC]; letters are read positionally. */
        val suffixAlphabet: String = DEFAULT_ALPHABET,
        /** Letters for [Mode.COMPACT], in order starting at [compactFirstExponent]. */
        val compactLetters: String = "KMBT",
        val compactFirstExponent: Int = 3,
        /** Maximum fractional digits for mantissas and small values. */
        val decimals: Int = 2,
        val rounding: Rounding = Rounding.HALF_UP,
        val useGrouping: Boolean = true,
        val groupSeparator: Char = ',',
        val groupSize: Int = 3,
        /** Fractional digits for [Mode.SCIENTIFIC]. */
        val scientificDigits: Int = 2,
        /**
         * Pre-round carry threshold for suffixed modes, or `null` to carry on rounded overflow.
         *
         * `COMPACT` sets `999.95` to reproduce the historical rule exactly (`999.95K` becomes
         * `1.00M`, while `999.9499K` stays `1000K`). Suffixes never carry past their last available
         * label, which is why the old `T` band printed `1000T` instead of promoting.
         */
        val carryThresholdMantissa: Double? = null
    ) {
        init {
            require(firstSuffixExponent >= 0) { "firstSuffixExponent must be >= 0" }
            require(exponentStep > 0) { "exponentStep must be > 0" }
            require(compactFirstExponent >= 0) { "compactFirstExponent must be >= 0" }
            require(suffixAlphabet.length >= 2) { "suffixAlphabet needs at least two symbols" }
            require(suffixAlphabet.toSet().size == suffixAlphabet.length) {
                "suffixAlphabet must not contain duplicates"
            }
            require(compactLetters.isNotEmpty()) { "compactLetters must not be empty" }
            require(decimals in 0..8) { "decimals must be in 0..8" }
            require(scientificDigits in 0..8) { "scientificDigits must be in 0..8" }
            require(groupSize in 1..4) { "groupSize must be in 1..4" }
        }

        companion object {
            const val DEFAULT_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

            /** Shipped default: grouped digits, then alphabetic suffixes from 10^12 in steps of 10^3. */
            val DEFAULT = Config()

            /** The pre-existing `K/M/B/T` notation, kept selectable so nothing is lost. */
            val COMPACT = Config(
                mode = Mode.COMPACT,
                useGrouping = false,
                decimals = 2,
                carryThresholdMantissa = 999.95
            )

            val GROUPED = Config(mode = Mode.GROUPED)
            val SCIENTIFIC = Config(mode = Mode.SCIENTIFIC)

            /** Every mode with default magnitudes, keyed by its stable settings name. */
            fun byName(name: String?): Config = when (name?.trim()?.uppercase()) {
                "COMPACT" -> COMPACT
                "GROUPED" -> GROUPED
                "SCIENTIFIC" -> SCIENTIFIC
                "ALPHABETIC" -> DEFAULT
                else -> DEFAULT
            }

            /** Settings round-trip a [Mode] by name; unknown names fall back to the default. */
            fun modeName(mode: Mode): String = mode.name

            /** One step per power of ten instead of three (alternative convention, same engine). */
            val STEP_ONE: Config = DEFAULT.copy(exponentStep = 1)
        }
    }

    // -------------------------------------------------------------- live config

    private val configRef = AtomicReference(Config.DEFAULT)

    /** Epoch of the active configuration; changes invalidate the cache. */
    private val epoch = AtomicInteger(0)

    /** The configuration every call without an explicit config uses. */
    val config: Config get() = configRef.get()

    /** Applies [newConfig] process-wide and drops cached results rendered under the old one. */
    fun setConfig(newConfig: Config) {
        configRef.set(newConfig)
        epoch.incrementAndGet()
        clearCache()
    }

    /** Convenience for the settings screen: switch notation, keep the magnitude configuration. */
    fun setMode(mode: Mode): Config {
        val next = configRef.get().copy(mode = mode)
        setConfig(next)
        return next
    }

    // ---------------------------------------------------------------- public API

    /** Formats [value] with the active configuration. */
    fun format(value: BigDecimal): String = format(value, configRef.get())

    /** Formats [value] with exactly [decimalPlaces] fractional digits (grouped, no suffix). */
    fun format(value: BigDecimal, decimalPlaces: Int): String {
        require(decimalPlaces in 0..18) { "decimalPlaces must be in 0..18" }
        val cfg = configRef.get()
        return renderFixed(value, cfg, decimalPlaces)
    }

    /** Formats [value] with an explicit configuration. */
    fun format(value: BigDecimal, cfg: Config): String = render(value, cfg, cfg.decimals)

    /** Formats an integer with an explicit configuration (no cache; used by tests and tooling). */
    fun format(value: Long, cfg: Config): String = render(BigDecimal.valueOf(value), cfg, cfg.decimals)

    /** Formats a double with an explicit configuration; non-finite values render as [NOT_AVAILABLE]. */
    fun format(value: Double, cfg: Config): String =
        if (value.isNaN() || value.isInfinite()) NOT_AVAILABLE
        else render(BigDecimal.valueOf(value), cfg, cfg.decimals)

    /** Formats an arbitrary-precision integer using the active configuration's grouping. */
    fun formatInteger(value: BigInteger): String {
        val cfg = configRef.get()
        return render(value.toBigDecimal(), cfg, cfg.decimals)
    }

    /** Formats [value] with an explicit configuration (arbitrary-precision integer). */
    fun formatInteger(value: BigInteger, cfg: Config): String =
        render(value.toBigDecimal(), cfg, cfg.decimals)

    /**
     * Formats a [Long] with the active configuration. Repeated values are served from the cache.
     */
    fun format(value: Long): String {
        if (value > -1_000L && value < 1_000L) {
            // Small integers are exact, need no suffix and no grouping: skip BigDecimal entirely.
            return value.toString()
        }
        val cfg = configRef.get()
        return cached(value, KIND_LONG) { render(BigDecimal.valueOf(value), cfg, cfg.decimals) }
    }

    /** Formats a [Double] with the active configuration. Non-finite values render as [NOT_AVAILABLE]. */
    fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return NOT_AVAILABLE
        val cfg = configRef.get()
        return cached(value.toRawBits(), KIND_DOUBLE) { render(BigDecimal.valueOf(value), cfg, cfg.decimals) }
    }

    /**
     * Bijective base-N label for [index]: `0 -> A`, `25 -> Z`, `26 -> AA`, `51 -> AZ`, `52 -> BA`,
     * `701 -> ZZ`, `702 -> AAA`, and onward without a fixed length limit.
     *
     * Iterative, allocation-light (one `CharArray` + one `String`), deterministic, no lookup table.
     * Returns `null` only for indices so large that a `Long` cannot count the labels (never reached
     * from a real value, since a value's exponent is bounded).
     */
    fun suffix(index: Long, alphabet: String = Config.DEFAULT_ALPHABET): String? {
        require(index >= 0) { "suffix index must be >= 0" }
        val base = alphabet.length
        require(base >= 2) { "alphabet needs at least two symbols" }

        var length = 1
        var span = base.toLong()
        var total = span
        while (index >= total) {
            length++
            if (span > Long.MAX_VALUE / base) return null
            span *= base
            if (total > Long.MAX_VALUE - span) return null
            total += span
        }

        val chars = CharArray(length)
        var remaining = index
        for (position in length - 1 downTo 0) {
            chars[position] = alphabet[(remaining % base).toInt()]
            remaining = remaining / base - 1L
        }
        return String(chars)
    }

    // ------------------------------------------------------------------- cache

    private class Entry(
        @JvmField val key: Long,
        @JvmField val epoch: Int,
        @JvmField val kind: Int,
        @JvmField val text: String
    )

    // A Long value and the raw bits of an unrelated Double can be equal; the kind bit keeps the two
    // key spaces apart so one can never be served the other's text.
    private const val KIND_DOUBLE = 0
    private const val KIND_LONG = 1

    private val cache = AtomicReferenceArray<Entry>(CACHE_CAPACITY)
    private val entryCount = AtomicInteger(0)
    private val hitCount = LongAdder()
    private val missCount = LongAdder()
    private val evictionCount = LongAdder()

    private const val CACHE_MASK = CACHE_CAPACITY - 1

    data class CacheStats(
        val hits: Long,
        val misses: Long,
        val evictions: Long,
        val entries: Int,
        val capacity: Int = CACHE_CAPACITY
    ) {
        val hitRate: Double
            get() = if (hits + misses == 0L) 0.0 else hits.toDouble() / (hits + misses)

        val isBounded: Boolean get() = entries <= capacity
    }

    fun cacheStats(): CacheStats = CacheStats(
        hits = hitCount.sum(),
        misses = missCount.sum(),
        evictions = evictionCount.sum(),
        entries = entryCount.get()
    )

    fun clearCache() {
        for (i in 0 until CACHE_CAPACITY) cache.set(i, null)
        entryCount.set(0)
        hitCount.reset()
        missCount.reset()
        evictionCount.reset()
    }

    private inline fun cached(key: Long, kind: Int, format: () -> String): String {
        val currentEpoch = epoch.get()
        val slot = spread(key) and CACHE_MASK
        val existing = cache.get(slot)
        if (existing != null && existing.key == key && existing.epoch == currentEpoch && existing.kind == kind) {
            hitCount.increment()
            return existing.text
        }
        missCount.increment()
        val text = format()
        if (existing == null) bumpEntryCount() else evictionCount.increment()
        cache.set(slot, Entry(key, currentEpoch, kind, text))
        return text
    }

    private fun bumpEntryCount() {
        while (true) {
            val current = entryCount.get()
            if (current >= CACHE_CAPACITY) return
            if (entryCount.compareAndSet(current, current + 1)) return
        }
    }

    private fun spread(key: Long): Int {
        var x = key * -0x61c8864680b583ebL
        x = x xor (x ushr 32)
        return x.toInt()
    }

    // ------------------------------------------------------------- rendering

    private val THOUSAND = BigDecimal.valueOf(1000L)
    private val TEN = BigDecimal.TEN
    private val HUNDRED = BigDecimal.valueOf(100L)
    private const val MAX_CARRY_STEPS = 4

    private fun render(value: BigDecimal, cfg: Config, maxDecimals: Int): String {
        if (value.signum() == 0) return "0"
        val negative = value.signum() < 0
        val magnitude = value.abs()
        val sb = StringBuilder(20)
        if (negative) sb.append('-')

        when (cfg.mode) {
            Mode.GROUPED -> appendGrouped(sb, magnitude, cfg, maxDecimals)
            Mode.SCIENTIFIC -> appendScientificMode(sb, magnitude, cfg)
            Mode.COMPACT -> appendSuffixed(
                sb = sb,
                magnitude = magnitude,
                cfg = cfg,
                maxDecimals = maxDecimals,
                firstExponent = cfg.compactFirstExponent,
                alphabetic = false
            )
            Mode.ALPHABETIC -> appendSuffixed(
                sb = sb,
                magnitude = magnitude,
                cfg = cfg,
                maxDecimals = maxDecimals,
                firstExponent = cfg.firstSuffixExponent,
                alphabetic = true
            )
        }
        return sb.toString()
    }

    /** `format(value, decimalPlaces)`: always grouped digits with exactly that many decimals. */
    private fun renderFixed(value: BigDecimal, cfg: Config, decimalPlaces: Int): String {
        if (value.signum() == 0) {
            return if (decimalPlaces == 0) "0" else "0." + "0".repeat(decimalPlaces)
        }
        val negative = value.signum() < 0
        val magnitude = value.abs().setScale(decimalPlaces, cfg.rounding.toJava())
        if (magnitude.signum() == 0) {
            return if (decimalPlaces == 0) "0" else "0." + "0".repeat(decimalPlaces)
        }
        val text = magnitude.toPlainString()
        val dot = text.indexOf('.')
        val integer = if (dot >= 0) text.substring(0, dot) else text
        val fraction = if (dot >= 0) text.substring(dot + 1) else ""
        val sb = StringBuilder(integer.length + fraction.length + 6)
        if (negative) sb.append('-')
        appendGroupedDigits(sb, integer, cfg)
        if (decimalPlaces > 0) {
            sb.append('.')
            sb.append(fraction.padEnd(decimalPlaces, '0'))
        }
        return sb.toString()
    }

    /**
     * Values below the first suffix: grouped digits (integers) or a small decimal for magnitudes
     * under one. `COMPACT` keeps the historical one-decimal rendering for those values.
     */
    private fun appendBelowFirstSuffix(
        sb: StringBuilder,
        magnitude: BigDecimal,
        cfg: Config,
        maxDecimals: Int,
        alphabetic: Boolean
    ) {
        if (alphabetic) {
            if (magnitude < BigDecimal.ONE) {
                sb.append(trimTrailingZeros(magnitude.setScale(maxDecimals, cfg.rounding.toJava()).toPlainString()))
            } else {
                appendGrouped(sb, magnitude, cfg, 0)
            }
            return
        }
        // COMPACT keeps the original rule for small values: integer, or exactly one decimal.
        if (magnitude.stripTrailingZeros().scale() <= 0) {
            appendGrouped(sb, magnitude, cfg, 0)
        } else {
            sb.append(magnitude.setScale(1, cfg.rounding.toJava()).toPlainString())
        }
    }

    /**
     * Suffixed rendering shared by `ALPHABETIC` (unbounded bijective letters) and `COMPACT`
     * (`K/M/B/T`, then scientific). Magnitudes and decimals come from [cfg], never from the letters.
     */
    private fun appendSuffixed(
        sb: StringBuilder,
        magnitude: BigDecimal,
        cfg: Config,
        maxDecimals: Int,
        firstExponent: Int,
        alphabetic: Boolean
    ) {
        val exponent = decimalExponent(magnitude)
        if (exponent < firstExponent) {
            appendBelowFirstSuffix(sb, magnitude, cfg, maxDecimals, alphabetic)
            return
        }

        var index = (exponent - firstExponent) / cfg.exponentStep
        if (index < 0) index = 0
        var carry = 0
        while (true) {
            val unitExponent = firstExponent + index * cfg.exponentStep
            val mantissa = magnitude.movePointLeft(unitExponent)
            val decimals = minOf(mantissaDecimals(mantissa), maxDecimals)
            val rounded = mantissa.setScale(decimals, cfg.rounding.toJava())

            // Carry into the next suffix only when that suffix exists: a finite letter list must not
            // invent a label, and the historical `T` band therefore stays "1000T".
            val threshold = cfg.carryThresholdMantissa
            val carryInto = isLabelAvailable(index + 1, cfg, alphabetic) &&
                carry < MAX_CARRY_STEPS &&
                if (threshold != null) mantissa >= BigDecimal.valueOf(threshold) else rounded >= THOUSAND
            if (carryInto) {
                index++
                carry++
                continue
            }

            val label = labelFor(index, cfg, alphabetic)
            if (label == null) {
                // Explicit fallback: this configuration cannot produce a label (past the last
                // compact letter, or an index a Long cannot count). Scientific notation is the
                // documented result, and the sign is already in the builder.
                sb.append(scientificDigits(magnitude, cfg))
                return
            }

            sb.append(formatMantissa(rounded, decimals))
            sb.append(label)
            return
        }
    }

    private fun appendScientificMode(sb: StringBuilder, magnitude: BigDecimal, cfg: Config) {
        val exponent = decimalExponent(magnitude)
        // Grouped for everyday magnitudes, scientific once it is long or tiny.
        if (exponent in SCIENTIFIC_GROUPED_FROM_EXPONENT until SCIENTIFIC_GROUPED_UNTIL_EXPONENT) {
            appendGrouped(sb, magnitude, cfg, cfg.decimals)
        } else {
            sb.append(scientificDigits(magnitude, cfg))
        }
    }

    /** Whether [index] has a label in this configuration. Alphabetic labels are unbounded. */
    private fun isLabelAvailable(index: Int, cfg: Config, alphabetic: Boolean): Boolean =
        if (alphabetic) index >= 0 else index in cfg.compactLetters.indices

    private fun labelFor(index: Int, cfg: Config, alphabetic: Boolean): String? =
        if (alphabetic) {
            suffix(index.toLong(), cfg.suffixAlphabet)
        } else if (index in cfg.compactLetters.indices) {
            cfg.compactLetters[index].toString()
        } else {
            null
        }

    private fun scientificDigits(magnitude: BigDecimal, cfg: Config): String {
        val exponent = decimalExponent(magnitude)
        val mantissa = magnitude.movePointLeft(exponent).setScale(cfg.scientificDigits, cfg.rounding.toJava())
        // A mantissa that rounds up to 10 belongs to the next exponent.
        val adjusted = if (mantissa >= TEN) {
            mantissa.movePointLeft(1).setScale(cfg.scientificDigits, cfg.rounding.toJava()) to exponent + 1
        } else {
            mantissa to exponent
        }
        val digits = adjusted.first.toPlainString()
        val suffixSign = if (adjusted.second < 0) "e-" else "e+"
        return digits + suffixSign + kotlin.math.abs(adjusted.second).toString()
    }

    private fun mantissaDecimals(mantissa: BigDecimal): Int = when {
        mantissa < TEN -> 2
        mantissa < HUNDRED -> 1
        else -> 0
    }

    private fun formatMantissa(rounded: BigDecimal, decimals: Int): String =
        if (decimals == 0) {
            rounded.setScale(0, RoundingMode.DOWN).toBigInteger().toString()
        } else {
            rounded.toPlainString()
        }

    private fun trimTrailingZeros(text: String): String {
        if (!text.contains('.')) return text
        var end = text.length
        while (end > 0 && text[end - 1] == '0') end--
        if (end > 0 && text[end - 1] == '.') end--
        return text.substring(0, end)
    }

    /** Appends an integer magnitude with the configured grouping. */
    private fun appendGrouped(sb: StringBuilder, magnitude: BigDecimal, cfg: Config, decimals: Int) {
        val rounded = magnitude.setScale(decimals, cfg.rounding.toJava())
        val text = rounded.toPlainString()
        val dot = text.indexOf('.')
        val integer = if (dot >= 0) text.substring(0, dot) else text
        appendGroupedDigits(sb, integer, cfg)
        if (decimals > 0) {
            // Trailing zeros are dropped here so `1,234` never renders as `1,234.00`; the explicit
            // `format(value, decimalPlaces)` path below keeps them for callers that asked for them.
            val fraction = if (dot >= 0) text.substring(dot + 1).trimEnd('0') else ""
            if (fraction.isNotEmpty()) sb.append('.').append(fraction)
        }
    }

    private fun appendGroupedDigits(sb: StringBuilder, integerDigits: String, cfg: Config) {
        if (!cfg.useGrouping) {
            sb.append(integerDigits)
            return
        }
        val size = cfg.groupSize
        val firstGroup = integerDigits.length % size
        var index = 0
        if (firstGroup > 0) {
            sb.append(integerDigits, 0, firstGroup)
            index = firstGroup
            if (index < integerDigits.length) sb.append(cfg.groupSeparator)
        }
        while (index < integerDigits.length) {
            sb.append(integerDigits, index, index + size)
            index += size
            if (index < integerDigits.length) sb.append(cfg.groupSeparator)
        }
    }

    /** Floor(log10(magnitude)) for a non-zero magnitude, computed exactly from the decimal digits. */
    private fun decimalExponent(magnitude: BigDecimal): Int {
        val stripped = magnitude.stripTrailingZeros()
        return stripped.precision() - stripped.scale() - 1
    }

    private const val SCIENTIFIC_GROUPED_UNTIL_EXPONENT = 6
    private const val SCIENTIFIC_GROUPED_FROM_EXPONENT = -3
}
