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
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.UpgradeCosts
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*

@Composable
fun EssenceScreen(
    state: GameState,
    onToggleAegis: () -> Unit,
    onUpgradeSmite: () -> Unit,
    onUpgradeDiscipline: (DisciplineCategory, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val essence = state.essence
    val scrollState = rememberScrollState()
    val smiteCost = UpgradeCosts.smite(essence.smiteLevel)
    val canAffordSmite = essence.faith >= smiteCost

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(8.dp)
            .testTag("essence_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. ESSENCE PROTOCOL OVERVIEW
        Card(
            modifier = Modifier.fillMaxWidth().testTag("essence_header_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ESSENCE HARMONIC PROTOCOL", style = TrueIdleType.title, color = ManaTeal)
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                            .border(1.dp, ManaTeal)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("FAITH: ${NumberFormatter.short(essence.faith)}", style = TrueIdleType.tag, color = ManaTeal)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                XpProgressBar(
                    skillName = "ESSENCE CONDUIT",
                    level = essence.lvl,
                    currentXp = essence.xp,
                    requiredXp = XpCurves.skill(essence.lvl),
                    barColor = ManaTeal
                )
            }
        }

        // 2. AEGIS BARRIER
        SciFiPanel(
            title = "AEGIS ENERGY BARRIER",
            accent = if (essence.aegisActive) GreenSuccess else HpRed,
            tag = if (essence.aegisActive) "ONLINE" else "OFFLINE"
        ) {
            Text(
                text = if (essence.aegisActive)
                    "Aegis is active: Incoming monster damage reduced by 20%. Consuming 0.5 Faith/sec in combat."
                else
                    "Aegis is offline: No incoming damage mitigation. Engage to protect defensive integrity.",
                style = TrueIdleType.body,
                color = if (essence.aegisActive) GreenSuccess else TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onToggleAegis,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (essence.aegisActive) HpRed.copy(alpha = 0.25f) else ManaTeal.copy(alpha = 0.25f),
                    contentColor = if (essence.aegisActive) HpRed else ManaTeal
                ),
                border = BorderStroke(1.dp, if (essence.aegisActive) HpRed else ManaTeal),
                modifier = Modifier.fillMaxWidth().testTag("toggle_aegis_btn")
            ) {
                Text(
                    text = if (essence.aegisActive) "DISENGAGE AEGIS BARRIER" else "ENGAGE AEGIS BARRIER",
                    style = TrueIdleType.tag,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. SOLAR SMITE RAY
        SciFiPanel(
            title = "SOLAR SMITE LANCE",
            accent = SecondaryGold,
            tag = "LVL ${essence.smiteLevel}"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PASSIVE BURST DAMAGE", style = TrueIdleType.label, color = MaterialTheme.colorScheme.onSurface)
                    Text("+${NumberFormatter.short(essence.smiteLevel.toLong() * 3L)} DPS added to hero combat output", style = TrueIdleType.meta, color = TextMuted)
                }
                Text("+${NumberFormatter.short(essence.smiteLevel.toLong() * 3L)} DPS", style = TrueIdleType.value, color = SecondaryGold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onUpgradeSmite,
                enabled = canAffordSmite,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAffordSmite) SecondaryGold else MaterialTheme.colorScheme.surface,
                    contentColor = if (canAffordSmite) Color.Black else TextMuted
                ),
                border = BorderStroke(1.dp, if (canAffordSmite) SecondaryGold else MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("upgrade_smite_btn")
            ) {
                Text("UPGRADE SMITE (COST: ${NumberFormatter.short(smiteCost)} FAITH)", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
            }
        }

        // 4. ESSENCE MATRIX UPGRADES
        SciFiPanel(
            title = "ESSENCE PROTOCOL UPGRADES",
            accent = PrimaryNeon,
            tag = "CONDUITS"
        ) {
            essence.upgrades.forEach { upg ->
                val cost = UpgradeCosts.nextRank(upg.baseCost, upg.rank, upg.costMultiplier)
                val canAfford = essence.faith >= cost
                val isMax = upg.rank >= upg.maxRank

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(upg.name, style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RectangleShape)
                                        .border(1.dp, ManaTeal)
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text("RANK ${upg.rank}/${upg.maxRank}", style = TrueIdleType.numberSmall, color = ManaTeal)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(upg.description, style = TrueIdleType.meta, color = TextMuted)
                        }

                        Button(
                            onClick = { onUpgradeDiscipline(DisciplineCategory.ESSENCE, upg.id) },
                            enabled = canAfford && !isMax,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) ManaTeal else MaterialTheme.colorScheme.surface,
                                contentColor = if (canAfford) Color.Black else TextMuted
                            ),
                            border = BorderStroke(1.dp, if (canAfford) ManaTeal else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.testTag("upgrade_essence_${upg.id}")
                        ) {
                            Text(if (isMax) "MAX" else "⚡ ${NumberFormatter.short(cost)}", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
