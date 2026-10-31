package com.vexedit.studio

import androidx.compose.ui.graphics.toArgb
import com.vexedit.studio.data.project.ProjectEntity
import com.vexedit.studio.ui.theme.VexDimensions
import com.vexedit.studio.ui.theme.VexPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun projectEntityKeepsCreationSettingsAndTrashState() {
        val project = ProjectEntity(
            id = "test-id",
            name = "Short Film",
            aspectRatio = "9:16",
            resolution = "1080p",
            fps = 60,
            createdAt = 100L,
            updatedAt = 200L
        )
        assertEquals("Short Film", project.name)
        assertEquals("9:16", project.aspectRatio)
        assertEquals("1080p", project.resolution)
        assertEquals(60, project.fps)
        assertFalse(project.isTrashed)
        assertTrue(project.copy(isTrashed = true).isTrashed)
    }
}
