package com.aistudio.trueidle

import com.aistudio.trueidle.core.BigNumberFormatter
import com.aistudio.trueidle.core.BigNumberFormatter.Config
import com.aistudio.trueidle.core.NumberFormatter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * The facade every screen calls: `NumberFormatter.short(...)`.
 *
 * Engine behaviour lives in `BigNumberFormatterTest`; this file proves the facade forwards to it,
 * that durations/percentages are untouched by notation settings, and that the legacy `K/M/B/T`
 * notation still reproduces the previous implementation byte for byte.
 */
class NumberFormatterTest {

    @Before
    fun defaults() {
        BigNumberFormatter.setConfig(Config.DEFAULT)
        BigNumberFormatter.clearCache()
    }

    @After
    fun restore() {
        BigNumberFormatter.setConfig(Config.DEFAULT)
        BigNumberFormatter.clearCache()
    }

    @Test
    fun facadeDelegatesToTheCanonicalEngine() {
        assertEquals("1,000,000", NumberFormatter.short(1_000_000L))
        assertEquals(BigNumberFormatter.format(1_234.5), NumberFormatter.short(1_234.5))
        assertEquals("1.00A", NumberFormatter.short(1_000_000_000_000L))
        assertEquals(NumberFormatter.CACHE_CAPACITY, BigNumberFormatter.CACHE_CAPACITY)
    }

    @Test
    fun notationSettingChangesTheFacadeOutput() {
        BigNumberFormatter.setConfig(Config.COMPACT)
        assertEquals("1.23K", NumberFormatter.short(1_234L))
        BigNumberFormatter.setConfig(Config.DEFAULT)
        assertEquals("1,234", NumberFormatter.short(1_234L))
    }

    @Test
    fun durationAndPercentIgnoreNotationSettings() {
        BigNumberFormatter.setConfig(Config.SCIENTIFIC)
        assertEquals("4s", NumberFormatter.duration(4L))
        assertEquals("3m 12s", NumberFormatter.duration(192L))
        assertEquals("2h 05m", NumberFormatter.duration(7500L))
        assertEquals("1d 04h", NumberFormatter.duration(100800L))
        assertEquals("0s", NumberFormatter.duration(-5L))
        assertEquals("35%", NumberFormatter.percent(0.35))
        assertEquals("100%", NumberFormatter.percent(1.0))
        assertEquals("—", NumberFormatter.percent(Double.NaN))
    }

    @Test
    fun invalidNumbers() {
        assertEquals("—", NumberFormatter.short(Double.NaN))
        assertEquals("—", NumberFormatter.short(Double.POSITIVE_INFINITY))
        assertEquals("—", NumberFormatter.short(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun cacheFacadeReportsUsage() {
        NumberFormatter.clearCache()
        NumberFormatter.short(1_234.5678)
        assertEquals(1L, NumberFormatter.cacheStats().misses)
        NumberFormatter.short(1_234.5678)
        assertEquals(1L, NumberFormatter.cacheStats().hits)
    }

    /**
     * Compact mode is the notation that shipped before this change. It must stay byte-identical to
     * the frozen [LegacyNumberFormatterOracle] (`String.format` based) across every threshold and a
     * large randomised sample.
     *
     * The carry rule is the historical one: a suffix promotes at `999.95` and never past its last
     * available letter, so the `T` band still reads `1000T` instead of inventing a label.
     */
    @Test
    fun compactModeMatchesTheLegacyFormatter() {
        val compact = Config.COMPACT
        val mismatches = mutableListOf<String>()

        fun check(value: Double) {
            val expected = LegacyNumberFormatterOracle.short(value)
            val actual = BigNumberFormatter.format(value, compact)
            if (expected != actual) mismatches += "$value expected '$expected' but was '$actual'"
        }

        val boundaries = listOf(
            0.0, -0.0, 1e-320, 0.001, 0.005, 0.05, 0.5, 1.0, 9.999, 9.995, 99.99, 99.95,
            999.0, 999.4, 999.5, 999.9, 999.94, 999.95, 999.99, 1000.0, 1005.0, 1234.0,
            9_999.5, 9_995.0, 12_345.0, 99_994.0, 99_995.0, 99_999.0, 100_000.0, 123_456.0,
            999_499.0, 999_500.0, 999_949.0, 999_949.9, 999_950.0, 999_999.0,
            1e6, 1e6 + 1, 1.0005e6, 9.9995e6, 1e7, 1e8, 9.9995e8, 1e9, 1e10, 1e11, 9.9995e11,
            1e12, 1e13, 1e14, 9.9995e14, 1e15, 1e16, 1e100, 1.7976931348623157e308
        )
        boundaries.forEach { check(it) }
        boundaries.forEach { check(-it) }

        val random = Random(20_261_028)
        repeat(20_000) {
            val exponent = random.nextInt(-6, 19)
            val mantissa = 1.0 + random.nextDouble() * 998.0
            check(mantissa * Math.pow(10.0, exponent.toDouble()))
        }

        assertTrue(
            "compact notation drifted (${mismatches.size} mismatches):\n" +
                mismatches.take(20).joinToString("\n"),
            mismatches.isEmpty()
        )
        // Explicitly pin the last-letter rule the sampler may not reach.
        assertEquals("1000T", BigNumberFormatter.format(999_950_000_000_000.0, compact))
        assertEquals("1.00e+15", BigNumberFormatter.format(1_000_000_000_000_000.0, compact))
    }

    @Test
    fun sustainedWorkloadThroughTheFacadeStaysBounded() {
        NumberFormatter.clearCache()
        val random = Random(31)
        var checksum = 0L
        repeat(100_000) { checksum += NumberFormatter.short(random.nextDouble() * 1e15).length }
        assertTrue(checksum > 0)
        assertTrue(NumberFormatter.cacheStats().isBounded)
    }
}
