package com.aistudio.trueidle.ui.screens

import androidx.compose.runtime.remember
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
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.theme.*

@Composable
fun TalentScreen(
    state: GameState,
    onActivateTalent: (String) -> Unit,
    onUpgradeTalent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val talent = state.talent
    val derived = com.aistudio.trueidle.ui.state.LocalDerived.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(8.dp)
            .testTag("talent_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. TALENT SYSTEM HEADER
        Card(
            modifier = Modifier.fillMaxWidth().testTag("talent_header_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("HERO TALENTS", style = TrueIdleType.titleLarge, color = PrimaryNeon)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Earn Talent Points (TP) by leveling up and defeating dungeon bosses.",
                        style = TrueIdleType.meta,
                        color = TextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                        .border(1.dp, PrimaryNeon, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${talent.talentPoints} TP AVAILABLE",
                        style = TrueIdleType.numberSmall,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 1b. AUTOMATED SKILLS EXECUTION
        SciFiPanel(
            title = "AUTO COMBAT SKILLS",
            accent = GreenSuccess,
            tag = "${talent.activeTalents.count { it.rank > 0 }}/${talent.activeTalents.size} POWERS ONLINE"
        ) {
            Text(
                text = "All unlocked combat skills (Rank > 0) trigger automatically when off cooldown. Abilities run on independent timers and can be active at the same time.",
                style = TrueIdleType.body,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            talent.activeTalents.filter { it.rank > 0 }.forEach { act ->
                val status = when {
                    act.isActive -> "ACTIVE ${act.durationRemaining.toInt()}s"
                    act.cooldownRemaining > 0.0 -> "CD ${act.cooldownRemaining.toInt()}s"
                    else -> "FIRING"
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(act.name.uppercase(), style = TrueIdleType.label, color = TextLight)
                    Text(
                        text = status,
                        style = TrueIdleType.tag,
                        color = if (act.isActive) GreenSuccess else PrimaryNeon
                    )
                }
            }
        }

        // 2. SURVIVAL PASSIVES
        SciFiPanel(
            title = "SURVIVAL PASSIVES",
            accent = HpRed,
            tag = "DAMAGE ENDURED: ${NumberFormatter.short(talent.struggleDamageTaken)}"
        ) {
            Text(
                text = "Every point of combat damage taken permanently augments your attributes, even while shields absorb damage.",
                style = TrueIdleType.body,
                color = TextLight
            )

            Spacer(modifier = Modifier.height(6.dp))

            talent.struggleTalents.forEach { st ->
                val (threshold, bonusText, currentBonus) = when (st.id) {
                    "struggle_hp" -> Triple(100L, "+${5 * st.rank} HP per 100 DMG taken", "+${NumberFormatter.short(derived.struggleHpBonus)} MAX HP")
                    "struggle_def" -> Triple(500L, "+${1 * st.rank} DEF per 500 DMG taken", "+${NumberFormatter.short(derived.struggleDefBonus)} DEF")
                    "struggle_mana" -> Triple(150L, "+${3 * st.rank} MANA per 150 DMG taken", "+${NumberFormatter.short(derived.struggleManaBonus)} MAX MANA")
                    "struggle_atk" -> Triple(250L, "+${2 * st.rank} ATK per 250 DMG taken", "+${NumberFormatter.short(derived.struggleAtkBonus)} ATK")
                    else -> Triple(100L, "", "")
                }

                val currentProgress = ((talent.struggleDamageTaken % threshold).toFloat() / threshold).coerceIn(0f, 1f)
                val canUpgrade = talent.talentPoints > 0 && st.rank < st.maxRank

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (canUpgrade) HpRed.copy(alpha = 0.6f) else BorderDark)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(st.name, style = TrueIdleType.title, color = TextLight)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(SurfaceVariantDark, RectangleShape)
                                        .border(1.dp, HpRed)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("RANK ${st.rank}/${st.maxRank}", style = TrueIdleType.numberSmall, color = HpRed)
                                }
                            }
                            Text(currentBonus, style = TrueIdleType.value, color = HpRed)
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${st.description} · Current rate: $bonusText",
                            style = TrueIdleType.meta,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = HpRed,
                            trackColor = SurfaceVariantDark
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { onUpgradeTalent(st.id) },
                            enabled = canUpgrade,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canUpgrade) HpRed else SurfaceVariantDark,
                                contentColor = if (canUpgrade) Color.White else TextMuted
                            ),
                            border = BorderStroke(1.dp, if (canUpgrade) HpRed else BorderDark),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("upgrade_struggle_${st.id}")
                        ) {
                            Text(
                                text = if (st.rank >= st.maxRank) "MAX RANK REACHED" else "UPGRADE PASSIVE (1 TP)",
                                style = TrueIdleType.tag,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. ACTIVE COMBAT SKILLS
        SciFiPanel(
            title = "ACTIVE COMBAT SKILLS",
            accent = SecondaryGold,
            tag = "AUTOMATIC POWERS"
        ) {
            talent.activeTalents.forEach { act ->
                val isOnCd = act.cooldownRemaining > 0.0
                val isActive = act.isActive
                val canUpgrade = talent.talentPoints > 0 && act.rank < act.maxRank

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) SecondaryGold.copy(alpha = 0.12f) else SurfaceDark
                    ),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (isActive) SecondaryGold else BorderDark)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(act.name, style = TrueIdleType.title, color = TextLight)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(SurfaceVariantDark, RectangleShape)
                                        .border(1.dp, SecondaryGold)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("RANK ${act.rank}/${act.maxRank}", style = TrueIdleType.numberSmall, color = SecondaryGold)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isActive) GreenSuccess.copy(alpha = 0.2f)
                                        else if (isOnCd) HpRed.copy(alpha = 0.2f)
                                        else PrimaryNeon.copy(alpha = 0.2f),
                                        RectangleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (isActive) GreenSuccess
                                        else if (isOnCd) HpRed
                                        else PrimaryNeon
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isActive) "ACTIVE (${act.durationRemaining.toInt()}s)"
                                    else if (isOnCd) "CD: ${act.cooldownRemaining.toInt()}s"
                                    else "READY",
                                    style = TrueIdleType.tag,
                                    color = if (isActive) GreenSuccess else if (isOnCd) HpRed else PrimaryNeon
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        Text(act.description, style = TrueIdleType.meta, color = TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Cooldown: ${act.cooldownSeconds.toInt()}s · Duration: ${act.durationSeconds.toInt()}s", style = TrueIdleType.meta, color = TextMuted)

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // ACTIVATE BUTTON
                            Button(
                                onClick = { onActivateTalent(act.id) },
                                enabled = !isOnCd,
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isOnCd) SecondaryGold else SurfaceVariantDark,
                                    contentColor = if (!isOnCd) Color.Black else TextMuted
                                ),
                                border = BorderStroke(1.dp, if (!isOnCd) SecondaryGold else BorderDark),
                                modifier = Modifier.weight(1f).height(40.dp).testTag("activate_talent_${act.id}")
                            ) {
                                Text(if (isOnCd) "COOLDOWN..." else "TRIGGER NOW", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                            }

                            // UPGRADE BUTTON
                            OutlinedButton(
                                onClick = { onUpgradeTalent(act.id) },
                                enabled = canUpgrade,
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, if (canUpgrade) PrimaryNeon else BorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (canUpgrade) PrimaryNeon.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (canUpgrade) PrimaryNeon else TextMuted
                                ),
                                modifier = Modifier.weight(1f).height(40.dp).testTag("upgrade_talent_${act.id}")
                            ) {
                                Text(if (act.rank >= act.maxRank) "MAX" else "UPGRADE (1 TP)", style = TrueIdleType.tag)
                            }
                        }
                    }
                }
            }
        }
    }
}
