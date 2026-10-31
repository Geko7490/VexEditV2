package com.vexedit.studio.ui.editor

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.ExportResult
import java.io.File

/**
 * Baseline native MP4 export. This exports the selected source clip with its source audio.
 * Timeline composition/multi-track audio mixing is intentionally not represented as complete yet.
 */
class ExportController(
    private val context: Context,
    private val onProgress: (Boolean, String) -> Unit
) {
    private var transformer: Transformer? = null

    fun exportSingleSource(uri: Uri, displayName: String = "vex-export", startMs: Long = 0L, endMs: Long = 0L): File {
        val output = File(context.getExternalFilesDir(null), "${displayName.replace(Regex("[^A-Za-z0-9_-]"), "_")}-${System.currentTimeMillis()}.mp4")
        output.parentFile?.mkdirs()
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                onProgress(false, "Export tamamlandı: ${output.absolutePath}")
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: androidx.media3.transformer.ExportException) {
                onProgress(false, "Export hatası: ${exportException.message ?: "Bilinmeyen hata"}")
            }
        }
        transformer = Transformer.Builder(context)
            .addListener(listener)
            .build()
        onProgress(true, "MP4 dışa aktarılıyor…")
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs.coerceAtLeast(0L))
            .apply { if (endMs > startMs) setEndPositionMs(endMs) }
            .build()
        val source = MediaItem.Builder().setUri(uri).setClippingConfiguration(clipping).build()
        transformer?.start(source, output.absolutePath)
        return output
    }

    fun cancel() {
        transformer?.cancel()
        transformer = null
        onProgress(false, "Export iptal edildi.")
    }
}
