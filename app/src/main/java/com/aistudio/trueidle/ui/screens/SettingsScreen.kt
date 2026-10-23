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
            .verticalScroll(scrollState)
            .padding(12.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Achievements Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("achievements_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Achievements", style = MaterialTheme.typography.titleMedium, color = SecondaryGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                state.achievements.forEach { ach ->
                    val isCompleted = ach.isUnlocked || ach.progress >= ach.target
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ach.title, style = MaterialTheme.typography.bodyMedium, color = if (isCompleted) SecondaryGold else TextLight, fontWeight = FontWeight.Bold)
                            Text(ach.desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isCompleted) GreenSuccess.copy(alpha = 0.2f) else SurfaceVariantDark)
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

        // Cloud & Offline Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Save & Engine Status", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Auto-Save: Active (Local Cache + StateFlow)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text("• Offline Progress: Enabled (Simulates ticks on resume)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text("• Combat Engine: 4 Ticks/sec Active", style = MaterialTheme.typography.bodySmall, color = PrimaryNeon)
                Text("• Game Version: 1.0.0 (True IDLE Mobile)", style = MaterialTheme.typography.bodySmall, color = TextLight)
            }
        }
    }
}
