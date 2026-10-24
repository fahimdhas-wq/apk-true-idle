package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.ClockSnapshot
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.Derived
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.LootItem
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.OfflineLoot
import com.aistudio.trueidle.model.SaveMeta
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

object OfflineEngine {
    const val SPAWN_GAP_S = 0.5 // identical to live respawnLeft
    const val SAFETY = 1.25

    fun trustedAwayMs(meta: SaveMeta, now: ClockSnapshot, capMs: Long = 8 * 3600_000L): Long {
        val raw = when {
            meta.savedWallMs == 0L -> 0L // first launch
            meta.bootCount == now.bootCount && now.elapsedMs >= meta.savedElapsedMs ->
                now.elapsedMs - meta.savedElapsedMs // monotonic: immune to clock edits
            now.wallMs >= meta.savedWallMs ->
                minOf(now.wallMs - meta.savedWallMs, 10 * 60_000L) // rebooted: wall clock, capped at 10 min
            else -> return -1L // clock moved backwards
        }
        return minOf(raw, capMs)
    }

    private class FloorStats(val perKillS: Double, val survivable: Boolean, val m: Monster)

    private fun stats(f: Int, dps: Double, d: Derived, s: GameState): FloorStats {
        val m = Balance.generateMonsterForFloor(f)
        val effDps = dps * CombatMath.mitigation(m.armor)
        val ttk = (m.maxHp + m.maxShield) / maxOf(effDps, 1e-6)
        val incoming = CombatMath.incomingPerSec(m, d, s.sanctified.aegisActive)
        val ehp = d.maxHp + d.maxShield
        return FloorStats(ttk + SPAWN_GAP_S, ehp / incoming > ttk * s.player.killsRequired * SAFETY, m)
    }

    fun compute(s: GameState, d: Derived, awayMs: Long, efficiency: Double, rnd: Random): OfflineLoot {
        val capMs = 8 * 3600_000L
        val capApplied = awayMs >= capMs
        var remaining = awayMs / 1000.0 * efficiency
        val dps = CombatMath.totalDps(s, d)
        var floor = s.player.currentFloor
        val ceiling = s.player.maxUnlockedFloor // offline never unlocks new floors
        repeat(10) { if (floor > 1 && !stats(floor, dps, d, s).survivable) floor-- }
        var killsOnFloor = if (floor == s.player.currentFloor) s.player.killsOnFloor else 0
        var kills = 0L
        var gold = 0L
        var xp = 0L
        var gained = 0
        val perFloorKills = mutableListOf<Pair<Int, Int>>()
        var guard = 0

        fun award(f: Int, k: Int) {
            if (k <= 0) return
            val m = Balance.generateMonsterForFloor(f)
            kills += k
            gold += k * m.goldReward
            xp += k * m.xpReward
            perFloorKills += f to k
        }

        while (remaining > 0 && guard++ < 500) {
            val st = stats(floor, dps, d, s)
            val need = s.player.killsRequired - killsOnFloor
            val clearS = need * st.perKillS
            val advance = s.player.autoAdvance && floor < ceiling && stats(floor + 1, dps, d, s).survivable
            if (advance && remaining >= clearS) {
                award(floor, need)
                remaining -= clearS
                floor++
                killsOnFloor = 0
                gained++
            } else {
                val k = (remaining / st.perKillS).toInt()
                award(floor, k)
                remaining = 0.0
                killsOnFloor = (killsOnFloor + k) % s.player.killsRequired
            }
        }

        val lootRolls = binomial(rnd, kills.toInt(), d.lootChance)
        val ore = binomial(rnd, lootRolls, 0.5)
        val herb = lootRolls - ore
        val tamed = minOf(binomial(rnd, kills.toInt(), d.tameChance), (8 - s.beasts.beasts.size).coerceAtLeast(0))

        return OfflineLoot(
            timeAwaySeconds = awayMs / 1000,
            kills = kills,
            goldEarned = gold,
            xpEarned = xp,
            bonesEarned = kills * 2,
            playerLevelsGained = 0,
            floorsGained = gained,
            finalFloor = floor,
            efficiency = efficiency,
            capApplied = capApplied,
            items = listOfNotNull(
                LootItem("Copper Ore", ore).takeIf { ore > 0 },
                LootItem("Felweed", herb).takeIf { herb > 0 }
            ),
            beastsTamed = tamed.coerceAtLeast(0),
            farmingGainsSummary = if (herb > 0) "$herb Felweed gathered" else "No herbs gathered",
            miningGainsSummary = if (ore > 0) "$ore Copper Ore mined" else "No ore mined",
            dropsSummary = listOfNotNull(
                "$ore Copper Ore".takeIf { ore > 0 },
                "$herb Felweed".takeIf { herb > 0 }
            )
        )
    }

    fun binomial(r: Random, n: Int, p: Double): Int = when {
        n <= 0 || p <= 0.0 -> 0
        p >= 1.0 -> n
        n <= 30 -> (0 until n).count { r.nextDouble() < p }
        else -> {
            val u = r.nextDouble().coerceAtLeast(1e-12)
            val v = r.nextDouble()
            val z = sqrt(-2.0 * ln(u)) * cos(2.0 * PI * v)
            (n * p + sqrt(n * p * (1 - p)) * z).roundToInt().coerceIn(0, n)
        }
    }
}
