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
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.theme.*

@Composable
fun SettingsScreen(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(10.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Achievements Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("achievements_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("ACHIEVEMENTS DATABASE", style = MaterialTheme.typography.titleMedium, color = SecondaryGold, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))

                state.achievements.forEach { ach ->
                    val isCompleted = ach.isUnlocked || ach.progress >= ach.target
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ach.title.uppercase(), style = MaterialTheme.typography.bodyMedium, color = if (isCompleted) SecondaryGold else TextLight, fontWeight = FontWeight.Bold)
                            Text(ach.desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Box(
                            modifier = Modifier
                                .background(if (isCompleted) GreenSuccess.copy(alpha = 0.15f) else SurfaceVariantDark, RectangleShape)
                                .border(1.dp, if (isCompleted) GreenSuccess else BorderDark, RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isCompleted) "CLAIMED" else "${ach.progress}/${ach.target}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCompleted) GreenSuccess else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Cloud & System Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("ENGINE & SYSTEM TELEMETRY", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• AUTO-SAVE: ACTIVE (LOCAL PERSISTENCE ENGINE)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text("• OFFLINE ACCUMULATOR: ENABLED (SIMULATES BACKGROUND TICKS)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text("• CORE CLOCK: 4 TICKS/SEC SYNCHRONIZED", style = MaterialTheme.typography.bodySmall, color = PrimaryNeon)
                Text("• BUILD REVISION: TRUE IDLE V1.0.0 (CYBERPUNK ENGINE)", style = MaterialTheme.typography.bodySmall, color = TextLight)
            }
        }
    }
}
