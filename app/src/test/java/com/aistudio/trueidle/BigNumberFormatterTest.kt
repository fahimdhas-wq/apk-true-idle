package com.aistudio.trueidle

import com.aistudio.trueidle.core.BigNumberFormatter
import com.aistudio.trueidle.core.BigNumberFormatter.Config
import com.aistudio.trueidle.core.BigNumberFormatter.Mode
import com.aistudio.trueidle.core.NumberFormatter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.random.Random

/**
 * Coverage for the one large-number engine: notation progression, suffix sequence, magnitude
 * configuration, arbitrary precision, rounding, cache behaviour and compatibility with the notation
 * that shipped before it.
 */
class BigNumberFormatterTest {

    @Before
    fun useDefaults() {
        BigNumberFormatter.setConfig(Config.DEFAULT)
        BigNumberFormatter.clearCache()
    }

    @After
    fun restoreDefaults() {
        BigNumberFormatter.setConfig(Config.DEFAULT)
        BigNumberFormatter.clearCache()
    }

    // ------------------------------------------------ the requested progression

    /**
     * The progression requested for the game: grouped digits up to 10^12, then `A, B, C, ...` with
     * `A = 10^12` and a three-exponent step.
     */
    @Test
    fun alphabeticProgressionMatchesTheRequestedSequence() {
        assertEquals("100", BigNumberFormatter.format(100L))
        assertEquals("1,000", BigNumberFormatter.format(1_000L))
        assertEquals("10,000", BigNumberFormatter.format(10_000L))
        assertEquals("100,000", BigNumberFormatter.format(100_000L))
        assertEquals("1,000,000", BigNumberFormatter.format(1_000_000L))
        assertEquals("10,000,000", BigNumberFormatter.format(10_000_000L))
        assertEquals("100,000,000", BigNumberFormatter.format(100_000_000L))
        assertEquals("1,000,000,000", BigNumberFormatter.format(1_000_000_000L))
        assertEquals("10,000,000,000", BigNumberFormatter.format(10_000_000_000L))
        assertEquals("100,000,000,000", BigNumberFormatter.format(100_000_000_000L))
        // The last grouped magnitude, then the suffix sequence starts.
        assertEquals("999,999,999,999", BigNumberFormatter.format(999_999_999_999L))
        assertEquals("1.00A", BigNumberFormatter.format(1_000_000_000_000L))
        assertEquals("10.0A", BigNumberFormatter.format(10_000_000_000_000L))
        assertEquals("100A", BigNumberFormatter.format(100_000_000_000_000L))
        assertEquals("1.00B", BigNumberFormatter.format(1_000_000_000_000_000L))
        assertEquals("1.00C", BigNumberFormatter.format(1_000_000_000_000_000_000L))
    }

    @Test
    fun suffixSequenceIsBijectiveBase26() {
        assertEquals("A", BigNumberFormatter.suffix(0))
        assertEquals("Z", BigNumberFormatter.suffix(25))
        assertEquals("AA", BigNumberFormatter.suffix(26))
        assertEquals("AZ", BigNumberFormatter.suffix(51))
        assertEquals("BA", BigNumberFormatter.suffix(52))
        assertEquals("ZZ", BigNumberFormatter.suffix(701))
        assertEquals("AAA", BigNumberFormatter.suffix(702))
        assertEquals("AAB", BigNumberFormatter.suffix(703))
    }

    @Test
    fun suffixSequenceIsUniqueAndMonotonicWithoutATable() {
        val seen = HashSet<String>()
        for (index in 0L until 20_000L) {
            val label = BigNumberFormatter.suffix(index)
            assertTrue("no label for $index", label != null)
            assertTrue("duplicate $label", seen.add(label!!))
            assertTrue(label.all { it in 'A'..'Z' })
        }
        assertEquals(20_000, seen.size)
        // Beyond ZZ/AAA the label keeps growing; there is no fixed maximum.
        assertTrue(BigNumberFormatter.suffix(20_000)!!.length >= 3)
        // A Long index that large cannot arise from a value's exponent (BigDecimal exponents are
        // bounded far below it), so it reports failure instead of overflowing.
        assertNull(BigNumberFormatter.suffix(Long.MAX_VALUE))
    }

    @Test
    fun customAlphabetIsHonoured() {
        assertEquals("0", BigNumberFormatter.suffix(0, "01"))
        assertEquals("1", BigNumberFormatter.suffix(1, "01"))
        assertEquals("00", BigNumberFormatter.suffix(2, "01"))
        assertEquals("01", BigNumberFormatter.suffix(3, "01"))
        assertEquals("10", BigNumberFormatter.suffix(4, "01"))
    }

