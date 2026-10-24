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
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*

enum class SkillSubSection(val label: String) {
    OVERVIEW("OVERVIEW"),
    GATHERING("GATHERING"),
    CRAFTING("CRAFTING"),
    BEASTS("BEASTS"),
    NECROMANCY("NECRO"),
    TRAINING("TRAINING")
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
        // High-End Cyberpunk Sub-Category Navigation Bar
        val navScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF000000))
                .border(BorderStroke(1.dp, BorderDark), RectangleShape)
                .horizontalScroll(navScroll)
                .padding(4.dp)
                .testTag("skills_sub_nav_row"),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SkillSubSection.values().forEach { section ->
                val isSelected = (currentSection == section)
                Box(
                    modifier = Modifier
                        .clickable { currentSection = section }
                        .background(
                            if (isSelected) PrimaryNeon.copy(alpha = 0.15f) else SurfaceDark,
                            RectangleShape
                        )
                        .border(
                            BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) PrimaryNeon else BorderDark
                            ),
                            RectangleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("skills_sub_tab_${section.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) PrimaryNeon else TextMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
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
                SubSectionHeader("GATHERING MATRIX (FARMING & MINING)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                GatheringScreen(
                    state = state,
                    onHarvestPlot = onHarvestPlot,
                    onMineVein = onMineVein
                )
            }
            SkillSubSection.CRAFTING -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("CRAFTING PROTOCOLS (SMITHING & HERBLORE)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                CraftingScreen(
                    state = state,
                    onCraftEquipment = onCraftEquipment,
                    onBrewPotion = onBrewPotion
                )
            }
            SkillSubSection.BEASTS -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("BEAST SANCTUARY (TAMING & SYNTHESIS)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                BeastsScreen(
                    state = state,
                    onMergeBeasts = onMergeBeasts
                )
            }
            SkillSubSection.NECROMANCY -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("ALTAR OF BONES (NECROMANCY PROTOCOLS)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                NecromancyScreen(
                    state = state,
                    onSummonMinion = onSummonMinion
                )
            }
            SkillSubSection.TRAINING -> Column(modifier = Modifier.fillMaxSize()) {
                SubSectionHeader("NEURAL DOJO (CORE TRAINING)") {
                    currentSection = SkillSubSection.OVERVIEW
                }
                TrainingScreen(
                    state = state,
                    onTrainNode = onTrainNode
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
            .background(SurfaceDark)
            .border(BorderStroke(1.dp, BorderDark), RectangleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryNeon,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        OutlinedButton(
            onClick = onBackToOverview,
            shape = RectangleShape,
            border = BorderStroke(1.dp, BorderDark),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceVariantDark,
                contentColor = TextLight
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("◀ OVERVIEW", fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SkillsOverviewPage(
    state: GameState,
    onNavigate: (SkillSubSection) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Master Matrix Header
        Card(
            modifier = Modifier.fillMaxWidth().testTag("skills_master_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SKILLS OVERVIEW & PROGRESSION",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryNeon,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Active progression matrix for all 6 disciplines. Each skill unlocks passive perks, production yields, and combat stats.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // 1. GATHERING SECTION
        SkillCategoryCard(
            title = "01. GATHERING DISCIPLINE",
            accentColor = GreenSuccess,
            description = "Farming herb plots and excavating mining veins.",
            actionLabel = "OPEN GATHERING",
            onAction = { onNavigate(SkillSubSection.GATHERING) }
        ) {
            XpProgressBar(
                skillName = "🌿 FARMING",
                level = state.gathering.farmingLvl,
                currentXp = state.gathering.farmingXp,
                requiredXp = Balance.skillXpReq(state.gathering.farmingLvl),
                barColor = GreenSuccess
            )
            Spacer(modifier = Modifier.height(6.dp))
            XpProgressBar(
                skillName = "⛏ MINING",
                level = state.gathering.miningLvl,
                currentXp = state.gathering.miningXp,
                requiredXp = Balance.skillXpReq(state.gathering.miningLvl),
                barColor = SecondaryGold
            )
        }

        // 2. CRAFTING SECTION
        SkillCategoryCard(
            title = "02. CRAFTING PROTOCOLS",
            accentColor = SecondaryGold,
            description = "Smithing weapons and armors; alchemy flask synthesis.",
            actionLabel = "OPEN CRAFTING",
            onAction = { onNavigate(SkillSubSection.CRAFTING) }
        ) {
            XpProgressBar(
                skillName = "🔨 SMITHING",
                level = state.crafting.smithingLvl,
                currentXp = state.crafting.smithingXp,
                requiredXp = Balance.skillXpReq(state.crafting.smithingLvl),
                barColor = SecondaryGold
            )
            Spacer(modifier = Modifier.height(6.dp))
            XpProgressBar(
                skillName = "🧪 HERBLORE",
                level = state.crafting.herbloreLvl,
                currentXp = state.crafting.herbloreXp,
                requiredXp = Balance.skillXpReq(state.crafting.herbloreLvl),
                barColor = GreenSuccess
            )
        }

        // 3. BEAST TAMING SECTION
        SkillCategoryCard(
            title = "03. BEAST TAMING",
            accentColor = GreenSuccess,
            description = "Taming combat companions and star merging for higher DPS.",
            actionLabel = "MANAGE BEASTS",
            onAction = { onNavigate(SkillSubSection.BEASTS) }
        ) {
            XpProgressBar(
                skillName = "🐾 BEAST TAMING",
                level = state.beasts.lvl,
                currentXp = state.beasts.xp,
                requiredXp = Balance.skillXpReq(state.beasts.lvl),
                barColor = GreenSuccess
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ACTIVE ROSTER: ${state.beasts.beasts.size}/8 BEASTS | TROPHIES: ${state.beasts.trophies}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 9.sp
            )
        }

        // 4. NECROMANCY SECTION
        SkillCategoryCard(
            title = "04. NECROMANCY DISCIPLINE",
            accentColor = NecroPurple,
            description = "Raising skeletal legions and bone sorcerers using essence.",
            actionLabel = "ENTER ALTAR",
            onAction = { onNavigate(SkillSubSection.NECROMANCY) }
        ) {
            XpProgressBar(
                skillName = "💀 NECROMANCY",
                level = state.necro.lvl,
                currentXp = state.necro.xp,
                requiredXp = Balance.skillXpReq(state.necro.lvl),
                barColor = NecroPurple
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "BONE ESSENCE: ${state.necro.boneEssence} 💀 | LEGION COUNT: ${state.necro.army.sumOf { it.count }} UNITS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 9.sp
            )
        }

        // 5. TRAINING SECTION
        SkillCategoryCard(
            title = "05. NEURAL DOJO TRAINING",
            accentColor = PrimaryNeon,
            description = "Permanent attribute rank enhancements funded by gold credits.",
            actionLabel = "UPGRADE DOJO",
            onAction = { onNavigate(SkillSubSection.TRAINING) }
        ) {
            XpProgressBar(
                skillName = "🥋 TRAINING",
                level = state.training.lvl,
                currentXp = state.training.xp,
                requiredXp = Balance.skillXpReq(state.training.lvl),
                barColor = PrimaryNeon
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TALENT POINTS: ${state.training.talentPoints} | ENHANCED NODES: ${state.training.nodes.sumOf { it.rank }} RANKS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun SkillCategoryCard(
    title: String,
    accentColor: Color,
    description: String,
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
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
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            content()
        }
    }
}
