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

    val d = DerivedStats.of(state)
    val playerDps = CombatMath.heroDps(d)
    val beastDps = CombatMath.beastDps(state.beasts)
    val armyDps = CombatMath.armyDps(state.necro)
    val totalDps = CombatMath.totalDps(state, d)

    val weapon = state.inventory.firstOrNull { it.isEquipped && it.slot == "Weapon" }
    val armor = state.inventory.firstOrNull { it.isEquipped && it.slot == "Armor" }
    val ring = state.inventory.firstOrNull { it.isEquipped && it.slot == "Ring" }
    val amulet = state.inventory.firstOrNull { it.isEquipped && it.slot == "Amulet" }

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

        // 1. EQUIPMENT 2x2 GEAR GRID (Direct from HTML preview)
        SciFiPanel(
            title = "EQUIPMENT",
            accent = PrimaryNeon,
            tag = "GS ${d.gearScore}"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Top Row: Weapon & Armor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Weapon Cell
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceVariantDark, RectangleShape)
                            .border(BorderStroke(1.dp, BorderDark), RectangleShape)
                            .padding(8.dp)
                    ) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(PrimaryNeon))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("WEAPON", style = TrueIdleType.meta, color = TextMuted)
                            Text(
                                text = (weapon?.name ?: p.weaponSlot).uppercase(),
                                style = TrueIdleType.label,
                                color = TextLight,
                                maxLines = 1
                            )
                            Text(
                                text = "DMG +${d.weaponDmg.toInt()}",
                                style = TrueIdleType.tag,
                                color = PrimaryNeon
                            )
                        }
                    }

                    // Armor Cell
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceVariantDark, RectangleShape)
                            .border(BorderStroke(1.dp, BorderDark), RectangleShape)
                            .padding(8.dp)
                    ) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(ShieldBlue))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ARMOR", style = TrueIdleType.meta, color = TextMuted)
                            Text(
                                text = (armor?.name ?: p.armorSlot).uppercase(),
                                style = TrueIdleType.label,
                                color = TextLight,
                                maxLines = 1
                            )
                            Text(
                                text = "DEF +${d.armorDef.toInt()}",
                                style = TrueIdleType.tag,
                                color = ShieldBlue
                            )
                        }
                    }
                }

                // Bottom Row: Ring & Amulet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ring Cell
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceVariantDark, RectangleShape)
                            .border(BorderStroke(1.dp, BorderDark), RectangleShape)
                            .padding(8.dp)
                    ) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(SecondaryGold))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("RING", style = TrueIdleType.meta, color = TextMuted)
                            Text(
                                text = (ring?.name ?: "EMBER BAND").uppercase(),
                                style = TrueIdleType.label,
                                color = TextLight,
                                maxLines = 1
                            )
                            Text(
                                text = ring?.statDesc ?: "CRIT +2.1%",
                                style = TrueIdleType.tag,
                                color = SecondaryGold
                            )
                        }
                    }

                    // Amulet Cell
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceVariantDark, RectangleShape)
                            .border(BorderStroke(1.dp, BorderDark), RectangleShape)
                            .padding(8.dp)
                    ) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(NecroPurple))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("AMULET", style = TrueIdleType.meta, color = TextMuted)
                            Text(
                                text = (amulet?.name ?: "WARDEN SIGIL").uppercase(),
                                style = TrueIdleType.label,
                                color = TextLight,
                                maxLines = 1
                            )
                            Text(
                                text = amulet?.statDesc ?: "VIT +18",
                                style = TrueIdleType.tag,
                                color = NecroPurple
                            )
                        }
                    }
                }
            }
        }

        // 2. DERIVED STATS CARD (With Colored Indicator Dots as in HTML)
        SciFiPanel(
            title = "DERIVED STATS",
            accent = SecondaryGold,
            tag = "LVL ${p.lvl}"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Strength
                StatDotRow(label = "STRENGTH", value = "${d.str.toInt()}", dotColor = PrimaryNeon, valueColor = PrimaryNeon)
                // Intelligence
                StatDotRow(label = "INTELLIGENCE", value = "${d.intVal.toInt()}", dotColor = SecondaryGold, valueColor = SecondaryGold)
                // Vitality
                StatDotRow(label = "VITALITY", value = "${d.vit.toInt()}", dotColor = GreenSuccess, valueColor = GreenSuccess)
                // Defense
                StatDotRow(label = "DEFENSE", value = "${d.armorDef.toInt()}", dotColor = NecroPurple, valueColor = NecroPurple)
                // Physical Damage
                StatDotRow(label = "PHYSICAL DAMAGE", value = "${d.physicalDamage.toInt()}", dotColor = null, valueColor = TextLight)
                // Crit Chance
                StatDotRow(label = "CRIT CHANCE", value = String.format(Locale.ROOT, "%.1f%%", d.critChance * 100), dotColor = null, valueColor = TextLight)
                // Crit Damage
                StatDotRow(label = "CRIT DAMAGE", value = "${(d.critDamage * 100).toInt()}%", dotColor = null, valueColor = SecondaryGold)
                // XP Gain
                StatDotRow(label = "XP GAIN", value = "${(d.xpGain * 100).toInt()}%", dotColor = null, valueColor = TextLight)
            }
        }

        // 3. COMBAT POWER BREAKDOWN
        SciFiPanel(
            title = "COMBAT TELEMETRY",
            accent = PrimaryNeon,
            tag = "COMBINED: ${NumberFormatter.short(totalDps)} DPS"
        ) {
            listOf(
                "HERO SOLO DPS" to "${NumberFormatter.short(playerDps)} DPS",
                "BEAST COMPANIONS DPS" to "${NumberFormatter.short(beastDps)} DPS (${state.beasts.beasts.size} BEASTS)",
                "UNDEAD LEGION DPS" to "${NumberFormatter.short(armyDps)} DPS (${state.necro.army.sumOf { it.count }} UNITS)",
                "ARMOR MITIGATION" to "${d.armorDef.toInt()} DEF (${(100 - (CombatMath.mitigation(d.armorDef) * 100)).toInt()}% REDUCTION)"
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

        // 4. LIFETIME TACTICAL TELEMETRY
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

@Composable
fun StatDotRow(
    label: String,
    value: String,
    dotColor: Color?,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, BorderDark))
            .background(Color(0xFF000000))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(dotColor)
                )
            }
            Text(label, style = TrueIdleType.label, color = TextMuted)
        }
        Text(value, style = TrueIdleType.value, color = valueColor)
    }
}
