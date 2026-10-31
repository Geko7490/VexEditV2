package com.vexedit.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vexedit.studio.ui.theme.VexDimensions
import com.vexedit.studio.ui.theme.VexPalette

@Composable
fun VexPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = VexPalette.Panel,
        shape = RoundedCornerShape(VexDimensions.CornerMedium),
        content = content
    )
}

@Composable
fun VexSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = VexPalette.TextPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VexPalette.TextSecondary)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun VexPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = VexDimensions.ComponentHeight),
        enabled = enabled,
        shape = RoundedCornerShape(VexDimensions.CornerSmall),
        colors = ButtonDefaults.buttonColors(
            containerColor = VexPalette.Purple,
            contentColor = Color.White,
            disabledContainerColor = VexPalette.PanelElevated,
            disabledContentColor = VexPalette.TextDisabled
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun VexSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = VexDimensions.ComponentHeight),
        shape = RoundedCornerShape(VexDimensions.CornerSmall)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun VexStatusChip(
    text: String,
    color: Color = VexPalette.Purple,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(VexDimensions.CornerSmall))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(VexDimensions.CornerSmall))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

enum class VexTrackKind(val label: String, val color: Color, val icon: ImageVector) {
    MUSIC("MUSIC", VexPalette.Music, Icons.Rounded.MusicNote),
    VOICE("VOICE", VexPalette.Voice, Icons.Rounded.Mic),
    SFX("SFX", VexPalette.Sfx, Icons.Rounded.GraphicEq)
}

@Composable
fun VexTrackBadge(kind: VexTrackKind, modifier: Modifier = Modifier) {
    VexStatusChip(text = kind.label, color = kind.color, modifier = modifier, icon = kind.icon)
}

@Composable
fun VexIconGlyph(
    image: ImageVector,
    contentDescription: String?,
    tint: Color = VexPalette.TextSecondary,
    modifier: Modifier = Modifier
) {
    Icon(imageVector = image, contentDescription = contentDescription, tint = tint, modifier = modifier.size(20.dp))
}
