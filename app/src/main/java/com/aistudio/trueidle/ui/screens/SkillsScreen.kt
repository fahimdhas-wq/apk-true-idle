package com.aistudio.trueidle.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SkillTelemetryStrip
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*
import kotlin.math.max

enum class SkillSubSection(val label: String, val icon: String, val color: Color) {
    OVERVIEW("OVERVIEW", "[+]", PrimaryNeon),
    GATHERING("GATHERING", "[G]", GreenSuccess),
    CRAFTING("CRAFTING", "[F]", SecondaryGold),
    BEASTS("BEASTS", "[B]", GreenSuccess),
    NECROMANCY("NECRO", "[N]", NecroPurple),
    TRAINING("TRAINING", "[T]", PrimaryNeon),
    SANCTIFIED("SANCTIFIED", "[S]", ManaTeal),
    SLAYER("SLAYER", "[X]", HpRed)
}

@Composable
fun SkillsScreen(
    state: GameState,
    onHarvestPlot: (Int) -> Unit,
    onMineVein: (Int) -> Unit,
    onCraftEquipment: (String, Int, String, String, Int) -> Unit,
    onBrewPotion: (String, String, Int) -> Unit,
    onMergeBeasts: (String, String) -> Unit,
    onSummonMinion: (String) -> Unit,
    onTrainNode: (String) -> Unit,
    onToggleSanctifiedAegis: () -> Unit = {},
    onUpgradeSanctifiedSmite: () -> Unit = {},
    onRerollSlayerTask: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentSection by remember { mutableStateOf(SkillSubSection.OVERVIEW) }
    BackHandler(enabled = currentSection != SkillSubSection.OVERVIEW) { currentSection = SkillSubSection.OVERVIEW }

    Column(modifier.fillMaxSize().background(BgDark)) {
        Row(
            Modifier.fillMaxWidth().background(SurfaceDark).border(1.dp, BorderNeon.copy(alpha = 0.32f), RectangleShape).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResourceReadout("CR", NumberFormatter.short(state.player.gold), SecondaryGold)
            ResourceReadout("FA", NumberFormatter.short(state.sanctified.faith), ManaTeal)
            ResourceReadout("SP", "${state.slayer.slayerPoints} PTS", HpRed)
        }

        Row(
            Modifier.fillMaxWidth().background(SurfaceDark).border(1.dp, BorderNeon.copy(alpha = 0.32f), RectangleShape)
                .horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 6.dp).testTag("skills_sub_nav_row"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SkillSubSection.values().forEach { section ->
                val selected = currentSection == section
                val level = when (section) {
                    SkillSubSection.OVERVIEW -> ""
                    SkillSubSection.GATHERING -> "LV ${max(state.gathering.farmingLvl, state.gathering.miningLvl)}"
                    SkillSubSection.CRAFTING -> "LV ${max(state.crafting.smithingLvl, state.crafting.herbloreLvl)}"
                    SkillSubSection.BEASTS -> "LV ${state.beasts.lvl}"
                    SkillSubSection.NECROMANCY -> "LV ${state.necro.lvl}"
                    SkillSubSection.TRAINING -> "LV ${state.training.lvl}"
                    SkillSubSection.SANCTIFIED -> "LV ${state.sanctified.lvl}"
                    SkillSubSection.SLAYER -> "LV ${state.slayer.lvl}"
                }
                Row(
                    Modifier.clickable { currentSection = section }
                        .background(if (selected) section.color.copy(alpha = 0.2f) else SurfaceVariantDark, RectangleShape)
                        .border(1.dp, if (selected) section.color else BorderDark, RectangleShape)
                        .padding(horizontal = 9.dp, vertical = 8.dp)
                        .testTag("skills_sub_tab_${section.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(section.icon, style = TrueIdleType.tag, color = if (selected) section.color else TextMuted)
                    Text(section.label, style = TrueIdleType.tag, color = if (selected) section.color else TextMuted)
                    if (level.isNotEmpty()) Text(level, style = TrueIdleType.meta, color = if (selected) section.color else TextMuted)
                }
            }
        }

        when (currentSection) {
            SkillSubSection.OVERVIEW -> SkillsOverviewPage(state) { currentSection = it }
            SkillSubSection.GATHERING -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // GATHERING MATRIX") { currentSection = SkillSubSection.OVERVIEW }
                GatheringScreen(state = state, onHarvestPlot = onHarvestPlot, onMineVein = onMineVein)
            }
            SkillSubSection.CRAFTING -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // FABRICATION BAY") { currentSection = SkillSubSection.OVERVIEW }
                CraftingScreen(state = state, onCraftEquipment = onCraftEquipment, onBrewPotion = onBrewPotion)
            }
            SkillSubSection.BEASTS -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // COMPANION NETWORK") { currentSection = SkillSubSection.OVERVIEW }
                BeastsScreen(state = state, onMergeBeasts = onMergeBeasts)
            }
            SkillSubSection.NECROMANCY -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // REANIMATION PROTOCOL") { currentSection = SkillSubSection.OVERVIEW }
                NecromancyScreen(state = state, onSummonMinion = onSummonMinion)
            }
            SkillSubSection.TRAINING -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // ENHANCEMENT BAY") { currentSection = SkillSubSection.OVERVIEW }
                TrainingScreen(state = state, onTrainNode = onTrainNode)
            }
            SkillSubSection.SANCTIFIED -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // SANCTIFIED FIELD") { currentSection = SkillSubSection.OVERVIEW }
                SanctifiedScreen(state = state, onToggleAegis = onToggleSanctifiedAegis, onUpgradeSmite = onUpgradeSanctifiedSmite)
            }
            SkillSubSection.SLAYER -> Column(Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // TARGETING DIRECTIVE") { currentSection = SkillSubSection.OVERVIEW }
                SlayerScreen(state = state, onRerollTask = onRerollSlayerTask)
            }
        }
    }
}

