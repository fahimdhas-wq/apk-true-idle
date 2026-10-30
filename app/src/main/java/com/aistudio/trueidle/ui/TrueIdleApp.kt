package com.aistudio.trueidle.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.ui.components.SciFiBadge
import com.aistudio.trueidle.ui.components.SciFiIconType
import com.aistudio.trueidle.ui.components.rememberStableShort
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.trueidle.BuildConfig
import com.aistudio.trueidle.R
import com.aistudio.trueidle.core.Derived
import com.aistudio.trueidle.ui.state.LocalDerived
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.ui.components.OfflineLootDialog
import com.aistudio.trueidle.ui.components.DevPanel
import com.aistudio.trueidle.ui.components.SciFiButton
import com.aistudio.trueidle.ui.components.SciFiDialog
import com.aistudio.trueidle.ui.components.SupportKind
import com.aistudio.trueidle.ui.screens.*
import com.aistudio.trueidle.ui.theme.*
import com.aistudio.trueidle.viewmodel.GameViewModel

enum class TrueIdleTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    COMBAT(R.string.tab_combat, Icons.Default.PlayArrow),
    CLASS(R.string.tab_class, Icons.Default.Build),
    STATS(R.string.tab_stats, Icons.Default.Star),
    INVENTORY(R.string.tab_arsenal, Icons.Default.ShoppingCart),
    SETTINGS(R.string.tab_system, Icons.Default.Settings)
}

