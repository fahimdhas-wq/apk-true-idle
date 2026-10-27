package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.ClockSnapshot
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.Derived
import com.aistudio.trueidle.core.NumberFormatter
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

/**
 * Offline (idle) simulation. Kept in step with the live engine: the same spawn gap, the same
 * kills-per-floor requirement, the same Soul Siphon bone multiplier and the same faith drain rate.
 */
object OfflineEngine {
    const val SAFETY = 1.25

    /** Aegis drain per second — deliberately equal to the live engine's Value in Balance. */
    const val AEGIS_DRAIN_PER_SEC = 0.5

    fun trustedAwayMs(meta: SaveMeta, now: ClockSnapshot, capMs: Long = Balance.OFFLINE_CAP_MS): Long {
        val raw = when {
            meta.savedWallMs == 0L -> 0L // first launch
            meta.bootCount == now.bootCount && now.elapsedMs >= meta.savedElapsedMs ->
                now.elapsedMs - meta.savedElapsedMs // monotonic: immune to clock edits
            now.wallMs >= meta.savedWallMs ->
                minOf(now.wallMs - meta.savedWallMs, WALL_FALLBACK_CAP_MS) // rebooted: wall clock, capped
            else -> return -1L // clock moved backwards
        }
        return minOf(raw, capMs)
    }

    /** When BOOT_COUNT is unavailable the wall-clock path is only trusted for this long. */
    const val WALL_FALLBACK_CAP_MS = 10 * 60_000L

    private class FloorStats(val perKillS: Double, val survivable: Boolean, val m: Monster)

    private fun stats(f: Int, dps: Double, d: Derived, s: GameState, aegisFraction: Double): FloorStats {
        val m = Balance.generateMonsterForFloor(f)
        val effDps = dps * CombatMath.mitigation(m.armor)
        val ttk = (m.maxHp + m.maxShield) / maxOf(effDps, 1e-6)
        // Aegis is only counted for the share of the window the faith actually pays for.
        val incoming = CombatMath.incomingPerSec(m, d, true) * aegisFraction +
            CombatMath.incomingPerSec(m, d, false) * (1.0 - aegisFraction)
        val ehp = d.maxHp + d.maxShield
        val regen = d.healthRegen + d.shieldRegen
        val killsRequired = m.killsRequired
        val fightS = (ttk + Balance.RESPAWN_GAP_SECONDS) * killsRequired
        val sustained = (ehp + regen * fightS) / incoming
        return FloorStats(ttk + Balance.RESPAWN_GAP_SECONDS, sustained > fightS * SAFETY, m)
    }

