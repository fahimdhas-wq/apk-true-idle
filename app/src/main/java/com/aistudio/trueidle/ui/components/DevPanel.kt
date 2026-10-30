package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.state.LocalDerived
import com.aistudio.trueidle.ui.theme.BorderDark
import com.aistudio.trueidle.ui.theme.GreenSuccess
import com.aistudio.trueidle.ui.theme.HpRed
import com.aistudio.trueidle.ui.theme.ManaTeal
import com.aistudio.trueidle.ui.theme.PrimaryNeon
import com.aistudio.trueidle.ui.theme.SecondaryGold
import com.aistudio.trueidle.ui.theme.SurfaceVariantDark
import com.aistudio.trueidle.ui.theme.TextLight
import com.aistudio.trueidle.ui.theme.TextMuted
import com.aistudio.trueidle.ui.theme.TrueIdleType

/**
 * Debug-only developer panel.
 *
 * Rendered by the app shell only when `BuildConfig.DEBUG` is true. It never talks to the engine
 * directly: every action is a callback into the view model, so release builds carry no debug code
 * path and this file has no engine or data dependency.
 */
@Composable
fun DevPanel(
    state: GameState,
    onAddCurrency: (String, Long) -> Unit,
    onMaxCurrency: (String) -> Unit,
    onSetGameSpeed: (Int) -> Unit,
    onUnlockAll: () -> Unit,
    onHardReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Floating bug toggle, bottom-right so it never covers the combat quick actions.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(52.dp)
                .background(Color(0xF205080F), RectangleShape)
                .border(1.dp, SecondaryGold, RectangleShape)
                .clickable(role = Role.Button) {
                    open = !open
                    if (!open) confirmReset = false
                }
                .testTag("dev_panel_toggle"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🐛", color = SecondaryGold, style = TrueIdleType.title)
                Text("DEV", style = TrueIdleType.tag, color = SecondaryGold)
            }
        }

        if (!open) return@Box

        // Scrim is a sibling so taps inside the panel never dismiss it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable { open = false; confirmReset = false }
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .background(Color(0xF205080F), RectangleShape)
                .border(1.dp, SecondaryGold, RectangleShape)
                .padding(10.dp)
                .testTag("dev_panel_overlay")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DEV PANEL // DEBUG BUILD", style = TrueIdleType.title, color = SecondaryGold)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SurfaceVariantDark, RectangleShape)
                        .border(1.dp, SecondaryGold, RectangleShape)
                        .clickable(role = Role.Button) { open = false; confirmReset = false }
                        .testTag("dev_panel_close"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", style = TrueIdleType.tag, color = TextLight)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DevSectionTitle("RESOURCES")
                DEV_CURRENCIES.forEach { kind ->
                    DevCurrencyRow(
                        kind = kind,
                        value = devValue(state, kind),
                        onAddCurrency = onAddCurrency,
                        onMaxCurrency = onMaxCurrency
                    )
                }

                DevSectionTitle("GAME SPEED")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1, 5, 10, 50).forEach { speed ->
                        val active = state.dev.gameSpeed == speed
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .background(
                                    if (active) SecondaryGold.copy(alpha = 0.2f) else SurfaceVariantDark,
                                    RectangleShape
                                )
                                .border(1.dp, if (active) SecondaryGold else BorderDark, RectangleShape)
                                .clickable { onSetGameSpeed(speed) }
                                .testTag("dev_speed_$speed"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${speed}X",
                                style = TrueIdleType.tag,
                                color = if (active) SecondaryGold else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                DevSectionTitle("UNLOCKS")
                DevActionButton(
                    label = "UNLOCK EVERYTHING (STAGES · ACHIEVEMENTS · CLASSES)",
                    accent = GreenSuccess,
                    testTag = "dev_unlock_all",
                    onClick = onUnlockAll
                )

                DevSectionTitle("LIVE STATE INSPECTOR")
                val d = LocalDerived.current
                DevReadout("PLAYER", "LV ${state.player.lvl} · ${NumberFormatter.short(state.player.gold)} CR")
                DevReadout("VITALS", "HP ${NumberFormatter.short(state.player.hp)}/${NumberFormatter.short(d.maxHp)} · MP ${NumberFormatter.short(state.player.mana)}/${NumberFormatter.short(d.maxMana)}")
                DevReadout("SECTOR", "${state.player.currentFloor} / ${state.player.maxUnlockedFloor} UNLOCKED")
                DevReadout("GEAR SCORE", "${d.gearScore} · ${state.inventory.count { it.isEquipped }} EQUIPPED")
                DevReadout("HERO DPS", NumberFormatter.short(d.physicalDamage))
                DevReadout("KILLS", "${NumberFormatter.short(state.lifetime.totalKills)} · DEFEATS ${state.lifetime.defeats}")
                DevReadout("SLAYER", "${state.slayer.lvl} LV · ${state.slayer.slayerPoints} PTS")
                DevReadout("TALENTS", "${state.talent.activeTalents.count { it.rank > 0 }} ONLINE · ${state.talent.talentPoints} TP")
                DevReadout("POTION BUFFS", "${state.activeBuffs.size} ACTIVE")
                DevReadout("INVENTORY", "${state.inventory.size} BANKS · ${state.logs.size} LOGS")
                DevReadout("SPEED", "${state.dev.gameSpeed}X")

                DevSectionTitle("DANGER ZONE")
                DevActionButton(
                    label = if (confirmReset) "CONFIRM HARD RESET (WIPES SAVE)" else "HARD RESET",
                    accent = HpRed,
                    testTag = "dev_hard_reset",
                    onClick = {
                        if (confirmReset) {
                            onHardReset()
                            confirmReset = false
                        } else {
                            confirmReset = true
                        }
                    }
                )
            }
        }
    }
}

