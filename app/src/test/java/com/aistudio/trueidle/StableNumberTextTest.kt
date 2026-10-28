package com.aistudio.trueidle

import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.StableNumberText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The dirty-state gate: an unchanged value must not reach the formatter again. */
class StableNumberTextTest {

    @Test
    fun unchangedValueIsFormattedExactlyOnce() {
        val holder = StableNumberText()
        assertEquals(0L, holder.formatCalls)
        val first = holder.text(12_345L)
        assertEquals("12,345", first)
        assertEquals(1L, holder.formatCalls)
        repeat(10_000) { assertEquals("12,345", holder.text(12_345L)) }
        assertEquals("formatter was called for an unchanged value", 1L, holder.formatCalls)
    }

    @Test
    fun changedValueIsReformattedImmediately() {
        val holder = StableNumberText()
        assertEquals("1,000", holder.text(1_000L))
        assertEquals("1,234", holder.text(1_234L))
        assertEquals(2L, holder.formatCalls)
        assertEquals("1,000", holder.text(1_000L))
        assertEquals(3L, holder.formatCalls)
    }

    @Test
    fun doublesAreKeyedOnTheirExactBits() {
        val holder = StableNumberText()
        assertEquals("1,235", holder.text(1_234.5))
        assertEquals(1L, holder.formatCalls)
        assertEquals("1,235", holder.text(1_234.5))
        assertEquals(1L, holder.formatCalls)

        // Two values that format identically are still distinct inputs: 1,234.5 vs 1,234.50001.
        val identicalText = holder.text(1_234.50001)
        assertEquals("1,235", identicalText)
        assertEquals(2L, holder.formatCalls)

        // +0.0 and -0.0 have different bit patterns but the same rendering; either behaviour is
        // correct as long as the text is right and no stale value leaks through.
        assertEquals("0", holder.text(0.0))
        assertEquals("0", holder.text(-0.0))
        assertNotEquals("1,235", holder.text(Double.NaN))
        assertEquals("—", holder.text(Double.NaN))
    }

    @Test
    fun invalidateForcesReformatting() {
        val holder = StableNumberText()
        holder.text(500L)
        assertEquals(1L, holder.formatCalls)
        holder.text(500L)
        assertEquals(1L, holder.formatCalls)
        holder.invalidate()
        holder.text(500L)
        assertEquals(2L, holder.formatCalls)
    }

    @Test
    fun aCustomFormatterIsOnlyInvokedOnChange() {
        var calls = 0
        val holder = StableNumberText(formatLong = { calls++; "v$it" })
        assertEquals("v7", holder.text(7L))
        assertEquals("v7", holder.text(7L))
        assertEquals(1, calls)
    }

    @Test
    fun cachedTextNeverGoesStaleAcrossRapidChanges() {
        val holder = StableNumberText()
        var expected = ""
        for (value in 1..5_000) {
            expected = NumberFormatter.short(value.toLong())
            assertEquals(expected, holder.text(value.toLong()))
        }
        assertEquals(5_000L, holder.formatCalls)
        assertTrue(holder.formatCalls <= 5_000L)
    }
}
