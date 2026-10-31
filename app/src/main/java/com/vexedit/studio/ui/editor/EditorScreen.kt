package com.vexedit.studio.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(project: ProjectEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    val clips = remember { mutableStateListOf<EditorClip>() }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var activeVideo by remember { mutableStateOf<Uri?>(null) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    var previewQuality by remember { mutableStateOf("Auto") }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var exportRunning by remember { mutableStateOf(false) }
    var audioVolume by remember { mutableFloatStateOf(1f) }
    var trimStart by remember { mutableLongStateOf(0L) }
    var trimEnd by remember { mutableLongStateOf(0L) }

    val selected = clips.firstOrNull { it.id == selectedId }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            activeVideo = uri
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = false
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Video"
            val clip = EditorClip(name = name, uri = uri, type = EditorTrackType.VIDEO, durationMs = 0L, sourceOutMs = 0L)
            clips.add(clip)
            selectedId = clip.id
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val clip = EditorClip(
                name = uri.lastPathSegment?.substringAfterLast('/') ?: "Audio",
                uri = uri,
                type = EditorTrackType.MUSIC,
                durationMs = 180_000L,
                sourceOutMs = 180_000L
            )
            clips.add(clip)
            selectedId = clip.id
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                    clips.indices.firstOrNull { clips[it].uri == activeVideo }?.let { index ->
                        val old = clips[index]
                        if (old.durationMs == 0L && duration > 0L) clips[index] = old.copy(durationMs = duration, sourceOutMs = duration)
                    }
                    if (trimEnd == 0L && duration > 0L) trimEnd = duration
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, activeVideo) {
        while (true) {
            if (player.isPlaying) position = player.currentPosition.coerceAtLeast(0L)
            kotlinx.coroutines.delay(120)
        }
    }

    val exportController = remember {
        ExportController(context) { running, message ->
            exportRunning = running
            exportMessage = message
        }
    }

    Column(Modifier.fillMaxSize().background(VexPalette.Background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri", tint = VexPalette.TextPrimary) }
            Column(Modifier.weight(1f)) {
                Text(project.name, color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text("${project.aspectRatio} • ${project.resolution} • ${project.fps} FPS", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = {
                val exportClip = selected?.takeIf { it.type == EditorTrackType.VIDEO && it.uri != null }
                    ?: clips.firstOrNull { it.type == EditorTrackType.VIDEO && it.uri != null }
                exportClip?.uri?.let { exportController.exportSingleSource(it, project.name, exportClip.sourceInMs, exportClip.sourceOutMs) }
            }, enabled = clips.any { it.type == EditorTrackType.VIDEO && it.uri != null } && !exportRunning) {
                Icon(Icons.Rounded.FileDownload, contentDescription = "MP4 dışa aktar", tint = VexPalette.Purple)
            }
        }

        Box(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)
                .background(Color.Black, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (activeVideo != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                    update = { it.player = player }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = VexPalette.Purple)
                    Text("Önizleme için video ekle", color = Color.White)
                    Button(onClick = { videoPicker.launch(arrayOf("video/*")) }) { Text("Video seç") }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }, enabled = activeVideo != null) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Oynat", tint = VexPalette.Purple)
                }
                Text("${timeLabel(position)} / ${timeLabel(duration)}", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Auto", "Low", "Mid", "High").forEach { q ->
                    FilterChip(selected = previewQuality == q, onClick = { previewQuality = q }, label = { Text(q) })
                }
            }
        }

        if (duration > 0L) {
            Slider(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                value = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                onValueChange = { player.seekTo((it * duration).toLong()) },
                valueRange = 0f..1f
            )
        }

        Column(
            Modifier.fillMaxWidth().weight(1.15f).background(VexPalette.Panel).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TIMELINE", color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { videoPicker.launch(arrayOf("video/*")) }) { Icon(Icons.Rounded.VideoLibrary, null); Text("Video") }
                    OutlinedButton(onClick = { audioPicker.launch(arrayOf("audio/*")) }) { Icon(Icons.Rounded.MusicNote, null); Text("Ses") }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(EditorTrackType.VIDEO, EditorTrackType.PHOTO, EditorTrackType.MUSIC, EditorTrackType.VOICE, EditorTrackType.SFX).forEach { type ->
                    val trackClips = clips.filter { it.type == type }
                    Column(Modifier.width(250.dp).background(VexPalette.Background, RoundedCornerShape(6.dp)).padding(8.dp)) {
                        Text(type.name, color = trackColor(type), style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(6.dp))
                        if (trackClips.isEmpty()) {
                            Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.CenterStart) {
                                Text("Klip eklemek için üstteki araçları kullan", color = VexPalette.TextDisabled, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                trackClips.forEach { clip ->
                                    Card(
                                        onClick = { selectedId = clip.id },
                                        colors = CardDefaults.cardColors(containerColor = if (selectedId == clip.id) VexPalette.PurpleTint else VexPalette.PanelElevated),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(clip.name.take(18), Modifier.padding(8.dp), color = VexPalette.TextPrimary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        val clip = selected
                        if (clip != null && clip.type == EditorTrackType.VIDEO && position > clip.sourceInMs && position < clip.sourceOutMs) {
                            val index = clips.indexOfFirst { it.id == clip.id }
                            if (index >= 0) {
                                clips[index] = clip.copy(sourceOutMs = position)
                                clips.add(index + 1, clip.copy(id = java.util.UUID.randomUUID().toString(), name = clip.name + " (2)", startMs = position, sourceInMs = position))
                            }
                        }
                    },
                    enabled = selected?.type == EditorTrackType.VIDEO && position > selected.sourceInMs && position < selected.sourceOutMs
                ) { Icon(Icons.Rounded.ContentCut, null); Text("Split") }
                OutlinedButton(onClick = {
                    selected?.let { clip ->
                        if (clip.type == EditorTrackType.VIDEO) {
                            val index = clips.indexOfFirst { it.id == clip.id }
                            if (index >= 0) clips[index] = clip.copy(sourceInMs = position.coerceIn(0L, clip.sourceOutMs))
                        }
                    }
                }, enabled = selected?.type == EditorTrackType.VIDEO) { Text("In") }
                OutlinedButton(onClick = {
                    selected?.let { clip ->
                        if (clip.type == EditorTrackType.VIDEO) {
                            val index = clips.indexOfFirst { it.id == clip.id }
                            if (index >= 0) clips[index] = clip.copy(sourceOutMs = position.coerceIn(clip.sourceInMs, clip.durationMs))
                        }
                    }
                }, enabled = selected?.type == EditorTrackType.VIDEO) { Text("Out") }
                OutlinedButton(onClick = {
                    selected?.let { clip -> clips.removeAll { it.id == clip.id }; if (selectedId == clip.id) selectedId = null }
                }, enabled = selected != null) { Text("Sil") }
            }
            if (selected != null) {
                Text("Seçili: ${selected.name} • ${timeLabel(selected.visibleDurationMs)}", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                if (selected.type == EditorTrackType.MUSIC || selected.type == EditorTrackType.VOICE || selected.type == EditorTrackType.SFX) {
                    Text("Ses seviyesi: ${(audioVolume * 100).toInt()}%", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Slider(value = selected.volume, onValueChange = { value ->
                        audioVolume = value
                        val index = clips.indexOfFirst { it.id == selected.id }
                        if (index >= 0) clips[index] = clips[index].copy(volume = value)
                    }, valueRange = 0f..1f)
                }
            }
            Text("Önizleme kalitesi seçimi arayüzü hazır; cihaz/codec tabanlı gerçek kalite profilleri sonraki optimizasyon adımında uygulanacak.", color = VexPalette.TextDisabled, style = MaterialTheme.typography.labelSmall)
        }
    }

    exportMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { if (!exportRunning) exportMessage = null },
            title = { Text(if (exportRunning) "Dışa aktarılıyor" else "MP4 Export") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { if (exportRunning) exportController.cancel() else exportMessage = null }) { Text(if (exportRunning) "İptal" else "Tamam") } }
        )
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
