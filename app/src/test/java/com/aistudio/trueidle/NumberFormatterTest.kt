package com.aistudio.trueidle

import com.aistudio.trueidle.core.NumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

/**
 * Correctness, boundary, cache and stress coverage for the canonical formatter.
 *
 * The notation assertions are not "whatever the new code does": every case is also compared against
 * [LegacyNumberFormatterOracle], a frozen copy of the formatter that shipped before the optimisation.
 */
class NumberFormatterTest {

    @Before
    fun resetCache() {
        NumberFormatter.clearCache()
    }

    // ------------------------------------------------------------------ notation

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
        assertEquals("—", NumberFormatter.short(Double.NEGATIVE_INFINITY))
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

    /**
     * The ten magnitudes the spec calls out. Each expected string is what the pre-optimisation
     * formatter produced (see [LegacyNumberFormatterOracle] and
     * [optimisedOutputMatchesTheLegacyFormatterOverBoundaryData]).
     */
    @Test
    fun notationIsPreservedAtEveryRequiredMagnitude() {
        assertEquals("100", NumberFormatter.short(100L))
        assertEquals("1.00K", NumberFormatter.short(1_000L))
        assertEquals("10.0K", NumberFormatter.short(10_000L))
        assertEquals("100K", NumberFormatter.short(100_000L))
        assertEquals("1.00M", NumberFormatter.short(1_000_000L))
        assertEquals("10.0M", NumberFormatter.short(10_000_000L))
        assertEquals("100M", NumberFormatter.short(100_000_000L))
        assertEquals("1.00B", NumberFormatter.short(1_000_000_000L))
        assertEquals("10.0B", NumberFormatter.short(10_000_000_000L))
        assertEquals("1.00T", NumberFormatter.short(1_000_000_000_000L))

        // Same values as doubles, and one step either side of every threshold.
        assertEquals("100", NumberFormatter.short(100.0))
        assertEquals("999", NumberFormatter.short(999.0))
        assertEquals("1.00K", NumberFormatter.short(1_000.0))
        assertEquals("999.6", NumberFormatter.short(999.6))
        assertEquals("999.9", NumberFormatter.short(999.9))
        assertEquals("1.00M", NumberFormatter.short(1_000_000.0))
        assertEquals("999K", NumberFormatter.short(999_499.0))
        assertEquals("1.00B", NumberFormatter.short(1_000_000_000.0))
        assertEquals("1.00T", NumberFormatter.short(1_000_000_000_000.0))
        assertEquals("1.00e+15", NumberFormatter.short(1_000_000_000_000_000.0))
    }

    @Test
    fun fractionalAndNegativeValuesAreRenderedLikeTheLegacyFormatter() {
        assertEquals("0", NumberFormatter.short(0.0))
        assertEquals("0", NumberFormatter.short(-0.0))
        assertEquals("0.5", NumberFormatter.short(0.5))
        assertEquals("-0.5", NumberFormatter.short(-0.5))
        assertEquals("12.3", NumberFormatter.short(12.34))
        assertEquals("-12.3", NumberFormatter.short(-12.34))
        assertEquals("999.9", NumberFormatter.short(999.94))
        assertEquals("-1.00K", NumberFormatter.short(-1_000.0))
        assertEquals("-1.23M", NumberFormatter.short(-1_234_567.0))
        assertEquals("-1.00e+15", NumberFormatter.short(-1e15))
    }

    @Test
    fun roundingTiesAndCarriesMatchTheReferenceFormatter() {
        // 1.005 is stored just below the tie, so the reference prints 1.00 rather than 1.01.
        assertEquals(LegacyNumberFormatterOracle.short(1.005), NumberFormatter.short(1.005))
        assertEquals("1.0", NumberFormatter.short(1.005))
        // The same value scaled into the K band (1.005 -> "%.2f"): the tie is resolved exactly the
        // way the reference resolves it, on both the fast and the fallback path.
        assertEquals(LegacyNumberFormatterOracle.short(1_005.0), NumberFormatter.short(1_005.0))
        assertEquals(LegacyNumberFormatterOracle.short(2.675), NumberFormatter.short(2.675))
        assertEquals(LegacyNumberFormatterOracle.short(0.05), NumberFormatter.short(0.05))
        assertEquals(LegacyNumberFormatterOracle.short(999.95), NumberFormatter.short(999.95))
        assertEquals(LegacyNumberFormatterOracle.short(999.9499999), NumberFormatter.short(999.9499999))
        assertEquals(LegacyNumberFormatterOracle.short(999_949.9), NumberFormatter.short(999_949.9))
        assertEquals("1000K", NumberFormatter.short(999_949.9))
        assertEquals(LegacyNumberFormatterOracle.short(999_949.99), NumberFormatter.short(999_949.99))
        assertEquals(LegacyNumberFormatterOracle.short(9.995e5), NumberFormatter.short(9.995e5))
    }