private fun devValue(state: GameState, kind: String): Long = when (kind) {
    "GOLD" -> state.player.gold
    "DIAMONDS" -> state.player.diamonds
    "FAITH" -> state.essence.faith
    "BONES" -> state.necro.boneEssence
    "SLAYER" -> state.slayer.slayerPoints.toLong()
    "TALENT" -> state.talent.talentPoints.toLong()
    else -> 0L
}

@Composable
private fun DevSectionTitle(label: String) {
    Text(
        text = "▸ $label",
        style = TrueIdleType.label,
        color = PrimaryNeon,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun DevCurrencyRow(
    kind: String,
    value: Long,
    onAddCurrency: (String, Long) -> Unit,
    onMaxCurrency: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariantDark, RectangleShape)
            .border(1.dp, BorderDark, RectangleShape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(kind, style = TrueIdleType.meta, color = TextMuted)
            Text(NumberFormatter.short(value), style = TrueIdleType.value, color = SecondaryGold)
        }
        DevSmallButton("+100", kind, "dev_add_100_$kind") { onAddCurrency(kind, 100L) }
        DevSmallButton("+10K", kind, "dev_add_10k_$kind") { onAddCurrency(kind, 10_000L) }
        DevSmallButton("MAX", kind, "dev_max_$kind") { onMaxCurrency(kind) }
    }
}

@Composable
private fun DevSmallButton(
    label: String,
    accentKind: String,
    testTag: String,
    onClick: () -> Unit
) {
    val accent = when (accentKind) {
        "DIAMONDS" -> PrimaryNeon
        "FAITH" -> ManaTeal
        "BONES" -> SecondaryGold
        else -> TextLight
    }
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .width(52.dp)
            .background(accent.copy(alpha = 0.12f), RectangleShape)
            .border(1.dp, accent.copy(alpha = 0.7f), RectangleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = TrueIdleType.tag, color = accent)
    }
}

@Composable
private fun DevActionButton(
    label: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(accent.copy(alpha = 0.15f), RectangleShape)
            .border(1.dp, accent, RectangleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = TrueIdleType.tag, color = accent, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DevReadout(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000), RectangleShape)
            .border(1.dp, BorderDark, RectangleShape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = TrueIdleType.meta, color = TextMuted)
        Text(value, style = TrueIdleType.meta, color = TextLight)
    }
}

/** Resource kinds the panel can grant; each maps to a devAddCurrency branch in the engine. */
private val DEV_CURRENCIES = listOf("GOLD", "DIAMONDS", "FAITH", "BONES", "SLAYER", "TALENT")
