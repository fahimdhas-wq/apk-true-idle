package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.XpCurves
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
    // Stacked combat potion quick actions: one-tap deploy plus a popover for loadout/thresholds.
    onUseHpPotion: () -> Unit = {},
    onUseMpPotion: () -> Unit = {},
    onSetHpThreshold: (Int) -> Unit = {},
    onSetMpThreshold: (Int) -> Unit = {},
    onSelectHpPotion: (String) -> Unit = {},
    onSelectMpPotion: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val p = state.player
    val m = state.monster
    val derived = com.aistudio.trueidle.ui.LocalDerived.current
    val totalDps = CombatMath.totalDps(state, derived)
    val mitigation = CombatMath.mitigation(m.armor)
    val effDps = totalDps * mitigation
    val incomingRate = CombatMath.incomingPerSec(m, derived, CombatMath.aegisReduction(state.essence))
    val reqXp = XpCurves.player(p.lvl)

    // Support module data mappings
    val beastsGroup = remember(state.beasts) {
        SupportGroupUi(
            count = state.beasts.beasts.size,
            cap = GameCaps.BEAST,
            totalDps = "${NumberFormatter.short(CombatMath.beastDps(state.beasts))} DPS",
            emptyText = "NO BEASTS ACTIVE. DEFEAT HOSTILES TO TAME.",
            top = state.beasts.beasts
                .sortedByDescending { it.dps * it.rank * (1.0 + (it.stars - 1) * 0.25) }
                .take(3)
                .map { b ->
                    SupportItemUi(
                        name = b.name.uppercase(),
                        stars = b.stars,
                        dpsText = "${NumberFormatter.short(b.dps * b.rank * (1.0 + (b.stars - 1) * 0.25))} DPS",
                        meta = "RANK ${b.rank} · ${b.trait.uppercase()}"
                    )
                }
        )
    }

    val undeadGroup = remember(state.necro) {
        SupportGroupUi(
            count = state.necro.army.sumOf { it.count },
            cap = GameCaps.MINION,
            totalDps = "${NumberFormatter.short(CombatMath.armyDps(state.necro))} DPS",
            emptyText = "NO MINIONS ACTIVE. REANIMATE AT ALTAR OF BONES.",
            top = state.necro.army.sortedByDescending { it.dps * it.count }.take(3).map { minion ->
                SupportItemUi(
                    name = minion.type.uppercase(),
                    stars = 0,
                    dpsText = "${NumberFormatter.short(minion.dps * minion.count * (1.0 + (minion.lvl - 1) * 0.2))} DPS",
                    meta = "CORPS x${minion.count} · LVL ${minion.lvl}"
                )
            }
        )
    }
    val onManageStable = remember(onManageSupport) { { kind: SupportKind -> onManageSupport?.invoke(kind); Unit } }

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
            tag = if (m.isBoss) "BOSS Lv${m.lvl}" else "NORMAL Lv${m.lvl}",
            borderColor = if (m.isBoss) HpRed else BorderDark,
            modifier = Modifier.testTag("hostile_combat_card")
        ) {
            SciFiGauge(
                label = "VITALITY (HP)",
                valueText = "${NumberFormatter.short(m.hp)} / ${NumberFormatter.short(m.maxHp)}",
                ratio = if (m.maxHp > 0) (m.hp / m.maxHp).toFloat() else 0f,
                color = HpRed
            )

            if (m.maxShield > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                SciFiGauge(
                    label = "ENERGY SHIELD",
                    valueText = "${NumberFormatter.short(m.shield)} / ${NumberFormatter.short(m.maxShield)}",
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
                    text = "ATK: ${NumberFormatter.short(m.damage)} DMG",
                    style = TrueIdleType.value,
                    color = HpRed
                )
                Text(
                    text = "DEF: ${NumberFormatter.short(m.armor)} (${(100 - (mitigation * 100)).toInt()}% MITIGATED)",
                    style = TrueIdleType.value,
                    color = SecondaryGold
                )
            }
        }

        // 3. CLASH TELEMETRY STATUS BANNER (§7.6)
        // Memoised so an unchanged value never re-enters the formatter on a combat tick.
        val dpsOutText = rememberStableShort(effDps)
        val dpsInText = rememberStableShort(incomingRate)
        val clashStatusText = when {
            p.recoverLeft > 0.0 -> "SYSTEM RECOVERING (${String.format(Locale.ROOT, "%.1f", p.recoverLeft)}s)"
            else -> "CLASH ACTIVE · $dpsOutText DPS OUT · $dpsInText DPS IN"
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
                valueText = "${NumberFormatter.short(p.hp)} / ${NumberFormatter.short(derived.maxHp)}",
                ratio = (p.hp / derived.maxHp).toFloat(),
                color = GreenSuccess
            )
            Spacer(modifier = Modifier.height(4.dp))
            SciFiGauge(
                label = "SHIELD",
                valueText = "${NumberFormatter.short(p.shield)} / ${NumberFormatter.short(derived.maxShield)}",
                ratio = (p.shield / derived.maxShield).toFloat(),
                color = ShieldBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            SciFiGauge(
                label = "MANA RESERVES",
                valueText = "${NumberFormatter.short(p.mana)} / ${NumberFormatter.short(derived.maxMana)}",
                ratio = (p.mana / derived.maxMana).toFloat(),
                color = ManaTeal
            )

            Spacer(modifier = Modifier.height(6.dp))
            // The XP bar lives in the hero panel: a dedicated full-width card for one progress bar
            // pushed the combat stack past a single screen on small devices.
            XpProgressBar(
                skillName = "HERO EXPERIENCE",
                level = p.lvl,
                currentXp = p.xp,
                requiredXp = reqXp,
                barColor = PrimaryNeon
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
                            SciFiBadge(SciFiIconType.WEAPON, "WEAPON", textColor = SecondaryGold)
                            Text("+${NumberFormatter.short(derived.weaponDmg)} ATK", style = TrueIdleType.numberSmall, color = SecondaryGold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Equipment.equippedName(state, Equipment.SLOT_RIFLE, Equipment.equippedName(state, Equipment.SLOT_WEAPON, "BARE HANDS")),
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
                            SciFiBadge(SciFiIconType.SHIELD, "ARMOR", textColor = ShieldBlue)
                            Text("+${NumberFormatter.short(derived.armorDef)} DEF", style = TrueIdleType.numberSmall, color = ShieldBlue)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Equipment.equippedName(state, Equipment.SLOT_VEST, Equipment.equippedName(state, Equipment.SLOT_ARMOR, "NONE")),
                            style = TrueIdleType.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 4b. STACKED HEALTH / MANA QUICK-DEPLOY POTS
        PotionQuickBar(
            state = state,
            onUseHp = onUseHpPotion,
            onUseMp = onUseMpPotion,
            onSetHpThreshold = onSetHpThreshold,
            onSetMpThreshold = onSetMpThreshold,
            onSelectHp = onSelectHpPotion,
            onSelectMp = onSelectMpPotion
        )

        // 5. SUPPORT SQUAD MODULE (§7.5, §7.6)
        SupportModule(
            beasts = beastsGroup,
            undead = undeadGroup,
            onManage = onManageStable,
            modifier = Modifier.testTag("combat_support_module")
        )

        // 6. ACTIVE SLAYER DIRECTIVE BANNER (§7.6)
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
                        text = "REWARD: +${NumberFormatter.short(task.rewardGold)} CREDITS · +${NumberFormatter.short(task.rewardPoints)} PTS",
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

        // 7. COMBAT DPS MATRIX TELEMETRY
        CombatTelemetryCard(state = state)

        // 8. LOGS PANEL
        LogView(
            logs = state.logs,
            currentFilter = logFilter,
            onFilterSelect = onSelectLogFilter,
            onClear = onClearLogs,
            playTimeSeconds = state.lifetime.playTimeSeconds,
            onExport = { com.aistudio.trueidle.core.Telemetry.report(state, state.logs) }
        )
    }
}

/** One potion row: tap the flask to deploy immediately, tap the cog to configure. */
@Composable
private fun PotionRow(
    label: String,
    potionName: String,
    count: Int,
    threshold: Int,
    accent: Color,
    onUse: () -> Unit,
    onSettings: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RectangleShape)
            .border(1.dp, accent, RectangleShape)
            .padding(6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .background(accent.copy(alpha = 0.18f), RectangleShape)
                .border(1.dp, accent, RectangleShape)
                .clickable(role = Role.Button, onClick = onUse)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (label.startsWith("HEALTH")) "♥" else "◆", color = accent, style = TrueIdleType.title)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(label, style = TrueIdleType.tag, color = accent, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = potionName.uppercase(),
                    style = TrueIdleType.value,
                    color = TextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(accent.copy(alpha = 0.25f), RectangleShape)
                    .border(1.dp, accent, RectangleShape)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("×$count", style = TrueIdleType.tag, color = accent)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(SurfaceVariantDark, RectangleShape)
                    .border(1.dp, accent, RectangleShape)
                    .clickable(role = Role.Button, onClick = onSettings)
                    .testTag("${testTag}_settings"),
                contentAlignment = Alignment.Center
            ) {
                Text("⚙", color = accent, style = TrueIdleType.title)
            }
            Text(
                text = if (threshold == 0) "AUTO OFF" else "AUTO $threshold%",
                style = TrueIdleType.meta,
                color = TextMuted
            )
        }
    }
}

