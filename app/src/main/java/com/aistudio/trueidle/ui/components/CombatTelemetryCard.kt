package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.theme.*

@Composable
fun CombatTelemetryCard(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val p = state.player
    val strBonus = p.stats.str * 1.2
    val weaponDmg = p.weaponDmg
    val sanctifiedBonus = if (state.sanctified.aegisActive) state.sanctified.smiteLevel * 3.0 else 0.0
    val playerDps = (strBonus + weaponDmg + sanctifiedBonus) * p.stats.atkSpeed
    val beastDps = state.beasts.beasts.sumOf { it.dps * it.rank * (1.0 + (it.stars - 1) * 0.25) }
    val armyDps = state.necro.army.sumOf { it.dps * it.count * (1.0 + (it.lvl - 1) * 0.2) }
    val totalDps = playerDps + beastDps + armyDps

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("combat_telemetry_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RectangleShape,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMBAT DPS MATRIX",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontSize = 8.sp
                )
                Box(
                    modifier = Modifier
                        .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                        .border(1.dp, PrimaryNeon, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TOTAL: ${totalDps.toInt()} DPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3-Column Breakdown: Hero | Beasts | Legion
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Hero DPS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF000000), RectangleShape)
                        .border(1.dp, BorderDark, RectangleShape)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HERO", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 7.sp)
                        Text("${playerDps.toInt()} DPS", style = MaterialTheme.typography.labelSmall, color = SecondaryGold, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }

                // Beasts DPS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF000000), RectangleShape)
                        .border(1.dp, BorderDark, RectangleShape)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BEASTS", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 7.sp)
                        Text("${beastDps.toInt()} DPS", style = MaterialTheme.typography.labelSmall, color = GreenSuccess, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }

                // Legion DPS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF000000), RectangleShape)
                        .border(1.dp, BorderDark, RectangleShape)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LEGION", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 7.sp)
                        Text("${armyDps.toInt()} DPS", style = MaterialTheme.typography.labelSmall, color = NecroPurple, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }

                // Speed / Loot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF000000), RectangleShape)
                        .border(1.dp, BorderDark, RectangleShape)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ATK SPEED", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 7.sp)
                        Text("${String.format("%.2f", p.stats.atkSpeed)}x", style = MaterialTheme.typography.labelSmall, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}
