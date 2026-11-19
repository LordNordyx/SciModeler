package scimodeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import scimodeler.core.ExportSpec
import scimodeler.core.Item
import scimodeler.core.ModelDoc
import scimodeler.core.Style
import java.io.File

class EditorState(initial: ModelDoc) {
    var doc by mutableStateOf(initial)
        private set
    var file by mutableStateOf<File?>(null)
    var dirty by mutableStateOf(false)
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    var exportSpec by mutableStateOf(ExportSpec())

    private val undoStack = ArrayDeque<ModelDoc>()
    private val redoStack = ArrayDeque<ModelDoc>()
    private var lastKey: String? = null
    private var lastTime = 0L

    /**
     * Applies a change. Consecutive changes with the same [key] within a short time
     * (e.g. typing in one field) are merged into a single undo step.
     */
    fun update(key: String? = null, change: (ModelDoc) -> ModelDoc) {
        val next = change(doc)
        if (next == doc) return
        val now = System.currentTimeMillis()
        val merge = key != null && key == lastKey && now - lastTime < 1500
        if (!merge) {
            undoStack.addLast(doc)
            if (undoStack.size > 300) undoStack.removeFirst()
        }
        lastKey = key
        lastTime = now
        redoStack.clear()
        doc = next
        dirty = true
        refreshFlags()
    }

    fun updateStyle(key: String? = null, change: (Style) -> Style) = update(key) { it.copy(style = change(it.style)) }

    fun updateItem(id: Long, key: String? = null, change: (Item) -> Item) = update(key?.let { "$it-$id" }) { d ->
        d.copy(items = d.items.map { if (it.id == id) change(it) else it })
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(doc)
        doc = prev
        lastKey = null
        dirty = true
        refreshFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(doc)
        doc = next
        lastKey = null
        dirty = true
        refreshFlags()
    }

    fun load(newDoc: ModelDoc, from: File?) {
        doc = newDoc
        file = from
        dirty = false
        undoStack.clear()
        redoStack.clear()
        lastKey = null
        refreshFlags()
    }

    private fun refreshFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    val displayName: String get() = file?.nameWithoutExtension ?: doc.title.ifBlank { "Untitled" }
}
