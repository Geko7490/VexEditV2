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
    var gapMode by remember { mutableStateOf(TimelineGapMode.KEEP_GAP) }
    val timelineScrollState = rememberScrollState()
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var exportRunning by remember { mutableStateOf(false) }
    var previewQuality by remember { mutableStateOf("Auto") }
    val undoStack = remember { mutableStateListOf<List<EditorClip>>() }
    val redoStack = remember { mutableStateListOf<List<EditorClip>>() }
    val selected = clips.firstOrNull { it.id == selectedId }
    val timelineDuration = (clips.maxOfOrNull { it.endMs } ?: 0L).coerceAtLeast(1000L)
    LaunchedEffect(project.id) {
        val firstVideo = clips.firstOrNull { it.type == EditorTrackType.VIDEO && it.uri != null }
        if (firstVideo != null) {
            activeVideo = firstVideo.uri
            player.setMediaItem(MediaItem.fromUri(firstVideo.uri!!))
            player.prepare()
            player.pause()
        } else {
            val firstPhoto = clips.firstOrNull { it.type == EditorTrackType.PHOTO && it.uri != null }
            activePhoto = firstPhoto?.uri
        }
    }

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
    LaunchedEffect(playing, playheadMs, zoom) {
        if (playing && timelineScrollState.viewportSize > 0) {
            val playheadPx = playheadMs / 1000f * 72f * zoom
            val target = (playheadPx - timelineScrollState.viewportSize / 2f).toInt().coerceAtLeast(0)
            timelineScrollState.animateScrollTo(target)
        }
    }

    val exportController = remember {
        ExportController(context) { running, message -> exportRunning = running; exportMessage = message }
    }

    Column(Modifier.fillMaxSize().background(VexPalette.Background)) {
        Row(
            Modifier.fillMaxWidth().height(58.dp).background(VexPalette.Panel).padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = VexPalette.TextPrimary) }
            Text(project.name, color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.weight(1f))
            IconButton(onClick = { undoStack.lastOrNull()?.let { previous -> redoStack.add(clips.toList()); undoStack.removeAt(undoStack.lastIndex); replaceAll(previous) } }, enabled = undoStack.isNotEmpty()) {
                Icon(Icons.Rounded.Undo, "Geri al", tint = VexPalette.TextPrimary)
            }
            IconButton(onClick = { redoStack.lastOrNull()?.let { next -> undoStack.add(clips.toList()); redoStack.removeAt(redoStack.lastIndex); replaceAll(next) } }, enabled = redoStack.isNotEmpty()) {
                Icon(Icons.Rounded.Redo, "Yinele", tint = VexPalette.TextPrimary)
            }
            IconButton(onClick = { store.save(clips.toList()) }) { Icon(Icons.Rounded.Save, "Kaydet", tint = VexPalette.TextPrimary) }
            Button(
                onClick = {
                    val clip = selected?.takeIf { it.type == EditorTrackType.VIDEO && it.uri != null }
                        ?: clips.firstOrNull { it.type == EditorTrackType.VIDEO && it.uri != null }
                    clip?.uri?.let { exportController.exportSingleSource(it, project.name, clip.sourceInMs, clip.sourceOutMs) }
                },
                enabled = clips.any { it.type == EditorTrackType.VIDEO && it.uri != null } && !exportRunning,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VexPalette.Purple)
            ) { Text("DIŞA AKTAR", maxLines = 1) }
        }

        Box(
            Modifier.fillMaxWidth().weight(0.95f).background(Color.Black).padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxHeight().aspectRatio(9f / 16f)
                    .background(Color.Black, RoundedCornerShape(8.dp))
            ) {
                when {
                    activeVideo != null -> AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false; resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT } }, modifier = Modifier.fillMaxSize(), update = { view -> view.player = player })
                    activePhoto != null -> AndroidView(factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setImageURI(activePhoto) } }, modifier = Modifier.fillMaxSize(), update = { view -> view.setImageURI(activePhoto) })
                    else -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.VideoLibrary, null, tint = VexPalette.Purple)
                        Text("Video veya fotoğraf ekle", color = Color.White)
                        Text("9:16 önizleme", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().height(66.dp).background(VexPalette.Panel).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(timecodePrecise(playheadMs), color = VexPalette.Purple, style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = { player.seekTo((player.currentPosition - 1000L).coerceAtLeast(0L)) }, enabled = activeVideo != null) {
                Icon(Icons.Rounded.SkipPrevious, "Geri sar", tint = VexPalette.TextPrimary)
            }
            FilledIconButton(
                onClick = { if (player.isPlaying) player.pause() else player.play() },
                enabled = activeVideo != null,
                modifier = Modifier.size(58.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = VexPalette.Purple)
            ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Oynat", modifier = Modifier.size(30.dp)) }
            IconButton(onClick = { player.seekTo(player.currentPosition + 1000L) }, enabled = activeVideo != null) {
                Icon(Icons.Rounded.SkipNext, "İleri sar", tint = VexPalette.TextPrimary)
            }
            IconButton(onClick = { photoPicker.launch(arrayOf("image/*")) }) {
                Icon(Icons.Rounded.AddPhotoAlternate, "Medya ekle", tint = VexPalette.Purple)
            }
        }



        Row(
            Modifier.fillMaxWidth().height(58.dp).background(VexPalette.Background).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { videoPicker.launch(arrayOf("video/*")) }, modifier = Modifier.weight(1.2f)) { Icon(Icons.Rounded.Add, null); Text("Medya Ekle", maxLines = 1) }
            OutlinedButton(onClick = {
                selected?.let { clip ->
                    val pair = TimelineEngine.split(clip, playheadMs)
                    if (pair != null) { snapshot(); val index = clips.indexOfFirst { it.id == clip.id }; clips[index] = pair.first; clips.add(index + 1, pair.second); selectedId = pair.second.id; store.save(clips.toList()) }
                }
            }, enabled = selected != null && playheadMs > (selected?.startMs ?: 0L) && playheadMs < (selected?.endMs ?: 0L), modifier = Modifier.weight(0.8f)) { Icon(Icons.Rounded.ContentCut, null); Text("Böl") }
            OutlinedButton(onClick = {
                selected?.let { clip -> snapshot(); val copy = clip.copy(id = UUID.randomUUID().toString(), name = clip.name + " Copy", startMs = clip.endMs); clips.add(copy); selectedId = copy.id; store.save(clips.toList()) }
            }, enabled = selected != null, modifier = Modifier.weight(0.9f)) { Icon(Icons.Rounded.ContentCopy, null) }
            OutlinedButton(onClick = { selected?.let { clip -> snapshot(); replaceAll(TimelineEngine.delete(clips.toList(), clip.id, gapMode)); selectedId = clips.firstOrNull()?.id } }, enabled = selected != null, modifier = Modifier.weight(0.65f)) { Icon(Icons.Rounded.Delete, null) }
            IconButton(onClick = { undoStack.lastOrNull()?.let { previous -> redoStack.add(clips.toList()); undoStack.removeAt(undoStack.lastIndex); replaceAll(previous) } }, enabled = undoStack.isNotEmpty()) {
                Icon(Icons.Rounded.ArrowBack, "Geri al", tint = VexPalette.TextPrimary)
            }
            IconButton(onClick = { redoStack.lastOrNull()?.let { next -> undoStack.add(clips.toList()); redoStack.removeAt(redoStack.lastIndex); replaceAll(next) } }, enabled = redoStack.isNotEmpty()) {
                Icon(Icons.Rounded.ArrowForward, "Yinele", tint = VexPalette.TextPrimary)
            }
        }
        Column(Modifier.fillMaxWidth().weight(1.25f).background(VexPalette.Panel).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Timeline", color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("Zoom", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 0.25f..1.5f, modifier = Modifier.width(100.dp))
                TextButton(onClick = { gapMode = if (gapMode == TimelineGapMode.KEEP_GAP) TimelineGapMode.RIPPLE else TimelineGapMode.KEEP_GAP }) {
                    Text(if (gapMode == TimelineGapMode.KEEP_GAP) "Keep Gap" else "Ripple")
                }
                IconButton(onClick = { undoStack.lastOrNull()?.let { previous -> redoStack.add(clips.toList()); undoStack.removeAt(undoStack.lastIndex); replaceAll(previous) } }, enabled = undoStack.isNotEmpty()) {
                    Icon(Icons.Rounded.Undo, "Undo", tint = if (undoStack.isNotEmpty()) VexPalette.TextPrimary else VexPalette.TextDisabled)
                }
                IconButton(onClick = { redoStack.lastOrNull()?.let { next -> undoStack.add(clips.toList()); redoStack.removeAt(redoStack.lastIndex); replaceAll(next) } }, enabled = redoStack.isNotEmpty()) {
                    Icon(Icons.Rounded.Redo, "Redo", tint = if (redoStack.isNotEmpty()) VexPalette.TextPrimary else VexPalette.TextDisabled)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("KATMANLAR", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { gapMode = if (gapMode == TimelineGapMode.KEEP_GAP) TimelineGapMode.RIPPLE else TimelineGapMode.KEEP_GAP }) { Text(if (gapMode == TimelineGapMode.KEEP_GAP) "Boşluk: Koru" else "Boşluk: Kapat") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("TIME", color = VexPalette.TextDisabled, modifier = Modifier.width(78.dp), style = MaterialTheme.typography.labelSmall)
                Row(Modifier.weight(1f).horizontalScroll(timelineScrollState), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("00:00", color = VexPalette.TextDisabled, style = MaterialTheme.typography.labelSmall)
                    repeat(12) { i -> Text(timeLabel((i + 1) * 1000L), color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width((72 * zoom).dp)) }
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EditorTrackType.entries.filter { type -> type != EditorTrackType.TEXT }.forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(78.dp).height(58.dp).background(VexPalette.Background, RoundedCornerShape(5.dp)).padding(start = 7.dp), verticalArrangement = Arrangement.Center) {
                            Text(
                                when (type) {
                                    EditorTrackType.VIDEO -> "Video"
                                    EditorTrackType.PHOTO -> "Overlay"
                                    EditorTrackType.MUSIC -> "Audio"
                                    EditorTrackType.VOICE -> "Audio"
                                    EditorTrackType.SFX -> "Effects"
                                    EditorTrackType.TEXT -> "Text"
                                },
                                color = VexPalette.TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                            Text(
                                when (type) {
                                    EditorTrackType.VIDEO -> "Video 1"
                                    EditorTrackType.PHOTO -> "Overlay"
                                    EditorTrackType.MUSIC -> "Audio 1"
                                    EditorTrackType.VOICE -> "Audio 2"
                                    EditorTrackType.SFX -> "VFX"
                                    EditorTrackType.TEXT -> "Text"
                                },
                                color = trackColor(type),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                        Box(Modifier.weight(1f).horizontalScroll(timelineScrollState)) {
                        TimelineClipLane(
                            trackType = type,
                            clips = clips,
                            selectedId = selectedId,
                            zoom = zoom,
                            timelineDurationMs = timelineDuration,
                            playheadMs = playheadMs,
                            onSelect = { clip -> selectedId = clip.id; playheadMs = clip.startMs },
                            onMove = { clip, proposedStart ->
                                val candidates = TimelineEngine.snapCandidates(clips.filterNot { it.id == clip.id }, playheadMs)
                                val snapped = TimelineEngine.snap(proposedStart, candidates, (10000f / (72f * zoom)).toLong().coerceAtLeast(30L))
                                playheadMs = snapped.positionMs
                                updateClip(clip.id) { TimelineEngine.move(it, snapped.positionMs) }
                            },
                            onDragPreview = { clip, proposedStart ->
                                val previewPosition = proposedStart.coerceAtLeast(0L)
                                playheadMs = previewPosition
                                if (clip.type == EditorTrackType.VIDEO && clip.uri != null) {
                                    if (activeVideo != clip.uri) {
                                        activeVideo = clip.uri
                                        activePhoto = null
                                        player.setMediaItem(MediaItem.fromUri(clip.uri))
                                        player.prepare()
                                    }
                                    val sourcePreviewMs = (clip.sourceInMs + (previewPosition - clip.startMs).coerceIn(0L, clip.visibleDurationMs))
                                        .coerceIn(clip.sourceInMs, clip.sourceOutMs)
                                    player.seekTo(sourcePreviewMs)
                                }
                            },
                            onTrimStart = { clip, sourceIn ->
                                updateClip(clip.id) { TimelineEngine.trimStart(it, sourceIn) }
                            },
                            onTrimEnd = { clip, sourceOut ->
                                updateClip(clip.id) { TimelineEngine.trimEnd(it, sourceOut) }
                            }
                        )
                        }
                    }
                }
            }
            if (clips.isEmpty()) {
                Text("Başlamak için Medya Ekle'ye dokun", color = VexPalette.TextDisabled, style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(
            Modifier.fillMaxWidth().height(82.dp).background(VexPalette.Panel).padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorToolItem(Icons.Rounded.Tune, "Düzenle", true) { }
            EditorToolItem(Icons.Rounded.Speed, "Hız") { }
            EditorToolItem(Icons.Rounded.VolumeUp, "Ses") { }
            EditorToolItem(Icons.Rounded.AutoAwesome, "Efektler") { }
            EditorToolItem(Icons.Rounded.Diamond, "Keyframe") { }
            EditorToolItem(Icons.Rounded.Crop, "Kırp") { }
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


@Composable
private fun EditorToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxHeight().width(58.dp), contentPadding = PaddingValues(2.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = if (selected) VexPalette.Purple else VexPalette.TextSecondary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(3.dp))
            Text(label, color = if (selected) VexPalette.Purple else VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

private fun timecodePrecise(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val hours = safe / 3_600_000L
    val minutes = (safe / 60_000L) % 60L
    val seconds = (safe / 1_000L) % 60L
    val frames = (safe % 1_000L) * 30L / 1_000L
    return "%02d:%02d:%02d:%02d".format(hours, minutes, seconds, frames)
}
