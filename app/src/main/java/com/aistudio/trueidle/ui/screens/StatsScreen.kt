package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*
import java.util.Locale

@Composable
fun StatsScreen(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val p = state.player
    val reqXp = Balance.playerXpReq(p.lvl)

    // D7 Fix: Authoritative single source of truth from DerivedStats and CombatMath
    val d = DerivedStats.of(state)
    val playerDps = CombatMath.heroDps(d)
    val beastDps = CombatMath.beastDps(state.beasts)
    val armyDps = CombatMath.armyDps(state.necro)
    val totalDps = CombatMath.totalDps(state, d)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(10.dp)
            .testTag("stats_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Header Card with XP Bar
        Card(
            modifier = Modifier.fillMaxWidth().testTag("stats_header_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HERO PROTOCOL",
                        style = TrueIdleType.title,
                        color = PrimaryNeon
                    )
                    Box(
                        modifier = Modifier
                            .background(SecondaryGold.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, SecondaryGold, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LEVEL ${p.lvl}",
                            style = TrueIdleType.tag,
                            color = SecondaryGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                XpProgressBar(
                    skillName = "HERO EXPERIENCE",
                    level = p.lvl,
                    currentXp = p.xp,
                    requiredXp = reqXp,
                    barColor = PrimaryNeon
                )
            }
        }

        // Real-Time Total Combat Power & DPS Matrix
        SciFiPanel(
            title = "COMBAT TELEMETRY",
            accent = PrimaryNeon,
            tag = "COMBINED: ${NumberFormatter.short(totalDps)} DPS"
        ) {
            listOf(
                "HERO SOLO DPS" to "${NumberFormatter.short(playerDps)} DPS (STR + WEAPON)",
                "BEAST COMPANIONS DPS" to "${NumberFormatter.short(beastDps)} DPS (${state.beasts.beasts.size} BEASTS)",
                "UNDEAD LEGION DPS" to "${NumberFormatter.short(armyDps)} DPS (${state.necro.army.sumOf { it.count }} UNITS)",
                "EFFECTIVE ARMOR DEFENSE" to "${d.armorDef.toInt()} DEF (${(d.armorDef * 2).toInt()}% MITIGATION)"
            ).forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = TrueIdleType.label, color = TextMuted)
                    Text(value, style = TrueIdleType.value, color = TextLight)
                }
            }
        }

        // Core Attributes Card (Derived)
        SciFiPanel(
            title = "CORE ATTRIBUTES",
            accent = SecondaryGold,
            tag = "ATTRIBUTES"
        ) {
            listOf(
                "STRENGTH (STR)" to "${d.str.toInt()} (+${(d.str * 1.2).toInt()} DPS)",
                "INTELLIGENCE (INT)" to "${d.intVal.toInt()} (+${d.maxMana.toInt()} MAX MANA)",
                "VITALITY (VIT)" to "${d.vit.toInt()} (+${d.maxHp.toInt()} MAX HP)",
                "LUCK INDEX" to "${(d.luck).toInt()}% (${(d.lootChance * 100).toInt()}% LOOT RATE)",
                "ATTACK FREQUENCY" to "${String.format(Locale.ROOT, "%.2f", d.atkSpeed)}x MULTIPLIER"
            ).forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = TrueIdleType.label, color = TextMuted)
                    Text(value, style = TrueIdleType.value, color = PrimaryNeon)
                }
            }
        }

        // Equipped Gear
        SciFiPanel(
            title = "EQUIPPED ARSENAL",
            accent = ShieldBlue,
            tag = "GEAR"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚔ MAIN WEAPON", style = TrueIdleType.label, color = TextMuted)
                Text("${p.weaponSlot.uppercase()} (+${d.weaponDmg.toInt()} ATK)", style = TrueIdleType.value, color = SecondaryGold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛡 ARMOR PLATING", style = TrueIdleType.label, color = TextMuted)
                Text("${p.armorSlot.uppercase()} (+${d.armorDef.toInt()} DEF)", style = TrueIdleType.value, color = ShieldBlue)
            }
        }

        // Lifetime Telemetry
        SciFiPanel(
            title = "TACTICAL TELEMETRY",
            accent = TextLight,
            tag = "LIFETIME"
        ) {
            listOf(
                "TOTAL HOSTILES ELIMINATED" to "${state.lifetime.totalKills}",
                "DEEPEST SECTOR UNLOCKED" to "SECTOR ${p.maxUnlockedFloor}",
                "CREDITS IN TREASURY" to "${NumberFormatter.short(p.gold)} 🪙",
                "FAITH ACCUMULATED" to "${NumberFormatter.short(state.sanctified.faith)} 🕊",
                "SLAYER POINTS" to "${state.slayer.slayerPoints} ⚔",
                "TAMED COMPANIONS" to "${state.beasts.beasts.size}",
                "UNDEAD CORPS SIZE" to "${state.necro.army.sumOf { it.count }}"
            ).forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = TrueIdleType.label, color = TextMuted)
                    Text(value, style = TrueIdleType.value, color = TextLight)
                }
            }
        }
    }
}
