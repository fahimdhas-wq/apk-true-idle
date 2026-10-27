package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.MinionCatalog
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.Beast
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.RosterRow
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*
import kotlin.math.pow

private enum class GenesisTab(val label: String, val icon: String, val color: Color) {
    BEASTS("BEASTS", "🐾", GreenSuccess),
    NECROMANCY("UNDEAD", "💀", NecroPurple),
    UPGRADES("UPGRADES", "⚡", PrimaryNeon)
}

@Composable
fun GenesisScreen(
    state: GameState,
    onMergeBeasts: (String, String) -> Unit,
    onSummonMinion: (String) -> Unit,
    onUpgradeDiscipline: (DisciplineCategory, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var activeSubTab by rememberSaveable { mutableStateOf(GenesisTab.BEASTS) }
    var selectedBeast1 by remember { mutableStateOf<String?>(null) }
    var selectedBeast2 by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    val beasts = state.beasts
    val necro = state.necro
    val totalBeastDps = CombatMath.beastDps(beasts).toInt()
    val totalArmyDps = CombatMath.armyDps(necro).toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(8.dp)
            .testTag("genesis_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. GENESIS COMMAND CARD
        Card(
            modifier = Modifier.fillMaxWidth().testTag("genesis_header_card"),
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
                    Text("GENESIS BIO-REANIMATION MATRIX", style = TrueIdleType.title, color = PrimaryNeon)
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                            .border(1.dp, PrimaryNeon)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("TOTAL DPS: ${totalBeastDps + totalArmyDps}", style = TrueIdleType.tag, color = PrimaryNeon)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                XpProgressBar(
                    skillName = "BEAST TAMING",
                    level = beasts.lvl,
                    currentXp = beasts.xp,
                    requiredXp = XpCurves.skill(beasts.lvl),
                    barColor = GreenSuccess
                )
                Spacer(modifier = Modifier.height(6.dp))
                XpProgressBar(
                    skillName = "NECROMANCY REANIMATION",
                    level = necro.lvl,
                    currentXp = necro.xp,
                    requiredXp = XpCurves.skill(necro.lvl),
                    barColor = NecroPurple
                )
            }
        }

        // Sub-Navigation Tabs inside Genesis
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RectangleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            GenesisTab.entries.forEach { tab ->
                val isSelected = activeSubTab == tab
                Button(
                    onClick = { activeSubTab = tab },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) tab.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) tab.color else TextMuted
                    ),
                    border = BorderStroke(1.dp, if (isSelected) tab.color else MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("${tab.icon} ${tab.label}", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                }
            }
        }

        when (activeSubTab) {
            GenesisTab.BEASTS -> {
                // BEAST COMPANION ROSTER
                SciFiPanel(
                    title = "COMPANION BEAST ROSTER",
                    accent = GreenSuccess,
                    tag = "${beasts.beasts.size}/${GameCaps.BEAST} BEASTS ACTIVE"
                ) {
                    if (beasts.beasts.isEmpty()) {
                        Text(
                            text = "NO COMPANIONS TAMED YET. ENGAGE HIGHER SECTORS TO TAME MONSTERS.",
                            style = TrueIdleType.meta,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    } else {
                        beasts.beasts.forEach { beast ->
                            val isSel1 = selectedBeast1 == beast.id
                            val isSel2 = selectedBeast2 == beast.id
                            val dpsVal = (beast.dps * beast.rank * (1.0 + (beast.stars - 1) * 0.25)).toInt()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        if (selectedBeast1 == beast.id) selectedBeast1 = null
                                        else if (selectedBeast2 == beast.id) selectedBeast2 = null
                                        else if (selectedBeast1 == null) selectedBeast1 = beast.id
                                        else selectedBeast2 = beast.id
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel1 || isSel2) GreenSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, if (isSel1 || isSel2) GreenSuccess else MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(beast.name.uppercase(), style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("★".repeat(beast.stars), style = TrueIdleType.tag, color = SecondaryGold)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "RANK ${beast.rank} · ${beast.trait.uppercase()} · ${beast.element.uppercase()}",
                                            style = TrueIdleType.meta,
                                            color = TextMuted
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("$dpsVal DPS", style = TrueIdleType.value, color = GreenSuccess)
                                        Text("HP: ${beast.hp.toInt()}/${beast.maxHp.toInt()}", style = TrueIdleType.meta, color = TextMuted)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        val canMerge = selectedBeast1 != null && selectedBeast2 != null && selectedBeast1 != selectedBeast2
                        Button(
                            onClick = {
                                if (canMerge) {
                                    onMergeBeasts(selectedBeast1!!, selectedBeast2!!)
                                    selectedBeast1 = null
                                    selectedBeast2 = null
                                }
                            },
                            enabled = canMerge,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canMerge) GreenSuccess else MaterialTheme.colorScheme.surface,
                                contentColor = if (canMerge) Color.Black else TextMuted
                            ),
                            border = BorderStroke(1.dp, if (canMerge) GreenSuccess else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth().testTag("merge_beasts_btn")
                        ) {
                            Text(if (canMerge) "GENETICALLY FUSE SELECTED BEASTS (+★)" else "SELECT 2 BEASTS TO FUSE", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            GenesisTab.NECROMANCY -> {
                // UNDEAD REANIMATION LEGION
                SciFiPanel(
                    title = "UNDEAD REANIMATION CORPS",
                    accent = NecroPurple,
                    tag = "💀 ${NumberFormatter.short(necro.boneEssence)} ESSENCE"
                ) {
                    // Read straight from the engine's minion table: the advertised DPS used to
                    // disagree with what summonMinion actually created.
                    val minionTypes = MinionCatalog.TYPES.map { type ->
                        Triple(type, MinionCatalog.cost(type), MinionCatalog.blurb(type))
                    }

                    // Active army summary
                    necro.army.forEach { minion ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, NecroPurple.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${minion.type.uppercase()} (x${minion.count})", style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                                    Text("LVL ${minion.lvl} · Absorption: +${(minion.soak * minion.count).toInt()} DEF", style = TrueIdleType.meta, color = TextMuted)
                                }
                                Text("+${(minion.dps * minion.count * (1.0 + (minion.lvl - 1) * 0.2)).toInt()} DPS", style = TrueIdleType.value, color = NecroPurple)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("REANIMATION RITUALS", style = TrueIdleType.label, color = MaterialTheme.colorScheme.onSurface)

                    minionTypes.forEach { (type, cost, desc) ->
                        val canSummon = necro.boneEssence >= cost
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
                                    Text(type.uppercase(), style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                                    Text(desc, style = TrueIdleType.meta, color = TextMuted)
                                }
                                Button(
                                    onClick = { onSummonMinion(type) },
                                    enabled = canSummon,
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canSummon) NecroPurple else MaterialTheme.colorScheme.surface,
                                        contentColor = if (canSummon) Color.Black else TextMuted
                                    ),
                                    border = BorderStroke(1.dp, if (canSummon) NecroPurple else MaterialTheme.colorScheme.outline)
                                ) {
                                    Text("💀 $cost", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            GenesisTab.UPGRADES -> {
                // GENESIS MATRIX UPGRADES
                SciFiPanel(
                    title = "GENESIS RESEARCH UPGRADES",
                    accent = PrimaryNeon,
                    tag = "BIOLOGY & SOUL"
                ) {
                    Text("COMPANION BEAST ENHANCEMENTS", style = TrueIdleType.label, color = GreenSuccess)
                    beasts.upgrades.forEach { upg ->
                        val cost = (upg.baseCost * upg.costMultiplier.pow(upg.rank.toDouble())).toLong()
                        val canAfford = state.player.gold >= cost
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
                                                .border(1.dp, GreenSuccess)
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                        ) {
                                            Text("RANK ${upg.rank}/${upg.maxRank}", style = TrueIdleType.numberSmall, color = GreenSuccess)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(upg.description, style = TrueIdleType.meta, color = TextMuted)
                                }

                                Button(
                                    onClick = { onUpgradeDiscipline(DisciplineCategory.GENESIS_BEAST, upg.id) },
                                    enabled = canAfford && !isMax,
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canAfford) GreenSuccess else MaterialTheme.colorScheme.surface,
                                        contentColor = if (canAfford) Color.Black else TextMuted
                                    ),
                                    border = BorderStroke(1.dp, if (canAfford) GreenSuccess else MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(if (isMax) "MAX" else "🪙 ${NumberFormatter.short(cost)}", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("NECROMANTIC SOUL UPGRADES", style = TrueIdleType.label, color = NecroPurple)
                    necro.upgrades.forEach { upg ->
                        val cost = (upg.baseCost * upg.costMultiplier.pow(upg.rank.toDouble())).toLong()
                        val canAfford = state.necro.boneEssence >= cost
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
                                                .border(1.dp, NecroPurple)
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                        ) {
                                            Text("RANK ${upg.rank}/${upg.maxRank}", style = TrueIdleType.numberSmall, color = NecroPurple)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(upg.description, style = TrueIdleType.meta, color = TextMuted)
                                }

                                Button(
                                    onClick = { onUpgradeDiscipline(DisciplineCategory.GENESIS_NECRO, upg.id) },
                                    enabled = canAfford && !isMax,
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canAfford) NecroPurple else MaterialTheme.colorScheme.surface,
                                        contentColor = if (canAfford) Color.Black else TextMuted
                                    ),
                                    border = BorderStroke(1.dp, if (canAfford) NecroPurple else MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(if (isMax) "MAX" else "💀 $cost", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
