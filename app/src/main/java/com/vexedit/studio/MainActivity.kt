package com.vexedit.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vexedit.studio.ui.components.VexPanel
import com.vexedit.studio.ui.components.VexPrimaryButton
import com.vexedit.studio.ui.components.VexSecondaryButton
import com.vexedit.studio.ui.components.VexSectionHeader
import com.vexedit.studio.ui.components.VexStatusChip
import com.vexedit.studio.ui.components.VexTrackBadge
import com.vexedit.studio.ui.components.VexTrackKind
import com.vexedit.studio.ui.theme.VexDimensions
import com.vexedit.studio.ui.theme.VexPalette
import com.vexedit.studio.ui.theme.VexEditTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VexEditTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = VexPalette.Background) {
                    DesignSystemScreen()
                }
            }
        }
    }
}

/** Temporary component catalogue used to validate the design system before feature screens arrive. */
@Composable
private fun DesignSystemScreen() {
    var actionMessage by remember { mutableStateOf("Design system loaded") }
    var selectedTrack by remember { mutableStateOf(VexTrackKind.MUSIC) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VexPalette.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VexDimensions.ScreenHorizontal, vertical = VexDimensions.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("VEX", color = VexPalette.Purple, style = MaterialTheme.typography.titleMedium)
            Text("EDIT STUDIO", color = VexPalette.TextPrimary, style = MaterialTheme.typography.displayMedium)
            Text("DESIGN SYSTEM  /  STEP 02", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
        }

        VexPanel(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(VexDimensions.PanelPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VexSectionHeader("VEX Purple", "Core brand color and dark surfaces")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VexStatusChip("PRIMARY", color = VexPalette.Purple)
                    VexStatusChip("DARK UI", color = VexPalette.TextSecondary)
                }
                Text("#A855F7  •  #0F0F13  •  #1C1B23", color = VexPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VexSectionHeader("Common actions", "Reusable button styles and feedback")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VexPrimaryButton("New project", onClick = { actionMessage = "Primary action component selected" }, icon = Icons.Rounded.Add, modifier = Modifier.weight(1f))
                VexSecondaryButton("Cancel", onClick = { actionMessage = "Secondary action component selected" }, icon = Icons.Rounded.Close, modifier = Modifier.weight(1f))
            }
            VexStatusChip(actionMessage, color = VexPalette.Success, icon = Icons.Rounded.CheckCircle)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VexSectionHeader("Timeline identities", "Semantic colors reserved for track types")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VexTrackBadge(VexTrackKind.MUSIC)
                VexTrackBadge(VexTrackKind.VOICE)
                VexTrackBadge(VexTrackKind.SFX)
            }
            VexPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(VexDimensions.PanelPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select a track type", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VexTrackKind.entries.forEach { kind ->
                            VexSecondaryButton(
                                text = kind.label,
                                onClick = { selectedTrack = kind },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    VexTrackBadge(selectedTrack)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VexSectionHeader("Typography", "Consistent hierarchy for phone layouts")
            Text("Display / 32sp", style = MaterialTheme.typography.displayMedium, color = VexPalette.TextPrimary)
            Text("Section title / 18sp", style = MaterialTheme.typography.titleLarge, color = VexPalette.TextPrimary)
            Text("Body copy / 14sp", style = MaterialTheme.typography.bodyMedium, color = VexPalette.TextSecondary)
            Text("LABEL / 10sp", style = MaterialTheme.typography.labelSmall, color = VexPalette.Purple)
        }

        Spacer(Modifier.height(8.dp))
    }
}