/**
 * Vertically stacked Health and Mana quick actions on the combat HUD, each with a remaining-count
 * badge and an auto-deploy settings sheet.
 */
@Composable
private fun PotionQuickBar(
    state: GameState,
    onUseHp: () -> Unit,
    onUseMp: () -> Unit,
    onSetHpThreshold: (Int) -> Unit,
    onSetMpThreshold: (Int) -> Unit,
    onSelectHp: (String) -> Unit,
    onSelectMp: (String) -> Unit
) {
    var sheet by remember { mutableStateOf<String?>(null) }
    val settings = state.potionSettings
    fun owned(name: String): Int = state.inventory.firstOrNull {
        it.category == ItemCatalog.CATEGORY_POTION && it.name.equals(name, ignoreCase = true)
    }?.count?.toInt() ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PotionRow(
            label = "HEALTH POTION",
            potionName = settings.selectedHpPotion,
            count = owned(settings.selectedHpPotion),
            threshold = settings.hpAutoThresholdPct,
            accent = HpRed,
            onUse = onUseHp,
            onSettings = { sheet = "HP" },
            testTag = "combat_hp_potion"
        )
        PotionRow(
            label = "MANA POTION",
            potionName = settings.selectedMpPotion,
            count = owned(settings.selectedMpPotion),
            threshold = settings.mpAutoThresholdPct,
            accent = ManaTeal,
            onUse = onUseMp,
            onSettings = { sheet = "MP" },
            testTag = "combat_mp_potion"
        )
    }

    sheet?.let { kind ->
        val isHp = kind == "HP"
        val accent = if (isHp) HpRed else ManaTeal
        val selected = if (isHp) settings.selectedHpPotion else settings.selectedMpPotion
        val threshold = if (isHp) settings.hpAutoThresholdPct else settings.mpAutoThresholdPct
        val options = if (isHp) ItemCatalog.HP_POTIONS else ItemCatalog.MANA_POTIONS
        SciFiDialog(
            title = if (isHp) "HEALTH POTION CONTROL" else "MANA POTION CONTROL",
            accent = accent,
            onDismiss = { sheet = null },
            actions = {
                SciFiButton(label = "CLOSE", onClick = { sheet = null }, modifier = Modifier.weight(1f), color = accent)
            }
        ) {
            Text("AUTO-DEPLOY THRESHOLD", style = TrueIdleType.meta, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0, 30, 50, 75).forEach { pct ->
                    val active = threshold == pct
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .background(
                                if (active) accent.copy(alpha = 0.2f) else SurfaceVariantDark,
                                RectangleShape
                            )
                            .border(1.dp, if (active) accent else BorderDark, RectangleShape)
                            .clickable {
                                if (isHp) onSetHpThreshold(pct) else onSetMpThreshold(pct)
                            }
                            .testTag("potion_threshold_${kind.lowercase()}_$pct"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (pct == 0) "OFF" else "$pct%",
                            style = TrueIdleType.tag,
                            color = if (active) accent else TextMuted
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("POTION LOADOUT", style = TrueIdleType.meta, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                options.forEach { def ->
                    val ownedCount = owned(def.name)
                    val active = def.name.equals(selected, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (active) accent.copy(alpha = 0.15f) else SurfaceVariantDark,
                                RectangleShape
                            )
                            .border(1.dp, if (active) accent else BorderDark, RectangleShape)
                            .clickable { if (isHp) onSelectHp(def.name) else onSelectMp(def.name) }
                            .padding(8.dp)
                            .testTag("potion_option_${kind.lowercase()}_${def.herb.lowercase().replace(' ', '_')}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(def.name.uppercase(), style = TrueIdleType.label, color = TextLight)
                            Text(def.effectDesc, style = TrueIdleType.meta, color = TextMuted)
                        }
                        Text(
                            text = "×$ownedCount",
                            style = TrueIdleType.value,
                            color = if (ownedCount > 0) GreenSuccess else TextMuted
                        )
                    }
                }
            }
        }
    }
}
