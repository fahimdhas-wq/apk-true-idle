package com.aistudio.trueidle

import com.aistudio.trueidle.core.NumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {

    @Test
    fun testShortNumberFormatting() {
        assertEquals("0", NumberFormatter.short(0L))
        assertEquals("999", NumberFormatter.short(999L))
        assertEquals("1.00K", NumberFormatter.short(1000L))
        assertEquals("12.3K", NumberFormatter.short(12345L))
        assertEquals("123K", NumberFormatter.short(123456L))
        assertEquals("1.23M", NumberFormatter.short(1234567L))
        assertEquals("1.23B", NumberFormatter.short(1234567890L))
        assertEquals("1.23T", NumberFormatter.short(1234567890123L))
    }

    @Test
    fun testCarryBoundary() {
        assertEquals("1.00M", NumberFormatter.short(999950.0))
        assertEquals("1.00B", NumberFormatter.short(999950000.0))
    }

    @Test
    fun testInvalidNumbers() {
        assertEquals("—", NumberFormatter.short(Double.NaN))
        assertEquals("—", NumberFormatter.short(Double.POSITIVE_INFINITY))
    }

    @Test
    fun testDurationFormatting() {
        assertEquals("4s", NumberFormatter.duration(4L))
        assertEquals("3m 12s", NumberFormatter.duration(192L))
        assertEquals("2h 05m", NumberFormatter.duration(7500L))
        assertEquals("1d 04h", NumberFormatter.duration(100800L))
    }

    @Test
    fun testPercentFormatting() {
        assertEquals("35%", NumberFormatter.percent(0.35))
        assertEquals("100%", NumberFormatter.percent(1.0))
        assertEquals("—", NumberFormatter.percent(Double.NaN))
    }
}
