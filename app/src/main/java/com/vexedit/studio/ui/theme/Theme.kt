package com.vexedit.studio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VexDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA855F7),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF22C55E),
    background = Color(0xFF0F0F13),
    surface = Color(0xFF1C1B23),
    onBackground = Color(0xFFF7F5FA),
    onSurface = Color(0xFFF7F5FA)
)

@Composable
fun VexEditTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VexDarkColorScheme,
        content = content
    )
}
