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
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*
import kotlin.math.floor
import kotlin.math.pow

@Composable
fun SanctifiedScreen(
    state: GameState,
    onToggleAegis: () -> Unit,
    onUpgradeSmite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sanctified = state.sanctified
    val reqXp = Balance.skillXpReq(sanctified.lvl)
    val smiteCost = floor(25.0 * 1.5.pow(sanctified.smiteLevel.toDouble())).toLong()
    val canAffordSmite = sanctified.faith >= smiteCost
    val d = DerivedStats.of(state)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sanctified Header Card with XP Bar
        Card(
            modifier = Modifier.fillMaxWidth().testTag("sanctified_header_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, ManaTeal.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SANCTIFIED PROTOCOL",
                        style = TrueIdleType.title,
                        color = ManaTeal
                    )
                    Box(
                        modifier = Modifier
                            .background(ManaTeal.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, ManaTeal, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LVL ${sanctified.lvl}",
                            style = TrueIdleType.tag,
                            color = ManaTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                XpProgressBar(
                    skillName = "SANCTIFIED EXPERIENCE",
                    level = sanctified.lvl,
                    currentXp = sanctified.xp,
                    requiredXp = reqXp,
                    barColor = ManaTeal
                )
            }
        }

        // Faith Treasury Card
        SciFiPanel(
            title = "FAITH RESERVOIR",
            accent = ManaTeal,
            tag = "${NumberFormatter.short(sanctified.faith)} FAITH"
        ) {
            Text(
                text = "Faith is accumulated by purifying hostiles in combat (+1 per kill, +5 per boss). Used to power divine shields and smite enhancements.",
                style = TrueIdleType.body,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000), RectangleShape)
                    .border(1.dp, BorderDark, RectangleShape)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CURRENT FAITH:", style = TrueIdleType.label, color = TextMuted)
                Text("🕊 ${sanctified.faith} FAITH", style = TrueIdleType.value, color = ManaTeal)
            }
        }

        // Aegis of Light Toggle Card
        SciFiPanel(
            title = "SANCTIFIED AEGIS",
            accent = if (sanctified.aegisActive) GreenSuccess else HpRed,
            tag = if (sanctified.aegisActive) "ONLINE" else "OFFLINE"
        ) {
            Text(
                text = "Divine kinetic barrier reduces all incoming combat damage by 20%. Upkeep costs 0.5 Faith per second during active clash.",
                style = TrueIdleType.body,
                color = TextLight
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (sanctified.aegisActive)
                    "STATUS: ACTIVE (-20% damage taken, drawing 0.5 faith/s)"
                else
                    "STATUS: INACTIVE (No faith consumed, shields vulnerable)",
                style = TrueIdleType.meta,
                color = if (sanctified.aegisActive) GreenSuccess else TextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onToggleAegis,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("toggle_aegis_button"),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sanctified.aegisActive) HpRed.copy(alpha = 0.25f) else ManaTeal.copy(alpha = 0.25f),
                    contentColor = if (sanctified.aegisActive) HpRed else ManaTeal
                ),
                border = BorderStroke(1.dp, if (sanctified.aegisActive) HpRed else ManaTeal)
            ) {
                Text(
                    text = if (sanctified.aegisActive) "DISENGAGE AEGIS" else "ENGAGE AEGIS BARRIER",
                    style = TrueIdleType.tag
                )
            }
        }

        // Smite Upgrade Card
        SciFiPanel(
            title = "DIVINE SMITE",
            accent = SecondaryGold,
            tag = "LVL ${sanctified.smiteLevel}"
        ) {
            Text(
                text = "Infuse all hero strikes with righteous fury. Grants flat +3 DPS per Smite level when Aegis is active.",
                style = TrueIdleType.body,
                color = TextLight
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CURRENT BONUS:", style = TrueIdleType.label, color = TextMuted)
                Text("+${sanctified.smiteLevel * 3} DPS", style = TrueIdleType.value, color = SecondaryGold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("UPGRADE COST:", style = TrueIdleType.label, color = TextMuted)
                Text("$smiteCost FAITH", style = TrueIdleType.value, color = if (canAffordSmite) ManaTeal else HpRed)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onUpgradeSmite,
                enabled = canAffordSmite,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("upgrade_smite_button"),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryGold.copy(alpha = 0.25f),
                    contentColor = SecondaryGold,
                    disabledContainerColor = SurfaceVariantDark,
                    disabledContentColor = TextMuted
                ),
                border = BorderStroke(1.dp, if (canAffordSmite) SecondaryGold else BorderDark)
            ) {
                Text(
                    text = if (canAffordSmite) "EMPOWER SMITE (+3 DPS)" else "REQUIRES $smiteCost FAITH",
                    style = TrueIdleType.tag
                )
            }
        }
    }
}
