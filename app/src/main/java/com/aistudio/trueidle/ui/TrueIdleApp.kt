package com.aistudio.trueidle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    COMBAT("Combat", Icons.Default.SportsMartialArts),
    STATS("Stats", Icons.Default.BarChart),
    TRAINING("Training", Icons.Default.FitnessCenter),
    GATHERING("Gathering", Icons.Default.Agriculture),
    CRAFTING("Crafting", Icons.Default.Build),
    BEASTS("Beasts", Icons.Default.Pets),
    NECRO("Necro", Icons.Default.Shield),
    INVENTORY("Inventory", Icons.Default.Inventory2),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrueIdleApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val logFilter by viewModel.logFilter.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(TrueIdleTab.COMBAT) }

    // Android back button handler
    BackHandler(enabled = currentTab != TrueIdleTab.COMBAT) {
        currentTab = TrueIdleTab.COMBAT
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(BgDark),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("◆ ", color = PrimaryNeon, fontSize = 16.sp)
                            Text(
                                text = "True IDLE",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextLight,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceVariantDark)
                                    .border(1.dp, BorderDark, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⚔️ F${state.player.currentFloor}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryNeon,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🪙 ${state.player.gold}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "💀 ${state.necro.boneEssence}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NecroPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextLight
                )
            )
        },
        bottomBar = {
            ScrollableTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = SurfaceDark,
                contentColor = PrimaryNeon,
                edgePadding = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                TrueIdleTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = { Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = PrimaryNeon,
                        unselectedContentColor = TextMuted,
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BgDark)
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