@Composable
fun TrueIdleApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val derived = remember(state) { DerivedStats.of(state) }
    val logFilter by viewModel.logFilter.collectAsStateWithLifecycle()
    val recoveryReason by viewModel.recoveryReason.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(TrueIdleTab.COMBAT) }
    // Hoisted so the Support module can open Genesis directly instead of the overview.
    var classSection by rememberSaveable { mutableStateOf(ClassSubSection.OVERVIEW) }

    // System back returns to Combat rather than dropping out of the app from any tab.
    BackHandler(enabled = currentTab != TrueIdleTab.COMBAT) {
        currentTab = TrueIdleTab.COMBAT
    }

    // Offline report modal. Only the explicit claim button banks the rewards.
    val pendingLoot = state.pendingOfflineLoot
    if (pendingLoot != null) {
        OfflineLootDialog(
            loot = pendingLoot,
            state = state,
            onClaim = { viewModel.claimOfflineLoot() },
            onDismiss = { }
        )
    }

    if (recoveryReason != null) {
        SciFiDialog(
            title = "SAVE RECOVERY",
            accent = Warn,
            onDismiss = { },
            dismissible = false,
            actions = {
                SciFiButton(
                    label = "START FRESH",
                    onClick = { viewModel.discardCorruptSave() },
                    modifier = Modifier.weight(1f),
                    color = HpRed
                )
                SciFiButton(
                    label = "RETRY LOAD",
                    onClick = { viewModel.retryLoad() },
                    modifier = Modifier.weight(1f),
                    color = PrimaryNeon,
                    filled = true
                )
            }
        ) {
            Text(
                text = "The stored save could not be read (${recoveryReason}). The raw bytes have " +
                    "been quarantined and saving is paused so nothing overwrites them. Retry the " +
                    "load, or start a fresh game.",
                style = TrueIdleType.body,
                color = TextMuted
            )
        }
    }

    CompositionLocalProvider(LocalDerived provides derived) {
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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, PrimaryNeon.copy(alpha = 0.6f), RectangleShape)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, SecondaryGold.copy(alpha = 0.5f), RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            SciFiBadge(
                                type = SciFiIconType.CREDIT,
                                label = rememberStableShort(state.player.gold),
                                textColor = SecondaryGold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, NecroPurple.copy(alpha = 0.5f), RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            SciFiBadge(
                                type = SciFiIconType.ESSENCE,
                                label = rememberStableShort(state.necro.boneEssence),
                                textColor = NecroPurple
                            )
                        }
                        // Diamonds were awarded for boss first clears but nothing ever displayed them.
                        if (state.player.diamonds > 0L) {
                            Box(
                                modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, PrimaryNeon.copy(alpha = 0.5f), RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                SciFiBadge(
                                    type = SciFiIconType.DIAMOND,
                                    label = rememberStableShort(state.player.diamonds),
                                    textColor = PrimaryNeon
                                )
                            }
                        }
                        // Top bar shortcut: Settings / Interface. Min 48dp touch target.
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, PrimaryNeon.copy(alpha = 0.7f), RectangleShape)
                                .clickable(role = Role.Button) { currentTab = TrueIdleTab.SETTINGS }
                                .testTag("topbar_settings_shortcut"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PrimaryNeon, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
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
                TrueIdleTab.entries.forEach { tab ->
                    val isSelected = (currentTab == tab)
                    val tint = if (isSelected) PrimaryNeon else TextMuted
                    val label = stringResource(tab.labelRes)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
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
                            .padding(vertical = 4.dp)
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            // The visible label already names the tab; a duplicate description
                            // would be read twice by TalkBack.
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
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
                    // Both squads are managed on the Genesis screen, so this navigates there
                    // rather than to the Class overview. The kind is kept for the component
                    // contract; per-squad deep links would need the Genesis sub-tab hoisted too.
                    onManageSupport = { classSection = ClassSubSection.GENESIS; currentTab = TrueIdleTab.CLASS },
                    onUseHpPotion = { viewModel.usePotionByName(state.potionSettings.selectedHpPotion) },
                    onUseMpPotion = { viewModel.usePotionByName(state.potionSettings.selectedMpPotion) },
                    onSetHpThreshold = { viewModel.setHpAutoThreshold(it) },
                    onSetMpThreshold = { viewModel.setMpAutoThreshold(it) },
                    onSelectHpPotion = { viewModel.selectHpPotion(it) },
                    onSelectMpPotion = { viewModel.selectMpPotion(it) }
                )
                TrueIdleTab.CLASS -> ClassScreen(
                    state = state,
                    currentSection = classSection,
                    onSectionChange = { classSection = it },
                    onHarvestPlot = { id -> viewModel.harvestPlot(id) },
                    onMineVein = { id -> viewModel.mineVein(id) },
                    onTogglePlotAutoQueue = { id -> viewModel.togglePlotAutoQueue(id) },
                    onToggleVeinAutoQueue = { id -> viewModel.toggleVeinAutoQueue(id) },
                    onCraftEquipment = { name, tier, slot, ore, qty -> viewModel.craftEquipment(name, tier, slot, ore, qty) },
                    onBrewPotion = { name, herb, qty -> viewModel.brewPotion(name, herb, qty) },
                    onMergeBeasts = { id1, id2 -> viewModel.mergeBeasts(id1, id2) },
                    onSummonMinion = { type -> viewModel.summonMinion(type) },
                    onTrainNode = { id -> viewModel.trainNode(id) },
                    onToggleSanctifiedAegis = { viewModel.toggleEssenceAegis() },
                    onUpgradeSanctifiedSmite = { viewModel.upgradeEssenceSmite() },
                    onRerollSlayerTask = { id -> viewModel.rerollSlayerTask(id) },
                    onUpgradeDiscipline = { cat, id -> viewModel.upgradeDiscipline(cat, id) },
                    onActivateTalent = { id -> viewModel.activateTalent(id) },
                    onUpgradeTalent = { id -> viewModel.upgradeTalent(id) },
                    onUnequipItem = { id -> viewModel.unequipItem(id) },
                    onAutoEquip = { viewModel.toggleAutoEquipBestGear() }
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
                    onClaimAchievement = { achId -> viewModel.claimAchievement(achId) },
                    onToggleAutoEquip = { viewModel.toggleAutoEquipBestGear() },
                    onToggleAutoMerge = { viewModel.toggleAutoMergeBeasts() },
                    onToggleRunInBackground = { viewModel.toggleRunInBackground() },
                    onSetCraftUsage = { viewModel.setCraftMaterialUsage(it) },
                    onFightPromotionBoss = { viewModel.fightPromotionBoss() },
                    onExportTelemetryLog = { viewModel.buildTelemetryReport() }
                )
            }

            // Debug builds only: no dev code path, no toggle, no recomposition cost in release.
            if (BuildConfig.DEBUG) {
                DevPanel(
                    state = state,
                    onAddCurrency = { kind, amount -> viewModel.devAddCurrency(kind, amount) },
                    onMaxCurrency = { kind -> viewModel.devMaxCurrency(kind) },
                    onSetGameSpeed = { speed -> viewModel.setGameSpeed(speed) },
                    onUnlockAll = { viewModel.devUnlockAll() },
                    onHardReset = { viewModel.devHardReset() }
                )
            }
        }
    }
}
}
