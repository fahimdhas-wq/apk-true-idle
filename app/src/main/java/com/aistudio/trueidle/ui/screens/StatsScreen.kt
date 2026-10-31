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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.TrainingCaps
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.ui.components.SciFiBadge
import com.aistudio.trueidle.ui.components.SciFiIconType
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aistudio.trueidle.core.DailyLoginDetector
import com.aistudio.trueidle.core.DailyRewardCatalog
import com.aistudio.trueidle.model.DailyLoginState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun StatsScreen(
    state: GameState,
    onClaimDailyLogin: (Boolean) -> Unit = {},
    onSimulateNextDay: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val p = state.player
    val reqXp = XpCurves.player(p.lvl)
    val d = com.aistudio.trueidle.ui.state.LocalDerived.current

    // Equipped weapons
    val rifle = Equipment.equipped(state, Equipment.SLOT_RIFLE)
    val sidearm = Equipment.equipped(state, Equipment.SLOT_SIDEARM)
    val blade = Equipment.equipped(state, Equipment.SLOT_BLADE)
    val totalWeaponAtk = Equipment.baseAttack(rifle) + Equipment.baseAttack(sidearm) + Equipment.baseAttack(blade)

    // Equipped armor (7 groups)
    val visor = Equipment.equipped(state, Equipment.SLOT_VISOR)
    val vest = Equipment.equipped(state, Equipment.SLOT_VEST)
    val grips = Equipment.equipped(state, Equipment.SLOT_GRIPS)
    val pants = Equipment.equipped(state, Equipment.SLOT_PANTS)
    val boots = Equipment.equipped(state, Equipment.SLOT_BOOTS)
    val duster = Equipment.equipped(state, Equipment.SLOT_DUSTER)
    val rig = Equipment.equipped(state, Equipment.SLOT_RIG)

    val armorList = listOf(visor, vest, grips, pants, boots, duster, rig)
    val totalArmorDef = armorList.sumOf { Equipment.baseDefense(it) }
    val totalArmorVit = armorList.sumOf { Equipment.baseVit(it) }

    // Equipped nodes (3 slots)
    val node1 = Equipment.equipped(state, Equipment.SLOT_NODE_I)
    val node2 = Equipment.equipped(state, Equipment.SLOT_NODE_II)
    val node3 = Equipment.equipped(state, Equipment.SLOT_NODE_III)

    val nodeList = listOf(node1, node2, node3)
    val totalNodeDef = nodeList.sumOf { Equipment.baseDefense(it) }
    val totalNodeVit = nodeList.sumOf { Equipment.baseVit(it) }

    // Telemetry derived from the exact combat formulas so the screen quotes real numbers.
    val mitigation = CombatMath.mitigation(d.armorDef)
    val rawPool = d.maxHp + d.maxShield
    val effectiveHp = if (mitigation >= 1.0) rawPool else rawPool / (1.0 - mitigation)
    val heroDps = CombatMath.heroDps(d)
    val companionDps = CombatMath.beastDps(state.beasts)
    val corpsDps = CombatMath.armyDps(state.necro)
    val trueDps = CombatMath.totalDps(state, d)
    val effectiveDps = trueDps * CombatMath.mitigation(state.monster.armor)
    val incomingDps = CombatMath.incomingPerSec(
        state.monster,
        d,
        CombatMath.aegisReduction(state.essence)
    )
    val incomingRaw = state.monster.damage
    val secondsToDeath = if (incomingDps > 0.0) rawPool / incomingDps else 0.0
    val weaponSharePct = (d.weaponDmg / (d.str * 1.2 + d.weaponDmg).coerceAtLeast(1.0) * 100.0).toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(8.dp)
            .testTag("stats_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hero Header Card with XP Bar
        Card(
            modifier = Modifier.fillMaxWidth().testTag("stats_header_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HERO PROFILE",
                        style = TrueIdleType.title,
                        color = PrimaryNeon,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .background(SecondaryGold.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, SecondaryGold, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (p.lvl >= GameCaps.MAX_LEVEL) "LEVEL ${p.lvl} MAX" else "LEVEL ${p.lvl}/${GameCaps.MAX_LEVEL}",
                            style = TrueIdleType.tag,
                            color = SecondaryGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                XpProgressBar(
                    skillName = "HERO EXPERIENCE",
                    level = p.lvl,
                    currentXp = p.xp,
                    requiredXp = reqXp,
                    barColor = PrimaryNeon
                )
            }
        }

        // Daily Login Rewards Card (Dual Tabs: 7-Day Sprint & 30-Day Calendar)
        DailyLoginCard(
            daily = state.dailyLogin,
            onClaim = onClaimDailyLogin,
            onSimulateNextDay = onSimulateNextDay
        )

        // 1. GEAR MATRIX OVERVIEW
        SciFiPanel(
            title = "TACTICAL LOADOUT SUMMARY",
            accent = SecondaryGold,
            tag = "GEAR SCORE ${d.gearScore}"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Equal-weight columns give the three readouts one shared baseline and gutter.
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TOTAL WEAPON ATK",
                            style = TrueIdleType.meta.copy(fontSize = 9.sp, lineHeight = 12.sp),
                            color = TextMuted,
                            maxLines = 1
                        )
                        Text(
                            text = "+${NumberFormatter.short(totalWeaponAtk)} ATK",
                            style = TrueIdleType.value.copy(fontSize = 11.sp),
                            color = PrimaryNeon,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TOTAL ARMOR DEF",
                            style = TrueIdleType.meta.copy(fontSize = 9.sp, lineHeight = 12.sp),
                            color = TextMuted,
                            maxLines = 1
                        )
                        Text(
                            text = "+${NumberFormatter.short(totalArmorDef + totalNodeDef)} DEF",
                            style = TrueIdleType.value.copy(fontSize = 11.sp),
                            color = ShieldBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TOTAL VITALITY",
                            style = TrueIdleType.meta.copy(fontSize = 9.sp, lineHeight = 12.sp),
                            color = TextMuted,
                            maxLines = 1
                        )
                        Text(
                            text = "+${NumberFormatter.short(totalArmorVit + totalNodeVit)} VIT",
                            style = TrueIdleType.value.copy(fontSize = 11.sp),
                            color = GreenSuccess,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 1b. EFFECTIVE HEALTH POOL (EHP)
        SciFiPanel(
            title = "EFFECTIVE HEALTH POOL (EHP)",
            accent = GreenSuccess,
            tag = "SURVIVABILITY"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatDotRow("RAW HP + SHIELD POOL", NumberFormatter.short(rawPool), GreenSuccess, GreenSuccess)
                StatDotRow(
                    "ARMOR MITIGATION",
                    "${String.format(Locale.ROOT, "%.1f", mitigation * 100)}% ABSORBED",
                    ShieldBlue,
                    ShieldBlue
                )
                StatDotRow("EFFECTIVE HP VS PHYSICAL", NumberFormatter.short(effectiveHp), null, PrimaryNeon)
                StatDotRow(
                    "INCOMING ${state.monster.name.uppercase()} DPS",
                    "${NumberFormatter.short(incomingRaw)} RAW ➔ ${NumberFormatter.short(incomingDps)} MITIGATED",
                    HpRed,
                    HpRed
                )
                StatDotRow(
                    "SECONDS TO DEATH",
                    "${String.format(Locale.ROOT, "%.1f", secondsToDeath)}s",
                    null,
                    Warn
                )
            }
        }

        // 1c. TRUE DPS BREAKDOWN
        SciFiPanel(
            title = "TRUE DPS BREAKDOWN",
            accent = HpRed,
            tag = "${NumberFormatter.short(trueDps)} DPS"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatDotRow("HERO DPS", NumberFormatter.short(heroDps), PrimaryNeon, PrimaryNeon)
                StatDotRow("COMPANION BEAST DPS", NumberFormatter.short(companionDps), SecondaryGold, SecondaryGold)
                StatDotRow("UNDEAD CORPS DPS", NumberFormatter.short(corpsDps), NecroPurple, NecroPurple)
                StatDotRow("TOTAL TRUE DPS", NumberFormatter.short(trueDps), null, TextLight)
                StatDotRow(
                    "VS ${state.monster.name.uppercase()} ARMOR",
                    NumberFormatter.short(effectiveDps),
                    null,
                    HpRed
                )
                StatDotRow(
                    "ATTACKS PER SECOND",
                    String.format(Locale.ROOT, "%.2f", d.atkSpeed),
                    null,
                    PrimaryNeon
                )
                StatDotRow(
                    "CRIT EXPECTATION",
                    "${(CombatMath.critMultiplier(d.critChance, d.critDamage) * 100).toInt()}% AVG",
                    null,
                    SecondaryGold
                )
            }
        }

        // 1d. GEAR CONTRIBUTION MATRIX ACROSS ALL 13 GROUPS
        SciFiPanel(
            title = "GEAR CONTRIBUTION MATRIX",
            accent = SecondaryGold,
            tag = "${Equipment.GROUPS.size} GROUPS"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatDotRow(
                    "WEAPONS (3) · ATK",
                    "+${NumberFormatter.short(totalWeaponAtk)} RAW ➔ ${NumberFormatter.short(d.weaponDmg)} EFFECTIVE",
                    PrimaryNeon,
                    PrimaryNeon
                )
                StatDotRow(
                    "ARMOR (7) · DEF / VIT",
                    "+${NumberFormatter.short(totalArmorDef)} DEF · +${NumberFormatter.short(totalArmorVit)} VIT",
                    ShieldBlue,
                    ShieldBlue
                )
                StatDotRow(
                    "NODES (3) · DEF / VIT",
                    "+${NumberFormatter.short(totalNodeDef)} DEF · +${NumberFormatter.short(totalNodeVit)} VIT",
                    NecroPurple,
                    NecroPurple
                )
                StatDotRow("WEAPON SHARE OF HERO BASE", "$weaponSharePct%", null, TextLight)
                StatDotRow(
                    "MITIGATION CURVE",
                    "ARMOR ${NumberFormatter.short(d.armorDef)} ➔ ${String.format(Locale.ROOT, "%.1f", mitigation * 100)}%",
                    null,
                    SecondaryGold
                )
            }
        }

        // 2. WEAPONS BREAKDOWN (3 Slots)
        SciFiPanel(
            title = "WEAPONS (3 SLOTS)",
            accent = PrimaryNeon,
            tag = "+${NumberFormatter.short(totalWeaponAtk)} ATK"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EquipmentItemRow("RIFLE", Equipment.SLOT_RIFLE, rifle)
                EquipmentItemRow("SIDEARM", Equipment.SLOT_SIDEARM, sidearm)
                EquipmentItemRow("BLADE", Equipment.SLOT_BLADE, blade)
            }
        }

        // 3. ARMOR BREAKDOWN (7 Slots)
        SciFiPanel(
            title = "ARMOR PIECES (7 SLOTS)",
            accent = ShieldBlue,
            tag = "+${NumberFormatter.short(totalArmorDef)} DEF"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EquipmentItemRow("VISOR", Equipment.SLOT_VISOR, visor)
                EquipmentItemRow("VEST", Equipment.SLOT_VEST, vest)
                EquipmentItemRow("GRIPS", Equipment.SLOT_GRIPS, grips)
                EquipmentItemRow("PANTS", Equipment.SLOT_PANTS, pants)
                EquipmentItemRow("BOOTS", Equipment.SLOT_BOOTS, boots)
                EquipmentItemRow("DUSTER", Equipment.SLOT_DUSTER, duster)
                EquipmentItemRow("RIG", Equipment.SLOT_RIG, rig)
            }
        }

        // 4. ACCESSORY NODES (3 Slots)
        SciFiPanel(
            title = "ACCESSORY NODES (3 SLOTS)",
            accent = NecroPurple,
            tag = "+${NumberFormatter.short(totalNodeDef)} DEF"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EquipmentItemRow("NODE I", Equipment.SLOT_NODE_I, node1)
                EquipmentItemRow("NODE II", Equipment.SLOT_NODE_II, node2)
                EquipmentItemRow("NODE III", Equipment.SLOT_NODE_III, node3)
            }
        }

        // 5. COMBAT ATTRIBUTES
        SciFiPanel(
            title = "COMBAT ATTRIBUTES",
            accent = SecondaryGold,
            tag = "LVL ${p.lvl}/${GameCaps.MAX_LEVEL}"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatDotRow("STRENGTH (ATK SCALING)", NumberFormatter.short(d.str), PrimaryNeon, PrimaryNeon)
                StatDotRow("INTELLIGENCE (BURST SCALING)", NumberFormatter.short(d.intVal), SecondaryGold, SecondaryGold)
                StatDotRow("VITALITY (HEALTH SCALING)", NumberFormatter.short(d.vit), GreenSuccess, GreenSuccess)
                StatDotRow("ARMOR DEFENSE", NumberFormatter.short(d.armorDef), NecroPurple, NecroPurple)
                StatDotRow("PHYSICAL HIT DAMAGE", NumberFormatter.short(d.physicalDamage), null, TextLight)
                StatDotRow(
                    "ATTACK SPEED",
                    "${String.format(Locale.ROOT, "%.2f", d.atkSpeed)} HITS/S (${String.format(Locale.ROOT, "%.2f", if (d.atkSpeed > 0.0) 1.0 / d.atkSpeed else 1.0)}s)",
                    null,
                    PrimaryNeon
                )
                StatDotRow("CRITICAL STRIKE CHANCE", String.format(Locale.ROOT, "%.1f%%", d.critChance * 100), null, TextLight)
                StatDotRow("CRITICAL DAMAGE MULTIPLIER", "${(d.critDamage * 100).toInt()}%", null, SecondaryGold)
                StatDotRow("ARMOR DAMAGE MITIGATION", "${(CombatMath.mitigation(d.armorDef) * 100).toInt()}% ABSORBED", null, SecondaryGold)
                StatDotRow("TOTAL HEALTH (HP)", "${NumberFormatter.short(p.hp)} / ${NumberFormatter.short(d.maxHp)}", GreenSuccess, GreenSuccess)
                StatDotRow("ENERGY BARRIER (SHIELD)", "${NumberFormatter.short(p.shield)} / ${NumberFormatter.short(d.maxShield)}", ShieldBlue, ShieldBlue)
                StatDotRow("MANA ENERGY", "${NumberFormatter.short(p.mana)} / ${NumberFormatter.short(d.maxMana)}", ManaTeal, ManaTeal)
                StatDotRow("XP ACCELERATION", "+${(d.xpGain * 100).toInt()}%", null, TextLight)
            }
        }

        // 6. SURVIVAL DAMAGE PASSIVES
        SciFiPanel(
            title = "SURVIVAL PASSIVES PROGRESS",
            accent = HpRed,
            tag = "${NumberFormatter.short(state.talent.struggleDamageTaken)} DMG"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Accumulates damage taken to unlock permanent attribute multipliers.",
                    style = TrueIdleType.body,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                StatDotRow("TOTAL DAMAGE ENDURED", "${NumberFormatter.short(state.talent.struggleDamageTaken)} DMG", HpRed, HpRed)
                state.talent.struggleTalents.forEach { st ->
                    StatDotRow(st.name.uppercase(), "RANK ${st.rank} (COST ${st.costPoints} TP)", SecondaryGold, TextLight)
                }
            }
        }

        // 6b. TRAINING LIMITS
        SciFiPanel(
            title = "TRAINING CAPS",
            accent = PrimaryNeon,
            tag = "LIMITS"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                fun nodeRank(id: String) = state.training.nodes.firstOrNull { it.id == id }?.rank ?: 0
                fun capLabel(id: String): String {
                    val r = nodeRank(id)
                    val cap = TrainingCaps.maxRank(id)
                    return "RANK ${NumberFormatter.short(r.toLong())}/${NumberFormatter.short(cap.toLong())}"
                }
                StatDotRow("CRITICAL CHANCE (MAX 100%)", String.format(Locale.ROOT, "%.1f%% · %s", d.critChance * 100, capLabel("crit_bonus")), PrimaryNeon, TextLight)
                StatDotRow("ATTACK SPEED NODE (MAX +100%)", String.format(Locale.ROOT, "+%d%% · %s", (nodeRank("speed_bonus") * 5).coerceAtMost(100), capLabel("speed_bonus")), PrimaryNeon, TextLight)
                StatDotRow("DROP CHANCE (MAX 100%)", String.format(Locale.ROOT, "%.0f%% · %s", d.lootChance * 100, capLabel("luck_bonus")), PrimaryNeon, TextLight)
                StatDotRow("XP BONUS NODE (MAX +100,000%)", capLabel("xp_bonus"), PrimaryNeon, TextLight)
            }
        }

        // 7. HERO PROFESSIONS
        SciFiPanel(
            title = "HERO PROFESSIONS",
            accent = PrimaryNeon,
            tag = "SKILLS"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatDotRow("TRAINING HALL", "LV ${state.training.lvl} (${state.training.nodes.sumOf { it.rank }} NODES)", PrimaryNeon, TextLight)
                StatDotRow("SLAYER GUILD", "LV ${state.slayer.lvl} (${state.slayer.slayerPoints} PTS)", HpRed, SecondaryGold)
                StatDotRow("FARMING", "LV ${state.harvest.farmingLvl}", GreenSuccess, TextLight)
                StatDotRow("MINING", "LV ${state.harvest.miningLvl}", SecondaryGold, TextLight)
                StatDotRow("FORGE SMITHING", "LV ${state.armory.smithingLvl}", SecondaryGold, TextLight)
                StatDotRow("HERB ALCHEMY", "LV ${state.armory.herbloreLvl}", ManaTeal, TextLight)
                StatDotRow("NECROMANCY", "LV ${state.necro.lvl} (${state.necro.boneEssence} BONE)", NecroPurple, TextLight)
                StatDotRow("BEAST TAMING", "LV ${state.beasts.lvl} (${state.beasts.beasts.size} BEASTS)", PrimaryNeon, TextLight)
                StatDotRow("SANCTIFIED FAITH", "LV ${state.essence.lvl} (${state.essence.faith} FAITH)", ManaTeal, TextLight)
            }
        }

        // 8. LIFETIME ADVENTURE STATS
        SciFiPanel(
            title = "LIFETIME ADVENTURE STATS",
            accent = TextLight,
            tag = "RECORD"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatDotRow("MONSTERS DEFEATED", NumberFormatter.short(state.lifetime.totalKills), null, TextLight)
                StatDotRow("DEEPEST FLOOR REACHED", "FLOOR ${p.maxUnlockedFloor}", null, PrimaryNeon)
                StatDotRow("TOTAL GOLD", "${NumberFormatter.short(p.gold)} GOLD", null, SecondaryGold)
                StatDotRow("DIAMONDS RESERVE", "${p.diamonds} DIA", null, PrimaryNeon)
                StatDotRow("FAITH ACCUMULATED", "${NumberFormatter.short(state.essence.faith)} FAITH", null, ManaTeal)
                StatDotRow("SLAYER POINTS", "${NumberFormatter.short(state.slayer.slayerPoints)} PTS", null, HpRed)
                StatDotRow("TAMED COMPANIONS", "${state.beasts.beasts.size}", null, TextLight)
                StatDotRow("UNDEAD MINIONS", "${state.necro.army.sumOf { it.count }}", null, TextLight)
            }
        }
    }
}