    @Test
    fun longEdgesAreHandled() {
        assertEquals(LegacyNumberFormatterOracle.short(Long.MAX_VALUE), NumberFormatter.short(Long.MAX_VALUE))
        assertEquals(LegacyNumberFormatterOracle.short(Long.MIN_VALUE), NumberFormatter.short(Long.MIN_VALUE))
        assertEquals("-999", NumberFormatter.short(-999L))
        assertEquals(LegacyNumberFormatterOracle.short(-1000L), NumberFormatter.short(-1000L))
        assertEquals("-1.00K", NumberFormatter.short(-1000L))
    }

    @Test
    fun veryLargeValuesStayReadableAndFinite() {
        assertEquals("1.80e+308", NumberFormatter.short(Double.MAX_VALUE))
        assertEquals("1.00e+100", NumberFormatter.short(1e100))
        assertEquals("1.23e+100", NumberFormatter.short(1.23e100))
        assertEquals(LegacyNumberFormatterOracle.short(1.23e100), NumberFormatter.short(1.23e100))
        assertEquals(LegacyNumberFormatterOracle.short(Double.MIN_VALUE), NumberFormatter.short(Double.MIN_VALUE))
    }

    /**
     * Exhaustive-ish equivalence check: every boundary in the notation table, every rounding tie
     * neighbourhood, plus a large randomised sample across all bands.
     */
    @Test
    fun optimisedOutputMatchesTheLegacyFormatterOverBoundaryData() {
        val mismatches = mutableListOf<String>()

        fun check(value: Double) {
            val expected = LegacyNumberFormatterOracle.short(value)
            val actual = NumberFormatter.short(value)
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

        // Walk the .5 rounding boundary for every band with 1-3 significant decimals.
        var step = 0.05
        while (step < 1e17) {
            for (offset in -3..3) check(step + offset * 1e-12)
            for (offset in -3..3) check(step * 10 + offset * 1e-9)
            step *= 10.0
        }

        val random = Random(20_261_028)
        repeat(40_000) {
            val exponent = random.nextInt(-6, 19)
            val mantissa = 1.0 + random.nextDouble() * 998.0
            check(mantissa * Math.pow(10.0, exponent.toDouble()))
        }

        assertTrue(
            "notation drifted from the reference formatter (${mismatches.size} mismatches):\n" +
                mismatches.take(20).joinToString("\n"),
            mismatches.isEmpty()
        )
    }

    // ------------------------------------------------------- alphabetic suffixes

    @Test
    fun suffixSequenceIsBijectiveBase26() {
        assertEquals("A", NumberFormatter.suffixLabel(0))
        assertEquals("B", NumberFormatter.suffixLabel(1))
        assertEquals("Y", NumberFormatter.suffixLabel(24))
        assertEquals("Z", NumberFormatter.suffixLabel(25))
        assertEquals("AA", NumberFormatter.suffixLabel(26))
        assertEquals("AB", NumberFormatter.suffixLabel(27))
        assertEquals("AZ", NumberFormatter.suffixLabel(51))
        assertEquals("BA", NumberFormatter.suffixLabel(52))
        assertEquals("BZ", NumberFormatter.suffixLabel(77))
        assertEquals("CA", NumberFormatter.suffixLabel(78))
        assertEquals("ZZ", NumberFormatter.suffixLabel(701))
        assertEquals("AAA", NumberFormatter.suffixLabel(702))
        assertEquals("AAZ", NumberFormatter.suffixLabel(727))
        assertEquals("ABA", NumberFormatter.suffixLabel(728))
    }

    @Test
    fun suffixLabelsRoundTripThroughTheirIndexRange() {
        // For every label of up to three letters, the mapping must be strictly increasing and unique.
        var index = 0L
        val seen = HashSet<String>()
        while (index < 18_278) { // 26 + 26^2 + 26^3 = 18,278 labels
            val label = NumberFormatter.suffixLabel(index)
            assertNotNull("expected a label for index $index", label)
            assertTrue("duplicate label '$label'", seen.add(label!!))
            assertTrue("'$label' must be uppercase A-Z", label.all { it in 'A'..'Z' })
            index++
        }
        assertEquals(18_278, seen.size)
        assertTrue("ZZZ" in seen)
        assertNull(NumberFormatter.suffixLabel(18_278, maxLength = 3))
    }

    @Test
    fun suffixLabelsRespectTheLengthLimit() {
        assertNull(NumberFormatter.suffixLabel(26, maxLength = 1))
        assertEquals("A", NumberFormatter.suffixLabel(0, maxLength = 1))
        assertNull(NumberFormatter.suffixLabel(Long.MAX_VALUE, maxLength = 10))
        // 1e12 fits in nine letters, 1e15 does not fit in ten.
        assertEquals(9, NumberFormatter.suffixLabel(1_000_000_000_000L, maxLength = 10)?.length)
        assertNull(NumberFormatter.suffixLabel(1_000_000_000_000_000L, maxLength = 10))
        assertEquals(11, NumberFormatter.suffixLabel(1_000_000_000_000_000L, maxLength = 12)?.length)
    }

    @Test
    fun alphabeticMagnitudeMappingIsExplicitAndTested() {
        val config = NumberFormatter.SuffixMagnitudeConfig.DEFAULT
        assertEquals(3, config.firstSuffixExponent)
        assertEquals(3, config.exponentStep)

        // Index 0 ("A") covers 1e3, index 1 ("B") covers 1e6, index 2 ("C") covers 1e9.
        assertEquals("1.00A", NumberFormatter.alphabeticShort(1_000.0))
        assertEquals("1.23A", NumberFormatter.alphabeticShort(1_234.0))
        assertEquals("12.0A", NumberFormatter.alphabeticShort(12_000.0))
        assertEquals("100A", NumberFormatter.alphabeticShort(100_000.0))
        assertEquals("1.00B", NumberFormatter.alphabeticShort(1_000_000.0))
        assertEquals("1.00C", NumberFormatter.alphabeticShort(1_000_000_000.0))
        assertEquals("1.00D", NumberFormatter.alphabeticShort(1_000_000_000_000.0))
        assertEquals("999", NumberFormatter.alphabeticShort(999.0))
        assertEquals("0", NumberFormatter.alphabeticShort(0.0))
        assertEquals("—", NumberFormatter.alphabeticShort(Double.NaN))
        assertEquals("—", NumberFormatter.alphabeticShort(Double.POSITIVE_INFINITY))
        assertEquals("-1.00A", NumberFormatter.alphabeticShort(-1_000.0))

        // 999.95 carries into the next suffix instead of printing "1000A".
        assertEquals("1.00B", NumberFormatter.alphabeticShort(999_950.0))

        // A different step is honoured, proving the mapping is configuration and not a constant.
        val stepFour = NumberFormatter.SuffixMagnitudeConfig(firstSuffixExponent = 3, exponentStep = 4)
        assertEquals("1.00A", NumberFormatter.alphabeticShort(1_000.0, stepFour))
        assertEquals("1.00B", NumberFormatter.alphabeticShort(10_000_000.0, stepFour))

        // Labels deeper in the sequence: 1e78 is suffix index 25 ("Z").
        assertTrue(NumberFormatter.alphabeticShort(1e78).endsWith("Z"))
        assertTrue(NumberFormatter.alphabeticShort(1e81).endsWith("AA"))
        assertTrue(NumberFormatter.alphabeticShort(1e156).endsWith("AZ"))
        assertTrue(NumberFormatter.alphabeticShort(1e159).endsWith("BA"))

        // Doubles stop at 1e308, i.e. suffix index 101 ("CX"); that is still a normal label.
        assertTrue(NumberFormatter.alphabeticShort(1e308).endsWith("CX"))

        // A config whose first exponent is past the supported range degrades to the documented
        // scientific fallback instead of throwing or inventing a label.
        val beyond = NumberFormatter.SuffixMagnitudeConfig(firstSuffixExponent = 309, exponentStep = 3)
        assertTrue(NumberFormatter.alphabeticShort(1e308, beyond).contains("e+"))
    }

    @Test
    fun alphabeticNotationFallsBackWhenTheConfigCannotProduceALabel() {
        val tiny = NumberFormatter.SuffixMagnitudeConfig(maxSuffixLength = 1)
        // Index 27 needs two letters ("AB"), which this config forbids -> scientific fallback.
        assertEquals("1.00e+84", NumberFormatter.alphabeticShort(1e84, tiny))
        assertEquals("1.00A", NumberFormatter.alphabeticShort(1e3, tiny))
        assertEquals("1.00Z", NumberFormatter.alphabeticShort(1e78, tiny))
    }

    // -------------------------------------------------------------------- cache

    @Test
    fun cacheReportsHitsAndMisses() {
        NumberFormatter.clearCache()
        val first = NumberFormatter.short(1234.5678)
        val afterMiss = NumberFormatter.cacheStats()
        assertEquals(1L, afterMiss.misses)
        assertEquals(0L, afterMiss.hits)

        val second = NumberFormatter.short(1234.5678)
        val afterHit = NumberFormatter.cacheStats()
        assertEquals(1L, afterHit.hits)
        assertEquals(first, second)
        assertTrue(afterHit.isBounded)
    }

    @Test
    fun cacheNeverReturnsAnotherValuesText() {
        NumberFormatter.clearCache()
        val random = Random(7)
        val values = DoubleArray(600) { random.nextDouble() * 1e12 }
        val expected = values.map { LegacyNumberFormatterOracle.short(it) }
        values.forEach { NumberFormatter.short(it) }
        values.forEachIndexed { index, value ->
            assertEquals(expected[index], NumberFormatter.short(value))
        }
        val stats = NumberFormatter.cacheStats()
        assertTrue("cache grew past capacity: $stats", stats.entries <= NumberFormatter.CACHE_CAPACITY)
        assertEquals(NumberFormatter.CACHE_CAPACITY, stats.capacity)
    }

    @Test
    fun cacheIsStrictlyBoundedAndEvicts() {
        NumberFormatter.clearCache()
        val random = Random(11)
        repeat(NumberFormatter.CACHE_CAPACITY * 4) {
            NumberFormatter.short(random.nextDouble() * 1e9 + it)
        }
        val stats = NumberFormatter.cacheStats()
        assertTrue("entries ${stats.entries} exceeded ${stats.capacity}", stats.isBounded)
        assertTrue("expected evictions, saw $stats", stats.evictions > 0)
        assertTrue("expected misses, saw $stats", stats.misses >= NumberFormatter.CACHE_CAPACITY)
        // A value that was evicted must still format correctly.
        assertEquals("1.00K", NumberFormatter.short(1_000.0))
    }

    @Test
    fun alphabeticResultsNeverComeFromTheCompactCache() {
        assertEquals("1.00K", NumberFormatter.short(1_000.0))
        assertEquals("1.00A", NumberFormatter.alphabeticShort(1_000.0))
        assertEquals("1.00K", NumberFormatter.short(1_000.0))
        val alphabetic = NumberFormatter.alphabeticShort(1_000_000.0)
        val compact = NumberFormatter.short(1_000_000.0)
        assertEquals("1.00B", alphabetic)
        assertEquals("1.00M", compact)
    }

    @Test
    fun cacheIsSafeUnderConcurrentFormatting() {
        NumberFormatter.clearCache()
        val threads = 4
        val start = CountDownLatch(1)
        val failure = AtomicReference<String?>(null)
        val workers = (0 until threads).map { worker ->
            Thread {
                start.await()
                val random = Random(worker)
                repeat(20_000) {
                    val value = random.nextDouble() * 1e13
                    val actual = NumberFormatter.short(value)
                    val expected = LegacyNumberFormatterOracle.short(value)
                    if (actual != expected) failure.compareAndSet(null, "$value -> $actual != $expected")
                }
            }.apply { start() }
        }
        start.countDown()
        workers.forEach { it.join() }
        assertNull(failure.get())
        assertTrue(NumberFormatter.cacheStats().isBounded)
    }

    @Test
    fun clearCacheResetsCounters() {
        NumberFormatter.short(424242.0)
        NumberFormatter.clearCache()
        val stats = NumberFormatter.cacheStats()
        assertEquals(0L, stats.hits)
        assertEquals(0L, stats.misses)
        assertEquals(0L, stats.evictions)
        assertEquals(0, stats.entries)
    }

    // ------------------------------------------------------------------- stress

    @Test
    fun sustainedMixedWorkloadStaysBoundedAndCorrect() {
        NumberFormatter.clearCache()
        val random = Random(99)
        var checksum = 0L
        val sample = ArrayList<Pair<Double, String>>(256)

        repeat(100_000) { i ->
            val value = when (i % 5) {
                0 -> random.nextLong(0, 1_000).toDouble()
                1 -> random.nextDouble() * 1_000
                2 -> random.nextDouble() * 1_000_000
                3 -> random.nextDouble() * 1_000_000_000_000
                else -> random.nextDouble() * 1e16
            }
            val text = NumberFormatter.short(value)
            checksum += text.length
            if (i % 397 == 0) sample += value to text
        }

        assertTrue(checksum > 0)
        val stats = NumberFormatter.cacheStats()
        assertTrue("cache unbounded: $stats", stats.isBounded)
        sample.forEach { (value, text) ->
            assertEquals(LegacyNumberFormatterOracle.short(value), text)
        }
    }
}
