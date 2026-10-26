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
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.*
import com.aistudio.trueidle.ui.theme.*
import java.util.Locale

@Composable
fun CombatScreen(
    state: GameState,
    logFilter: String,
    onNavFloor: (Int) -> Unit,
    onToggleAuto: () -> Unit,
    onSelectLogFilter: (String) -> Unit,
    onClearLogs: () -> Unit,
    onManageSupport: ((SupportKind) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val p = state.player
    val m = state.monster
    val derived = DerivedStats.of(state)
    val totalDps = CombatMath.totalDps(state, derived)
    val mitigation = CombatMath.mitigation(m.armor)
    val effDps = totalDps * mitigation
    val incomingRate = CombatMath.incomingPerSec(m, derived, state.essence.aegisActive)
    val reqXp = Balance.playerXpReq(p.lvl)

    // Support module data mappings
    val beastsGroup = SupportGroupUi(
        count = state.beasts.beasts.size,
        cap = 8,
        totalDps = "${CombatMath.beastDps(state.beasts).toInt()} DPS",
        emptyText = "NO BEASTS ACTIVE. DEFEAT HOSTILES TO TAME.",
        top3 = state.beasts.beasts.take(3).map { b ->
            SupportItemUi(
                name = b.name.uppercase(),
                stars = b.stars,
                dpsText = "${(b.dps * b.rank * (1.0 + (b.stars - 1) * 0.25)).toInt()} DPS",
                meta = "RANK ${b.rank} · ${b.trait.uppercase()}"
            )
        }
    )

    val undeadGroup = SupportGroupUi(
        count = state.necro.army.sumOf { it.count },
        cap = 12,
        totalDps = "${CombatMath.armyDps(state.necro).toInt()} DPS",
        emptyText = "NO MINIONS ACTIVE. REANIMATE AT ALTAR OF BONES.",
        top3 = state.necro.army.take(3).map { minion ->
            SupportItemUi(
                name = minion.type.uppercase(),
                stars = minion.lvl,
                dpsText = "${(minion.dps * minion.count * (1.0 + (minion.lvl - 1) * 0.2)).toInt()} DPS",
                meta = "CORPS x${minion.count} · LVL ${minion.lvl}"
            )
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(8.dp)
            .testTag("combat_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Floor Navigation & Auto-Advance Header (§7.6)
        FloorControls(
            currentFloor = p.currentFloor,
            maxFloor = p.maxUnlockedFloor,
            autoAdvance = p.autoAdvance,
            killsOnFloor = p.killsOnFloor,
            killsRequired = p.killsRequired,
            onNavFloor = onNavFloor,
            onToggleAuto = onToggleAuto
        )

        // 2. HOSTILE ENEMY CARD (Single-Column, Full Width)
        SciFiPanel(
            title = "HOSTILE // ${m.name.uppercase()}",
            accent = if (m.isBoss) HpRed else PrimaryNeon,
            tag = if (m.isBoss) "BOSS Lv${m.lvl}" else "ELITE Lv${m.lvl}",
            borderColor = if (m.isBoss) HpRed else BorderDark,
            modifier = Modifier.testTag("hostile_combat_card")
        ) {
            SciFiGauge(
                label = "VITALITY (HP)",
                valueText = "${m.hp.toInt()} / ${m.maxHp.toInt()}",
                ratio = if (m.maxHp > 0) (m.hp / m.maxHp).toFloat() else 0f,
                color = HpRed
            )

            if (m.maxShield > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                SciFiGauge(
                    label = "ENERGY SHIELD",
                    valueText = "${m.shield.toInt()} / ${m.maxShield.toInt()}",
                    ratio = if (m.maxShield > 0) (m.shield / m.maxShield).toFloat() else 0f,
                    color = ShieldBlue
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000), RectangleShape)
                    .border(1.dp, BorderDark, RectangleShape)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ATK: ${m.damage.toInt()} DMG",
                    style = TrueIdleType.value,
                    color = HpRed
                )
                Text(
                    text = "DEF: ${m.armor.toInt()} (${(100 - (mitigation * 100)).toInt()}% MITIGATED)",
                    style = TrueIdleType.value,
                    color = SecondaryGold
                )
                Text(
                    text = if (m.isBoss) "CRITICAL" else "HIGH",
                    style = TrueIdleType.tag,
                    color = if (m.isBoss) HpRed else PrimaryNeon
                )
            }
        }

        // 3. CLASH TELEMETRY STATUS BANNER (§7.6)
        val clashStatusText = when {
            p.recoverLeft > 0.0 -> "SYSTEM RECOVERING (${String.format(Locale.ROOT, "%.1f", p.recoverLeft)}s)"
            else -> "CLASH ACTIVE · ${NumberFormatter.short(effDps)} DPS OUT · ${NumberFormatter.short(incomingRate)} DPS IN"
        }
        val clashColor = if (p.recoverLeft > 0.0) Warn else PrimaryNeon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF000000), RectangleShape)
                .border(1.dp, clashColor, RectangleShape)
                .padding(vertical = 6.dp, horizontal = 10.dp)
                .testTag("clash_status_banner"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = clashStatusText,
                style = TrueIdleType.tag,
                color = clashColor
            )
        }

        // 4. HERO PROTOCOL CARD (Single-Column, Full Width)
        SciFiPanel(
            title = "HERO // PROTOCOL",
            accent = PrimaryNeon,
            tag = "LVL ${p.lvl}",
            modifier = Modifier.testTag("hero_combat_card")
        ) {
            SciFiGauge(
                label = "HEALTH (HP)",
                valueText = "${p.hp.toInt()} / ${derived.maxHp.toInt()}",
                ratio = (p.hp / derived.maxHp).toFloat(),
                color = GreenSuccess
            )
            Spacer(modifier = Modifier.height(4.dp))
            SciFiGauge(
                label = "SHIELD",
                valueText = "${p.shield.toInt()} / ${derived.maxShield.toInt()}",
                ratio = (p.shield / derived.maxShield).toFloat(),
                color = ShieldBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            SciFiGauge(
                label = "MANA RESERVES",
                valueText = "${p.mana.toInt()} / ${derived.maxMana.toInt()}",
                ratio = (p.mana / derived.maxMana).toFloat(),
                color = ManaTeal
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Weapon Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚔ WEAPON", style = TrueIdleType.tag, color = SecondaryGold)
                            Text("+${derived.weaponDmg.toInt()} ATK", style = TrueIdleType.numberSmall, color = SecondaryGold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = p.weaponSlot,
                            style = TrueIdleType.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Armor Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡 ARMOR", style = TrueIdleType.tag, color = ShieldBlue)
                            Text("+${derived.armorDef.toInt()} DEF", style = TrueIdleType.numberSmall, color = ShieldBlue)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = p.armorSlot,
                            style = TrueIdleType.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 5. HERO EXPERIENCE PROGRESSION
        Card(
            modifier = Modifier.fillMaxWidth().testTag("combat_player_xp_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                XpProgressBar(
                    skillName = "HERO EXPERIENCE",
                    level = p.lvl,
                    currentXp = p.xp,
                    requiredXp = reqXp,
                    barColor = PrimaryNeon
                )
            }
        }

        // 6. SUPPORT SQUAD MODULE (§7.5, §7.6)
        SupportModule(
            beasts = beastsGroup,
            undead = undeadGroup,
            onManage = { kind -> onManageSupport?.invoke(kind) },
            modifier = Modifier.testTag("combat_support_module")
        )

        // 7. ACTIVE SLAYER DIRECTIVE BANNER (§7.6)
        val task = state.slayer.currentTask
        SciFiPanel(
            title = "TACTICAL DIRECTIVE",
            accent = HpRed,
            tag = if (task.isCompleted) "CLAIMABLE" else "IN PROGRESS"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TARGET: ${task.targetName.uppercase()}",
                        style = TrueIdleType.label,
                        color = TextLight
                    )
                    Text(
                        text = "REWARD: +${task.rewardGold} 🪙 · +${task.rewardPoints} ⚔ PTS",
                        style = TrueIdleType.meta,
                        color = TextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .background(HpRed.copy(alpha = 0.2f), RectangleShape)
                        .border(1.dp, HpRed, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${task.currentKills}/${task.requiredKills}",
                        style = TrueIdleType.value,
                        color = HpRed
                    )
                }
            }
        }

        // 8. COMBAT DPS MATRIX TELEMETRY
        CombatTelemetryCard(state = state)

        // 9. LOGS PANEL
        LogView(
            logs = state.logs,
            currentFilter = logFilter,
            onFilterSelect = onSelectLogFilter,
            onClear = onClearLogs,
            state = state
        )
    }
}
