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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.*
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

        // Real-Time Combat DPS Matrix Telemetry Card
        CombatTelemetryCard(state = state)

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
                    // Header Row (Fixed 24.dp height)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HERO",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                                .border(1.dp, PrimaryNeon, RectangleShape)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LVL ${p.lvl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryNeon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }

                    // Stat Bars (HP, SHIELD, MANA)
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

                    // Equipment Telemetry (Fixed Height for exact symmetry)
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
                            Text("⚔ ", fontSize = 8.sp, color = SecondaryGold)
                            Text(
                                text = p.weaponSlot.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 8.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡 ", fontSize = 8.sp, color = ShieldBlue)
                            Text(
                                text = p.armorSlot.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 8.sp
                            )
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
                    // Header Row (Fixed 24.dp height)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = m.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (m.isBoss) SecondaryGold else HpRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(if (m.isBoss) SecondaryGold.copy(alpha = 0.15f) else HpRed.copy(alpha = 0.15f), RectangleShape)
                                .border(1.dp, if (m.isBoss) SecondaryGold else HpRed, RectangleShape)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = m.tag.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (m.isBoss) SecondaryGold else HpRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }

                    // Stat Bars (Matching 3 rows for uniform height: HP, SHIELD, ARMOR RESIST)
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
                        label = "ARMOR",
                        current = m.armor,
                        max = (m.armor * 2.0).coerceAtLeast(10.0),
                        fillColor = SecondaryGold,
                        tag = "enemy_armor_bar"
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Hostile Specs (Fixed Height for exact symmetry)
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
                            Text("💥 ", fontSize = 8.sp, color = HpRed)
                            Text(
                                text = "ATK: ${m.damage.toInt()} DMG",
                                style = MaterialTheme.typography.labelSmall,
                                color = HpRed,
                                fontSize = 8.sp,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡 ", fontSize = 8.sp, color = TextMuted)
                            Text(
                                text = "DEF: ${m.armor.toInt()} DEF",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 8.sp,
                                maxLines = 1
                            )
                        }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🐾 BEASTS [${state.beasts.beasts.size}/8]",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenSuccess,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 9.sp
                    )
                    if (state.beasts.beasts.isEmpty()) {
                        Text("No beasts active", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 9.sp)
                        Text("Defeat hostiles to tame", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 8.sp)
                    } else {
                        val bList = state.beasts.beasts.take(2)
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            bList.forEach { b ->
                                Text(
                                    text = "› ${b.name.uppercase()} ⭐${b.stars} (${b.dps.toInt()} DPS)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 8.sp
                                )
                            }
                            if (bList.size == 1) {
                                Text("› 1 ACTIVE COMPANION", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 8.sp)
                            }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "💀 UNDEAD ARMY",
                        style = MaterialTheme.typography.labelSmall,
                        color = NecroPurple,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 9.sp
                    )
                    if (state.necro.army.isEmpty()) {
                        Text("No minions active", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 9.sp)
                        Text("Reanimate at Altar", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 8.sp)
                    } else {
                        val mList = state.necro.army.take(2)
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            mList.forEach { minion ->
                                Text(
                                    text = "› ${minion.type.uppercase()} x${minion.count} (${(minion.dps * minion.count).toInt()} DPS)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 8.sp
                                )
                            }
                            if (mList.size == 1) {
                                Text("› SKELETON CORPS ACTIVE", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 8.sp)
                            }
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
