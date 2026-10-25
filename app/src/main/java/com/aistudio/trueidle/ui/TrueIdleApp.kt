package com.aistudio.trueidle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.ui.components.NotificationBanner
import com.aistudio.trueidle.ui.components.NotificationHistoryDialog
import com.aistudio.trueidle.ui.components.OfflineLootDialog
import com.aistudio.trueidle.ui.screens.*
import com.aistudio.trueidle.ui.theme.*
import com.aistudio.trueidle.viewmodel.GameViewModel

enum class TrueIdleTab(val label: String, val icon: ImageVector) {
    COMBAT("COMBAT", Icons.Default.SportsMartialArts),
    SKILLS("SKILLS", Icons.Default.AutoFixHigh),
    STATS("STATS", Icons.Default.BarChart),
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
    var currentTab by rememberSaveable { mutableStateOf(TrueIdleTab.COMBAT) }
    var showNotificationHistory by remember { mutableStateOf(false) }

    // Android system back button handler
    BackHandler(enabled = currentTab != TrueIdleTab.COMBAT) {
        currentTab = TrueIdleTab.COMBAT
    }

    // Offline Loot Claim Modal
    if (state.pendingOfflineLoot != null) {
        OfflineLootDialog(
            loot = state.pendingOfflineLoot!!,
            onClaim = { viewModel.claimOfflineLoot() }
        )
    }

    // Notification History Modal ("Show All")
    if (showNotificationHistory) {
        NotificationHistoryDialog(
            history = state.notificationHistory,
            onClear = { viewModel.clearNotificationHistory() },
            onDismiss = { showNotificationHistory = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // High-End Cyberpunk Top Status Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("◈ ", color = PrimaryNeon, style = TrueIdleType.title)
                        Text(
                            text = "TRUE IDLE",
                            style = TrueIdleType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, PrimaryNeon.copy(alpha = 0.6f), RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SECTOR ${state.player.currentFloor.toString().padStart(2, '0')}",
                                style = TrueIdleType.tag,
                                color = PrimaryNeon
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, SecondaryGold.copy(alpha = 0.5f), RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🪙 ${NumberFormatter.short(state.player.gold)}",
                                style = TrueIdleType.tag,
                                color = SecondaryGold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, NecroPurple.copy(alpha = 0.5f), RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "💀 ${NumberFormatter.short(state.necro.boneEssence)}",
                                style = TrueIdleType.tag,
                                color = NecroPurple
                            )
                        }
                        // Top bar shortcut: ⚙ Settings / Interface
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, PrimaryNeon.copy(alpha = 0.7f), RectangleShape)
                                .clickable { currentTab = TrueIdleTab.SETTINGS }
                                .testTag("topbar_settings_shortcut"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚙", style = TrueIdleType.tag, color = PrimaryNeon)
                        }
                    }
                }

                // Anti-stacking interactive notification banner with Show All option
                NotificationBanner(
                    notifications = state.notifications,
                    onDismiss = { viewModel.dismissNotification(it) },
                    onShowAll = { showNotificationHistory = true },
                    historyCount = state.notificationHistory.size
                )
            }
        },
        bottomBar = {
            // Accessible Cyberpunk 5-Tab Navigation Bar (§7.4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 2.dp, horizontal = 2.dp)
                    .testTag("main_navigation_bar"),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrueIdleTab.values().forEach { tab ->
                    val isSelected = (currentTab == tab)
                    val tint = if (isSelected) PrimaryNeon else TextMuted
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .selectable(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                role = Role.Tab
                            )
                            .drawBehind {
                                if (isSelected) {
                                    drawRect(PrimaryNeon, size = Size(size.width, 2.dp.toPx()))
                                }
                            }
                            .padding(vertical = 6.dp)
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = TrueIdleType.tag,
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                TrueIdleTab.COMBAT -> CombatScreen(
                    state = state,
                    logFilter = logFilter,
                    onNavFloor = { delta -> viewModel.navFloor(delta) },
                    onToggleAuto = { viewModel.toggleAutoAdvance() },
                    onSelectLogFilter = { filter -> viewModel.setLogFilter(filter) },
                    onClearLogs = { viewModel.clearLogs() },
                    onManageSupport = { currentTab = TrueIdleTab.SKILLS }
                )
                TrueIdleTab.SKILLS -> SkillsScreen(
                    state = state,
                    onHarvestPlot = { id -> viewModel.harvestPlot(id) },
                    onMineVein = { id -> viewModel.mineVein(id) },
                    onCraftEquipment = { name, tier, slot, ore, qty -> viewModel.craftEquipment(name, tier, slot, ore, qty) },
                    onBrewPotion = { name, herb, qty -> viewModel.brewPotion(name, herb, qty) },
                    onMergeBeasts = { id1, id2 -> viewModel.mergeBeasts(id1, id2) },
                    onSummonMinion = { type -> viewModel.summonMinion(type) },
                    onTrainNode = { id -> viewModel.trainNode(id) },
                    onToggleSanctifiedAegis = { viewModel.toggleEssenceAegis() },
                    onUpgradeSanctifiedSmite = { viewModel.upgradeEssenceSmite() },
                    onRerollSlayerTask = { viewModel.rerollSlayerTask() },
                    onUpgradeDiscipline = { cat, id -> viewModel.upgradeDiscipline(cat, id) },
                    onActivateTalent = { id -> viewModel.activateTalent(id) },
                    onUpgradeTalent = { id -> viewModel.upgradeTalent(id) }
                )
                TrueIdleTab.STATS -> StatsScreen(
                    state = state
                )
                TrueIdleTab.INVENTORY -> InventoryScreen(
                    state = state,
                    onEquipItem = { id -> viewModel.equipItem(id) },
                    onUnequipItem = { id -> viewModel.unequipItem(id) },
                    onSellItem = { id, count -> viewModel.sellItem(id, count) },
                    onUsePotion = { id -> viewModel.usePotion(id) }
                )
                TrueIdleTab.SETTINGS -> SettingsScreen(
                    state = state,
                    onExportSave = { viewModel.exportSave() },
                    onImportSave = { code -> viewModel.importSave(code) },
                    onResetGame = { viewModel.resetGame() },
                    onToggleNotifications = { viewModel.toggleNotificationBanners() },
                    onClearNotificationHistory = { viewModel.clearNotificationHistory() },
                    onClaimAchievement = { achId -> viewModel.claimAchievement(achId) },
                    onToggleAutoEquip = { viewModel.toggleAutoEquipBestGear() },
                    onToggleAutoMerge = { viewModel.toggleAutoMergeBeasts() },
                    onSetCraftUsage = { viewModel.setCraftMaterialUsage(it) },
                    onFightPromotionBoss = { viewModel.fightPromotionBoss() },
                    onSetThemeMode = { isDark -> viewModel.setThemeMode(isDark) },
                    onExportTelemetryLog = { viewModel.buildTelemetryReport() }
                )
            }
        }
    }
}
