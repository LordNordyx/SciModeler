package scimodeler.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import scimodeler.core.DocIO
import scimodeler.core.Exporter
import scimodeler.core.Renderer
import scimodeler.core.Template
import java.awt.Desktop
import java.io.File

class Actions(
    private val state: EditorState,
    private val window: ComposeWindow,
    private val scope: CoroutineScope,
    val snackbar: SnackbarHostState,
) {
    /** Action waiting for the "save changes?" dialog. */
    var pending by mutableStateOf<(() -> Unit)?>(null)
    var busy by mutableStateOf(false)

    fun guard(action: () -> Unit) {
        if (state.dirty) pending = action else action()
    }

    fun newFrom(t: Template) = state.load(t.doc(), null)

    fun open() {
        val f = FileOps.chooseOpen(window) ?: return
        openFile(f)
    }

    fun openFile(f: File) {
        runCatching { FileOps.read(f) }
            .onSuccess { state.load(it, f) }
            .onFailure { toast("Could not open ${f.name}: ${it.message}") }
    }

    /** Returns true if the document was saved. */
    fun save(): Boolean {
        val f = state.file ?: return saveAs()
        return writeTo(f)
    }

    fun saveAs(): Boolean {
        val f = FileOps.chooseSave(window, "Save model", FileOps.slug(state.displayName), DocIO.EXT) ?: return false
        return writeTo(f)
    }

    private fun writeTo(f: File): Boolean = runCatching { FileOps.write(f, state.doc) }
        .onSuccess { state.file = f; state.dirty = false; toast("Saved ${f.name}") }
        .onFailure { toast("Could not save: ${it.message}") }
        .isSuccess

    fun exportPng() = export("png") { scene, f -> Exporter.png(scene, state.exportSpec, f) }

    fun exportSvg() = export("svg") { scene, f -> Exporter.svg(scene, state.exportSpec, f) }

    private fun export(ext: String, write: (scimodeler.core.Scene, File) -> Unit) {
        val base = FileOps.slug(state.doc.title.ifBlank { state.displayName })
        val f = FileOps.chooseSave(window, "Export ${ext.uppercase()}", base, ext) ?: return
        val doc = state.doc
        scope.launch {
            busy = true
            val result = withContext(Dispatchers.Default) { runCatching { write(Renderer.scene(doc), f) } }
            busy = false
            result.onSuccess {
                val r = snackbar.showSnackbar("Exported ${f.name}", actionLabel = "Show file", duration = SnackbarDuration.Short)
                if (r == SnackbarResult.ActionPerformed) reveal(f)
            }.onFailure { toast("Export failed: ${it.message}") }
        }
    }

    fun copyImage() {
        val doc = state.doc
        val spec = state.exportSpec
        scope.launch {
            busy = true
            val img = withContext(Dispatchers.Default) {
                runCatching { Renderer.scene(doc).let { Renderer.image(it, spec.scaleFor(it)) } }
            }
            busy = false
            img.onSuccess { FileOps.copyImage(it); toast("Image copied – paste it into Word or PowerPoint") }
                .onFailure { toast("Copy failed: ${it.message}") }
        }
    }

    private fun reveal(f: File) {
        runCatching {
            val d = Desktop.getDesktop()
            if (d.isSupported(Desktop.Action.BROWSE_FILE_DIR)) d.browseFileDirectory(f) else d.open(f.parentFile)
        }
    }

    fun toast(msg: String) {
        scope.launch { snackbar.showSnackbar(msg, duration = SnackbarDuration.Short) }
    }
}
