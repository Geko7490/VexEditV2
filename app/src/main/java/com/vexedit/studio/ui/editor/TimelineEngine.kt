package com.vexedit.studio.ui.editor

import java.util.UUID
import kotlin.math.abs

enum class TimelineGapMode { KEEP_GAP, RIPPLE }
enum class TimelineTrackRole { VIDEO, PHOTO, MUSIC, VOICE, SFX, TEXT, OVERLAY }

data class TimelineTrack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: TimelineTrackRole,
    val order: Int,
    val heightDp: Int = 54,
    val isLocked: Boolean = false,
    val isVisible: Boolean = true,
    val isMuted: Boolean = false
) {
    init {
        require(heightDp in 32..180) { "Track height must be between 32dp and 180dp." }
    }
}

data class TimelineSnap(
    val positionMs: Long,
    val snapped: Boolean,
    val targetPositionMs: Long? = null
)

/**
 * Pure timeline operations. All positions are project-time milliseconds;
 * sourceIn/sourceOut remain media-time milliseconds.
 */
object TimelineEngine {
    const val MIN_CLIP_DURATION_MS = 1L

    fun ordered(clips: List<EditorClip>, trackId: String? = null): List<EditorClip> =
        clips.filter { trackId == null || it.id == trackId }
            .sortedWith(compareBy<EditorClip> { it.startMs }.thenBy { it.id })

    fun trimStart(clip: EditorClip, newSourceInMs: Long): EditorClip {
        val sourceIn = newSourceInMs.coerceIn(0L, clip.sourceOutMs - MIN_CLIP_DURATION_MS)
        val removed = sourceIn - clip.sourceInMs
        return clip.copy(
            sourceInMs = sourceIn,
            startMs = (clip.startMs + removed).coerceAtLeast(0L)
        )
    }

    fun trimEnd(clip: EditorClip, newSourceOutMs: Long): EditorClip {
        val sourceOut = newSourceOutMs.coerceIn(clip.sourceInMs + MIN_CLIP_DURATION_MS, clip.durationMs)
        return clip.copy(sourceOutMs = sourceOut)
    }

    fun split(clip: EditorClip, projectPositionMs: Long): Pair<EditorClip, EditorClip>? {
        if (projectPositionMs <= clip.startMs || projectPositionMs >= clip.endMs) return null
        val sourceSplit = clip.sourceInMs + (projectPositionMs - clip.startMs)
        if (sourceSplit <= clip.sourceInMs || sourceSplit >= clip.sourceOutMs) return null
        val left = clip.copy(sourceOutMs = sourceSplit)
        val right = clip.copy(
            id = UUID.randomUUID().toString(),
            name = "${clip.name} (2)",
            startMs = projectPositionMs,
            sourceInMs = sourceSplit
        )
        return left to right
    }

    fun move(clip: EditorClip, newStartMs: Long): EditorClip =
        clip.copy(startMs = newStartMs.coerceAtLeast(0L))

    fun delete(
        clips: List<EditorClip>,
        clipId: String,
        gapMode: TimelineGapMode
    ): List<EditorClip> {
        val removed = clips.firstOrNull { it.id == clipId } ?: return clips
        val duration = removed.visibleDurationMs
        return clips.filterNot { it.id == clipId }.map { clip ->
            if (gapMode == TimelineGapMode.RIPPLE &&
                clip.type == removed.type &&
                clip.startMs >= removed.endMs
            ) clip.copy(startMs = (clip.startMs - duration).coerceAtLeast(0L))
            else clip
        }
    }

    fun insertGap(
        clips: List<EditorClip>,
        trackType: EditorTrackType,
        atMs: Long,
        gapDurationMs: Long
    ): List<EditorClip> {
        if (gapDurationMs <= 0L) return clips
        return clips.map { clip ->
            if (clip.type == trackType && clip.startMs >= atMs) {
                clip.copy(startMs = clip.startMs + gapDurationMs)
            } else clip
        }
    }

    fun snap(
        proposedMs: Long,
        candidatesMs: Collection<Long>,
        thresholdMs: Long
    ): TimelineSnap {
        val candidate = candidatesMs.minByOrNull { abs(it - proposedMs) }
        return if (candidate != null && abs(candidate - proposedMs) <= thresholdMs) {
            TimelineSnap(candidate, true, candidate)
        } else {
            TimelineSnap(proposedMs.coerceAtLeast(0L), false)
        }
    }

    fun snapCandidates(clips: List<EditorClip>, playheadMs: Long): Set<Long> = buildSet {
        add(0L)
        add(playheadMs.coerceAtLeast(0L))
        clips.forEach { clip ->
            add(clip.startMs)
            add(clip.endMs)
        }
    }
}
