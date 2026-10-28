package com.aistudio.trueidle.core

/**
 * Canonical, immutable tier configuration: one authoritative table for every tier identifier,
 * its rank, and its colour.
 *
 * Design notes (why it looks like this):
 *
 * - **One source of truth.** The ordered [TIER_IDS] array *is* the canonical progression. Rank is
 *   the array index, so rank order can never disagree with the table. A separate colour table keeps
 *   "what colour" independent from "which rank", so nothing infers rank from a colour string.
 * - **Colours are parsed exactly once**, at class initialisation, into Android-style ARGB ints
 *   (`0xFFRRGGBB`). Steady-state lookups never parse, format or normalise anything.
 * - **Lookups are allocation-free.** [rankOf] / [colorOf] / [of] only hash the caller's already
 *   interned id and read an array slot; no temporaries, no defensive copies, no locks. Only the
 *   initialisation path allocates.
 * - **Unknown ids are explicit and safe.** The game can load a save written by a build that knew a
 *   tier this build does not (or a hand-edited save). Rather than throwing, every accessor maps an
 *   unknown id onto [UNKNOWN_RANK] / [UNKNOWN_COLOR], and rank comparisons order unknown ids
 *   *below* every known tier. See [of], [rankOf] and [compareRanks].
 * - **No Android dependency.** `android.graphics.Color.parseColor` is avoided deliberately: `core`
 *   is a pure Kotlin module (see `ArchitectureTest`), and parsing by hand keeps the table testable
 *   on the JVM with no shadow implementation.
 *
 * Hex parsing matches `android.graphics.Color.parseColor` / CSS for the forms used here:
 * `#rgb` expands each nibble (`#888` -> `0xFF888888`) and `#rrggbb` is taken verbatim with an opaque
 * alpha channel.
 */
object TierConfig {

    /** Rank reported for an identifier that is not part of the canonical table. */
    const val UNKNOWN_RANK: Int = -1

    /** Colour reported for an identifier that is not part of the canonical table (muted grey). */
    const val UNKNOWN_COLOR: Int = 0xFF8899AC.toInt()

    /**
     * Canonical progression, weakest first. This ordering is the rank order: `F-` is rank 0 and
     * `Infinite EX` is the last rank. Order is defined here explicitly and is never derived from a
     * map's iteration order, a colour, or an alphabetical sort.
     */
    private val TIER_IDS: Array<String> = arrayOf(
        "F-", "F", "F+",
        "E-", "E", "E+",
        "D-", "D", "D+",
        "C-", "C", "C+",
        "B-", "B", "B+",
        "A-", "A", "A+",
        "S-", "S", "S+",
        "SS-", "SS", "SS+",
        "SSS-", "SSS", "SSS+",
        "SSR", "SSR+",
        "UR", "UR+", "URR",
        "EX", "EX+",
        "Transcendent", "Transcendent+", "Transcendent++", "Transcendent+++", "Transcendent EX",
        "Ethereal", "Ethereal+", "Ethereal++", "Ethereal+++", "Ethereal EX",
        "Cosmic", "Cosmic+", "Cosmic++", "Cosmic+++", "Cosmic EX",
        "Omni", "Omni+", "Omni++", "Omni+++", "Omni EX",
        "Singularity", "Singularity+", "Singularity++", "Singularity+++", "Singularity EX",
        "Continuum", "Continuum+", "Continuum++", "Continuum+++", "Continuum EX",
        "Paradox", "Paradox+", "Paradox++", "Paradox+++", "Paradox EX",
        "Reality", "Reality+", "Reality++", "Reality+++", "Reality EX",
        "Chronos", "Chronos+", "Chronos++", "Chronos+++", "Chronos EX",
        "Eternal", "Eternal+", "Eternal++", "Eternal+++", "Eternal EX",
        "Absolute", "Absolute+", "Absolute++", "Absolute+++", "Absolute EX",
        "Omega", "Omega+", "Omega++", "Omega+++", "Omega EX",
        "Zenith", "Zenith+", "Zenith++", "Zenith+++", "Zenith EX",
        "Apotheosis", "Apotheosis+", "Apotheosis++", "Apotheosis+++", "Apotheosis EX",
        "Infinite", "Infinite+", "Infinite++", "Infinite+++", "Infinite EX"
    )

