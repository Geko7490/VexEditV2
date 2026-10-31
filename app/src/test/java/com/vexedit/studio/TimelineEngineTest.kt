package com.vexedit.studio

import com.vexedit.studio.ui.editor.EditorClip
import com.vexedit.studio.ui.editor.EditorTrackType
import com.vexedit.studio.ui.editor.TimelineEngine
import com.vexedit.studio.ui.editor.TimelineGapMode
import org.junit.Assert.*
import org.junit.Test

class TimelineEngineTest {
    private fun clip(
        id: String,
        start: Long = 0L,
        sourceIn: Long = 0L,
        sourceOut: Long = 10_000L,
        type: EditorTrackType = EditorTrackType.VIDEO
    ) = EditorClip(
        id = id, name = id, uri = null, type = type,
        durationMs = 10_000L, startMs = start,
        sourceInMs = sourceIn, sourceOutMs = sourceOut
    )

    @Test fun trimStartMovesTimelineStartByRemovedSourceDuration() {
        val result = TimelineEngine.trimStart(clip("a", start = 2_000L), 3_000L)
        assertEquals(5_000L, result.startMs)
        assertEquals(3_000L, result.sourceInMs)
        assertEquals(7_000L, result.visibleDurationMs)
    }

    @Test fun trimEndKeepsTimelineStart() {
        val result = TimelineEngine.trimEnd(clip("a", start = 4_000L), 6_000L)
        assertEquals(4_000L, result.startMs)
        assertEquals(6_000L, result.sourceOutMs)
    }

    @Test fun splitCreatesAdjacentSourceRanges() {
        val pair = TimelineEngine.split(clip("a", start = 2_000L), 6_000L)!!
        assertEquals(4_000L, pair.first.visibleDurationMs)
        assertEquals(6_000L, pair.second.startMs)
        assertEquals(pair.first.sourceOutMs, pair.second.sourceInMs)
    }

    @Test fun keepGapDoesNotMoveFollowingClip() {
        val items = listOf(clip("a", 0L), clip("b", 10_000L))
        val result = TimelineEngine.delete(items, "a", TimelineGapMode.KEEP_GAP)
        assertEquals(10_000L, result.single().startMs)
    }

    @Test fun rippleDeleteClosesGapOnSameTrackOnly() {
        val items = listOf(
            clip("a", 0L),
            clip("b", 10_000L),
            clip("music", 10_000L, type = EditorTrackType.MUSIC)
        )
        val result = TimelineEngine.delete(items, "a", TimelineGapMode.RIPPLE)
        assertEquals(0L, result.first { it.id == "b" }.startMs)
        assertEquals(10_000L, result.first { it.id == "music" }.startMs)
    }

    @Test fun snapUsesNearestCandidateWithinThreshold() {
        val result = TimelineEngine.snap(4_950L, listOf(0L, 5_000L, 10_000L), 100L)
        assertTrue(result.snapped)
        assertEquals(5_000L, result.positionMs)
    }
}
