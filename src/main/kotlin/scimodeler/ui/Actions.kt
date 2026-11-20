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
import scimodeler.core.PresetStore
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
    var presets by mutableStateOf(PresetStore.list())
        private set
    /** Non-null while the "save to My models" dialog is open (holds the proposed name). */
    var savePresetName by mutableStateOf<String?>(null)
    /** Preset being renamed. */
    var renamePreset by mutableStateOf<PresetStore.Preset?>(null)

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

    // ---------------------------------------------------------------- Freeform elements

    private fun pickImage(): String? {
        val f = dialogs.pickImage() ?: return null
        return runCatching { Images.import(f) }
            .onSuccess { IconStore.remember(it); recentImages = IconStore.recent() }
            .onFailure { toast("Could not use ${f.name}: ${it.message}") }
            .getOrNull()
    }

    fun addImageElement(ui: scimodeler.ui.freeform.FreeformUi) {
        val img = pickImage() ?: return
        val (w, h) = Images.size(img)
        val k = minOf(260f / w, 220f / h, 1.5f)
        val ff = ui.ff
        ui.addElement(scimodeler.core.FfElement(
            kind = scimodeler.core.FfKind.IMAGE, x = ff.width / 2f - w * k / 2f, y = ff.height / 2f - h * k / 2f,
            w = w * k, h = h * k, image = img, slot = scimodeler.core.Slots.NEUTRAL,
        ))
    }

    fun chooseElementImage(ui: scimodeler.ui.freeform.FreeformUi, id: Long) {
        val img = pickImage() ?: return
        ui.edit { f -> scimodeler.core.FreeformOps.update(f, setOf(id)) { e ->
            if (e.kind == scimodeler.core.FfKind.IMAGE) {
                // Keep the element's width; height follows the image's proportions.
                val (w, h) = Images.size(img); e.copy(image = img, h = e.w * h / w)
            } else e.copy(image = img)
        } }
    }

    fun chooseElementIconImage(ui: scimodeler.ui.freeform.FreeformUi, id: Long) {
        val img = pickImage() ?: return
        ui.edit { f -> scimodeler.core.FreeformOps.update(f, setOf(id)) { it.copy(image = img) } }
    }

    // ---------------------------------------------------------------- My models (presets)

    fun requestSavePreset() { savePresetName = state.doc.title.ifBlank { "My model" } }

    fun savePreset(name: String) {
        runCatching { PresetStore.save(name, state.doc) }
            .onSuccess { presets = PresetStore.list(); savePresetName = null; toast("Saved “${it.name}” to My models") }
            .onFailure { toast("Could not save: ${it.message}") }
    }

    fun openPreset(p: PresetStore.Preset) = guard {
        state.load(p.doc.copy(items = p.doc.items), null)
        toast("Opened “${p.name}” — changes don't affect the saved model until you save it again")
    }

    fun duplicatePreset(p: PresetStore.Preset) { runCatching { PresetStore.duplicate(p) }; presets = PresetStore.list() }
    fun deletePreset(p: PresetStore.Preset) { PresetStore.delete(p); presets = PresetStore.list(); toast("Deleted “${p.name}”") }
    fun doRenamePreset(p: PresetStore.Preset, name: String) { runCatching { PresetStore.rename(p, name) }; presets = PresetStore.list(); renamePreset = null }

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