    /** Authoritative identifier -> source hex string, exactly as specified. */
    private val TIER_HEX: Map<String, String> = linkedMapOf(
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

    /** Precomputed ARGB colour per rank, parallel to [TIER_IDS]. */
    private val COLORS_BY_RANK: IntArray

    /** Precomputed RGB (no alpha) colour per rank, parallel to [TIER_IDS]. */
    private val RGB_BY_RANK: IntArray

    /** Direct id -> rank index. Immutable after initialisation; never returned to callers. */
    private val RANK_BY_ID: Map<String, Int>

    /** Unmodifiable view handed to callers; never the internal array itself. */
    private val IDS_VIEW: List<String> = java.util.Collections.unmodifiableList(TIER_IDS.toList())

    /** Number of tiers in the canonical table. */
    val size: Int = TIER_IDS.size

    init {
        require(TIER_IDS.size == TIER_HEX.size) {
            "tier table mismatch: ${TIER_IDS.size} ids vs ${TIER_HEX.size} colours"
        }
        require(TIER_IDS.toSet().size == TIER_IDS.size) { "duplicate tier identifier" }

        COLORS_BY_RANK = IntArray(TIER_IDS.size)
        RGB_BY_RANK = IntArray(TIER_IDS.size)
        val rankById = HashMap<String, Int>(TIER_IDS.size * 2)
        for (rank in TIER_IDS.indices) {
            val id = TIER_IDS[rank]
            val hex = TIER_HEX[id] ?: error("missing colour for tier '$id'")
            val argb = parseHexColor(hex)
            COLORS_BY_RANK[rank] = argb
            RGB_BY_RANK[rank] = argb and 0x00FFFFFF
            rankById[id] = rank
        }
        RANK_BY_ID = rankById
    }

    /** Canonical ids in rank order, as an unmodifiable copy: callers cannot mutate the table. */
    val ids: List<String>
        get() = IDS_VIEW

    /** Rank of [id], or [UNKNOWN_RANK]. Allocation-free. */
    fun rankOf(id: String): Int = RANK_BY_ID[id] ?: UNKNOWN_RANK

    /** Whether [id] is part of the canonical table. Allocation-free. */
    fun contains(id: String): Boolean = RANK_BY_ID.containsKey(id)

    /** ARGB colour of [id], or [UNKNOWN_COLOR]. Allocation-free. */
    fun colorOf(id: String): Int {
        val rank = RANK_BY_ID[id] ?: return UNKNOWN_COLOR
        return COLORS_BY_RANK[rank]
    }

    /** RGB colour (alpha stripped) of [id], or `UNKNOWN_COLOR and 0x00FFFFFF`. Allocation-free. */
    fun rgbOf(id: String): Int {
        val rank = RANK_BY_ID[id] ?: return UNKNOWN_COLOR and 0x00FFFFFF
        return RGB_BY_RANK[rank]
    }

    /** ARGB colour at [rank], or [UNKNOWN_COLOR] when the rank is out of range. Allocation-free. */
    fun colorAt(rank: Int): Int =
        if (rank in COLORS_BY_RANK.indices) COLORS_BY_RANK[rank] else UNKNOWN_COLOR

    /** Canonical id at [rank], or `null` when the rank is out of range. Allocation-free. */
    fun idAt(rank: Int): String? =
        if (rank in TIER_IDS.indices) TIER_IDS[rank] else null

    /**
     * Rank comparison using the canonical progression: negative when [a] ranks below [b], positive
     * when it ranks above, `0` when both have the same rank (or both are unknown). Never compares
     * strings alphabetically, never allocates.
     */
    fun compareRanks(a: String, b: String): Int = rankOf(a).compareTo(rankOf(b))

    /** True when [a] is a strictly higher tier than [b]. Allocation-free. */
    fun isHigher(a: String, b: String): Boolean = rankOf(a) > rankOf(b)

    /** Convenience for the (id, rank, colour) triple, used by UI and tests. */
    fun of(id: String): TierInfo = TierInfo(id, rankOf(id), colorOf(id))

    /**
     * Parses `#rgb` or `#rrggbb` into `0xFFRRGGBB`. Only called during initialisation, never from a
     * lookup path. Throws [IllegalArgumentException] on malformed input so a bad table fails loudly
     * at startup instead of silently rendering an invisible colour.
     */
    internal fun parseHexColor(hex: String): Int {
        require(hex.length == 4 || hex.length == 7) { "unsupported colour literal '$hex'" }
        require(hex[0] == '#') { "colour literal must start with '#': '$hex'" }
        return if (hex.length == 4) {
            val r = hexDigit(hex[1])
            val g = hexDigit(hex[2])
            val b = hexDigit(hex[3])
            // Each nibble is duplicated, so #888 -> 0x88 and #f0a -> 0xFF00AA.
            0xFF shl 24 or (r * 17 shl 16) or (g * 17 shl 8) or (b * 17)
        } else {
            var value = 0
            for (i in 1..6) {
                value = (value shl 4) or hexDigit(hex[i])
            }
            0xFF shl 24 or value
        }
    }

    private fun hexDigit(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> throw IllegalArgumentException("invalid hex digit '$c'")
    }
}

/** Immutable value returned by [TierConfig.of]. */
data class TierInfo(
    val id: String,
    val rank: Int,
    val color: Int
) {
    val isKnown: Boolean get() = rank != TierConfig.UNKNOWN_RANK
}
