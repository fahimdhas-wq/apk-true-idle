package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.ui.theme.*

enum class SciFiIconType {
    CREDIT,
    DIAMOND,
    ESSENCE,
    WEAPON,
    SHIELD
}

object SciFiVectors {
    // 24x24 sci-fi hexagonal credit coin
    val Credit: ImageVector by lazy {
        ImageVector.Builder(
            name = "SciFiCredit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                lineTo(21f, 7f)
                lineTo(21f, 17f)
                lineTo(12f, 22f)
                lineTo(3f, 17f)
                lineTo(3f, 7f)
                close()
                // Inner hex cutout
                moveTo(12f, 5.5f)
                lineTo(5.5f, 9.2f)
                lineTo(5.5f, 14.8f)
                lineTo(12f, 18.5f)
                lineTo(18.5f, 14.8f)
                lineTo(18.5f, 9.2f)
                close()
                // Center core
                moveTo(10f, 9f)
                lineTo(14f, 9f)
                lineTo(14f, 11f)
                lineTo(11f, 11f)
                lineTo(11f, 13f)
                lineTo(14f, 13f)
                lineTo(14f, 15f)
                lineTo(10f, 15f)
                close()
            }
        }.build()
    }

    // 24x24 faceted sci-fi gem / diamond
    val Diamond: ImageVector by lazy {
        ImageVector.Builder(
            name = "SciFiDiamond",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 3f)
                lineTo(18f, 3f)
                lineTo(22f, 9f)
                lineTo(12f, 22f)
                lineTo(2f, 9f)
                close()
                // Facet details
                moveTo(7.5f, 8.5f)
                lineTo(12f, 19.2f)
                lineTo(16.5f, 8.5f)
                close()
            }
        }.build()
    }

    // 24x24 cyber-necro skull (sharp angular polygon)
    val Essence: ImageVector by lazy {
        ImageVector.Builder(
            name = "SciFiEssence",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Outer angular skull
                moveTo(7f, 3f)
                lineTo(17f, 3f)
                lineTo(21f, 8f)
                lineTo(21f, 15f)
                lineTo(17f, 18f)
                lineTo(17f, 22f)
                lineTo(7f, 22f)
                lineTo(7f, 18f)
                lineTo(3f, 15f)
                lineTo(3f, 8f)
                close()
                // Left eye socket
                moveTo(7f, 10f)
                lineTo(10f, 10f)
                lineTo(9f, 14f)
                lineTo(6f, 14f)
                close()
                // Right eye socket
                moveTo(14f, 10f)
                lineTo(17f, 10f)
                lineTo(18f, 14f)
                lineTo(15f, 14f)
                close()
                // Center nasal
                moveTo(12f, 14f)
                lineTo(13f, 17f)
                lineTo(11f, 17f)
                close()
            }
        }.build()
    }

    // 24x24 crossed energy blades
    val Weapon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SciFiWeapon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6.92f, 5f)
                lineTo(5f, 6.92f)
                lineTo(8.08f, 10f)
                lineTo(3f, 15.08f)
                lineTo(3f, 17f)
                lineTo(4.92f, 17f)
                lineTo(10f, 11.92f)
                lineTo(13.08f, 15f)
                lineTo(15f, 13.08f)
                lineTo(11.92f, 10f)
                lineTo(17f, 4.92f)
                lineTo(17f, 3f)
                lineTo(15.08f, 3f)
                lineTo(10f, 8.08f)
                close()
                // Cross guard
                moveTo(18.08f, 16.08f)
                lineTo(21f, 19f)
                lineTo(19f, 21f)
                lineTo(16.08f, 18.08f)
                close()
            }
        }.build()
    }

    // 24x24 cyber aegis shield
    val Shield: ImageVector by lazy {
        ImageVector.Builder(
            name = "SciFiShield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                lineTo(21f, 5f)
                lineTo(21f, 12f)
                lineTo(17f, 18f)
                lineTo(12f, 22f)
                lineTo(7f, 18f)
                lineTo(3f, 12f)
                lineTo(3f, 5f)
                close()
                // Inner core
                moveTo(12f, 5f)
                lineTo(18f, 7.5f)
                lineTo(18f, 11.5f)
                lineTo(15f, 16f)
                lineTo(12f, 19f)
                lineTo(9f, 16f)
                lineTo(6f, 11.5f)
                lineTo(6f, 7.5f)
                close()
            }
        }.build()
    }
}

@Composable
fun SciFiIcon(
    type: SciFiIconType,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tint: Color = when (type) {
        SciFiIconType.CREDIT -> SecondaryGold
        SciFiIconType.DIAMOND -> PrimaryNeon
        SciFiIconType.ESSENCE -> NecroPurple
        SciFiIconType.WEAPON -> HpRed
        SciFiIconType.SHIELD -> ShieldBlue
    }
) {
    val vector = when (type) {
        SciFiIconType.CREDIT -> SciFiVectors.Credit
        SciFiIconType.DIAMOND -> SciFiVectors.Diamond
        SciFiIconType.ESSENCE -> SciFiVectors.Essence
        SciFiIconType.WEAPON -> SciFiVectors.Weapon
        SciFiIconType.SHIELD -> SciFiVectors.Shield
    }
    Icon(
        imageVector = vector,
        contentDescription = type.name,
        modifier = modifier.size(size),
        tint = tint
    )
}

@Composable
fun SciFiBadge(
    type: SciFiIconType,
    label: String,
    modifier: Modifier = Modifier,
    textColor: Color = when (type) {
        SciFiIconType.CREDIT -> SecondaryGold
        SciFiIconType.DIAMOND -> PrimaryNeon
        SciFiIconType.ESSENCE -> NecroPurple
        SciFiIconType.WEAPON -> SecondaryGold
        SciFiIconType.SHIELD -> ShieldBlue
    },
    iconTint: Color = textColor,
    iconSize: Dp = 12.dp,
    textStyle: TextStyle = TrueIdleType.tag
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SciFiIcon(type = type, size = iconSize, tint = iconTint)
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = label, style = textStyle, color = textColor)
    }
}
