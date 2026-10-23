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
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.theme.*

@Composable
fun StatsScreen(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val p = state.player
    val reqXp = Balance.playerXpReq(p.lvl)
    val xpProgress = (p.xp.toFloat() / reqXp.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(10.dp)
            .testTag("stats_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("stats_header_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HERO PROTOCOL", style = MaterialTheme.typography.titleLarge, color = PrimaryNeon, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Box(
                        modifier = Modifier
                            .background(SecondaryGold.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, SecondaryGold, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("LEVEL ${p.lvl}", style = MaterialTheme.typography.labelSmall, color = SecondaryGold, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("EXPERIENCE PROGRESSION", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 0.5.sp)
                    Text("${p.xp} / $reqXp XP", style = MaterialTheme.typography.labelSmall, color = TextLight)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFF000000), RectangleShape)
                        .border(1.dp, BorderDark, RectangleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(xpProgress)
                            .background(PrimaryNeon, RectangleShape)
                    )
                }
            }
        }

        // Core Attributes Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("CORE ATTRIBUTES", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    "STRENGTH (STR)" to "${p.stats.str.toInt()} (+${(p.stats.str * 1.2).toInt()} DMG)",
                    "INTELLIGENCE (INT)" to "${p.stats.intVal.toInt()} (+${(p.stats.intVal * 2).toInt()} MANA)",
                    "VITALITY (VIT)" to "${p.stats.vit.toInt()} (+${(p.stats.vit * 10).toInt()} HP)",
                    "LUCK INDEX" to "${p.stats.luck.toInt()}% (LOOT RATE)",
                    "ATTACK FREQUENCY" to "${String.format("%.2f", p.stats.atkSpeed)}x MULTIPLIER"
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(value, style = MaterialTheme.typography.bodyMedium, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Equipped Gear
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("EQUIPPED ARSENAL", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⚔ MAIN WEAPON", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Text("${p.weaponSlot.uppercase()} (+${p.weaponDmg.toInt()} DMG)", style = MaterialTheme.typography.bodyMedium, color = SecondaryGold, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🛡 ARMOR PLATING", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Text("${p.armorSlot.uppercase()} (+${p.armorDef.toInt()} DEF)", style = MaterialTheme.typography.bodyMedium, color = ShieldBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Lifetime Telemetry
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("TACTICAL TELEMETRY", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    "TOTAL HOSTILES ELIMINATED" to "${state.totalKills}",
                    "DEEPEST SECTOR UNLOCKED" to "SECTOR ${p.maxUnlockedFloor}",
                    "CREDITS IN TREASURY" to "${p.gold} 🪙",
                    "TAMED COMPANIONS" to "${state.beasts.beasts.size}",
                    "UNDEAD CORPS SIZE" to "${state.necro.army.sumOf { it.count }}"
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text(value, style = MaterialTheme.typography.bodySmall, color = TextLight, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
