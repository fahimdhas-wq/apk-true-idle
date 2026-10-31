package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.theme.*

@Composable
fun CombatTelemetryCard(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val d = com.aistudio.trueidle.ui.state.LocalDerived.current
    val playerDps = CombatMath.heroDps(d)
    val beastDps = CombatMath.beastDps(state.beasts)
    val armyDps = CombatMath.armyDps(state.necro)
    val totalDps = CombatMath.totalDps(state, d)

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
                    text = "COMBAT DPS BREAKDOWN",
                    style = TrueIdleType.tag,
                    color = TextMuted
                )
                Box(
                    modifier = Modifier
                        .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                        .border(1.dp, PrimaryNeon, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TOTAL: ${NumberFormatter.short(totalDps)} DPS",
                        style = TrueIdleType.tag,
                        color = PrimaryNeon
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Two rows of two. Four columns in one row left each cell ~70dp wide, which clipped
            // its value once the system font size was raised.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TelemetryCell("HERO", NumberFormatter.short(playerDps), SecondaryGold, Modifier.weight(1f))
                TelemetryCell("BEASTS", NumberFormatter.short(beastDps), GreenSuccess, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TelemetryCell("LEGION", NumberFormatter.short(armyDps), NecroPurple, Modifier.weight(1f))
                TelemetryCell(
                    "SPEED",
                    String.format(java.util.Locale.ROOT, "%.2fx", d.atkSpeed),
                    TextLight,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TelemetryCell(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF000000), RectangleShape)
            .border(1.dp, BorderDark, RectangleShape)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = TrueIdleType.meta, color = TextMuted)
            Text("$value DPS", style = TrueIdleType.label, color = accent)
        }
    }
}
