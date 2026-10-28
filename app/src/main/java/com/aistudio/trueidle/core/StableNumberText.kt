package com.aistudio.trueidle.core

/**
 * Memoises the formatted text of one displayed number.
 *
 * The cost that matters for a 20-tick-per-second idle screen is not the microsecond spent inside
 * [NumberFormatter]; it is formatting at all when the value did not change. A `StateFlow` emission
 * recomposes the tree even when `gold` is identical, so the old code re-formatted every visible
 * number on every tick. This holder compares the raw value (and the formatter's configuration
 * version) and returns the previous string without touching the formatter on a match.
 *
 * One instance belongs to one displayed field. It is not thread-safe by design: it lives in a single
 * composition/thread and holds no reference to a `View`, `Context` or `Activity`, so it cannot leak.
 *
 * [formatCalls] exposes how many real formatting calls happened, which is what the tests assert on.
 */
class StableNumberText(
    private val formatLong: (Long) -> String = { NumberFormatter.short(it) },
    private val formatDouble: (Double) -> String = { NumberFormatter.short(it) }
) {

    private var configVersion: Int = NumberFormatter.CONFIG_VERSION

    private var longRaw: Long = 0L
    private var longHasValue: Boolean = false
    private var longText: String = ""

    private var doubleRaw: Long = 0L
    private var doubleHasValue: Boolean = false
    private var doubleText: String = ""

    /** Number of times the underlying formatter was actually invoked. */
    var formatCalls: Long = 0L
        private set

    /** Text for [value], formatted only when the value (or the formatter configuration) changed. */
    fun text(value: Long): String {
        ensureConfig()
        if (longHasValue && longRaw == value) return longText
        val formatted = formatLong(value)
        formatCalls++
        longRaw = value
        longText = formatted
        longHasValue = true
        return formatted
    }

    /** Text for [value]; keyed on the exact IEEE-754 bit pattern so no value can alias another. */
    fun text(value: Double): String {
        ensureConfig()
        val bits = if (value.isNaN()) Double.NaN.toRawBits() else value.toRawBits()
        if (doubleHasValue && doubleRaw == bits) return doubleText
        val formatted = formatDouble(value)
        formatCalls++
        doubleRaw = bits
        doubleText = formatted
        doubleHasValue = true
        return formatted
    }

    /** Forces the next [text] call to re-format (e.g. after a display setting change). */
    fun invalidate() {
        longHasValue = false
        doubleHasValue = false
    }

    private fun ensureConfig() {
        val current = NumberFormatter.CONFIG_VERSION
        if (current != configVersion) {
            configVersion = current
            invalidate()
        }
    }
}