@Composable
private fun ResourceReadout(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = TrueIdleType.tag, color = TextMuted)
        Text(value, style = TrueIdleType.value, color = color)
    }
}

@Composable
fun SubSectionHeader(title: String, onBackToOverview: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(SurfaceDark).border(1.dp, BorderNeon.copy(alpha = 0.32f), RectangleShape).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = TrueIdleType.tag, color = PrimaryNeon, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onBackToOverview, shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = PrimaryNeon.copy(alpha = 0.12f), contentColor = PrimaryNeon),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) { Text("◀ OVERVIEW", style = TrueIdleType.tag) }
    }
}

@Composable
fun SkillsOverviewPage(state: GameState, onNavigate: (SkillSubSection) -> Unit) {
    val scrollState = rememberScrollState()
    val totalLevel = state.player.lvl + state.gathering.farmingLvl + state.gathering.miningLvl +
        state.crafting.smithingLvl + state.crafting.herbloreLvl + state.beasts.lvl + state.necro.lvl +
        state.training.lvl + state.sanctified.lvl + state.slayer.lvl
    val active = (if (state.gathering.plots.isNotEmpty()) 1 else 0) +
        (if (state.gathering.veins.isNotEmpty()) 1 else 0) + (if (state.beasts.beasts.isNotEmpty()) 1 else 0) +
        (if (state.necro.army.isNotEmpty()) 1 else 0) + (if (state.sanctified.aegisActive) 1 else 0) + 1
    val ores = state.inventory.filter { it.category == "Resource" || it.name.contains("Ore", true) }.sumOf { it.count }
    val potions = state.inventory.filter { it.category == "Potion" }.sumOf { it.count }
    val beastDps = state.beasts.beasts.sumOf { it.dps * it.rank * (1.0 + (it.stars - 1) * 0.25) }
    val armyDps = state.necro.army.sumOf { it.dps * it.count * (1.0 + (it.lvl - 1) * 0.2) }
    val minions = state.necro.army.sumOf { it.count }
    val ranks = state.training.nodes.sumOf { it.rank }

    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("SYSTEMS", "$active ONLINE", PrimaryNeon, Modifier.weight(1f))
            MetricTile("TOTAL LEVEL", totalLevel.toString(), SecondaryGold, Modifier.weight(1f))
        }
        Card(
            Modifier.fillMaxWidth().testTag("skills_master_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.65f))
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SKILL NETWORK // STATUS", style = TrueIdleType.title, color = PrimaryNeon)
                Text("ALL DISCIPLINES REPORTING · AUTONOMOUS OPERATIONS", style = TrueIdleType.meta, color = TextMuted)
            }
        }
        SkillCategoryCard("01 // COMBAT", "[C]", PrimaryNeon, "SECTOR ${state.player.currentFloor} · ${state.lifetime.totalKills} ELIMINATIONS", "OPEN", {}, state.player.lvl, state.player.xp, Balance.playerXpReq(state.player.lvl), "TACTICAL") {
            XpProgressBar("COMBAT LEVEL", state.player.lvl, state.player.xp, Balance.playerXpReq(state.player.lvl), PrimaryNeon)
        }
        SkillCategoryCard("02 // GATHERING", "[G]", GreenSuccess, "${state.gathering.plots.count { !it.isReady }} CULTURES · ${state.gathering.veins.count { !it.isReady }} VEINS ACTIVE", "OPEN", { onNavigate(SkillSubSection.GATHERING) }, max(state.gathering.farmingLvl, state.gathering.miningLvl), max(state.gathering.farmingXp, state.gathering.miningXp), Balance.skillXpReq(max(state.gathering.farmingLvl, state.gathering.miningLvl)), "EXTRACTION") {
            XpProgressBar("BIO-FARMING", state.gathering.farmingLvl, state.gathering.farmingXp, Balance.skillXpReq(state.gathering.farmingLvl), GreenSuccess)
            XpProgressBar("ORE MINING", state.gathering.miningLvl, state.gathering.miningXp, Balance.skillXpReq(state.gathering.miningLvl), SecondaryGold)
        }
        SkillCategoryCard("03 // FABRICATION", "[F]", SecondaryGold, "$ores RAW MATERIALS · $potions CHEMICALS", "OPEN", { onNavigate(SkillSubSection.CRAFTING) }, max(state.crafting.smithingLvl, state.crafting.herbloreLvl), max(state.crafting.smithingXp, state.crafting.herbloreXp), Balance.skillXpReq(max(state.crafting.smithingLvl, state.crafting.herbloreLvl)), "ASSEMBLY") {
            XpProgressBar("SMITHING", state.crafting.smithingLvl, state.crafting.smithingXp, Balance.skillXpReq(state.crafting.smithingLvl), SecondaryGold)
            XpProgressBar("HERBLORE", state.crafting.herbloreLvl, state.crafting.herbloreXp, Balance.skillXpReq(state.crafting.herbloreLvl), GreenSuccess)
        }
        SkillCategoryCard("04 // COMPANIONS", "[B]", GreenSuccess, "${state.beasts.beasts.size}/8 UNITS · ${beastDps.toInt()} DPS", "OPEN", { onNavigate(SkillSubSection.BEASTS) }, state.beasts.lvl, state.beasts.xp, Balance.skillXpReq(state.beasts.lvl), "BIO-LINK") {
            XpProgressBar("COMPANION BOND", state.beasts.lvl, state.beasts.xp, Balance.skillXpReq(state.beasts.lvl), GreenSuccess)
        }
        SkillCategoryCard("05 // NECROMANCY", "[N]", NecroPurple, "${state.necro.boneEssence} ESSENCE · $minions UNITS · ${armyDps.toInt()} DPS", "OPEN", { onNavigate(SkillSubSection.NECROMANCY) }, state.necro.lvl, state.necro.xp, Balance.skillXpReq(state.necro.lvl), "REANIMATION") {
            XpProgressBar("NECROMANCY", state.necro.lvl, state.necro.xp, Balance.skillXpReq(state.necro.lvl), NecroPurple)
        }
        SkillCategoryCard("06 // ENHANCEMENT", "[T]", PrimaryNeon, "$ranks RANKS INSTALLED · PERMANENT BUFFS", "OPEN", { onNavigate(SkillSubSection.TRAINING) }, state.training.lvl, state.training.xp, Balance.skillXpReq(state.training.lvl), "NEURAL LINK") {
            XpProgressBar("CORE TRAINING", state.training.lvl, state.training.xp, Balance.skillXpReq(state.training.lvl), PrimaryNeon)
        }
        SkillCategoryCard("07 // SANCTIFIED", "[S]", ManaTeal, "${state.sanctified.faith} CHARGE · SMITE ${state.sanctified.smiteLevel} · ${if (state.sanctified.aegisActive) "FIELD ON" else "FIELD OFF"}", "OPEN", { onNavigate(SkillSubSection.SANCTIFIED) }, state.sanctified.lvl, state.sanctified.xp, Balance.skillXpReq(state.sanctified.lvl), "ENERGY FIELD") {
            XpProgressBar("SANCTIFIED", state.sanctified.lvl, state.sanctified.xp, Balance.skillXpReq(state.sanctified.lvl), ManaTeal)
        }
        SkillCategoryCard("08 // SLAYER", "[X]", HpRed, "TARGET ${state.slayer.currentTask.targetName.uppercase()} · ${state.slayer.currentTask.currentKills}/${state.slayer.currentTask.requiredKills} · ${state.slayer.slayerPoints} PTS", "OPEN", { onNavigate(SkillSubSection.SLAYER) }, state.slayer.lvl, state.slayer.xp, Balance.slayerXpReq(state.slayer.lvl), "TARGETING") {
            XpProgressBar("SLAYER", state.slayer.lvl, state.slayer.xp, Balance.slayerXpReq(state.slayer.lvl), HpRed)
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(SurfaceDark).border(1.dp, color.copy(alpha = 0.4f), RectangleShape).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(label, style = TrueIdleType.meta, color = TextMuted)
        Text(value, style = TrueIdleType.tag, color = color)
    }
}

@Composable
fun SkillCategoryCard(
    title: String,
    icon: String,
    accentColor: Color,
    quickSummary: String,
    actionLabel: String,
    onAction: () -> Unit,
    level: Int,
    xp: Long,
    requiredXp: Long,
    status: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RectangleShape, border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(icon, style = TrueIdleType.tag, color = accentColor)
                    Column(Modifier.weight(1f)) {
                        Text(title, style = TrueIdleType.title, color = TextLight, maxLines = 1)
                        Text(quickSummary, style = TrueIdleType.meta, color = TextMuted, maxLines = 1)
                    }
                }
                OutlinedButton(
                    onClick = onAction, shape = RectangleShape, border = BorderStroke(1.dp, accentColor),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = accentColor.copy(alpha = 0.12f), contentColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) { Text(actionLabel, style = TrueIdleType.tag) }
            }
            SkillTelemetryStrip(status, level, xp, requiredXp, accentColor)
            content()
        }
    }
}
