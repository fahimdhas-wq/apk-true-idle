package com.aistudio.trueidle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.trueidle.ui.screens.*
import com.aistudio.trueidle.ui.theme.*
import com.aistudio.trueidle.viewmodel.GameViewModel

enum class TrueIdleTab(val label: String, val icon: ImageVector) {
    COMBAT("COMBAT", Icons.Default.SportsMartialArts),
    STATS("STATS", Icons.Default.BarChart),
    TRAINING("TRAINING", Icons.Default.FitnessCenter),
    GATHERING("GATHERING", Icons.Default.Agriculture),
    CRAFTING("CRAFTING", Icons.Default.Build),
    BEASTS("BEASTS", Icons.Default.Pets),
    NECRO("NECRO", Icons.Default.Shield),
    INVENTORY("ARSENAL", Icons.Default.Inventory2),
    SETTINGS("SYSTEM", Icons.Default.Settings)
}

@Composable
fun TrueIdleApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val logFilter by viewModel.logFilter.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(TrueIdleTab.COMBAT) }

    // Android system back button handler
    BackHandler(enabled = currentTab != TrueIdleTab.COMBAT) {
        currentTab = TrueIdleTab.COMBAT
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(BgDark),
        containerColor = BgDark,
        topBar = {
            // High-End Cyberpunk Top Status Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .statusBarsPadding()
                    .border(BorderStroke(1.dp, BorderDark), RectangleShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("◆ ", color = PrimaryNeon, fontSize = 14.sp)
                        Text(
                            text = "TRUE IDLE",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF000000), RectangleShape)
                                .border(1.dp, PrimaryNeon.copy(alpha = 0.6f), RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SECTOR ${state.player.currentFloor.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryNeon,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF000000), RectangleShape)
                                .border(1.dp, SecondaryGold.copy(alpha = 0.5f), RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🪙 ${state.player.gold}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF000000), RectangleShape)
                                .border(1.dp, NecroPurple.copy(alpha = 0.5f), RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "💀 ${state.necro.boneEssence}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NecroPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Re-Engineered High-End Cyberpunk Navigation Rail
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .border(BorderStroke(1.dp, BorderDark), RectangleShape)
                    .horizontalScroll(scrollState)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .testTag("main_navigation_bar")
            ) {
                TrueIdleTab.values().forEach { tab ->
                    val isSelected = (currentTab == tab)
                    Box(
                        modifier = Modifier
                            .clickable { currentTab = tab }
                            .background(
                                if (isSelected) PrimaryNeon.copy(alpha = 0.15f) else Color.Transparent,
                                RectangleShape
                            )
                            .border(
                                BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryNeon else Color.Transparent
                                ),
                                RectangleShape
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) PrimaryNeon else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) PrimaryNeon else TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BgDark) // Pure solid black
        ) {
            when (currentTab) {
                TrueIdleTab.COMBAT -> CombatScreen(
                    state = state,
                    logFilter = logFilter,
                    onNavFloor = { delta -> viewModel.navFloor(delta) },
                    onToggleAuto = { viewModel.toggleAutoAdvance() },
                    onSelectLogFilter = { filter -> viewModel.setLogFilter(filter) },
                    onClearLogs = { viewModel.clearLogs() }
                )
                TrueIdleTab.STATS -> StatsScreen(
                    state = state
                )
                TrueIdleTab.TRAINING -> TrainingScreen(
                    state = state,
                    onTrainNode = { id -> viewModel.trainNode(id) }
                )
                TrueIdleTab.GATHERING -> GatheringScreen(
                    state = state,
                    onHarvestPlot = { id -> viewModel.harvestPlot(id) },
                    onMineVein = { id -> viewModel.mineVein(id) }
                )
                TrueIdleTab.CRAFTING -> CraftingScreen(
                    state = state,
                    onCraftEquipment = { name, tier, slot, ore, qty -> viewModel.craftEquipment(name, tier, slot, ore, qty) },
                    onBrewPotion = { name, herb, qty -> viewModel.brewPotion(name, herb, qty) }
                )
                TrueIdleTab.BEASTS -> BeastsScreen(
                    state = state,
                    onMergeBeasts = { id1, id2 -> viewModel.mergeBeasts(id1, id2) }
                )
                TrueIdleTab.NECRO -> NecromancyScreen(
                    state = state,
                    onSummonMinion = { type -> viewModel.summonMinion(type) }
                )
                TrueIdleTab.INVENTORY -> InventoryScreen(
                    state = state,
                    onEquipItem = { id -> viewModel.equipItem(id) }
                )
                TrueIdleTab.SETTINGS -> SettingsScreen(
                    state = state
                )
            }
        }
    }
}
