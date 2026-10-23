package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class SummonOption(
    val type: String,
    val cost: Long,
    val dps: Double,
    val soak: Double,
    val desc: String
)

@Composable
fun NecromancyScreen(
    state: GameState,
    onSummonMinion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summonOptions = listOf(
        SummonOption("Skeleton", 10L, 4.0, 10.0, "Standard frontline bone combatant"),
        SummonOption("Bone Mage", 25L, 8.0, 5.0, "High-frequency arcane death caster"),
        SummonOption("Dread Knight", 50L, 16.0, 25.0, "Armored heavy vanguard unit"),
        SummonOption("Bone Lich", 100L, 35.0, 15.0, "Supreme necro-sorcerer entity")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(10.dp)
            .testTag("necromancy_screen")
    ) {
        // Necromancy Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, NecroPurple.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ALTAR OF BONES", style = MaterialTheme.typography.titleLarge, color = NecroPurple, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Reanimate defeated hostiles into combat servitors", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Box(
                    modifier = Modifier
                        .background(NecroPurple.copy(alpha = 0.15f), RectangleShape)
                        .border(1.dp, NecroPurple, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "💀 ${state.necro.boneEssence} BONES",
                        style = MaterialTheme.typography.titleMedium,
                        color = NecroPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Undead Legion Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("ACTIVE UNDEAD CADRE", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (state.necro.army.isEmpty()) {
                    Text("No entities currently animated.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    state.necro.army.forEach { minion ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("› ${minion.type.uppercase()} x${minion.count}", style = MaterialTheme.typography.bodyMedium, color = NecroPurple)
                            Text("DPS: ${(minion.dps * minion.count).toInt()} | SOAK: ${(minion.soak * minion.count).toInt()}", style = MaterialTheme.typography.bodySmall, color = TextLight)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("ANIMATION PROTOCOLS", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))

        // Summoning Options
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(summonOptions) { opt ->
                val canSummon = state.necro.boneEssence >= opt.cost

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("summon_opt_${opt.type}"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (canSummon) NecroPurple.copy(alpha = 0.5f) else BorderDark)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(opt.type.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(opt.desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("BASE DPS: ${opt.dps.toInt()} • SOAK: ${opt.soak.toInt()}", style = MaterialTheme.typography.labelSmall, color = NecroPurple)
                        }

                        OutlinedButton(
                            onClick = { onSummonMinion(opt.type) },
                            enabled = canSummon,
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, if (canSummon) NecroPurple else BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (canSummon) NecroPurple.copy(alpha = 0.2f) else SurfaceVariantDark,
                                contentColor = if (canSummon) NecroPurple else TextMuted
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("ANIMATE [${opt.cost} 💀]", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
