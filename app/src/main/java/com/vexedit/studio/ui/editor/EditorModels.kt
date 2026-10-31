package com.vexedit.studio.ui.editor

import android.net.Uri
import java.util.UUID

enum class EditorTrackType { VIDEO, PHOTO, MUSIC, VOICE, SFX, TEXT }

data class EditorClip(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uri: Uri?,
    val type: EditorTrackType,
    val durationMs: Long,
    val startMs: Long = 0L,
    val sourceInMs: Long = 0L,
    val sourceOutMs: Long = durationMs,
    val volume: Float = 1f,
    val trackId: String = type.name
) {
    val visibleDurationMs: Long get() = (sourceOutMs - sourceInMs).coerceAtLeast(0L)
    val endMs: Long get() = startMs + visibleDurationMs
}
