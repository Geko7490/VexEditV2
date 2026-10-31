package com.vexedit.studio

import androidx.compose.ui.graphics.toArgb
import com.vexedit.studio.ui.theme.VexDimensions
import com.vexedit.studio.ui.theme.VexPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectConfigurationTest {
    @Test
    fun applicationIdentityIsStable() {
        assertEquals("com.vexedit.studio", "com.vexedit.studio")
    }

    @Test
    fun vexBrandPaletteUsesExpectedColors() {
        assertEquals(0xFFA855F7.toInt(), VexPalette.Purple.toArgb())
        assertEquals(0xFF0F0F13.toInt(), VexPalette.Background.toArgb())
        assertEquals(0xFF1C1B23.toInt(), VexPalette.Panel.toArgb())
    }

    @Test
    fun semanticTrackColorsAreDistinct() {
        assertTrue(VexPalette.Music != VexPalette.Voice)
        assertTrue(VexPalette.Voice != VexPalette.Sfx)
        assertTrue(VexPalette.Music != VexPalette.Sfx)
    }

    @Test
    fun sharpCornerDimensionsStayWithinSixToEightDp() {
        assertEquals(6f, VexDimensions.CornerSmall.value, 0.01f)
        assertEquals(8f, VexDimensions.CornerMedium.value, 0.01f)
        assertTrue(VexDimensions.CornerLarge.value in 6f..8f)
    }
}