@Composable
private fun EquipmentItemRow(
    slotLabel: String,
    slotName: String,
    item: InventoryItem?
) {
    val tierInfo = item?.let { TierConfig.forOrdinal(it.tier) }
    val tierColor = tierInfo?.let { Color(it.color) } ?: BorderDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariantDark, RectangleShape)
            .border(BorderStroke(1.dp, BorderDark), RectangleShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(tierColor.copy(alpha = 0.2f), RectangleShape)
                    .border(1.dp, tierColor, RectangleShape)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (item != null) "[${tierInfo?.id ?: "F-"}]" else "[EMPTY]",
                    style = TrueIdleType.tag,
                    color = if (item != null) tierColor else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slotLabel,
                    style = TrueIdleType.meta,
                    color = TextMuted,
                    maxLines = 1
                )
                Text(
                    text = item?.name?.uppercase() ?: "EMPTY SLOT",
                    style = TrueIdleType.label,
                    color = if (item != null) TextLight else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Stats readout
        if (item != null) {
            val isWeapon = Equipment.isWeapon(slotName)
            // Readouts keep their own column, so +ATK / +DEF never slide around per row.
            if (isWeapon) {
                val atk = Equipment.baseAttack(item)
                Text(
                    text = "+${NumberFormatter.short(atk)} ATK",
                    style = TrueIdleType.value,
                    color = PrimaryNeon
                )
            } else {
                val def = Equipment.baseDefense(item)
                val vit = Equipment.baseVit(item)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${NumberFormatter.short(def)} DEF",
                        style = TrueIdleType.value,
                        color = ShieldBlue
                    )
                    if (vit > 0.0) {
                        Text(
                            text = "+${NumberFormatter.short(vit)} VIT",
                            style = TrueIdleType.tag,
                            color = GreenSuccess
                        )
                    }
                }
            }
        } else {
            Text("—", style = TrueIdleType.meta, color = TextMuted)
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
            .heightIn(min = 30.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Reserve the dot gutter on dotless rows so every label starts on the same x.
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(dotColor)
                )
            } else {
                Spacer(modifier = Modifier.width(11.dp))
            }
            Text(
                text = label,
                style = TrueIdleType.label.copy(fontSize = 9.sp, lineHeight = 12.sp),
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = TrueIdleType.value.copy(fontSize = 10.sp, lineHeight = 13.sp),
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DailyLoginCard(
    daily: DailyLoginState,
    onClaim: (Boolean) -> Unit,
    onSimulateNextDay: () -> Unit = {}
) {
    var isSprintTab by remember { mutableStateOf(true) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val claimedDays = if (isSprintTab) daily.sprint7ClaimedDays else daily.calendar30ClaimedDays
    val maxDays = if (isSprintTab) 7 else 30
    val rewards = if (isSprintTab) DailyRewardCatalog.SPRINT_7_REWARDS else DailyRewardCatalog.CALENDAR_30_REWARDS
    val nextDay = claimedDays + 1
    val isComplete = claimedDays >= maxDays

    val canClaimToday = if (isSprintTab) {
        DailyLoginDetector.canClaimSprint(daily, nowMs)
    } else {
        DailyLoginDetector.canClaimCalendar(daily, nowMs)
    }
    val msUntilMidnight = DailyLoginDetector.msUntilNextDay(nowMs)
    val countdownStr = DailyLoginDetector.formatCountdown(msUntilMidnight)

    Card(
        modifier = Modifier.fillMaxWidth().testTag("daily_login_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RectangleShape,
        border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DAILY LOGIN REWARDS", style = TrueIdleType.titleLarge, color = SecondaryGold)
                    Text("Midnight reset in: $countdownStr", style = TrueIdleType.meta, color = TextMuted)
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (canClaimToday && !isComplete) GreenSuccess.copy(alpha = 0.2f) else SecondaryGold.copy(alpha = 0.15f),
                            RectangleShape
                        )
                        .border(1.dp, if (canClaimToday && !isComplete) GreenSuccess else SecondaryGold, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isComplete) "COMPLETED" else if (canClaimToday) "READY TO CLAIM" else "DAY $nextDay OF $maxDays",
                        style = TrueIdleType.numberSmall,
                        color = if (canClaimToday && !isComplete) GreenSuccess else SecondaryGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dual Tab Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 7-Day Sprint Tab Button
                val sprintClaimable = DailyLoginDetector.canClaimSprint(daily, nowMs)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(
                            if (isSprintTab) SecondaryGold.copy(alpha = 0.2f) else SurfaceVariantDark,
                            RectangleShape
                        )
                        .border(1.dp, if (isSprintTab) SecondaryGold else BorderDark, RectangleShape)
                        .clickable { isSprintTab = true }
                        .testTag("tab_login_sprint_7"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡ 7-DAY SPRINT (${daily.sprint7ClaimedDays}/7)${if (sprintClaimable) " ●" else ""}",
                        style = TrueIdleType.tag.copy(fontWeight = FontWeight.Bold),
                        color = if (isSprintTab) SecondaryGold else if (sprintClaimable) GreenSuccess else TextMuted
                    )
                }

                // 30-Day Calendar Tab Button
                val calendarClaimable = DailyLoginDetector.canClaimCalendar(daily, nowMs)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(
                            if (!isSprintTab) PrimaryNeon.copy(alpha = 0.2f) else SurfaceVariantDark,
                            RectangleShape
                        )
                        .border(1.dp, if (!isSprintTab) PrimaryNeon else BorderDark, RectangleShape)
                        .clickable { isSprintTab = false }
                        .testTag("tab_login_calendar_30"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📅 30-DAY CALENDAR (${daily.calendar30ClaimedDays}/30)${if (calendarClaimable) " ●" else ""}",
                        style = TrueIdleType.tag.copy(fontWeight = FontWeight.Bold),
                        color = if (!isSprintTab) PrimaryNeon else if (calendarClaimable) GreenSuccess else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal scrollable days list
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(rewards) { reward ->
                    val isClaimed = reward.day <= claimedDays
                    val isCurrent = reward.day == nextDay && !isComplete
                    val accentColor = if (isCurrent) (if (isSprintTab) SecondaryGold else PrimaryNeon)
                        else if (isClaimed) GreenSuccess else BorderDark

                    Box(
                        modifier = Modifier
                            .width(118.dp)
                            .height(130.dp)
                            .background(
                                if (isCurrent) accentColor.copy(alpha = 0.15f) else SurfaceVariantDark,
                                RectangleShape
                            )
                            .border(1.dp, accentColor, RectangleShape)
                            .padding(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("DAY ${reward.day}", style = TrueIdleType.tag, color = if (isCurrent) accentColor else TextLight)
                                if (isClaimed) {
                                    Text("✓", style = TrueIdleType.tag, color = GreenSuccess)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (reward.gold > 0L) {
                                    Text("+${NumberFormatter.short(reward.gold)} Gold", style = TrueIdleType.meta.copy(fontSize = 9.sp), color = SecondaryGold)
                                }
                                if (reward.diamonds > 0L) {
                                    Text("+${reward.diamonds} Dia", style = TrueIdleType.meta.copy(fontSize = 9.sp), color = PrimaryNeon)
                                }
                                if (reward.faith > 0L) {
                                    Text("+${reward.faith} Faith", style = TrueIdleType.meta.copy(fontSize = 9.sp), color = ManaTeal)
                                }
                                if (reward.bones > 0L) {
                                    Text("+${reward.bones} Bones", style = TrueIdleType.meta.copy(fontSize = 9.sp), color = NecroPurple)
                                }
                                if (reward.potionName.isNotEmpty()) {
                                    Text("+${reward.potionCount} ${reward.potionName}", style = TrueIdleType.meta.copy(fontSize = 8.sp), color = TextLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isClaimed) GreenSuccess.copy(alpha = 0.2f)
                                        else if (isCurrent && canClaimToday) GreenSuccess.copy(alpha = 0.25f)
                                        else if (isCurrent) accentColor.copy(alpha = 0.15f)
                                        else Color(0xFF000000),
                                        RectangleShape
                                    )
                                    .border(1.dp, if (isClaimed) GreenSuccess else if (isCurrent && canClaimToday) GreenSuccess else if (isCurrent) accentColor else BorderDark, RectangleShape)
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isClaimed) "CLAIMED" else if (isCurrent && canClaimToday) "READY" else if (isCurrent) "WAITING" else "LOCKED",
                                    style = TrueIdleType.meta.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = if (isClaimed || (isCurrent && canClaimToday)) GreenSuccess else if (isCurrent) accentColor else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Claim Action and Advance Day Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onClaim(isSprintTab) },
                    enabled = canClaimToday && !isComplete,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSprintTab) SecondaryGold else PrimaryNeon,
                        contentColor = Color.Black,
                        disabledContainerColor = SurfaceVariantDark,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("claim_daily_login_btn")
                ) {
                    Text(
                        text = if (isComplete) "ALL REWARDS CLAIMED!"
                        else if (canClaimToday) "CLAIM DAY $nextDay REWARD"
                        else "CLAIMED TODAY (NEXT IN $countdownStr)",
                        style = TrueIdleType.label.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Advance to next day button for testing and simulation
                OutlinedButton(
                    onClick = onSimulateNextDay,
                    shape = RectangleShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SecondaryGold
                    ),
                    border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("simulate_next_day_btn")
                ) {
                    Text(
                        text = "NEXT DAY (TEST)",
                        style = TrueIdleType.meta.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
