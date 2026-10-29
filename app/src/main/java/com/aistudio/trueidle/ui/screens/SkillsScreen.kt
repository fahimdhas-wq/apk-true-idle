package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*
import kotlin.math.max

enum class SkillSubSection(val label: String, val icon: String, val color: Color) {
    OVERVIEW("OVERVIEW", "[+]", PrimaryNeon),
    HARVEST("HARVEST", "[H]", GreenSuccess),
    FORGE("FORGE", "[F]", SecondaryGold),
    ARMORY("ARMORY", "[A]", SecondaryGold),
    GENESIS("GENESIS", "[G]", NecroPurple),
    ESSENCE("ESSENCE", "[E]", ManaTeal),
    TRAINING("TRAINING", "[T]", PrimaryNeon),
    SLAYER("SLAYER", "[S]", HpRed),
    TALENTS("TALENTS", "[★]", SecondaryGold)
}

@Composable
fun SkillsScreen(
    state: GameState,
    /** Hoisted so a tab switch can open the right sub-section (e.g. MANAGE SQUAD). */
    currentSection: SkillSubSection,
    onSectionChange: (SkillSubSection) -> Unit,
    onHarvestPlot: (Int) -> Unit,
    onMineVein: (Int) -> Unit,
    onTogglePlotAutoQueue: (Int) -> Unit = {},
    onToggleVeinAutoQueue: (Int) -> Unit = {},
    onCraftEquipment: (String, Int, String, String, Int) -> Unit,
    onBrewPotion: (String, String, Int) -> Unit,
    onMergeBeasts: (String, String) -> Unit,
    onSummonMinion: (String) -> Unit,
    onTrainNode: (String) -> Unit,
    onToggleSanctifiedAegis: () -> Unit = {},
    onUpgradeSanctifiedSmite: () -> Unit = {},
    onRerollSlayerTask: () -> Unit = {},
    onUpgradeDiscipline: (DisciplineCategory, String) -> Unit = { _, _ -> },
    onActivateTalent: (String) -> Unit = {},
    onUpgradeTalent: (String) -> Unit = {},
    onUnequipItem: (String) -> Unit = {},
    onAutoEquip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // High-Tech Cyber Header Strip
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RectangleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResourceReadout("CR", NumberFormatter.short(state.player.gold), SecondaryGold)
            ResourceReadout("FA", NumberFormatter.short(state.essence.faith), ManaTeal)
            ResourceReadout("SP", "${NumberFormatter.short(state.slayer.slayerPoints)} PTS", HpRed)
            ResourceReadout("TP", "${NumberFormatter.short(state.talent.talentPoints)} TP", PrimaryNeon)
        }

        // Sub-Navigation Tabs with Uniform Alignment
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RectangleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag("skills_sub_nav_row"),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SkillSubSection.entries.forEach { section ->
                val selected = currentSection == section
                val level = when (section) {
                    SkillSubSection.OVERVIEW -> ""
                    SkillSubSection.HARVEST -> "LV ${NumberFormatter.short(max(state.harvest.farmingLvl, state.harvest.miningLvl))}"
                    SkillSubSection.FORGE -> "LV ${NumberFormatter.short(state.armory.smithingLvl)}"
                    SkillSubSection.ARMORY -> "LV ${NumberFormatter.short(max(state.armory.smithingLvl, state.armory.herbloreLvl))}"
                    SkillSubSection.GENESIS -> "LV ${NumberFormatter.short(max(state.beasts.lvl, state.necro.lvl))}"
                    SkillSubSection.ESSENCE -> "LV ${NumberFormatter.short(state.essence.lvl)}"
                    SkillSubSection.TRAINING -> "LV ${NumberFormatter.short(state.training.lvl)}"
                    SkillSubSection.SLAYER -> "LV ${NumberFormatter.short(state.slayer.lvl)}"
                    SkillSubSection.TALENTS -> "${NumberFormatter.short(state.talent.talentPoints)} TP"
                }
                Row(
                    Modifier
                        .clickable(role = Role.Tab) { onSectionChange(section) }
                        .heightIn(min = 48.dp)
                        .background(if (selected) section.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                        .border(1.dp, if (selected) section.color else MaterialTheme.colorScheme.outline, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("skills_sub_tab_${section.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Decorative: the bracket glyph must not be announced alongside the label.
                    Text(
                        text = section.icon,
                        style = TrueIdleType.tag,
                        color = if (selected) section.color else TextMuted,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                    Text(section.label, style = TrueIdleType.tag, color = if (selected) section.color else TextMuted)
                    if (level.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(if (selected) section.color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (selected) section.color else MaterialTheme.colorScheme.outline)
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(level, style = TrueIdleType.numberSmall, color = if (selected) section.color else TextMuted)
                        }
                    }
                }
            }
        }

        when (currentSection) {
            SkillSubSection.OVERVIEW -> SkillsOverviewPage(state) { onSectionChange(it) }
            SkillSubSection.HARVEST -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // HARVEST & EXTRACTION MATRIX") { onSectionChange(SkillSubSection.OVERVIEW) }
                HarvestScreen(
                    state = state,
                    onHarvestPlot = onHarvestPlot,
                    onMineVein = onMineVein,
                    onTogglePlotAutoQueue = onTogglePlotAutoQueue,
                    onToggleVeinAutoQueue = onToggleVeinAutoQueue,
                    onUpgradeDiscipline = onUpgradeDiscipline
                )
            }
            SkillSubSection.FORGE -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // FORGE FABRICATION MATRIX") { onSectionChange(SkillSubSection.OVERVIEW) }
                ForgeScreen(
                    state = state,
                    onCraftEquipment = onCraftEquipment,
                    onBrewPotion = onBrewPotion
                )
            }
            SkillSubSection.ARMORY -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // ARMORY & TACTICAL LOADOUT") { onSectionChange(SkillSubSection.OVERVIEW) }
                ArmoryScreen(
                    state = state,
                    onCraftEquipment = onCraftEquipment,
                    onBrewPotion = onBrewPotion,
                    onUpgradeDiscipline = onUpgradeDiscipline,
                    onUnequipItem = onUnequipItem,
                    onAutoEquip = onAutoEquip,
                    onOpenForge = { onSectionChange(SkillSubSection.FORGE) }
                )
            }
            SkillSubSection.GENESIS -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // GENESIS BIO-REANIMATION") { onSectionChange(SkillSubSection.OVERVIEW) }
                GenesisScreen(
                    state = state,
                    onMergeBeasts = onMergeBeasts,
                    onSummonMinion = onSummonMinion,
                    onUpgradeDiscipline = onUpgradeDiscipline
                )
            }
            SkillSubSection.ESSENCE -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // ESSENCE PROTOCOL") { onSectionChange(SkillSubSection.OVERVIEW) }
                EssenceScreen(
                    state = state,
                    onToggleAegis = onToggleSanctifiedAegis,
                    onUpgradeSmite = onUpgradeSanctifiedSmite,
                    onUpgradeDiscipline = onUpgradeDiscipline
                )
            }
            SkillSubSection.TRAINING -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // NEURAL ENHANCEMENT BAY") { onSectionChange(SkillSubSection.OVERVIEW) }
                TrainingScreen(state = state, onTrainNode = onTrainNode)
            }
            SkillSubSection.SLAYER -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // TARGETING DIRECTIVES") { onSectionChange(SkillSubSection.OVERVIEW) }
                SlayerScreen(state = state, onRerollTask = onRerollSlayerTask)
            }
            SkillSubSection.TALENTS -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // TACTICAL & STRUGGLE TALENTS") { onSectionChange(SkillSubSection.OVERVIEW) }
                TalentScreen(
                    state = state,
                    onActivateTalent = onActivateTalent,
                    onUpgradeTalent = onUpgradeTalent
                )
            }
        }
    }
}

