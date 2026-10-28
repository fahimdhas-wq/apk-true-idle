package com.aistudio.trueidle

import com.aistudio.trueidle.core.TierConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the authoritative tier table: every identifier, every colour, and the canonical rank order.
 */
class TierConfigTest {

    /** The authoritative identifier -> source hex mapping, copied verbatim from the spec. */
    private val authoritative: List<Pair<String, String>> = listOf(
        "F-" to "#888",
        "F" to "#777",
        "F+" to "#888",

        "E-" to "#aaa",
        "E" to "#aaa",
        "E+" to "#bbb",

        "D-" to "#4caf50",
        "D" to "#4caf50",
        "D+" to "#66bb6a",

        "C-" to "#42a5f5",
        "C" to "#42a5f5",
        "C+" to "#64b5f6",

        "B-" to "#ab47bc",
        "B" to "#ab47bc",
        "B+" to "#ce93d8",

        "A-" to "#ff9800",
        "A" to "#ff9800",
        "A+" to "#ffb74d",

        "S-" to "#ef5350",
        "S" to "#ef5350",
        "S+" to "#e57373",

        "SS-" to "#ffd700",
        "SS" to "#ffd700",
        "SS+" to "#ffd700",

        "SSS-" to "#00e5ff",
        "SSS" to "#00e5ff",
        "SSS+" to "#00e5ff",

        "SSR" to "#ff6f00",
        "SSR+" to "#ff6f00",

        "UR" to "#e040fb",
        "UR+" to "#e040fb",
        "URR" to "#e040fb",

        "EX" to "#00e5ff",
        "EX+" to "#00e5ff",

        "Transcendent" to "#00e5ff",
        "Transcendent+" to "#00e5ff",
        "Transcendent++" to "#00e5ff",
        "Transcendent+++" to "#00e5ff",
        "Transcendent EX" to "#00e5ff",

        "Ethereal" to "#b388ff",
        "Ethereal+" to "#b388ff",
        "Ethereal++" to "#b388ff",
        "Ethereal+++" to "#b388ff",
        "Ethereal EX" to "#b388ff",

        "Cosmic" to "#40c4ff",
        "Cosmic+" to "#40c4ff",
        "Cosmic++" to "#40c4ff",
        "Cosmic+++" to "#40c4ff",
        "Cosmic EX" to "#40c4ff",

        "Omni" to "#ffab40",
        "Omni+" to "#ffab40",
        "Omni++" to "#ffab40",
        "Omni+++" to "#ffab40",
        "Omni EX" to "#ffab40",

        "Singularity" to "#e040fb",
        "Singularity+" to "#e040fb",
        "Singularity++" to "#e040fb",
        "Singularity+++" to "#e040fb",
        "Singularity EX" to "#e040fb",

        "Continuum" to "#00e5ff",
        "Continuum+" to "#00e5ff",
        "Continuum++" to "#00e5ff",
        "Continuum+++" to "#00e5ff",
        "Continuum EX" to "#00e5ff",

        "Paradox" to "#ff5252",
        "Paradox+" to "#ff5252",
        "Paradox++" to "#ff5252",
        "Paradox+++" to "#ff5252",
        "Paradox EX" to "#ff5252",

        "Reality" to "#69f0ae",
        "Reality+" to "#69f0ae",
        "Reality++" to "#69f0ae",
        "Reality+++" to "#69f0ae",
        "Reality EX" to "#69f0ae",

        "Chronos" to "#ffd700",
        "Chronos+" to "#ffd700",
        "Chronos++" to "#ffd700",
        "Chronos+++" to "#ffd700",
        "Chronos EX" to "#ffd700",

        "Eternal" to "#ffffff",
        "Eternal+" to "#ffffff",
        "Eternal++" to "#ffffff",
        "Eternal+++" to "#ffffff",
        "Eternal EX" to "#ffffff",

        "Absolute" to "#ff4081",
        "Absolute+" to "#ff4081",
        "Absolute++" to "#ff4081",
        "Absolute+++" to "#ff4081",
        "Absolute EX" to "#ff4081",

        "Omega" to "#7c4dff",
        "Omega+" to "#7c4dff",
        "Omega++" to "#7c4dff",
        "Omega+++" to "#7c4dff",
        "Omega EX" to "#7c4dff",

        "Zenith" to "#18ffff",
        "Zenith+" to "#18ffff",
        "Zenith++" to "#18ffff",
        "Zenith+++" to "#18ffff",
        "Zenith EX" to "#18ffff",

        "Apotheosis" to "#ff6d00",
        "Apotheosis+" to "#ff6d00",
        "Apotheosis++" to "#ff6d00",
        "Apotheosis+++" to "#ff6d00",
        "Apotheosis EX" to "#ff6d00",

        "Infinite" to "#00e676",
        "Infinite+" to "#00e676",
        "Infinite++" to "#00e676",
        "Infinite+++" to "#00e676",
        "Infinite EX" to "#00e676"
    )

    @Test
    fun everyAuthoritativeIdentifierExistsExactlyOnce() {
        assertEquals(authoritative.map { it.first }.toSet().size, TierConfig.size)
        assertEquals(authoritative.size, TierConfig.size)
        authoritative.forEach { (id, _) ->
            assertTrue("missing tier '$id'", TierConfig.contains(id))
            assertEquals("rank of '$id'", authoritative.map { it.first }.indexOf(id), TierConfig.rankOf(id))
        }
    }