    @Test
    fun labelsAreReachableFromRealMagnitudes() {
        // Z = 12 + 25*3 = 10^87, AA = 10^90, AZ = 10^156, BA = 10^159.
        // index = (exponent - 12) / 3 with step 3, so Z=25 -> 10^87, AA=26 -> 10^90,
        // AZ=51 -> 10^165, BA=52 -> 10^168, ZZ=701 -> 10^2115.
        assertTrue(BigNumberFormatter.format(BigDecimal.TEN.pow(87)).endsWith("Z"))
        assertTrue(BigNumberFormatter.format(BigDecimal.TEN.pow(90)).endsWith("AA"))
        assertTrue(BigNumberFormatter.format(BigDecimal.TEN.pow(165)).endsWith("AZ"))
        assertTrue(BigNumberFormatter.format(BigDecimal.TEN.pow(168)).endsWith("BA"))
        assertTrue(BigNumberFormatter.format(BigDecimal.TEN.pow(12 + 701 * 3)).endsWith("ZZ"))
    }

    // ------------------------------------------------ magnitude configuration

    @Test
    fun magnitudeConventionIsExplicitlyConfigurable() {
        // Step 3 (shipped default): A=10^12, B=10^15.
        val stepThree = Config.DEFAULT
        assertEquals(12, stepThree.firstSuffixExponent)
        assertEquals(3, stepThree.exponentStep)
        assertEquals("1.00A", BigNumberFormatter.format(BigDecimal.TEN.pow(12), stepThree))
        assertEquals("1.00B", BigNumberFormatter.format(BigDecimal.TEN.pow(15), stepThree))

        // Step 1: each letter is one power of ten, so A=10^12 and B=10^13.
        val stepOne = Config.STEP_ONE
        assertEquals(1, stepOne.exponentStep)
        assertEquals("1.00A", BigNumberFormatter.format(BigDecimal.TEN.pow(12), stepOne))
        assertEquals("1.00B", BigNumberFormatter.format(BigDecimal.TEN.pow(13), stepOne))
        assertEquals("1.00Z", BigNumberFormatter.format(BigDecimal.TEN.pow(37), stepOne))
        assertEquals("1.00AA", BigNumberFormatter.format(BigDecimal.TEN.pow(38), stepOne))

        // A different start exponent moves the whole ladder.
        val fromSix = Config.DEFAULT.copy(firstSuffixExponent = 6)
        assertEquals("1,000,000", BigNumberFormatter.format(BigDecimal.TEN.pow(6), Config.DEFAULT))
        assertEquals("1.00A", BigNumberFormatter.format(BigDecimal.TEN.pow(6), fromSix))
    }

    @Test
    fun compactModeKeepsTheOriginalNotation() {
        val compact = Config.COMPACT
        assertEquals("1.23K", BigNumberFormatter.format(1_234L, compact))
        assertEquals("1.00M", BigNumberFormatter.format(999_950L, compact))
        assertEquals("1.00B", BigNumberFormatter.format(999_950_000L, compact))
        assertEquals("1.00T", BigNumberFormatter.format(1_000_000_000_000L, compact))
        assertEquals("1.00e+15", BigNumberFormatter.format(1_000_000_000_000_000L, compact))
        assertEquals("999", BigNumberFormatter.format(999L, compact))
        assertEquals("12.3", BigNumberFormatter.format(12.34, compact))
    }

    @Test
    fun groupedAndScientificModesAreAvailable() {
        assertEquals(
            "1,234,567,890,123",
            BigNumberFormatter.format(BigDecimal("1234567890123"), Config.GROUPED)
        )
        assertEquals("123.46", BigNumberFormatter.format(BigDecimal("123.456"), Config.GROUPED))
        assertEquals("1,234", BigNumberFormatter.format(1234L, Config.SCIENTIFIC))
        assertEquals("1.50e+6", BigNumberFormatter.format(1_500_000L, Config.SCIENTIFIC))
        assertEquals("1.23e+12", BigNumberFormatter.format(BigDecimal("1.234e12"), Config.SCIENTIFIC))
        assertEquals("1.23e-7", BigNumberFormatter.format(BigDecimal("1.234e-7"), Config.SCIENTIFIC))
    }

    @Test
    fun separatorsAndGroupingAreConfigurable() {
        val narrow = Config.GROUPED.copy(groupSeparator = ' ', groupSize = 3)
        assertEquals("1 234 567", BigNumberFormatter.format(1_234_567L, narrow))
        val plain = Config.GROUPED.copy(useGrouping = false)
        assertEquals("1234567", BigNumberFormatter.format(1_234_567L, plain))
    }

    // ------------------------------------------------ numerical correctness

