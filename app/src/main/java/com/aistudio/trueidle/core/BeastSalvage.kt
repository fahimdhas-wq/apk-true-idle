package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.Beast

/** Salvage yield for a tamed beast: Bone Essence for the Necromancy tab plus a Credit refund. */
object BeastSalvage {
    private const val MAX_STAR_SHIFT = 30

    /** Doubles per star so merged beasts always salvage for more than the beasts that formed them. */
    fun bones(beast: Beast): Long {
        val shift = (beast.stars - 1).coerceIn(0, MAX_STAR_SHIFT)
        return (5L + 3L * beast.rank.coerceAtLeast(1)) shl shift
    }

    fun gold(beast: Beast): Long {
        val stars = beast.stars.coerceAtLeast(1).toLong()
        return 50L * stars * beast.rank.coerceAtLeast(1)
    }

    fun totalBones(beasts: List<Beast>): Long = beasts.fold(0L) { acc, b -> saturatingAdd(acc, bones(b)) }

    fun totalGold(beasts: List<Beast>): Long = beasts.fold(0L) { acc, b -> saturatingAdd(acc, gold(b)) }

    fun saturatingAdd(a: Long, b: Long): Long =
        if (Long.MAX_VALUE - a < b) Long.MAX_VALUE else a + b
}