    @Test
    fun everyTierHasTheExactExpectedArgbColour() {
        authoritative.forEach { (id, hex) ->
            val expected = expectedArgb(hex)
            assertEquals("colour of '$id'", expected, TierConfig.colorOf(id))
            assertEquals("rgb of '$id'", expected and 0x00FFFFFF, TierConfig.rgbOf(id))
        }
    }

    @Test
    fun colourIntegersMatchTheirRgbTriplets() {
        // "#888" must be the CSS/Android short form 0xFF888888, not 0xFF000888 or 0xFF008888.
        assertEquals(0xFF888888.toInt(), TierConfig.colorOf("F-"))
        assertEquals(0xFF777777.toInt(), TierConfig.colorOf("F"))
        assertEquals(0xFF4CAF50.toInt(), TierConfig.colorOf("D-"))
        assertEquals(0xFFFFFFFF.toInt(), TierConfig.colorOf("Eternal"))
        assertEquals(0xFF00E5FF.toInt(), TierConfig.colorOf("EX"))
        assertEquals(0xFF8899AC.toInt(), TierConfig.UNKNOWN_COLOR)
    }

    @Test
    fun rankOrderFollowsTheCanonicalProgression() {
        assertEquals(0, TierConfig.rankOf("F-"))
        assertTrue(TierConfig.rankOf("F-") < TierConfig.rankOf("F"))
        assertTrue(TierConfig.rankOf("F") < TierConfig.rankOf("F+"))
        assertTrue(TierConfig.rankOf("SSS+") < TierConfig.rankOf("SSR"))
        assertTrue(TierConfig.rankOf("EX+") < TierConfig.rankOf("Transcendent"))
        assertTrue(TierConfig.rankOf("Infinite+++") < TierConfig.rankOf("Infinite EX"))
        assertEquals(TierConfig.size - 1, TierConfig.rankOf("Infinite EX"))

        // Rank order is not alphabetical: "Absolute" sorts before "EX" as a string but ranks above.
        assertTrue(TierConfig.isHigher("Absolute", "EX"))
        assertTrue(TierConfig.compareRanks("Absolute", "EX") > 0)
        assertTrue(TierConfig.compareRanks("EX", "Absolute") < 0)
        assertEquals(0, TierConfig.compareRanks("Transcendent", "Transcendent"))
    }

    @Test
    fun rankAndIdLookupsAreInverse() {
        for (rank in 0 until TierConfig.size) {
            val id = TierConfig.idAt(rank)
            assertNotNull(id)
            assertEquals(rank, TierConfig.rankOf(id!!))
            assertEquals(TierConfig.colorOf(id), TierConfig.colorAt(rank))
        }
        assertNull(TierConfig.idAt(-1))
        assertNull(TierConfig.idAt(TierConfig.size))
    }

    @Test
    fun unknownTierIsDeterministicAndSafe() {
        val unknown = "Not A Tier"
        assertFalse(TierConfig.contains(unknown))
        assertEquals(TierConfig.UNKNOWN_RANK, TierConfig.rankOf(unknown))
        assertEquals(TierConfig.UNKNOWN_COLOR, TierConfig.colorOf(unknown))
        assertEquals(TierConfig.UNKNOWN_COLOR and 0x00FFFFFF, TierConfig.rgbOf(unknown))
        // Unknown ranks below every known tier, and comparing two unknowns is stable.
        assertTrue(TierConfig.compareRanks(unknown, "F-") < 0)
        assertTrue(TierConfig.compareRanks("F-", unknown) > 0)
        assertEquals(0, TierConfig.compareRanks(unknown, "another unknown"))
        assertEquals(TierConfig.UNKNOWN_RANK, TierConfig.rankOf(""))
        assertEquals(TierConfig.UNKNOWN_COLOR, TierConfig.colorOf(""))
        assertTrue(TierConfig.of(unknown).rank == TierConfig.UNKNOWN_RANK)
        assertFalse(TierConfig.of(unknown).isKnown)
        assertTrue(TierConfig.of("Zenith").isKnown)
    }

    @Test
    fun externalCallersCannotMutateTheConfiguration() {
        val snapshot = TierConfig.ids
        var refused = false
        try {
            @Suppress("UNCHECKED_CAST")
            (snapshot as MutableList<String>)[0] = "MUTATED"
        } catch (expected: UnsupportedOperationException) {
            refused = true
        }
        assertTrue("TierConfig.ids must be unmodifiable", refused)
        assertEquals("F-", TierConfig.idAt(0))
        assertFalse(TierConfig.contains("MUTATED"))

        val again = TierConfig.ids
        again.forEachIndexed { index, id -> assertEquals(TierConfig.idAt(index), id) }
        assertEquals(authoritative.size, again.size)
    }

    @Test
    fun colourParsingRejectsMalformedInput() {
        assertEquals(0xFFFFFFFF.toInt(), TierConfig.parseHexColor("#fff"))
        assertEquals(0xFFFFFFFF.toInt(), TierConfig.parseHexColor("#FFFFFF"))
        assertEquals(0xFF102030.toInt(), TierConfig.parseHexColor("#102030"))
        var threw = false
        try {
            TierConfig.parseHexColor("888")
        } catch (expected: IllegalArgumentException) {
            threw = true
        }
        assertTrue("expected IllegalArgumentException for a missing '#'", threw)
    }

    private fun expectedArgb(hex: String): Int {
        val body = hex.substring(1)
        return if (body.length == 3) {
            val r = body[0].digitToInt(16) * 17
            val g = body[1].digitToInt(16) * 17
            val b = body[2].digitToInt(16) * 17
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        } else {
            (0xFF shl 24) or body.toInt(16)
        }
    }
}
