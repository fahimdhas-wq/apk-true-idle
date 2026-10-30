package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.XpCurves

/** Overflow-safe progression maths shared by the engine and its systems. */
internal object ProgressOps {

    fun advanceProficiency(currentLvl: Int, currentXp: Long, gain: Long): Pair<Int, Long> {
        var lvl = currentLvl.coerceIn(1, 100)
        val safeCurrentXp = currentXp.coerceAtLeast(0L)
        val safeGain = gain.coerceAtLeast(0L)
        var xp = if (Long.MAX_VALUE - safeCurrentXp < safeGain) Long.MAX_VALUE else safeCurrentXp + safeGain

        while (lvl < 100 && xp >= XpCurves.proficiency(lvl)) {
            xp -= XpCurves.proficiency(lvl)
            lvl++
        }

        // A capped proficiency must not retain an unbounded overflow bank.
        if (lvl >= 100) xp = 0L
        return Pair(lvl, xp)
    }

    fun safeAddNonNegative(current: Double, amount: Double): Double {
        val base = when {
            current.isNaN() || current < 0.0 -> 0.0
            current.isInfinite() -> Double.MAX_VALUE
            else -> current
        }
        val increment = when {
            amount.isNaN() || amount < 0.0 -> 0.0
            amount.isInfinite() -> Double.MAX_VALUE
            else -> amount
        }
        val sum = base + increment
        return if (sum.isFinite()) sum else Double.MAX_VALUE
    }
}
