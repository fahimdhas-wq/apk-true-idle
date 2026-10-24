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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*
import kotlin.math.max

enum class SkillSubSection(val label: String, val icon: String, val color: Color) {
    OVERVIEW("OVERVIEW", "⬡", PrimaryNeon),
    GATHERING("GATHERING", "🌿", GreenSuccess),
    CRAFTING("CRAFTING", "🔨", SecondaryGold),
    BEASTS("BEASTS", "🐾", GreenSuccess),
    NECROMANCY("NECRO", "💀", NecroPurple),
    TRAINING("TRAINING", "🥋", PrimaryNeon),
    SANCTIFIED("SANCTIFIED", "🕊", ManaTeal),
    SLAYER("SLAYER", "⚔", HpRed)
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

    // Intercept back button to return to overview if inside a sub-section
    BackHandler(enabled = currentSection != SkillSubSection.OVERVIEW) {
        currentSection = SkillSubSection.OVERVIEW
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Resource Header: GOLD · FAITH · SLAYER POINTS (§7.7)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF000000))
                .border(BorderStroke(1.dp, BorderDark), RectangleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🪙", style = TrueIdleType.value)
                Text(NumberFormatter.short(state.player.gold), style = TrueIdleType.value, color = SecondaryGold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🕊", style = TrueIdleType.value)
                Text(NumberFormatter.short(state.sanctified.faith), style = TrueIdleType.value, color = ManaTeal)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⚔", style = TrueIdleType.value)
                Text("${state.slayer.slayerPoints} PTS", style = TrueIdleType.value, color = HpRed)
            }
        }

        // True Idle Live internal navigation tab bar
        val navScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF000000))
                .border(BorderStroke(1.dp, BorderDark), RectangleShape)
                .horizontalScroll(navScroll)
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .testTag("skills_sub_nav_row"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SkillSubSection.values().forEach { section ->
                val isSelected = (currentSection == section)
                val levelText = when (section) {
                    SkillSubSection.OVERVIEW -> ""
                    SkillSubSection.GATHERING -> "LV ${max(state.gathering.farmingLvl, state.gathering.miningLvl)}"
                    SkillSubSection.CRAFTING -> "LV ${max(state.crafting.smithingLvl, state.crafting.herbloreLvl)}"
                    SkillSubSection.BEASTS -> "LV ${state.beasts.lvl}"
                    SkillSubSection.NECROMANCY -> "LV ${state.necro.lvl}"
                    SkillSubSection.TRAINING -> "LV ${state.training.lvl}"
                    SkillSubSection.SANCTIFIED -> "LV ${state.sanctified.lvl}"
                    SkillSubSection.SLAYER -> "LV ${state.slayer.lvl}"
                }

                Box(
                    modifier = Modifier
                        .clickable { currentSection = section }
                        .background(
                            if (isSelected) section.color.copy(alpha = 0.2f) else SurfaceDark,
                            RectangleShape
                        )
                        .border(
                            BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) section.color else BorderDark
                            ),
                            RectangleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("skills_sub_tab_${section.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = section.icon,
                            style = TrueIdleType.label,
                            color = if (isSelected) section.color else TextMuted
                        )
                        Text(
                            text = section.label,
                            style = TrueIdleType.tag,
                            color = if (isSelected) section.color else TextMuted
                        )
                        if (levelText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) section.color.copy(alpha = 0.25f) else SurfaceVariantDark,
                                        RectangleShape
                                    )
                                    .border(
                                        0.5.dp,
                                        if (isSelected) section.color else BorderDark,
                                        RectangleShape
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = levelText,
                                    style = TrueIdleType.meta,
                                    color = if (isSelected) section.color else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Content
        when (currentSection) {
            SkillSubSection.OVERVIEW -> SkillsOverviewPage(
                state = state,
                onNavigate = { currentSection = it }
            )
            SkillSubSection.GATHERING -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // GATHERING MATRIX (FARMING & MINING)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                GatheringScreen(
                    state = state,
                    onHarvestPlot = onHarvestPlot,
                    onMineVein = onMineVein
                )
            }
            SkillSubSection.CRAFTING -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // CRAFTING PROTOCOLS (SMITHING & ALCHEMY)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                CraftingScreen(
                    state = state,
                    onCraftEquipment = onCraftEquipment,
                    onBrewPotion = onBrewPotion
                )
            }
            SkillSubSection.BEASTS -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // BEAST SANCTUARY (TAMING & SYNTHESIS)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                BeastsScreen(
                    state = state,
                    onMergeBeasts = onMergeBeasts
                )
            }
            SkillSubSection.NECROMANCY -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // ALTAR OF BONES (NECROMANCY PROTOCOLS)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                NecromancyScreen(
                    state = state,
                    onSummonMinion = onSummonMinion
                )
            }
            SkillSubSection.TRAINING -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // NEURAL DOJO (CORE TRAINING)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                TrainingScreen(
                    state = state,
                    onTrainNode = onTrainNode
                )
            }
            SkillSubSection.SANCTIFIED -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // SANCTIFIED CONCLAVE (FAITH & SMITE)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                SanctifiedScreen(
                    state = state,
                    onToggleAegis = onToggleSanctifiedAegis,
                    onUpgradeSmite = onUpgradeSanctifiedSmite
                )
            }
            SkillSubSection.SLAYER -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("SKILLS // SLAYER SECTOR (DIRECTIVES & HUNT)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                SlayerScreen(
                    state = state,
                    onRerollTask = onRerollSlayerTask
                )
            }
        }
    }
}

