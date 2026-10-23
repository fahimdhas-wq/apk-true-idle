package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
        SummonOption("Skeleton", 10L, 4.0, 10.0, "Frontline bone soldier"),
        SummonOption("Bone Mage", 25L, 8.0, 5.0, "Ranged necro-blast caster"),
        SummonOption("Dread Knight", 50L, 16.0, 25.0, "Heavy armored death knight"),
        SummonOption("Bone Lich", 100L, 35.0, 15.0, "High DPS undead sorcerer")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("necromancy_screen")
    ) {
        // Necromancy Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Necromancy Altar", style = MaterialTheme.typography.titleLarge, color = NecroPurple, fontWeight = FontWeight.Bold)
                    Text("Raise the fallen to fight by your side", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NecroPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "💀 ${state.necro.boneEssence} Bones",
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
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Current Undead Legion", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                if (state.necro.army.isEmpty()) {
                    Text("No minions currently animated.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    state.necro.army.forEach { minion ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${minion.type} x${minion.count}", style = MaterialTheme.typography.bodyMedium, color = NecroPurple)
                            Text("DPS: ${(minion.dps * minion.count).toInt()} | Soak: ${(minion.soak * minion.count).toInt()}", style = MaterialTheme.typography.bodySmall, color = TextLight)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Summon Reinforcements", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
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
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(opt.type, style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                            Text(opt.desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Base DPS: ${opt.dps.toInt()} • Soak: ${opt.soak.toInt()}", style = MaterialTheme.typography.labelSmall, color = NecroPurple)
                        }

                        Button(
                            onClick = { onSummonMinion(opt.type) },
                            enabled = canSummon,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canSummon) NecroPurple else SurfaceVariantDark,
                                contentColor = if (canSummon) BgDark else TextMuted
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Summon (${opt.cost} 💀)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
