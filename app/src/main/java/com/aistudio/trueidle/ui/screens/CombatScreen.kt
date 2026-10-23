package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Player Box
            Card(
                modifier = Modifier.weight(1f).testTag("player_combat_box"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hero",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryNeon.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
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
                    Spacer(modifier = Modifier.height(4.dp))
                    StatBar(
                        label = "Shield",
                        current = state.player.shield,
                        max = state.player.maxShield,
                        fillColor = ShieldBlue,
                        tag = "player_shield_bar"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatBar(
                        label = "Mana",
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
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceVariantDark)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚔️ ", fontSize = 11.sp)
                            Text(state.player.weaponSlot, style = MaterialTheme.typography.labelSmall, color = TextLight, maxLines = 1)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️ ", fontSize = 11.sp)
                            Text(state.player.armorSlot, style = MaterialTheme.typography.labelSmall, color = TextLight, maxLines = 1)
                        }
                    }
                }
            }

            // Enemy Box
            Card(
                modifier = Modifier.weight(1f).testTag("enemy_combat_box"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.monster.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.monster.isBoss) SecondaryGold else HpRed,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (state.monster.isBoss) SecondaryGold.copy(alpha = 0.2f) else HpRed.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = state.monster.tag,
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
                    Spacer(modifier = Modifier.height(4.dp))
                    StatBar(
                        label = "Shield",
                        current = state.monster.shield,
                        max = state.monster.maxShield,
                        fillColor = ShieldBlue,
                        tag = "enemy_shield_bar"
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceVariantDark)
                            .padding(6.dp),
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Beasts
            Card(
                modifier = Modifier.weight(1f).testTag("combat_beasts_summary"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "🐾 Beasts (${state.beasts.beasts.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (state.beasts.beasts.isEmpty()) {
                        Text("No active beasts", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    } else {
                        state.beasts.beasts.take(2).forEach { b ->
                            Text(
                                text = "• ${b.name} ⭐${b.stars} (${b.dps.toInt()} DPS)",
                                style = MaterialTheme.typography.labelSmall,
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
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "💀 Undead Army",
                        style = MaterialTheme.typography.labelSmall,
                        color = NecroPurple,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (state.necro.army.isEmpty()) {
                        Text("No minions summoned", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    } else {
                        state.necro.army.take(2).forEach { m ->
                            Text(
                                text = "• ${m.type} x${m.count} (${m.dps.toInt()} DPS)",
                                style = MaterialTheme.typography.labelSmall,
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