@Composable
fun SubSectionHeader(
    title: String,
    onBackToOverview: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
            .border(BorderStroke(1.dp, BorderDark), RectangleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = TrueIdleType.tag,
            color = PrimaryNeon,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = onBackToOverview,
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PrimaryNeon.copy(alpha = 0.12f),
                contentColor = PrimaryNeon
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("◀ OVERVIEW", style = TrueIdleType.tag)
        }
    }
}

@Composable
fun SkillsOverviewPage(
    state: GameState,
    onNavigate: (SkillSubSection) -> Unit
) {
    val scrollState = rememberScrollState()

    val totalSkillLevel = state.player.lvl + state.gathering.farmingLvl + state.gathering.miningLvl +
            state.crafting.smithingLvl + state.crafting.herbloreLvl +
            state.beasts.lvl + state.necro.lvl + state.training.lvl +
            state.sanctified.lvl + state.slayer.lvl

    val growingPlots = state.gathering.plots.count { !it.isReady }
    val readyPlots = state.gathering.plots.count { it.isReady }
    val activeVeins = state.gathering.veins.count { !it.isReady }
    val readyVeins = state.gathering.veins.count { it.isReady }
    val oresInSupply = state.inventory.filter { it.category == "Resource" || it.name.contains("Ore", ignoreCase = true) }.sumOf { it.count }
    val potionsReady = state.inventory.filter { it.category == "Potion" }.sumOf { it.count }
    val runningCount = (if (growingPlots > 0 || readyPlots > 0) 1 else 0) +
            (if (activeVeins > 0 || readyVeins > 0) 1 else 0) +
            (if (state.beasts.beasts.isNotEmpty()) 1 else 0) +
            (if (state.necro.army.isNotEmpty()) 1 else 0) +
            (if (state.sanctified.aegisActive) 1 else 0) + 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Master Matrix Header (featuring HTML Active Skills '4 RUNNING' badge & Total Level)
        Card(
            modifier = Modifier.fillMaxWidth().testTag("skills_master_card"),
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
                    Column {
                        Text(
                            text = "ACTIVE DISCIPLINES",
                            style = TrueIdleType.title,
                            color = PrimaryNeon
                        )
                        Text(
                            text = "10 Interlocking automated systems",
                            style = TrueIdleType.meta,
                            color = TextMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                                .border(1.dp, PrimaryNeon, RectangleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$runningCount RUNNING",
                                style = TrueIdleType.tag,
                                color = PrimaryNeon
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(SecondaryGold.copy(alpha = 0.15f), RectangleShape)
                                .border(1.dp, SecondaryGold, RectangleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "TOTAL: $totalSkillLevel",
                                style = TrueIdleType.tag,
                                color = SecondaryGold
                            )
                        }
                    }
                }
            }
        }

        // 1. COMBAT DISCIPLINE
        SkillCategoryCard(
            title = "01. COMBAT DISCIPLINE",
            icon = "⚔",
            accentColor = PrimaryNeon,
            quickSummary = "FLOOR ${state.player.currentFloor} · ${state.lifetime.totalKills} TOTAL KILLS",
            actionLabel = "COMBAT",
            onAction = { /* Handled via primary nav */ }
        ) {
            XpProgressBar(
                skillName = "COMBAT LEVEL",
                level = state.player.lvl,
                currentXp = state.player.xp,
                requiredXp = Balance.playerXpReq(state.player.lvl),
                barColor = PrimaryNeon
            )
        }

        // 2. GATHERING SECTION CARD
        SkillCategoryCard(
            title = "02. GATHERING DISCIPLINE",
            icon = "🌿",
            accentColor = GreenSuccess,
            quickSummary = "$activeVeins VEINS ACTIVE · $growingPlots PLOTS GROWING · ($readyPlots PLOTS, $readyVeins VEINS READY)",
            actionLabel = "GATHERING",
            onAction = { onNavigate(SkillSubSection.GATHERING) }
        ) {
            XpProgressBar(
                skillName = "BIO-FARMING",
                level = state.gathering.farmingLvl,
                currentXp = state.gathering.farmingXp,
                requiredXp = Balance.skillXpReq(state.gathering.farmingLvl),
                barColor = GreenSuccess
            )
            Spacer(modifier = Modifier.height(4.dp))
            XpProgressBar(
                skillName = "ORE MINING",
                level = state.gathering.miningLvl,
                currentXp = state.gathering.miningXp,
                requiredXp = Balance.skillXpReq(state.gathering.miningLvl),
                barColor = SecondaryGold
            )
        }

        // 3. CRAFTING SECTION CARD
        SkillCategoryCard(
            title = "03. CRAFTING PROTOCOLS",
            icon = "🔨",
            accentColor = SecondaryGold,
            quickSummary = "$oresInSupply ORES IN SUPPLY · $potionsReady POTIONS READY",
            actionLabel = "CRAFTING",
            onAction = { onNavigate(SkillSubSection.CRAFTING) }
        ) {
            XpProgressBar(
                skillName = "CYBER SMITHING",
                level = state.crafting.smithingLvl,
                currentXp = state.crafting.smithingXp,
                requiredXp = Balance.skillXpReq(state.crafting.smithingLvl),
                barColor = SecondaryGold
            )
            Spacer(modifier = Modifier.height(4.dp))
            XpProgressBar(
                skillName = "HERBLORE LAB",
                level = state.crafting.herbloreLvl,
                currentXp = state.crafting.herbloreXp,
                requiredXp = Balance.skillXpReq(state.crafting.herbloreLvl),
                barColor = GreenSuccess
            )
        }

        // 4. BEAST TAMING SECTION CARD
        val totalBeastDps = state.beasts.beasts.sumOf { it.dps * it.rank * (1.0 + (it.stars - 1) * 0.25) }
        SkillCategoryCard(
            title = "04. BEAST TAMING",
            icon = "🐾",
            accentColor = GreenSuccess,
            quickSummary = "${state.beasts.beasts.size}/8 BEASTS | ${totalBeastDps.toInt()} TOTAL DPS",
            actionLabel = "BEASTS",
            onAction = { onNavigate(SkillSubSection.BEASTS) }
        ) {
            XpProgressBar(
                skillName = "BEAST TAMING",
                level = state.beasts.lvl,
                currentXp = state.beasts.xp,
                requiredXp = Balance.skillXpReq(state.beasts.lvl),
                barColor = GreenSuccess
            )
        }

        // 5. NECROMANCY SECTION CARD
        val totalArmyDps = state.necro.army.sumOf { it.dps * it.count * (1.0 + (it.lvl - 1) * 0.2) }
        val totalMinions = state.necro.army.sumOf { it.count }
        SkillCategoryCard(
            title = "05. NECROMANCY DISCIPLINE",
            icon = "💀",
            accentColor = NecroPurple,
            quickSummary = "${state.necro.boneEssence} 💀 ESSENCE | $totalMinions UNITS (${totalArmyDps.toInt()} DPS)",
            actionLabel = "NECRO",
            onAction = { onNavigate(SkillSubSection.NECROMANCY) }
        ) {
            XpProgressBar(
                skillName = "NECROMANCY",
                level = state.necro.lvl,
                currentXp = state.necro.xp,
                requiredXp = Balance.skillXpReq(state.necro.lvl),
                barColor = NecroPurple
            )
        }

        // 6. TRAINING SECTION CARD
        val totalRanks = state.training.nodes.sumOf { it.rank }
        SkillCategoryCard(
            title = "06. NEURAL DOJO TRAINING",
            icon = "🥋",
            accentColor = PrimaryNeon,
            quickSummary = "$totalRanks RANKS ACQUIRED | PERMANENT STAT BUFFS",
            actionLabel = "TRAINING",
            onAction = { onNavigate(SkillSubSection.TRAINING) }
        ) {
            XpProgressBar(
                skillName = "CORE TRAINING",
                level = state.training.lvl,
                currentXp = state.training.xp,
                requiredXp = Balance.skillXpReq(state.training.lvl),
                barColor = PrimaryNeon
            )
        }

        // 7. SANCTIFIED SECTION CARD
        SkillCategoryCard(
            title = "07. SANCTIFIED DISCIPLINE",
            icon = "🕊",
            accentColor = ManaTeal,
            quickSummary = "${state.sanctified.faith} FAITH | SMITE LVL ${state.sanctified.smiteLevel} (${if (state.sanctified.aegisActive) "AEGIS ON" else "AEGIS OFF"})",
            actionLabel = "SANCTIFIED",
            onAction = { onNavigate(SkillSubSection.SANCTIFIED) }
        ) {
            XpProgressBar(
                skillName = "SANCTIFIED",
                level = state.sanctified.lvl,
                currentXp = state.sanctified.xp,
                requiredXp = Balance.skillXpReq(state.sanctified.lvl),
                barColor = ManaTeal
            )
        }

        // 8. SLAYER SECTION CARD
        SkillCategoryCard(
            title = "08. SLAYER DIRECTIVES",
            icon = "⚔",
            accentColor = HpRed,
            quickSummary = "TARGET: ${state.slayer.currentTask.targetName.uppercase()} (${state.slayer.currentTask.currentKills}/${state.slayer.currentTask.requiredKills}) | ${state.slayer.slayerPoints} PTS",
            actionLabel = "SLAYER",
            onAction = { onNavigate(SkillSubSection.SLAYER) }
        ) {
            XpProgressBar(
                skillName = "SLAYER",
                level = state.slayer.lvl,
                currentXp = state.slayer.xp,
                requiredXp = Balance.slayerXpReq(state.slayer.lvl),
                barColor = HpRed
            )
        }
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
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RectangleShape,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(icon, style = TrueIdleType.title)
                    Text(
                        text = title,
                        style = TrueIdleType.title,
                        color = accentColor
                    )
                }

                OutlinedButton(
                    onClick = onAction,
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, accentColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = accentColor.copy(alpha = 0.15f),
                        contentColor = accentColor
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$actionLabel ➔",
                        style = TrueIdleType.tag
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = quickSummary,
                style = TrueIdleType.meta,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))

            content()
        }
    }
}