    fun compute(s: GameState, d: Derived, awayMs: Long, efficiency: Double, rnd: Random): OfflineLoot {
        val effectiveAwayMs = minOf(awayMs, Balance.OFFLINE_CAP_MS)
        val capApplied = awayMs >= Balance.OFFLINE_CAP_MS
        val awaySeconds = effectiveAwayMs / 1000.0
        var remaining = awaySeconds * efficiency
        val dps = CombatMath.totalDps(s, d)
        val startFloor = s.player.currentFloor
        var floor = s.player.currentFloor
        val ceiling = s.player.maxUnlockedFloor // offline never unlocks new floors

        // Faith is spent while away, so Aegis only covers part of the window.
        val faithPreserveRank = s.essence.upgrades.firstOrNull { it.id == "faith_preserve" }?.rank ?: 0
        val drainMult = (1.0 - 0.15 * faithPreserveRank).coerceAtLeast(0.2)
        val drainPerSec = AEGIS_DRAIN_PER_SEC * drainMult
        val aegisSeconds = if (s.essence.aegisActive && drainPerSec > 0.0) {
            minOf(awaySeconds, s.essence.faith / drainPerSec)
        } else 0.0
        val aegisFraction = if (awaySeconds > 0.0) (aegisSeconds / awaySeconds).coerceIn(0.0, 1.0) else 0.0

        repeat(10) { if (floor > 1 && !stats(floor, dps, d, s, aegisFraction).survivable) floor-- }
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
            val st = stats(floor, dps, d, s, aegisFraction)
            // Clamp: a floor that is already complete (imported save, floor mismatch) must not
            // subtract negative clearance time and walk the loop backwards.
            val required = Balance.killsRequiredFor(floor)
            val need = (required - killsOnFloor).coerceAtLeast(0)
            if (need == 0) {
                if (floor >= ceiling) break
                floor++
                killsOnFloor = 0
                continue
            }
            val clearS = need * st.perKillS
            val advance = s.player.autoAdvance && floor < ceiling &&
                stats(floor + 1, dps, d, s, aegisFraction).survivable
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
                killsOnFloor = (killsOnFloor + k) % required
            }
        }

        val lootRolls = binomial(rnd, kills.toInt(), d.lootChance)
        val ore = binomial(rnd, lootRolls, 0.5)
        val herb = lootRolls - ore
        val tamed = minOf(binomial(rnd, kills.toInt(), d.tameChance), (Balance.BEAST_CAP - s.beasts.beasts.size).coerceAtLeast(0))

        // Soul Siphon applies offline exactly as it does live.
        val boneMult = 1.0 + 0.25 * (s.necro.upgrades.firstOrNull { it.id == "necro_harvest" }?.rank ?: 0)
        val bones = (kills * 2.0 * boneMult).toLong()

        val effectiveHours = (effectiveAwayMs / 3600_000.0).coerceAtLeast(0.01)
        val kPerHour = (kills / effectiveHours).toLong()
        val gPerHour = (gold / effectiveHours).toLong()
        val xPerHour = (xp / effectiveHours).toLong()

        // Real per-floor data instead of a fabricated two-hour narrative.
        val timeline = buildList {
            add(
                "AWAY ${NumberFormatter.duration(effectiveAwayMs / 1000)} AT " +
                    "${(efficiency * 100).toInt()}% EFFICIENCY"
            )
            perFloorKills.takeLast(5).forEach { (f, k) ->
                add("SECTOR $f // $k hostiles neutralized")
            }
            if (perFloorKills.size > 5) {
                add("... and ${perFloorKills.size - 5} earlier sector engagement(s)")
            }
            if (gained > 0) add("ADVANCED $gained SECTOR(S): $startFloor -> $floor")
            else add("HELD SECTOR $floor with no fatal breaches")
            add("SECURED +$gold CREDITS, +$xp XP, +$bones BONE ESSENCE")
            if (capApplied) add("${Balance.OFFLINE_CAP_MS / 3600_000L}-HOUR CAP ENGAGED")
        }

        return OfflineLoot(
            timeAwaySeconds = effectiveAwayMs / 1000,
            kills = kills,
            goldEarned = gold,
            xpEarned = xp,
            bonesEarned = bones,
            playerLevelsGained = 0,
            floorsGained = gained,
            startFloor = startFloor,
            finalFloor = floor,
            killsOnFloor = killsOnFloor,
            efficiency = efficiency,
            capApplied = capApplied,
            killsPerHour = kPerHour,
            goldPerHour = gPerHour,
            xpPerHour = xPerHour,
            items = listOfNotNull(
                LootItem(ItemCatalog.COPPER_ORE, ore).takeIf { ore > 0 },
                LootItem(ItemCatalog.FELWEED, herb).takeIf { herb > 0 }
            ),
            beastsTamed = tamed.coerceAtLeast(0),
            farmingGainsSummary = if (herb > 0) "$herb ${ItemCatalog.FELWEED} gathered" else "No herbs gathered",
            miningGainsSummary = if (ore > 0) "$ore ${ItemCatalog.COPPER_ORE} mined" else "No ore mined",
            dropsSummary = listOfNotNull(
                "$ore ${ItemCatalog.COPPER_ORE}".takeIf { ore > 0 },
                "$herb ${ItemCatalog.FELWEED}".takeIf { herb > 0 }
            ),
            timelineLog = timeline
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
