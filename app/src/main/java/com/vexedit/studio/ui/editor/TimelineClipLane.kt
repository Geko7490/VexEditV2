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
import androidx.compose.ui.unit.dp
import com.vexedit.studio.ui.theme.VexPalette
import kotlin.math.roundToLong

@Composable
fun TimelineClipLane(
    trackType: EditorTrackType,
    clips: List<EditorClip>,
    selectedId: String?,
    zoom: Float,
    timelineDurationMs: Long,
    onSelect: (EditorClip) -> Unit,
    onMove: (EditorClip, Long) -> Unit,
    onTrimStart: (EditorClip, Long) -> Unit,
    onTrimEnd: (EditorClip, Long) -> Unit
) {
    val pixelsPerSecond = 14f * zoom
    val contentWidth = (timelineDurationMs / 1000f * pixelsPerSecond).coerceAtLeast(240f)
    val laneClips = clips.filter { it.type == trackType }.sortedBy { it.startMs }

    Box(
        Modifier.width(contentWidth.dp)
            .height(52.dp)
            .background(VexPalette.Background, RoundedCornerShape(5.dp))
    ) {
        laneClips.forEach { clip ->
            val selected = clip.id == selectedId
            val clipWidth = (clip.visibleDurationMs / 1000f * pixelsPerSecond).coerceAtLeast(34f)
            val clipOffset = (clip.startMs / 1000f * pixelsPerSecond).coerceAtLeast(0f)
            val drag = remember(clip.id) { mutableFloatStateOf(0f) }
            val trimLeft = remember(clip.id) { mutableFloatStateOf(0f) }
            val trimRight = remember(clip.id) { mutableFloatStateOf(0f) }

            Box(
                Modifier.offset(x = clipOffset.dp)
                    .width(clipWidth.dp)
                    .fillMaxHeight()
            ) {
                Card(
                    modifier = Modifier.fillMaxSize()
                        .padding(horizontal = if (selected) 9.dp else 1.dp, vertical = 2.dp)
                        .pointerInput(clip.id, zoom) {
                            detectDragGestures(
                                onDragStart = { drag.floatValue = 0f },
                                onDragEnd = {
                                    if (kotlin.math.abs(drag.floatValue) > 3f) {
                                        val deltaMs = (drag.floatValue / pixelsPerSecond * 1000f).roundToLong()
                                        onMove(clip, (clip.startMs + deltaMs).coerceAtLeast(0L))
                                    }
                                    drag.floatValue = 0f
                                },
                                onDragCancel = { drag.floatValue = 0f },
                                onDrag = { change, amount ->
                                    change.consume()
                                    drag.floatValue += amount.x
                                }
                            )
                        },
                    onClick = { onSelect(clip) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) VexPalette.PurpleTint else trackColorForLane(trackType).copy(alpha = 0.30f)
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(clip.name.take(18), color = VexPalette.TextPrimary, maxLines = 1)
                        Text(formatTimelineTime(clip.visibleDurationMs), color = VexPalette.TextSecondary, maxLines = 1)
                    }
                }

                if (selected) {
                    Box(
                        Modifier.align(Alignment.CenterStart)
                            .width(10.dp).fillMaxHeight()
                            .background(VexPalette.Purple, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                            .pointerInput(clip.id, zoom) {
                                detectDragGestures(
                                    onDragStart = { trimLeft.floatValue = 0f },
                                    onDragEnd = {
                                        val deltaMs = (trimLeft.floatValue / pixelsPerSecond * 1000f).roundToLong()
                                        if (deltaMs != 0L) onTrimStart(clip, (clip.sourceInMs + deltaMs).coerceAtLeast(0L))
                                        trimLeft.floatValue = 0f
                                    },
                                    onDragCancel = { trimLeft.floatValue = 0f },
                                    onDrag = { change, amount -> change.consume(); trimLeft.floatValue += amount.x }
                                )
                            }
                    )
                    Box(
                        Modifier.align(Alignment.CenterEnd)
                            .width(10.dp).fillMaxHeight()
                            .background(VexPalette.Purple, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .pointerInput(clip.id, zoom) {
                                detectDragGestures(
                                    onDragStart = { trimRight.floatValue = 0f },
                                    onDragEnd = {
                                        val deltaMs = (trimRight.floatValue / pixelsPerSecond * 1000f).roundToLong()
                                        if (deltaMs != 0L) onTrimEnd(clip, (clip.sourceOutMs + deltaMs).coerceAtLeast(clip.sourceInMs + 1L))
                                        trimRight.floatValue = 0f
                                    },
                                    onDragCancel = { trimRight.floatValue = 0f },
                                    onDrag = { change, amount -> change.consume(); trimRight.floatValue += amount.x }
                                )
                            }
                    )
                }
            }
        }
        val playheadX = (playheadMs / 1000f * pixelsPerSecond).coerceAtLeast(0f)
        Box(
            Modifier.offset(x = playheadX.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.9f))
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