@Composable
private fun ResourceReadout(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.15f), RectangleShape)
                .border(1.dp, color.copy(alpha = 0.5f), RectangleShape)
                .padding(horizontal = 3.dp, vertical = 2.dp)
        ) {
            Text(label, style = TrueIdleType.tag, color = color)
        }
        Text(value, style = TrueIdleType.value, color = color)
    }
}

@Composable
fun SubSectionHeader(title: String, onBackToOverview: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = TrueIdleType.title, color = PrimaryNeon, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onBackToOverview,
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PrimaryNeon.copy(alpha = 0.12f),
                contentColor = PrimaryNeon
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text("◀ OVERVIEW", style = TrueIdleType.tag)
        }
    }
}

@Composable
fun SkillsOverviewPage(state: GameState, onNavigate: (SkillSubSection) -> Unit) {
    val scrollState = rememberScrollState()
    val derived = com.aistudio.trueidle.ui.LocalDerived.current
    val totalLevel = state.player.lvl + state.harvest.farmingLvl + state.harvest.miningLvl +
        state.armory.smithingLvl + state.armory.herbloreLvl + state.beasts.lvl + state.necro.lvl +
        state.training.lvl + state.essence.lvl + state.slayer.lvl
    val active = (if (state.harvest.plots.isNotEmpty()) 1 else 0) +
        (if (state.harvest.veins.isNotEmpty()) 1 else 0) + (if (state.beasts.beasts.isNotEmpty()) 1 else 0) +
        (if (state.necro.army.isNotEmpty()) 1 else 0) + (if (state.essence.aegisActive) 1 else 0) + 1
    // Ores are real ores, not every Resource (which also counted herbs).
    val ores = state.inventory.filter { ItemCatalog.isOre(it.name) }.sumOf { it.count }
    val potions = state.inventory.filter { it.category == ItemCatalog.CATEGORY_POTION }.sumOf { it.count }
    // Shares the engine's DPS formulas instead of re-deriving them in the UI.
    val beastDps = CombatMath.beastDps(state.beasts)
    val armyDps = CombatMath.armyDps(state.necro)
    val minions = state.necro.army.sumOf { it.count }
    val ranks = state.training.nodes.sumOf { it.rank }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // High-level Telemetry Tiles
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetricTile("DISCIPLINES", "$active/6 OPERATIONAL", PrimaryNeon, Modifier.weight(1f))
            MetricTile("TOTAL LEVEL", NumberFormatter.short(totalLevel.toLong()), SecondaryGold, Modifier.weight(1f))
        }

        Card(
            Modifier.fillMaxWidth().testTag("skills_master_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("SKILL NETWORK // AUTONOMOUS MATRIX", style = TrueIdleType.title, color = PrimaryNeon)
                Text("NEURAL PROGRESSION ACTIVE · ALL TELEMETRY SYNCHRONIZED", style = TrueIdleType.meta, color = TextMuted)
            }
        }

        // 01 COMBAT
        SkillCategoryCard(
            title = "01 // COMBAT",
            icon = "[C]",
            accentColor = PrimaryNeon,
            quickSummary = "SECTOR ${state.player.currentFloor} · ${NumberFormatter.short(state.lifetime.totalKills)} ELIMINATIONS",
            actionLabel = null,
            onAction = null,
            level = state.player.lvl,
            xp = state.player.xp,
            requiredXp = XpCurves.player(state.player.lvl),
            status = "COMBAT PROTOCOL"
        ) {
            XpProgressBar("COMBAT LEVEL", state.player.lvl, state.player.xp, XpCurves.player(state.player.lvl), PrimaryNeon)
        }

        // 02 HARVEST
        SkillCategoryCard(
            title = "02 // HARVEST",
            icon = "[H]",
            accentColor = GreenSuccess,
            quickSummary = "${state.harvest.plots.count { !it.isReady }} PLOTS · ${state.harvest.veins.count { !it.isReady }} VEINS ACTIVE",
            actionLabel = "OPEN",
            onAction = { onNavigate(SkillSubSection.HARVEST) },
            level = max(state.harvest.farmingLvl, state.harvest.miningLvl),
            xp = max(state.harvest.farmingXp, state.harvest.miningXp),
            requiredXp = XpCurves.skill(max(state.harvest.farmingLvl, state.harvest.miningLvl)),
            status = "EXTRACTION PROTOCOL"
        ) {
            XpProgressBar("BIO-FARMING", state.harvest.farmingLvl, state.harvest.farmingXp, XpCurves.skill(state.harvest.farmingLvl), GreenSuccess)
            Spacer(Modifier.height(3.dp))
            XpProgressBar("ORE MINING", state.harvest.miningLvl, state.harvest.miningXp, XpCurves.skill(state.harvest.miningLvl), SecondaryGold)
        }

        // 03 FORGE
        SkillCategoryCard(
            title = "03 // FORGE",
            icon = "[F]",
            accentColor = SecondaryGold,
            quickSummary = "13 EQUIPMENT GROUPS · 20 ALCHEMY POTIONS",
            actionLabel = "OPEN",
            onAction = { onNavigate(SkillSubSection.FORGE) },
            level = state.armory.smithingLvl,
            xp = state.armory.smithingXp,
            requiredXp = XpCurves.skill(state.armory.smithingLvl),
            status = "FABRICATION MATRIX"
        ) {
            XpProgressBar("SMITHING FORGE", state.armory.smithingLvl, state.armory.smithingXp, XpCurves.skill(state.armory.smithingLvl), SecondaryGold)
            Spacer(Modifier.height(3.dp))
            XpProgressBar("HERB SYNTHESIS", state.armory.herbloreLvl, state.armory.herbloreXp, XpCurves.skill(state.armory.herbloreLvl), GreenSuccess)
        }

        // 04 ARMORY
        SkillCategoryCard(
            title = "04 // ARMORY",
            icon = "[A]",
            accentColor = SecondaryGold,
            quickSummary = "13 GEAR SLOTS · TACTICAL LOADOUT & ENHANCEMENTS",
            actionLabel = "OPEN",
            onAction = { onNavigate(SkillSubSection.ARMORY) },
            level = max(state.armory.smithingLvl, state.armory.herbloreLvl),
            xp = max(state.armory.smithingXp, state.armory.herbloreXp),
            requiredXp = XpCurves.skill(max(state.armory.smithingLvl, state.armory.herbloreLvl)),
            status = "TACTICAL LOADOUT"
        ) {
            XpProgressBar("ARMORY MASTERY", max(state.armory.smithingLvl, state.armory.herbloreLvl), max(state.armory.smithingXp, state.armory.herbloreXp), XpCurves.skill(max(state.armory.smithingLvl, state.armory.herbloreLvl)), SecondaryGold)
        }

        // 04 GENESIS
        SkillCategoryCard(
            title = "04 // GENESIS",
            icon = "[G]",
            accentColor = NecroPurple,
            quickSummary = "${state.beasts.beasts.size}/${GameCaps.BEAST} BEASTS · ${NumberFormatter.short(state.necro.boneEssence)} ESSENCE · ${NumberFormatter.short(beastDps + armyDps)} TOTAL DPS",
            actionLabel = "OPEN",
            onAction = { onNavigate(SkillSubSection.GENESIS) },
            level = max(state.beasts.lvl, state.necro.lvl),
            xp = max(state.beasts.xp, state.necro.xp),
            requiredXp = XpCurves.skill(max(state.beasts.lvl, state.necro.lvl)),
            status = "BIO-REANIMATION"
        ) {
            XpProgressBar("BEAST TAMING", state.beasts.lvl, state.beasts.xp, XpCurves.skill(state.beasts.lvl), GreenSuccess)
            Spacer(Modifier.height(3.dp))
            XpProgressBar("REANIMATION CORPS", state.necro.lvl, state.necro.xp, XpCurves.skill(state.necro.lvl), NecroPurple)
        }

        // 05 ESSENCE
        SkillCategoryCard(
            title = "05 // ESSENCE",
            icon = "[E]",
            accentColor = ManaTeal,
            quickSummary = "${NumberFormatter.short(state.essence.faith)} FAITH · SMITE LV ${state.essence.smiteLevel} · ${if (state.essence.aegisActive) "AEGIS ONLINE" else "AEGIS OFFLINE"}",
            actionLabel = "OPEN",
            onAction = { onNavigate(SkillSubSection.ESSENCE) },
            level = state.essence.lvl,
            xp = state.essence.xp,
            requiredXp = XpCurves.skill(state.essence.lvl),
            status = "ENERGY SHIELD"
        ) {
            XpProgressBar("ESSENCE FIELD", state.essence.lvl, state.essence.xp, XpCurves.skill(state.essence.lvl), ManaTeal)
        }

        // 06 ENHANCEMENT
        SkillCategoryCard(
            title = "06 // TRAINING",
            icon = "[T]",
            accentColor = PrimaryNeon,
            quickSummary = "$ranks DOJO RANKS INSTALLED · PERMANENT BUFFS",
            actionLabel = "OPEN",
            onAction = { onNavigate(SkillSubSection.TRAINING) },
            level = state.training.lvl,
            xp = state.training.xp,
            requiredXp = XpCurves.skill(state.training.lvl),
            status = "NEURAL LINK"
        ) {
            XpProgressBar("CORE TRAINING", state.training.lvl, state.training.xp, XpCurves.skill(state.training.lvl), PrimaryNeon)
        }

        // 07 SLAYER
        SkillCategoryCard(
            title = "07 // SLAYER",
            icon = "[S]",
            accentColor = HpRed,
            quickSummary = "TARGET: ${state.slayer.currentTask.targetName.uppercase()} (${state.slayer.currentTask.currentKills}/${state.slayer.currentTask.requiredKills}) · ${NumberFormatter.short(state.slayer.slayerPoints)} PTS",
            actionLabel = "OPEN",
            onAction = { onNavigate(SkillSubSection.SLAYER) },
            level = state.slayer.lvl,
            xp = state.slayer.xp,
            requiredXp = XpCurves.skill(state.slayer.lvl),
            status = "ELIMINATION TARGET"
        ) {
            XpProgressBar("SLAYER MASTERY", state.slayer.lvl, state.slayer.xp, XpCurves.skill(state.slayer.lvl), HpRed)
        }

        // 08 TALENTS
        SkillCategoryCard(
            title = "08 // TALENTS & STRUGGLE",
            icon = "[★]",
            accentColor = SecondaryGold,
            quickSummary = "${NumberFormatter.short(state.talent.talentPoints)} TP AVAILABLE · ${NumberFormatter.short(state.talent.struggleDamageTaken)} DMG ENDURED",
            actionLabel = "OPEN",
            onAction = { onNavigate(SkillSubSection.TALENTS) },
            level = state.talent.activeTalents.sumOf { it.rank },
            xp = state.talent.struggleDamageTaken,
            requiredXp = XpCurves.skill(state.talent.activeTalents.sumOf { it.rank }.coerceAtLeast(1)),
            status = "TACTICAL MATRIX"
        ) {
            Text(
                text = "STRUGGLE BONUSES: +${NumberFormatter.short(derived.struggleHpBonus)} HP · +${NumberFormatter.short(derived.struggleDefBonus)} DEF · +${NumberFormatter.short(derived.struggleAtkBonus)} ATK",
                style = TrueIdleType.tag,
                color = SecondaryGold
            )
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RectangleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(label, style = TrueIdleType.tag, color = TextMuted)
            Spacer(Modifier.height(2.dp))
            Text(value, style = TrueIdleType.numberSmall, color = color)
        }
    }
}

@Composable
private fun SkillCategoryCard(
    title: String,
    icon: String,
    accentColor: Color,
    quickSummary: String,
    // Nullable action: the button is shown when an action exists rather than being hidden by a
    // string comparison against "TACTICAL".
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    level: Int,
    xp: Long,
    requiredXp: Long,
    status: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RectangleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, accentColor, RectangleShape)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(icon, style = TrueIdleType.tag, color = accentColor)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(title, style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                        .border(1.dp, accentColor)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(status, style = TrueIdleType.tag, color = accentColor)
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(quickSummary, style = TrueIdleType.meta, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            content()

            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onAction,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.2f),
                        contentColor = accentColor
                    ),
                    border = BorderStroke(1.dp, accentColor),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp)
                ) {
                    Text("ACCESS $actionLabel PROTOCOL", style = TrueIdleType.tag)
                }
            }
        }
    }
}
