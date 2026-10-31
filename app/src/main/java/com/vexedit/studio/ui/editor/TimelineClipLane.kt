package com.vexedit.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.vexedit.studio.ui.theme.VexPalette
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun TimelineClipLane(
    trackType: EditorTrackType,
    clips: List<EditorClip>,
    selectedId: String?,
    zoom: Float,
    timelineDurationMs: Long,
    playheadMs: Long,
    onSelect: (EditorClip) -> Unit,
    onMove: (EditorClip, Long) -> Unit,
    onDragPreview: (EditorClip, Long) -> Unit,
    onTrimStart: (EditorClip, Long) -> Unit,
    onTrimEnd: (EditorClip, Long) -> Unit
) {
    val density = LocalDensity.current.density
    val dpPerSecond = 72f * zoom
    val contentWidth = (timelineDurationMs / 1000f * dpPerSecond).coerceAtLeast(280f)
    val laneClips = clips.filter { it.type == trackType }.sortedBy { it.startMs }

    Box(
        Modifier.width(contentWidth.dp)
            .height(58.dp)
            .background(VexPalette.Background, RoundedCornerShape(6.dp))
    ) {
        laneClips.forEach { clip ->
            val selected = clip.id == selectedId
            val clipWidth = (clip.visibleDurationMs / 1000f * dpPerSecond).coerceAtLeast(42f)
            val clipOffset = (clip.startMs / 1000f * dpPerSecond).coerceAtLeast(0f)
            val dragPx = remember(clip.id) { mutableFloatStateOf(0f) }
            val trimLeftPx = remember(clip.id) { mutableFloatStateOf(0f) }
            val trimRightPx = remember(clip.id) { mutableFloatStateOf(0f) }

            Box(
                Modifier.offset(x = clipOffset.dp)
                    .width(clipWidth.dp)
                    .fillMaxHeight()
            ) {
                Card(
                    modifier = Modifier.fillMaxSize()
                        .padding(horizontal = if (selected) 12.dp else 2.dp, vertical = 3.dp)
                        .pointerInput(clip.id, zoom, density) {
                            detectDragGestures(
                                onDragStart = {
                                    dragPx.floatValue = 0f
                                    onSelect(clip)
                                },
                                onDragEnd = {
                                    val deltaDp = dragPx.floatValue / density
                                    if (abs(deltaDp) > 2f) {
                                        val deltaMs = (deltaDp / dpPerSecond * 1000f).roundToLong()
                                        onMove(clip, (clip.startMs + deltaMs).coerceAtLeast(0L))
                                    }
                                    dragPx.floatValue = 0f
                                },
                                onDragCancel = { dragPx.floatValue = 0f },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragPx.floatValue += amount.x
                                    val deltaMs = (dragPx.floatValue / density / dpPerSecond * 1000f).roundToLong()
                                    onDragPreview(clip, (clip.startMs + deltaMs).coerceAtLeast(0L))
                                }
                            )
                        },
                    onClick = { onSelect(clip) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) VexPalette.PurpleTint else trackColorForLane(trackType).copy(alpha = 0.32f)
                    ),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(clip.name.take(20), color = VexPalette.TextPrimary, maxLines = 1)
                        Text(formatTimelineTime(clip.visibleDurationMs), color = VexPalette.TextSecondary, maxLines = 1)
                    }
                }

                if (selected) {
                    Box(
                        Modifier.align(Alignment.CenterStart)
                            .width(14.dp).fillMaxHeight()
                            .background(VexPalette.Purple, RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp))
                            .pointerInput(clip.id, zoom, density) {
                                detectDragGestures(
                                    onDragStart = { trimLeftPx.floatValue = 0f; onSelect(clip) },
                                    onDragEnd = {
                                        val deltaMs = (trimLeftPx.floatValue / density / dpPerSecond * 1000f).roundToLong()
                                        if (deltaMs != 0L) onTrimStart(clip, (clip.sourceInMs + deltaMs).coerceIn(0L, clip.sourceOutMs - 1L))
                                        trimLeftPx.floatValue = 0f
                                    },
                                    onDragCancel = { trimLeftPx.floatValue = 0f },
                                    onDrag = { change, amount -> change.consume(); trimLeftPx.floatValue += amount.x }
                                )
                            }
                    )
                    Box(
                        Modifier.align(Alignment.CenterEnd)
                            .width(14.dp).fillMaxHeight()
                            .background(VexPalette.Purple, RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))
                            .pointerInput(clip.id, zoom, density) {
                                detectDragGestures(
                                    onDragStart = { trimRightPx.floatValue = 0f; onSelect(clip) },
                                    onDragEnd = {
                                        val deltaMs = (trimRightPx.floatValue / density / dpPerSecond * 1000f).roundToLong()
                                        if (deltaMs != 0L) onTrimEnd(clip, (clip.sourceOutMs + deltaMs).coerceAtLeast(clip.sourceInMs + 1L))
                                        trimRightPx.floatValue = 0f
                                    },
                                    onDragCancel = { trimRightPx.floatValue = 0f },
                                    onDrag = { change, amount -> change.consume(); trimRightPx.floatValue += amount.x }
                                )
                            }
                    )
                }
            }
        }
        val playheadX = (playheadMs / 1000f * dpPerSecond).coerceAtLeast(0f)
        Box(
            Modifier.offset(x = playheadX.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.95f))
        )
    }
}

private fun trackColorForLane(type: EditorTrackType): Color = when (type) {
    EditorTrackType.VIDEO, EditorTrackType.PHOTO -> VexPalette.Purple
    EditorTrackType.MUSIC -> VexPalette.Music
    EditorTrackType.VOICE -> VexPalette.Voice
    EditorTrackType.SFX -> VexPalette.Sfx
    EditorTrackType.TEXT -> VexPalette.TextSecondary
}

private fun formatTimelineTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
