package com.vexedit.studio.ui.editor

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** Stores editable timeline metadata locally per project. Media itself remains in its original URI. */
class TimelineStore(context: Context, private val projectId: String) {
    private val prefs = context.getSharedPreferences("vex_timeline_projects", Context.MODE_PRIVATE)
    private val key = "timeline_$projectId"

    fun load(): List<EditorClip> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]") ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val type = runCatching { EditorTrackType.valueOf(item.optString("type")) }.getOrDefault(EditorTrackType.VIDEO)
                add(
                    EditorClip(
                        id = item.optString("id"),
                        name = item.optString("name", "Clip"),
                        uri = item.optString("uri").takeIf { it.isNotBlank() }?.let(Uri::parse),
                        type = type,
                        trackId = item.optString("trackId", type.name),
                        durationMs = item.optLong("durationMs"),
                        startMs = item.optLong("startMs"),
                        sourceInMs = item.optLong("sourceInMs"),
                        sourceOutMs = item.optLong("sourceOutMs"),
                        volume = item.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun save(clips: List<EditorClip>) {
        val array = JSONArray()
        clips.forEach { clip ->
            array.put(
                JSONObject()
                    .put("id", clip.id)
                    .put("name", clip.name)
                    .put("uri", clip.uri?.toString().orEmpty())
                    .put("type", clip.type.name)
                    .put("trackId", clip.trackId)
                    .put("durationMs", clip.durationMs)
                    .put("startMs", clip.startMs)
                    .put("sourceInMs", clip.sourceInMs)
                    .put("sourceOutMs", clip.sourceOutMs)
                    .put("volume", clip.volume.toDouble())
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
