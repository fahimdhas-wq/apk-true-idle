package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.HarvestXp
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.UpgradeCosts
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.DisciplineUpgrade
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiBadge
import com.aistudio.trueidle.ui.components.SciFiIconType
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*
import java.util.Locale

enum class HarvestActivity(val label: String, val color: Color) {
    FARMING("BIO-FARMING", GreenSuccess),
    MINING("PLASMA MINING", SecondaryGold)
}

@Composable
fun HarvestScreen(
    state: GameState,
    onHarvestPlot: (Int) -> Unit,
    onMineVein: (Int) -> Unit,
    onTogglePlotAutoQueue: (Int) -> Unit = {},
    onToggleVeinAutoQueue: (Int) -> Unit = {},
    onUpgradeDiscipline: (DisciplineCategory, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val harvest = state.harvest
    val scrollState = rememberScrollState()
    var selectedActivity by rememberSaveable { mutableStateOf(HarvestActivity.FARMING) }

    val speedRank = harvest.upgrades.firstOrNull { it.id == "harvest_speed" }?.rank ?: 0
    val miningRank = harvest.upgrades.firstOrNull { it.id == "mining_speed" }?.rank ?: 0
    val extraYield = harvest.upgrades.firstOrNull { it.id == "harvest_yield" }?.rank ?: 0
    val extraOreYield = harvest.upgrades.firstOrNull { it.id == "mining_yield" }?.rank ?: 0
    val farmingQueueCapacity = 1 + harvest.farmingLvl / 10
    val miningQueueCapacity = 1 + harvest.miningLvl / 10
    val activeFarmingQueues = harvest.plots.count { it.autoQueue }
    val activeMiningQueues = harvest.veins.count { it.autoQueue }
    val farmMult = (Math.pow(0.97, (harvest.farmingLvl - 1).toDouble()) * Math.pow(0.92, speedRank.toDouble()))
        .coerceAtLeast(0.20).toFloat()
    val mineMult = (Math.pow(0.97, (harvest.miningLvl - 1).toDouble()) * Math.pow(0.92, miningRank.toDouble()))
        .coerceAtLeast(0.20).toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(8.dp)
            .testTag("harvest_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TOP INTERFACE SELECTOR: FARMING vs MINING
        Row(
            modifier = Modifier.fillMaxWidth().testTag("harvest_activity_selector"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HarvestActivity.entries.forEach { activity ->
                val selected = selectedActivity == activity
                Button(
                    onClick = { selectedActivity = activity },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) activity.color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) activity.color else TextMuted
                    ),
                    border = BorderStroke(1.dp, if (selected) activity.color else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f).testTag("tab_${activity.name.lowercase()}")
                ) {
                    Text(
                        text = activity.label,
                        style = TrueIdleType.title,
                        color = if (selected) activity.color else TextMuted
                    )
                }
            }
        }

        // =====================================================================
        // VIEW 1: FARMING INTERFACE
        // =====================================================================
        if (selectedActivity == HarvestActivity.FARMING) {
            // Farming Overview
            Card(
                modifier = Modifier.fillMaxWidth().testTag("farming_header_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RectangleShape,
                border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("HYDROPONIC BIO-CULTIVATION", style = TrueIdleType.title, color = GreenSuccess)
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, GreenSuccess)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AUTO QUEUE [$activeFarmingQueues/$farmingQueueCapacity UNLOCKED]",
                                style = TrueIdleType.tag,
                                color = GreenSuccess
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    XpProgressBar(
                        skillName = "BIO-FARMING",
                        level = harvest.farmingLvl,
                        currentXp = harvest.farmingXp,
                        requiredXp = XpCurves.farmingSkillXp(harvest.farmingLvl),
                        barColor = GreenSuccess,
                        isMaxLevel = harvest.farmingLvl >= XpCurves.GATHERING_SKILL_MAX_LEVEL
                    )
                }
            }

            // Farming Upgrades
            val farmingUpgrades = harvest.upgrades.filter { it.id.startsWith("harvest_") }
            if (farmingUpgrades.isNotEmpty()) {
                SciFiPanel(
                    title = "CULTIVATION ENHANCEMENTS",
                    accent = GreenSuccess,
                    tag = "BIO-TECH"
                ) {
                    farmingUpgrades.forEach { upgrade ->
                        HarvestUpgradeCard(
                            upgrade = upgrade,
                            playerCredits = state.player.gold,
                            onUpgrade = { onUpgradeDiscipline(DisciplineCategory.HARVEST, upgrade.id) }
                        )
                    }
                }
            }

            // 20 Bio-Plots List
            SciFiPanel(
                title = "HYDROPONIC BIO-PLOTS (${harvest.plots.size} SPECIMENS)",
                accent = GreenSuccess,
                tag = "${harvest.plots.count { it.isReady }}/${harvest.plots.size} READY"
            ) {
                harvest.plots.forEach { plot ->
                    val isLocked = harvest.farmingLvl < plot.reqLvl
                    val duration = (plot.durationSeconds * farmMult).coerceAtLeast(0.1f)
                    val remainingSec = ((1f - plot.growthProgress) * duration).coerceAtLeast(0f)
                    val timeText = when {
                        isLocked -> "LOCKED (REQ LV ${plot.reqLvl})"
                        plot.isReady -> "READY"
                        else -> String.format(Locale.ROOT, "%.1fs", remainingSec)
                    }
                    val canEnableQueue = plot.autoQueue || activeFarmingQueues < farmingQueueCapacity
                    val profMult = XpCurves.proficiencyYieldMultiplier(plot.proficiencyLevel)
                    val reqProfXp = XpCurves.proficiency(plot.proficiencyLevel)
                    val profRatio = if (plot.proficiencyLevel >= 100) 1f else (plot.proficiencyXp.toFloat() / reqProfXp.toFloat()).coerceIn(0f, 1f)
                    val totalYield = (2.0 + extraYield) * profMult
                    val skillXpReward = if (harvest.farmingLvl < XpCurves.GATHERING_SKILL_MAX_LEVEL) HarvestXp.forYield(plot.herbName, totalYield) else 0.0
                    val rewardAmt = totalYield.toLong().coerceAtLeast(1L)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .testTag("plot_card_${plot.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (isLocked) MaterialTheme.colorScheme.outline else if (plot.autoQueue) GreenSuccess else if (plot.isReady) GreenSuccess.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(plot.herbName.uppercase(), style = TrueIdleType.title, color = if (isLocked) TextMuted else MaterialTheme.colorScheme.onSurface)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "LVL ${plot.proficiencyLevel}/100",
                                        style = TrueIdleType.tag,
                                        color = if (plot.proficiencyLevel >= 100) SecondaryGold else PrimaryNeon
                                    )
                                    Text(
                                        text = "x${NumberFormatter.short(profMult)} YIELD",
                                        style = TrueIdleType.tag,
                                        color = GreenSuccess
                                    )
                                    Text(
                                        text = timeText,
                                        style = TrueIdleType.tag,
                                        color = if (plot.isReady) GreenSuccess else TextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            // Proficiency Progress Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CLASS +${NumberFormatter.short(skillXpReward)} XP · PROF +${NumberFormatter.short(rewardAmt)} XP",
                                    style = TrueIdleType.meta,
                                    color = TextMuted
                                )
                                Text(
                                    text = if (plot.proficiencyLevel >= 100) "MAX RANK (100)" else "${NumberFormatter.short(plot.proficiencyXp.toDouble())} / ${NumberFormatter.short(reqProfXp.toDouble())} XP",
                                    style = TrueIdleType.meta,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { profRatio },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = PrimaryNeon,
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            // Growth Progress Bar
                            LinearProgressIndicator(
                                progress = { if (isLocked) 0f else plot.growthProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = GreenSuccess,
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onTogglePlotAutoQueue(plot.id) },
                                    enabled = !isLocked && canEnableQueue,
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (plot.autoQueue) GreenSuccess.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                        contentColor = if (plot.autoQueue) GreenSuccess else TextMuted
                                    ),
                                    border = BorderStroke(1.dp, if (plot.autoQueue) GreenSuccess else MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.weight(1f).testTag("auto_queue_plot_${plot.id}")
                                ) {
                                    Text(
                                        text = when {
                                            isLocked -> "LOCKED"
                                            plot.autoQueue -> "AUTO [ACTIVE]"
                                            farmingQueueCapacity == 0 -> "AUTO [LVL 10]"
                                            !canEnableQueue -> "QUEUE FULL"
                                            else -> "AUTO [OFF]"
                                        },
                                        style = TrueIdleType.tag,
                                        color = if (plot.autoQueue) GreenSuccess else TextMuted
                                    )
                                }

                                Button(
                                    onClick = { onHarvestPlot(plot.id) },
                                    enabled = plot.isReady && !isLocked,
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GreenSuccess,
                                        contentColor = Color.Black,
                                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        disabledContentColor = TextMuted
                                    ),
                                    modifier = Modifier.weight(2f).testTag("harvest_plot_btn_${plot.id}")
                                ) {
                                    Text(
                                        text = when {
                                            isLocked -> "LOCKED (REQ LV ${plot.reqLvl})"
                                            plot.isReady -> "HARVEST (+${NumberFormatter.short(totalYield)})"
                                            plot.autoQueue -> "AUTORUN (${timeText})"
                                            else -> "GROWING (${timeText})"
                                        },
                                        style = TrueIdleType.tag
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // VIEW 2: MINING INTERFACE
        // =====================================================================
        if (selectedActivity == HarvestActivity.MINING) {
            // Mining Overview
            Card(
                modifier = Modifier.fillMaxWidth().testTag("mining_header_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RectangleShape,
                border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SUBTERRANEAN PLASMA MINING", style = TrueIdleType.title, color = SecondaryGold)
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                                .border(1.dp, SecondaryGold)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AUTO QUEUE [$activeMiningQueues/$miningQueueCapacity UNLOCKED]",
                                style = TrueIdleType.tag,
                                color = SecondaryGold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    XpProgressBar(
                        skillName = "PLASMA MINING",
                        level = harvest.miningLvl,
                        currentXp = harvest.miningXp,
                        requiredXp = XpCurves.miningSkillXp(harvest.miningLvl),
                        barColor = SecondaryGold,
                        isMaxLevel = harvest.miningLvl >= XpCurves.GATHERING_SKILL_MAX_LEVEL
                    )
                }
            }

            // Mining Upgrades
            val miningUpgrades = harvest.upgrades.filter { it.id.startsWith("mining_") }
            if (miningUpgrades.isNotEmpty()) {
                SciFiPanel(
                    title = "DRILL ENHANCEMENTS",
                    accent = SecondaryGold,
                    tag = "HEAVY-DRILL"
                ) {
                    miningUpgrades.forEach { upgrade ->
                        HarvestUpgradeCard(
                            upgrade = upgrade,
                            playerCredits = state.player.gold,
                            onUpgrade = { onUpgradeDiscipline(DisciplineCategory.HARVEST, upgrade.id) }
                        )
                    }
                }
            }

            // 20 Plasma Veins List
            SciFiPanel(
                title = "SUBTERRANEAN PLASMA VEINS (${harvest.veins.size} MINERALS)",
                accent = SecondaryGold,
                tag = "${harvest.veins.count { it.isReady }}/${harvest.veins.size} READY"
            ) {
                harvest.veins.forEach { vein ->
                    val isLocked = harvest.miningLvl < vein.reqLvl
                    val canEnableQueue = vein.autoQueue || activeMiningQueues < miningQueueCapacity
                    val duration = (vein.durationSeconds * mineMult).coerceAtLeast(0.1f)
                    val remainingSec = ((1f - vein.miningProgress) * duration).coerceAtLeast(0f)
                    val timeText = when {
                        isLocked -> "REQ LVL ${vein.reqLvl}"
                        vein.isReady -> "READY"
                        else -> String.format(Locale.ROOT, "%.1fs", remainingSec)
                    }
                    val profMult = XpCurves.proficiencyYieldMultiplier(vein.proficiencyLevel)
                    val reqProfXp = XpCurves.proficiency(vein.proficiencyLevel)
                    val profRatio = if (vein.proficiencyLevel >= 100) 1f else (vein.proficiencyXp.toFloat() / reqProfXp.toFloat()).coerceIn(0f, 1f)
                    val totalYield = (3.0 + extraYield + extraOreYield) * profMult
                    val skillXpReward = if (harvest.miningLvl < XpCurves.GATHERING_SKILL_MAX_LEVEL) HarvestXp.forYield(vein.oreName, totalYield) else 0.0
                    val rewardAmt = totalYield.toLong().coerceAtLeast(1L)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .testTag("vein_card_${vein.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (vein.autoQueue) SecondaryGold else if (vein.isReady && !isLocked) SecondaryGold.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(vein.oreName.uppercase(), style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "LVL ${vein.proficiencyLevel}/100",
                                        style = TrueIdleType.tag,
                                        color = if (vein.proficiencyLevel >= 100) SecondaryGold else PrimaryNeon
                                    )
                                    Text(
                                        text = "x${NumberFormatter.short(profMult)} YIELD",
                                        style = TrueIdleType.tag,
                                        color = SecondaryGold
                                    )
                                    Text(
                                        text = timeText,
                                        style = TrueIdleType.tag,
                                        color = when {
                                            isLocked -> TextMuted
                                            vein.isReady -> SecondaryGold
                                            else -> TextMuted
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            // Proficiency Progress Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CLASS +${NumberFormatter.short(skillXpReward)} XP · PROF +${NumberFormatter.short(rewardAmt)} XP",
                                    style = TrueIdleType.meta,
                                    color = TextMuted
                                )
                                Text(
                                    text = if (vein.proficiencyLevel >= 100) "MAX RANK (100)" else "${NumberFormatter.short(vein.proficiencyXp.toDouble())} / ${NumberFormatter.short(reqProfXp.toDouble())} XP",
                                    style = TrueIdleType.meta,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { profRatio },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = PrimaryNeon,
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            // Drilling Progress Bar
                            LinearProgressIndicator(
                                progress = { if (isLocked) 0f else vein.miningProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = SecondaryGold,
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onToggleVeinAutoQueue(vein.id) },
                                    enabled = !isLocked && canEnableQueue,
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (vein.autoQueue) SecondaryGold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                        contentColor = if (vein.autoQueue) SecondaryGold else TextMuted
                                    ),
                                    border = BorderStroke(1.dp, if (vein.autoQueue) SecondaryGold else MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.weight(1f).testTag("auto_queue_vein_${vein.id}")
                                ) {
                                    Text(
                                        text = when {
                                            isLocked -> "LOCKED"
                                            vein.autoQueue -> "AUTO [ACTIVE]"
                                            miningQueueCapacity == 0 -> "AUTO [LVL 10]"
                                            !canEnableQueue -> "QUEUE FULL"
                                            else -> "AUTO [OFF]"
                                        },
                                        style = TrueIdleType.tag,
                                        color = if (vein.autoQueue) SecondaryGold else TextMuted
                                    )
                                }

                                Button(
                                    onClick = { onMineVein(vein.id) },
                                    enabled = vein.isReady && !isLocked,
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SecondaryGold,
                                        contentColor = Color.Black,
                                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        disabledContentColor = TextMuted
                                    ),
                                    modifier = Modifier.weight(2f).testTag("mine_vein_btn_${vein.id}")
                                ) {
                                    Text(
                                        text = when {
                                            isLocked -> "LOCKED (REQ LV ${vein.reqLvl})"
                                            vein.isReady -> "EXTRACT (+${NumberFormatter.short(totalYield)})"
                                            vein.autoQueue -> "AUTORUN (${timeText})"
                                            else -> "DRILLING (${timeText})"
                                        },
                                        style = TrueIdleType.tag
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HarvestUpgradeCard(
    upgrade: DisciplineUpgrade,
    playerCredits: Long,
    onUpgrade: () -> Unit
) {
    val cost = UpgradeCosts.nextRank(upgrade.baseCost, upgrade.rank)
    val maxed = upgrade.rank >= upgrade.maxRank
    val canAfford = !maxed && playerCredits >= cost

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RectangleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(upgrade.name, style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                Text(upgrade.description, style = TrueIdleType.meta, color = TextMuted)
                Text("RANK ${upgrade.rank}/${upgrade.maxRank}", style = TrueIdleType.tag, color = SecondaryGold)
            }
            Button(
                onClick = onUpgrade,
                enabled = canAfford,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryGold,
                    contentColor = Color.Black,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = TextMuted
                ),
                border = BorderStroke(1.dp, if (canAfford) SecondaryGold else MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = if (maxed) "MAX" else "${NumberFormatter.short(cost)} CR",
                    style = TrueIdleType.tag
                )
            }
        }
    }
}