    @Test
    fun zeroNegativeAndSmallValues() {
        assertEquals("0", BigNumberFormatter.format(0L))
        assertEquals("0", BigNumberFormatter.format(BigDecimal.ZERO))
        assertEquals("0", BigNumberFormatter.format(0.0))
        assertEquals("-999", BigNumberFormatter.format(-999L))
        assertEquals("-1,234", BigNumberFormatter.format(-1_234L))
        assertEquals("-1.23K", BigNumberFormatter.format(-1_234L, Config.COMPACT))
        assertEquals("-1.00A", BigNumberFormatter.format(-1_000_000_000_000L))
        assertEquals("0.5", BigNumberFormatter.format(0.5))
        assertEquals("-0.5", BigNumberFormatter.format(-0.5))
        assertEquals("0.35", BigNumberFormatter.format(0.35))
        assertEquals("—", BigNumberFormatter.format(Double.NaN))
        assertEquals("—", BigNumberFormatter.format(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun roundingIsExplicitPerMode() {
        val value = BigDecimal("1.005")
        val twoDecimals = Config.GROUPED.copy(decimals = 2, rounding = BigNumberFormatter.Rounding.HALF_UP)
        assertEquals("1.01", BigNumberFormatter.format(value, twoDecimals))
        // Grouped display drops trailing zeros, so DOWN renders 1.005 as "1" (not "1.00").
        assertEquals("1", BigNumberFormatter.format(value, twoDecimals.copy(rounding = BigNumberFormatter.Rounding.DOWN)))
        assertEquals("1.01", BigNumberFormatter.format(value, twoDecimals.copy(rounding = BigNumberFormatter.Rounding.UP)))
        // A clearer DOWN case: 1.999 keeps two digits but never rounds up.
        assertEquals("2", BigNumberFormatter.format(BigDecimal("1.999"), twoDecimals))
        assertEquals("1.99", BigNumberFormatter.format(BigDecimal("1.999"), twoDecimals.copy(rounding = BigNumberFormatter.Rounding.DOWN)))
        // The explicit decimalPlaces overload keeps the zeros the caller asked for (1.005 rounds up,
        // so 1.5 is the case that proves padding rather than rounding).
        assertEquals("1.50", BigNumberFormatter.format(BigDecimal("1.5"), 2))
        assertEquals("1.01", BigNumberFormatter.format(value, 2))
        // A carry across the suffix boundary, not "1000K".
        assertEquals("1.00M", BigNumberFormatter.format(BigDecimal("999999.9"), Config.COMPACT))
        // Below the first suffix the value is grouped, so a rounding carry just climbs to the next
        // grouped magnitude.
        assertEquals("1,000,000,000,000", BigNumberFormatter.format(BigDecimal("999999999999.9")))
        // At/above the first suffix the carry promotes to the next letter instead of printing 1000A.
        assertEquals("1.00A", BigNumberFormatter.format(BigDecimal("1e12")))
        assertEquals("1.00B", BigNumberFormatter.format(BigDecimal("999960000000000")))
    }

    @Test
    fun fractionalMantissasAndDecimalPlaces() {
        assertEquals("1.50A", BigNumberFormatter.format(BigDecimal("1.5e12")))
        assertEquals("15.0A", BigNumberFormatter.format(BigDecimal("1.5e13")))
        assertEquals("150A", BigNumberFormatter.format(BigDecimal("1.5e14")))
        assertEquals("1,234.57", BigNumberFormatter.format(BigDecimal("1234.5678"), Config.GROUPED))
        assertEquals("1,234", BigNumberFormatter.format(1_234L, Config.GROUPED))
        // format(value, decimalPlaces) keeps exactly that many digits, grouped, no suffix.
        assertEquals("1,234.50", BigNumberFormatter.format(BigDecimal("1234.5"), 2))
        assertEquals("1,235", BigNumberFormatter.format(BigDecimal("1234.5"), 0))
        assertEquals("0.00", BigNumberFormatter.format(BigDecimal.ZERO, 2))
    }

    @Test
    fun valuesBeyondLongMaxValueStayExact() {
        val huge = BigDecimal("123456789012345678901234567890.987654321")
        val text = BigNumberFormatter.format(huge)
        // exponent 29 -> suffix index (29-12)/3 = 5 -> "F"; a mantissa >= 100 uses no decimals.
        assertEquals("123F", text)

        val beyond = BigInteger("9".repeat(400))
        val rendered = BigNumberFormatter.formatInteger(beyond)
        assertTrue("unexpected: $rendered", Regex("[A-Z]{2}$").containsMatchIn(rendered))
        assertNotEquals("", rendered)

        // Largest double still renders in every mode that has a fallback.
        assertEquals("1.80e+308", BigNumberFormatter.format(Double.MAX_VALUE, Config.COMPACT))
        assertEquals("1.80e+308", BigNumberFormatter.format(Double.MAX_VALUE, Config.SCIENTIFIC))
        assertEquals("0", BigNumberFormatter.format(Double.MIN_VALUE))
    }

    @Test
    fun integerFormattingUsesTheCanonicalTable() {
        assertEquals("1,000,000", BigNumberFormatter.formatInteger(BigInteger.valueOf(1_000_000)))
        assertEquals("1.00A", BigNumberFormatter.formatInteger(BigInteger.TEN.pow(12)))
        assertEquals("-1.00A", BigNumberFormatter.formatInteger(BigInteger.TEN.pow(12).negate()))
        var rejected = false
        try {
            BigNumberFormatter.suffix(-1)
        } catch (expected: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("a negative suffix index must be rejected", rejected)
    }

    // ------------------------------------------------ configuration + cache

    @Test
    fun switchingModeChangesOutputAndInvalidatesTheCache() {
        assertEquals("1.23K", BigNumberFormatter.format(1_234L, Config.COMPACT))
        assertEquals("1,234", BigNumberFormatter.format(1_234L))

        BigNumberFormatter.setConfig(Config.COMPACT)
        assertEquals("1.23K", BigNumberFormatter.format(1_234L))
        assertEquals(Mode.COMPACT, BigNumberFormatter.config.mode)
        // Cached under the previous config must not leak through.
        BigNumberFormatter.setConfig(Config.DEFAULT)
        assertEquals("1,234", BigNumberFormatter.format(1_234L))
        assertEquals(Mode.ALPHABETIC, BigNumberFormatter.config.mode)
    }

    @Test
    fun cacheIsBoundedAndReportsUsage() {
        BigNumberFormatter.clearCache()
        BigNumberFormatter.format(1_234.5)
        val first = BigNumberFormatter.cacheStats()
        assertEquals(1L, first.misses)
        assertEquals(0L, first.hits)

        BigNumberFormatter.format(1_234.5)
        assertEquals(1L, BigNumberFormatter.cacheStats().hits)

        val random = Random(5)
        repeat(BigNumberFormatter.CACHE_CAPACITY * 4) {
            BigNumberFormatter.format(random.nextDouble() * 1e9 + it)
        }
        val stats = BigNumberFormatter.cacheStats()
        assertTrue("cache grew past capacity: $stats", stats.isBounded)
        assertTrue("expected evictions", stats.evictions > 0)
        // A value formatted long ago must still be correct after eviction.
        assertEquals("1.23K", BigNumberFormatter.format(1_234L, Config.COMPACT))
    }

    @Test
    fun defaultsAreTheRequestedProgression() {
        assertEquals(Mode.ALPHABETIC, BigNumberFormatter.config.mode)
        assertEquals(12, BigNumberFormatter.config.firstSuffixExponent)
        assertEquals(3, BigNumberFormatter.config.exponentStep)
        assertEquals(Mode.ALPHABETIC, Config.byName("alphabetiC").mode)
        assertEquals(Mode.COMPACT, Config.byName("compact").mode)
        assertEquals(Mode.GROUPED, Config.byName("GROUPED").mode)
        assertEquals(Mode.SCIENTIFIC, Config.byName("scientific").mode)
        assertEquals(Mode.ALPHABETIC, Config.byName(null).mode)
        assertEquals(Mode.ALPHABETIC, Config.byName("nonsense").mode)
    }

    @Test
    fun numberFormatterFacadeDelegatesAndDurationPercentStayPlain() {
        assertEquals(BigNumberFormatter.format(1_000_000L), NumberFormatter.short(1_000_000L))
        assertEquals("4s", NumberFormatter.duration(4L))
        assertEquals("3m 12s", NumberFormatter.duration(192L))
        assertEquals("2h 05m", NumberFormatter.duration(7500L))
        assertEquals("1d 04h", NumberFormatter.duration(100800L))
        assertEquals("35%", NumberFormatter.percent(0.35))
        assertEquals("—", NumberFormatter.percent(Double.NaN))
    }

    @Test
    fun sustainedMixedWorkloadStaysBounded() {
        BigNumberFormatter.clearCache()
        val random = Random(77)
        var checksum = 0L
        repeat(100_000) { i ->
            val text = when (i % 4) {
                0 -> BigNumberFormatter.format(random.nextLong(0, 1_000_000))
                1 -> BigNumberFormatter.format(random.nextDouble() * 1e12)
                2 -> BigNumberFormatter.format(BigDecimal.TEN.pow(random.nextInt(1, 60)), Config.DEFAULT)
                else -> BigNumberFormatter.format(1_234L, Config.COMPACT)
            }
            checksum += text.length
        }
        assertTrue(checksum > 0)
        assertTrue(BigNumberFormatter.cacheStats().isBounded)
    }
}
