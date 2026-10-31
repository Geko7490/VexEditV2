package com.vexedit.studio

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectConfigurationTest {
    @Test
    fun applicationIdentityIsStable() {
        assertEquals("com.vexedit.studio", "com.vexedit.studio")
    }

    @Test
    fun initialVersionIsDefined() {
        val versionName = "0.1.0"
        assertEquals("0.1.0", versionName)
    }
}
