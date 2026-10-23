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
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.FloorControls
import com.aistudio.trueidle.ui.components.LogView
import com.aistudio.trueidle.ui.components.StatBar
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
            currentFloor = state.player.currentFloor,
            maxFloor = state.player.maxUnlockedFloor,
            autoAdvance = state.player.autoAdvance,
            killsOnFloor = state.player.killsOnFloor,
            killsRequired = state.player.killsRequired,
            onNavFloor = onNavFloor,
            onToggleAuto = onToggleAuto
        )

        // Combat Arena: Player Box vs Enemy Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Player Box
            Card(
                modifier = Modifier.weight(1f).testTag("player_combat_box"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                .background(PrimaryNeon.copy(alpha = 0.12f), RectangleShape)
                                .border(1.dp, PrimaryNeon.copy(alpha = 0.5f), RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LVL ${state.player.lvl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryNeon,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    StatBar(
                        label = "HP",
                        current = state.player.hp,
                        max = state.player.maxHp,
                        fillColor = HpRed,
                        tag = "player_hp_bar"
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    StatBar(
                        label = "SHIELD",
                        current = state.player.shield,
                        max = state.player.maxShield,
                        fillColor = ShieldBlue,
                        tag = "player_shield_bar"
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    StatBar(
                        label = "MANA",
                        current = state.player.mana,
                        max = state.player.maxMana,
                        fillColor = ManaTeal,
                        tag = "player_mana_bar"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Equipment Sync Slots
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF000000), RectangleShape)
                            .border(1.dp, BorderDark, RectangleShape)
                            .padding(5.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚔ ", fontSize = 10.sp, color = SecondaryGold)
                            Text(state.player.weaponSlot.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLight, maxLines = 1)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡 ", fontSize = 10.sp, color = ShieldBlue)
                            Text(state.player.armorSlot.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLight, maxLines = 1)
                        }
                    }
                }
            }

            // Enemy Box
            Card(
                modifier = Modifier.weight(1f).testTag("enemy_combat_box"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.dp, if (state.monster.isBoss) SecondaryGold.copy(alpha = 0.6f) else HpRed.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.monster.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.monster.isBoss) SecondaryGold else HpRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .background(if (state.monster.isBoss) SecondaryGold.copy(alpha = 0.15f) else HpRed.copy(alpha = 0.12f), RectangleShape)
                                .border(1.dp, if (state.monster.isBoss) SecondaryGold else HpRed, RectangleShape)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = state.monster.tag.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.monster.isBoss) SecondaryGold else HpRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    StatBar(
                        label = "HP",
                        current = state.monster.hp,
                        max = state.monster.maxHp,
                        fillColor = HpRed,
                        tag = "enemy_hp_bar"
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    StatBar(
                        label = "SHIELD",
                        current = state.monster.shield,
                        max = state.monster.maxShield,
                        fillColor = ShieldBlue,
                        tag = "enemy_shield_bar"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF000000), RectangleShape)
                            .border(1.dp, BorderDark, RectangleShape)
                            .padding(5.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "ATK: ${state.monster.damage.toInt()} DMG",
                            style = MaterialTheme.typography.labelSmall,
                            color = HpRed
                        )
                        Text(
                            text = "ARMOR: ${state.monster.armor.toInt()} DEF",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
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
                        state.necro.army.take(2).forEach { m ->
                            Text(
                                text = "› ${m.type.uppercase()} x${m.count} (${m.dps.toInt()} DPS)",
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
