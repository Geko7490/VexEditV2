package com.vexedit.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexedit.studio.ui.theme.VexEditTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VexEditTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VexColors.Background
                ) {
                    ProjectFoundationScreen()
                }
            }
        }
    }
}

private object VexColors {
    val Background = Color(0xFF0F0F13)
    val Panel = Color(0xFF1C1B23)
    val Purple = Color(0xFFA855F7)
    val Muted = Color(0xFFAAA6B5)
}

@Composable
private fun ProjectFoundationScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VexColors.Background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "VEX",
            color = VexColors.Purple,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp
        )
        Text(
            text = "EDIT STUDIO",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(5.dp)
                .background(VexColors.Purple, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Native Android foundation is ready.",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Kotlin • Jetpack Compose • Offline-first",
            color = VexColors.Muted,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            color = VexColors.Panel,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "STEP 01  /  PROJECT FOUNDATION",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                color = VexColors.Purple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
