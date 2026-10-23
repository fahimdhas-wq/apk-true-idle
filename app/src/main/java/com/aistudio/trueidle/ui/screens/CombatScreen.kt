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
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.FloorControls
import com.aistudio.trueidle.ui.components.LogView
import com.aistudio.trueidle.ui.components.StatBar
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*

@Composable
fun CombatScreen(
    state: GameState,
    logFilter: String,
    onNavFloor: (Int) -> Unit,
    onToggleAuto: () -> Unit,
    onSelectLogFilter: (String) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val p = state.player
    val m = state.monster
    val reqXp = Balance.playerXpReq(p.lvl)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Floor Controls
        FloorControls(
            currentFloor = p.currentFloor,
            maxFloor = p.maxUnlockedFloor,
            autoAdvance = p.autoAdvance,
            killsOnFloor = p.killsOnFloor,
            killsRequired = p.killsRequired,
            onNavFloor = onNavFloor,
            onToggleAuto = onToggleAuto
        )

        // Hero Player XP Bar on Combat Screen
        Card(
            modifier = Modifier.fillMaxWidth().testTag("combat_player_xp_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                XpProgressBar(
                    skillName = "HERO PROGRESSION",
                    level = p.lvl,
                    currentXp = p.xp,
                    requiredXp = reqXp,
                    barColor = PrimaryNeon
                )
            }
        }

        // Combat Arena: Perfectly Symmetrical Player Box vs Enemy Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Player Box (Symmetrical Layout)
            Card(
                modifier = Modifier.weight(1f).testTag("player_combat_box"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HERO",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                                .border(1.dp, PrimaryNeon, RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LVL ${p.lvl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryNeon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Stat Bars
                    StatBar(
                        label = "HP",
                        current = p.hp,
                        max = p.maxHp,
                        fillColor = HpRed,
                        tag = "player_hp_bar"
                    )
                    StatBar(
                        label = "SHIELD",
                        current = p.shield,
                        max = p.maxShield,
                        fillColor = ShieldBlue,
                        tag = "player_shield_bar"
                    )
                    StatBar(
                        label = "MANA",
                        current = p.mana,
                        max = p.maxMana,
                        fillColor = ManaTeal,
                        tag = "player_mana_bar"
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Equipment Telemetry (Fixed Height for symmetry)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color(0xFF000000), RectangleShape)
                            .border(1.dp, BorderDark, RectangleShape)
                            .padding(4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚔ ", fontSize = 9.sp, color = SecondaryGold)
                            Text(p.weaponSlot.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLight, maxLines = 1, fontSize = 9.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡 ", fontSize = 9.sp, color = ShieldBlue)
                            Text(p.armorSlot.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLight, maxLines = 1, fontSize = 9.sp)
                        }
                    }
                }
            }

            // Enemy Box (Symmetrical Layout matching Player Box)
            Card(
                modifier = Modifier.weight(1f).testTag("enemy_combat_box"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.dp, if (m.isBoss) SecondaryGold.copy(alpha = 0.6f) else HpRed.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = m.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (m.isBoss) SecondaryGold else HpRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .background(if (m.isBoss) SecondaryGold.copy(alpha = 0.15f) else HpRed.copy(alpha = 0.15f), RectangleShape)
                                .border(1.dp, if (m.isBoss) SecondaryGold else HpRed, RectangleShape)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = m.tag.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (m.isBoss) SecondaryGold else HpRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Stat Bars (Matching 3 rows for uniform height)
                    StatBar(
                        label = "HP",
                        current = m.hp,
                        max = m.maxHp,
                        fillColor = HpRed,
                        tag = "enemy_hp_bar"
                    )
                    StatBar(
                        label = "SHIELD",
                        current = m.shield,
                        max = m.maxShield.coerceAtLeast(1.0),
                        fillColor = ShieldBlue,
                        tag = "enemy_shield_bar"
                    )
                    StatBar(
                        label = "ARMOR RESIST",
                        current = m.armor,
                        max = (m.armor * 2.0).coerceAtLeast(10.0),
                        fillColor = SecondaryGold,
                        tag = "enemy_armor_bar"
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Hostile Specs (Fixed Height for symmetry)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color(0xFF000000), RectangleShape)
                            .border(1.dp, BorderDark, RectangleShape)
                            .padding(4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ATK: ${m.damage.toInt()} DMG",
                            style = MaterialTheme.typography.labelSmall,
                            color = HpRed,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "DEF: ${m.armor.toInt()} DEF",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // Active Summons Row (Beasts & Undead Army)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Beasts
            Card(
                modifier = Modifier.weight(1f).testTag("combat_beasts_summary"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "🐾 BEASTS [${state.beasts.beasts.size}/8]",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenSuccess,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (state.beasts.beasts.isEmpty()) {
                        Text("No active beasts", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    } else {
                        state.beasts.beasts.take(2).forEach { b ->
                            Text(
                                text = "› ${b.name.uppercase()} ⭐${b.stars} (${b.dps.toInt()} DPS)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Undead Army
            Card(
                modifier = Modifier.weight(1f).testTag("combat_army_summary"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "💀 UNDEAD ARMY",
                        style = MaterialTheme.typography.labelSmall,
                        color = NecroPurple,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (state.necro.army.isEmpty()) {
                        Text("No minions active", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    } else {
                        state.necro.army.take(2).forEach { minion ->
                            Text(
                                text = "› ${minion.type.uppercase()} x${minion.count} (${minion.dps.toInt()} DPS)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Combat Logs
        LogView(
            logs = state.logs,
            currentFilter = logFilter,
            onFilterSelect = onSelectLogFilter,
            onClear = onClearLogs
        )
    }
}
