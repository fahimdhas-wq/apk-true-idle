package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .verticalScroll(scrollState)
            .padding(12.dp)
            .testTag("stats_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("stats_header_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hero Character", style = MaterialTheme.typography.titleLarge, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                    Text("LVL ${p.lvl}", style = MaterialTheme.typography.titleMedium, color = SecondaryGold, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Experience", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("${p.xp} / $reqXp XP", style = MaterialTheme.typography.labelSmall, color = TextLight)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceVariantDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(xpProgress)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryNeon)
                    )
                }
            }
        }

        // Attributes Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Core Attributes", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                listOf(
                    "Strength (STR)" to "${p.stats.str.toInt()} (+${(p.stats.str * 1.2).toInt()} Damage)",
                    "Intelligence (INT)" to "${p.stats.intVal.toInt()} (+${(p.stats.intVal * 2).toInt()} Mana Pool)",
                    "Vitality (VIT)" to "${p.stats.vit.toInt()} (+${(p.stats.vit * 10).toInt()} Max HP)",
                    "Luck (LUCK)" to "${p.stats.luck.toInt()}% (+Drop Multiplier)",
                    "Attack Speed" to "${String.format("%.2f", p.stats.atkSpeed)}x Multiplier"
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
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Equipped Gear", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⚔️ Main Hand Weapon", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Text("${p.weaponSlot} (+${p.weaponDmg.toInt()} DMG)", style = MaterialTheme.typography.bodyMedium, color = SecondaryGold, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🛡️ Body Armor", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Text("${p.armorSlot} (+${p.armorDef.toInt()} DEF)", style = MaterialTheme.typography.bodyMedium, color = ShieldBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Lifetime Statistics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Adventure Statistics", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    "Total Monsters Defeated" to "${state.totalKills}",
                    "Deepest Floor Unlocked" to "Floor ${p.maxUnlockedFloor}",
                    "Gold in Treasury" to "${p.gold} 🪙",
                    "Beasts in Sanctuary" to "${state.beasts.beasts.size}",
                    "Undead Legion Size" to "${state.necro.army.sumOf { it.count }}"
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextLight)
                    }
                }
            }
        }
    }
}
