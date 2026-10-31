package com.vexedit.studio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Central VEX palette. Keep semantic colors here instead of scattering hex values in screens. */
object VexPalette {
    val Background = Color(0xFF0F0F13)
    val BackgroundElevated = Color(0xFF101014)
    val Panel = Color(0xFF1C1B23)
    val PanelElevated = Color(0xFF24222D)
    val Outline = Color(0xFF35313F)
    val Purple = Color(0xFFA855F7)
    val PurplePressed = Color(0xFF9333EA)
    val PurpleTint = Color(0xFF2B1B3B)
    val TextPrimary = Color(0xFFF7F5FA)
    val TextSecondary = Color(0xFFAAA6B5)
    val TextDisabled = Color(0xFF706B79)
    val Music = Color(0xFF22C55E)
    val Voice = Color(0xFFF59E0B)
    val Sfx = Color(0xFF22D3EE)
    val Error = Color(0xFFEF4444)
    val Success = Color(0xFF22C55E)
}

/** Android layouts use dp (density-independent pixels), not physical px. */
object VexDimensions {
    val CornerSmall = 6.dp
    val CornerMedium = 8.dp
    val CornerLarge = 8.dp
    val ScreenHorizontal = 20.dp
    val ScreenVertical = 24.dp
    val PanelPadding = 16.dp
    val ComponentHeight = 48.dp
    val IconButtonSize = 44.dp
}

val VexShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

val VexTypography = Typography(
    displayLarge = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
)

private val VexDarkColorScheme = darkColorScheme(
    primary = VexPalette.Purple,
    onPrimary = Color.White,
    primaryContainer = VexPalette.PurpleTint,
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = VexPalette.Music,
    onSecondary = Color(0xFF06210F),
    tertiary = VexPalette.Sfx,
    onTertiary = Color(0xFF062329),
    error = VexPalette.Error,
    onError = Color.White,
    background = VexPalette.Background,
    onBackground = VexPalette.TextPrimary,
    surface = VexPalette.Panel,
    onSurface = VexPalette.TextPrimary,
    surfaceVariant = VexPalette.PanelElevated,
    onSurfaceVariant = VexPalette.TextSecondary,
    outline = VexPalette.Outline
)

@Composable
fun VexEditTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VexDarkColorScheme,
        typography = VexTypography,
        shapes = VexShapes,
        content = content
    )
}
