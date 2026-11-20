package scimodeler.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import scimodeler.core.AppSettings
import scimodeler.core.DocIO
import scimodeler.core.ExportSpec
import scimodeler.core.Exporter
import scimodeler.core.IconStore
import scimodeler.core.Images
import scimodeler.core.Renderer
import scimodeler.core.Scene
import scimodeler.core.Session
import scimodeler.core.SessionStore
import scimodeler.core.SettingsStore
import scimodeler.core.Template
import java.awt.Desktop
import java.io.File

/** All user commands. UI elements call these; nothing here depends on a particular window. */
class Actions(
    private val state: EditorState,
    private val dialogs: Dialogs,
    private val scope: CoroutineScope,
    val snackbar: SnackbarHostState,
) {
    var pending by mutableStateOf<(() -> Unit)?>(null)
    var busy by mutableStateOf(false)
    var settings by mutableStateOf(SettingsStore.load())
        private set
    var recentImages by mutableStateOf(IconStore.recent())
        private set

    init {
        state.exportSpec = ExportSpec(settings.exportWidthCm, settings.exportDpi)
    }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        settings = change(settings)
        SettingsStore.save(settings)
    }

    fun setExportSpec(spec: ExportSpec) {
        state.exportSpec = spec
        updateSettings { it.copy(exportWidthCm = spec.widthCm, exportDpi = spec.dpi) }
    }

    fun guard(action: () -> Unit) {
        if (state.dirty) pending = action else action()
    }

    // ---------------------------------------------------------------- Documents

    fun newFrom(t: Template) = state.load(t.doc(), null)

    fun open() {
        val f = dialogs.openModel() ?: return
        openFile(f)
    }

    fun openFile(f: File) {
        runCatching { FileOps.read(f) }
            .onSuccess { state.load(it, f) }
            .onFailure { toast("Could not open ${f.name}: ${it.message}") }
    }

    fun save(): Boolean = state.file?.let { writeTo(it) } ?: saveAs()

    fun saveAs(): Boolean {
        val f = dialogs.saveFile("Save model", FileOps.slug(state.displayName), DocIO.EXT) ?: return false
        return writeTo(f)
    }

    private fun writeTo(f: File): Boolean = runCatching { FileOps.write(f, state.doc) }
        .onSuccess { state.file = f; state.dirty = false; saveSession(); toast("Saved ${f.name}") }
        .onFailure { toast("Could not save: ${it.message}") }
        .isSuccess

    /** Persists the open document so it is restored on the next start. */
    fun saveSession() {
        SessionStore.save(Session(state.doc, state.file, state.dirty))
    }

    // ---------------------------------------------------------------- Icons

    fun setIcon(itemId: Long, iconId: String) = state.updateItem(itemId) { it.copy(icon = iconId, image = null) }

    fun setImage(itemId: Long, base64: String) {
        state.updateItem(itemId) { it.copy(image = base64) }
        IconStore.remember(base64)
        recentImages = IconStore.recent()
    }

    /** Opens the native image chooser and assigns the picked image (proportions preserved). */
    fun chooseImage(itemId: Long) {
        val f = dialogs.pickImage() ?: return
        runCatching { Images.import(f) }
            .onSuccess { setImage(itemId, it); toast("Icon updated") }
            .onFailure { toast("Could not use ${f.name}: ${it.message}") }
    }

    fun restoreDefaultIcon(itemId: Long) = state.updateItem(itemId) { it.copy(icon = null, image = null) }

    // ---------------------------------------------------------------- Export

    fun exportPng() = export("png") { scene, f -> Exporter.png(scene, state.exportSpec, f) }

    fun exportSvg() = export("svg") { scene, f -> Exporter.svg(scene, state.exportSpec, f) }

    private fun export(ext: String, write: (Scene, File) -> Unit) {
        val base = FileOps.slug(state.doc.title.ifBlank { state.displayName })
        val f = dialogs.saveFile("Export ${ext.uppercase()}", base, ext) ?: return
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
            val img = withContext(Dispatchers.Default) { runCatching { Renderer.scene(doc).let { Renderer.image(it, spec.scaleFor(it)) } } }
            busy = false
            img.onSuccess { FileOps.copyImage(it); toast("Image copied — paste it into Word or PowerPoint") }
                .onFailure { toast("Copy failed: ${it.message}") }
        }
    }

    fun openDataFolder() = reveal(scimodeler.core.AppDirs.root)

    private fun reveal(f: File) {
        runCatching {
            val d = Desktop.getDesktop()
            when {
                f.isDirectory -> d.open(f)
                d.isSupported(Desktop.Action.BROWSE_FILE_DIR) -> d.browseFileDirectory(f)
                else -> d.open(f.parentFile)
            }
        }
    }

    fun toast(msg: String) {
        scope.launch { snackbar.showSnackbar(msg, duration = SnackbarDuration.Short) }
    }
}
