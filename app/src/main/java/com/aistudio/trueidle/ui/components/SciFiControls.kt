package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.trueidle.ui.theme.BorderDark
import com.aistudio.trueidle.ui.theme.PrimaryNeon
import com.aistudio.trueidle.ui.theme.SurfaceVariantDark
import com.aistudio.trueidle.ui.theme.TextMuted
import com.aistudio.trueidle.ui.theme.TrueIdleType

/**
 * Square HUD button with the app-wide 48dp minimum touch target. [filled] draws a solid block;
 * otherwise a tinted outline. Pass `Modifier.testTag(...)` and sizing through [modifier].
 */
@Composable
fun SciFiButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = PrimaryNeon,
    filled: Boolean = false,
    enabled: Boolean = true
) {
    val accent = if (enabled) color else TextMuted
    val container = when {
        !enabled -> SurfaceVariantDark
        filled -> color
        else -> color.copy(alpha = 0.12f)
    }
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(container, RectangleShape)
            .border(1.dp, if (enabled) color else BorderDark, RectangleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TrueIdleType.tag,
            color = if (filled && enabled) Color.Black else accent,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Modal in the game's own style. A Dialog is a separate window whose view resets LocalDensity to
 * the raw device density, which drops the global UI_SCALE and renders the dialog oversized; the
 * scaled density captured here is re-provided inside the window.
 */
@Composable
fun SciFiDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = PrimaryNeon,
    dismissible: Boolean = true,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val appDensity = LocalDensity.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        CompositionLocalProvider(LocalDensity provides appDensity) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.surface, RectangleShape)
                    .border(1.dp, accent, RectangleShape)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(3.dp, 12.dp).background(accent))
                    Spacer(Modifier.width(6.dp))
                    Text(title, style = TrueIdleType.title, color = accent)
                }
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) { content() }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) { actions() }
            }
        }
    }
}
