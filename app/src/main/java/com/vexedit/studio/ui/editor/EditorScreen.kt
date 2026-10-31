package com.vexedit.studio.ui.editor

import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.vexedit.studio.data.project.ProjectEntity
import com.vexedit.studio.ui.theme.VexPalette
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(project: ProjectEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(project.id) { TimelineStore(context, project.id) }
    val clips = remember(project.id) { mutableStateListOf<EditorClip>().apply { addAll(store.load()) } }
    val player = remember { ExoPlayer.Builder(context).build() }
    var selectedId by remember { mutableStateOf(clips.firstOrNull()?.id) }
    var activeVideo by remember { mutableStateOf<Uri?>(null) }
    var activePhoto by remember { mutableStateOf<Uri?>(null) }
    var duration by remember { mutableLongStateOf(0L) }
    var playerPosition by remember { mutableLongStateOf(0L) }
    var playheadMs by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(0.65f) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var exportRunning by remember { mutableStateOf(false) }
    var previewQuality by remember { mutableStateOf("Auto") }
    val undoStack = remember { mutableStateListOf<List<EditorClip>>() }
    val redoStack = remember { mutableStateListOf<List<EditorClip>>() }
    val selected = clips.firstOrNull { it.id == selectedId }
    val timelineDuration = (clips.maxOfOrNull { it.endMs } ?: 0L).coerceAtLeast(1000L)

    fun snapshot() {
        undoStack.add(clips.toList())
        if (undoStack.size > 40) undoStack.removeAt(0)
        redoStack.clear()
    }
    fun replaceAll(items: List<EditorClip>) {
        clips.clear()
        clips.addAll(items)
        store.save(items)
        if (selectedId !in clips.map { it.id }) selectedId = clips.firstOrNull()?.id
    }
    fun updateClip(id: String, transform: (EditorClip) -> EditorClip) {
        val index = clips.indexOfFirst { it.id == id }
        if (index >= 0) {
            snapshot()
            clips[index] = transform(clips[index])
            store.save(clips.toList())
        }
    }
    fun moveClip(id: String, direction: Int) {
        val index = clips.indexOfFirst { it.id == id }
        if (index < 0) return
        val sameTrack = clips.withIndex().filter { it.value.type == clips[index].type }
        val localIndex = sameTrack.indexOfFirst { it.value.id == id }
        val target = localIndex + direction
        if (target !in sameTrack.indices) return
        snapshot()
        val otherIndex = sameTrack[target].index
        val a = clips[index]
        val b = clips[otherIndex]
        clips[index] = a.copy(startMs = b.startMs)
        clips[otherIndex] = b.copy(startMs = a.startMs)
        store.save(clips.toList())
    }
    fun addMedia(uri: Uri, type: EditorTrackType, name: String) {
        runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        snapshot()
        val clipDuration = if (type == EditorTrackType.PHOTO) 5000L else if (type == EditorTrackType.MUSIC || type == EditorTrackType.VOICE || type == EditorTrackType.SFX) 180000L else 0L
        val trackEnd = clips.filter { it.type == type }.maxOfOrNull { it.endMs } ?: 0L
        val clip = EditorClip(name = name, uri = uri, type = type, durationMs = clipDuration, startMs = trackEnd, sourceOutMs = clipDuration)
        clips.add(clip)
        selectedId = clip.id
        store.save(clips.toList())
        when (type) {
            EditorTrackType.VIDEO -> {
                activeVideo = uri
                activePhoto = null
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.pause()
            }
            EditorTrackType.PHOTO -> {
                activePhoto = uri
                activeVideo = null
                player.pause()
            }
            else -> Unit
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> addMedia(uri, EditorTrackType.VIDEO, uri.lastPathSegment?.substringAfterLast('/') ?: "Video") }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> addMedia(uri, EditorTrackType.PHOTO, uri.lastPathSegment?.substringAfterLast('/') ?: "Photo") }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> addMedia(uri, EditorTrackType.MUSIC, uri.lastPathSegment?.substringAfterLast('/') ?: "Audio") }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                    activeVideo?.let { uri ->
                        val index = clips.indexOfFirst { it.uri == uri && it.type == EditorTrackType.VIDEO }
                        if (index >= 0 && clips[index].durationMs == 0L && duration > 0L) {
                            val updated = clips[index].copy(durationMs = duration, sourceOutMs = duration)
                            clips[index] = updated
                            store.save(clips.toList())
                        }
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(player) {
        while (true) {
            if (player.isPlaying) {
                playerPosition = player.currentPosition.coerceAtLeast(0L)
                val clip = clips.firstOrNull { it.uri == activeVideo && it.type == EditorTrackType.VIDEO }
                playheadMs = (clip?.startMs ?: 0L) + (playerPosition - (clip?.sourceInMs ?: 0L)).coerceAtLeast(0L)
            }
            delay(100)
        }
    }
    LaunchedEffect(clips.toList()) {
        delay(250)
        store.save(clips.toList())
    }

    val exportController = remember {
        ExportController(context) { running, message -> exportRunning = running; exportMessage = message }
    }

    Column(Modifier.fillMaxSize().background(VexPalette.Background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = VexPalette.TextPrimary) }
            Column(Modifier.weight(1f)) {
                Text(project.name, color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text("${project.aspectRatio} • ${project.resolution} • ${project.fps} FPS", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = {
                val clip = selected?.takeIf { it.type == EditorTrackType.VIDEO && it.uri != null }
                    ?: clips.firstOrNull { it.type == EditorTrackType.VIDEO && it.uri != null }
                clip?.uri?.let { exportController.exportSingleSource(it, project.name, clip.sourceInMs, clip.sourceOutMs) }
            }, enabled = clips.any { it.type == EditorTrackType.VIDEO && it.uri != null } && !exportRunning) {
                Icon(Icons.Rounded.FileDownload, "Export MP4", tint = VexPalette.Purple)
            }
        }

        Box(Modifier.fillMaxWidth().weight(0.85f).padding(horizontal = 12.dp).background(Color.Black, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            when {
                activeVideo != null -> AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } }, modifier = Modifier.fillMaxSize(), update = { view -> view.player = player })
                activePhoto != null -> AndroidView(factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setImageURI(activePhoto) } }, modifier = Modifier.fillMaxSize(), update = { view -> view.setImageURI(activePhoto) })
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.VideoLibrary, null, tint = VexPalette.Purple)
                    Text("Videolarını veya fotoğraflarını ekle", color = Color.White)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }, enabled = activeVideo != null) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause", tint = VexPalette.Purple)
                }
                Text("${timeLabel(playheadMs)} / ${timeLabel(timelineDuration)}", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Row {
                listOf("Auto", "Low", "Mid", "High").forEach { q ->
                    FilterChip(selected = previewQuality == q, onClick = { previewQuality = q }, label = { Text(q, maxLines = 1) })
                }
            }
        }

        Slider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            value = playheadMs.toFloat().coerceIn(0f, timelineDuration.toFloat()),
            onValueChange = { value ->
                playheadMs = value.toLong()
                val clip = clips.filter { it.type == EditorTrackType.VIDEO && it.uri != null }
                    .firstOrNull { playheadMs in it.startMs..it.endMs }
                if (clip != null) {
                    if (activeVideo != clip.uri) {
                        activeVideo = clip.uri
                        activePhoto = null
                        player.setMediaItem(MediaItem.fromUri(clip.uri!!))
                        player.prepare()
                    }
                    player.seekTo((clip.sourceInMs + playheadMs - clip.startMs).coerceAtLeast(0L))
                } else if (selected?.type == EditorTrackType.VIDEO && selected.uri != null) {
                    player.seekTo((selected.sourceInMs + playheadMs - selected.startMs).coerceIn(selected.sourceInMs, selected.sourceOutMs))
                }
            },
            valueRange = 0f..timelineDuration.toFloat()
        )

        Column(Modifier.fillMaxWidth().weight(1.15f).background(VexPalette.Panel).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("TIMELINE", color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("Zoom", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 0.25f..1.5f, modifier = Modifier.width(100.dp))
                IconButton(onClick = { undoStack.lastOrNull()?.let { previous -> redoStack.add(clips.toList()); undoStack.removeAt(undoStack.lastIndex); replaceAll(previous) } }, enabled = undoStack.isNotEmpty()) {
                    Icon(Icons.Rounded.Undo, "Undo", tint = if (undoStack.isNotEmpty()) VexPalette.TextPrimary else VexPalette.TextDisabled)
                }
                IconButton(onClick = { redoStack.lastOrNull()?.let { next -> undoStack.add(clips.toList()); redoStack.removeAt(redoStack.lastIndex); replaceAll(next) } }, enabled = redoStack.isNotEmpty()) {
                    Icon(Icons.Rounded.Redo, "Redo", tint = if (redoStack.isNotEmpty()) VexPalette.TextPrimary else VexPalette.TextDisabled)
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { videoPicker.launch(arrayOf("video/*")) }) { Icon(Icons.Rounded.VideoLibrary, null); Text("Video +") }
                OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*")) }) { Icon(Icons.Rounded.Image, null); Text("Foto +") }
                OutlinedButton(onClick = { audioPicker.launch(arrayOf("audio/*")) }) { Icon(Icons.Rounded.MusicNote, null); Text("Ses +") }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("00:00", color = VexPalette.TextDisabled, style = MaterialTheme.typography.labelSmall)
                repeat(8) { i -> Text(timeLabel((i + 1) * 5000L), color = VexPalette.TextDisabled, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width((70 * zoom).dp)) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                EditorTrackType.entries.filter { type -> type != EditorTrackType.TEXT }.forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(type.name, color = trackColor(type), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(62.dp))
                        Row(Modifier.width((timelineDuration / 1000f * 14f * zoom).coerceAtLeast(240f).dp).height(48.dp).background(VexPalette.Background, RoundedCornerShape(5.dp)).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            clips.filter { it.type == type }.sortedBy { it.startMs }.forEach { clip ->
                                val width = (clip.visibleDurationMs / 1000f * 14f * zoom).coerceIn(52f, 320f)
                                var dragDistance by remember(clip.id) { mutableFloatStateOf(0f) }
                                Card(
                                    modifier = Modifier.width(width.dp).fillMaxHeight().pointerInput(clip.id) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                if (dragDistance > 35f) moveClip(clip.id, 1)
                                                if (dragDistance < -35f) moveClip(clip.id, -1)
                                                dragDistance = 0f
                                            },
                                            onDragCancel = { dragDistance = 0f },
                                            onDrag = { change, amount -> change.consume(); dragDistance += amount.x }
                                        )
                                    },
                                    onClick = { selectedId = clip.id; playheadMs = clip.startMs },
                                    colors = CardDefaults.cardColors(containerColor = if (selectedId == clip.id) VexPalette.PurpleTint else trackColor(type).copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Column(Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 3.dp), verticalArrangement = Arrangement.Center) {
                                        Text(clip.name.take(14), color = VexPalette.TextPrimary, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                        Text(timeLabel(clip.visibleDurationMs), color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (selected != null) {
                Text("Seçili: ${selected.name} • ${timeLabel(selected.visibleDurationMs)}", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedButton(onClick = {
                        val splitAt = (playheadMs - selected.startMs + selected.sourceInMs).coerceIn(selected.sourceInMs, selected.sourceOutMs)
                        if (splitAt > selected.sourceInMs && splitAt < selected.sourceOutMs) {
                            snapshot()
                            val index = clips.indexOfFirst { it.id == selected.id }
                            clips[index] = selected.copy(sourceOutMs = splitAt)
                            val second = selected.copy(id = UUID.randomUUID().toString(), name = selected.name + " (2)", startMs = playheadMs, sourceInMs = splitAt)
                            clips.add(index + 1, second)
                            selectedId = second.id
                            store.save(clips.toList())
                        }
                    }, enabled = selected.type == EditorTrackType.VIDEO && playheadMs > selected.startMs && playheadMs < selected.endMs) { Icon(Icons.Rounded.ContentCut, null); Text("Böl") }
                    OutlinedButton(onClick = { updateClip(selected.id) { it.copy(sourceInMs = (it.sourceInMs + (playheadMs - it.startMs).coerceAtLeast(0L)).coerceAtMost(it.sourceOutMs - 250L)) } }, enabled = selected.type == EditorTrackType.VIDEO && playheadMs > selected.startMs) { Text("Baştan kırp") }
                    OutlinedButton(onClick = { updateClip(selected.id) { it.copy(sourceOutMs = (it.sourceInMs + (playheadMs - it.startMs)).coerceIn(it.sourceInMs + 250L, it.durationMs)) } }, enabled = selected.type == EditorTrackType.VIDEO && playheadMs < selected.endMs) { Text("Sondan kırp") }
                    OutlinedButton(onClick = { updateClip(selected.id) { it.copy(startMs = (it.startMs - 500L).coerceAtLeast(0L)) } }) { Text("← 0.5s") }
                    OutlinedButton(onClick = { updateClip(selected.id) { it.copy(startMs = it.startMs + 500L) } }) { Text("0.5s →") }
                    OutlinedButton(onClick = {
                        snapshot()
                        val copy = selected.copy(id = UUID.randomUUID().toString(), name = selected.name + " Copy", startMs = selected.endMs)
                        clips.add(copy); selectedId = copy.id; store.save(clips.toList())
                    }) { Icon(Icons.Rounded.ContentCopy, null); Text("Çoğalt") }
                    OutlinedButton(onClick = { snapshot(); clips.removeAll { it.id == selected.id }; selectedId = clips.firstOrNull()?.id; store.save(clips.toList()) }) { Icon(Icons.Rounded.Delete, null); Text("Sil") }
                }
                if (selected.type == EditorTrackType.MUSIC || selected.type == EditorTrackType.VOICE || selected.type == EditorTrackType.SFX) {
                    Text("Ses seviyesi ${(selected.volume * 100).roundToInt()}%", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Slider(value = selected.volume, onValueChange = { value ->
                        val index = clips.indexOfFirst { it.id == selected.id }
                        if (index >= 0) { clips[index] = clips[index].copy(volume = value); store.save(clips.toList()) }
                    }, valueRange = 0f..1f)
                }
            } else {
                Text("Klip seç: sürükleyerek aynı kanalda sırala. Kırpma ve bölme araçları seçili klibe uygulanır.", color = VexPalette.TextDisabled, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    exportMessage?.let { message ->
        AlertDialog(onDismissRequest = { if (!exportRunning) exportMessage = null }, title = { Text(if (exportRunning) "Dışa aktarılıyor" else "MP4 Export") }, text = { Text(message) }, confirmButton = {
            TextButton(onClick = { if (exportRunning) exportController.cancel() else exportMessage = null }) { Text(if (exportRunning) "İptal" else "Tamam") }
        })
    }
}

private fun timeLabel(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}

private fun trackColor(type: EditorTrackType): Color = when (type) {
    EditorTrackType.VIDEO, EditorTrackType.PHOTO -> VexPalette.Purple
    EditorTrackType.MUSIC -> VexPalette.Music
    EditorTrackType.VOICE -> VexPalette.Voice
    EditorTrackType.SFX -> VexPalette.Sfx
    EditorTrackType.TEXT -> VexPalette.TextSecondary
}
