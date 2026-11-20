package scimodeler.ui.freeform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import scimodeler.core.Component
import scimodeler.core.FfElement
import scimodeler.core.FfKind
import scimodeler.core.Freeform
import scimodeler.core.FreeformOps
import scimodeler.core.FreeformOps.AlignMode
import scimodeler.core.FreeformOps.Order
import scimodeler.ui.EditorState

enum class Tool(val label: String) { SELECT("Select"), CONNECT("Connect") }

enum class LeftTab(val label: String) { COMPONENTS("Components"), LAYERS("Layers"), MODELS("Models") }

/** Interface state and commands of the freeform builder. Document changes go through [EditorState] (undo/redo). */
class FreeformUi(val state: EditorState) {
    var selectionRaw by mutableStateOf<Set<Long>>(emptySet())
    var tool by mutableStateOf(Tool.SELECT)
    var gridOn by mutableStateOf(true)
    var grid by mutableStateOf(10f)
    var showGrid by mutableStateOf(true)
    var guidesOn by mutableStateOf(true)
    var nudgeStep by mutableStateOf(10f)
    var leftTab by mutableStateOf(LeftTab.COMPONENTS)

    // Transient visual feedback drawn by the canvas overlay
    var guidesX by mutableStateOf<List<Float>>(emptyList())
    var guidesY by mutableStateOf<List<Float>>(emptyList())
    var hover by mutableStateOf<Long?>(null)
    var marquee by mutableStateOf<FloatArray?>(null)
    var connectFrom by mutableStateOf<Pair<Float, Float>?>(null)
    var connectTo by mutableStateOf<Pair<Float, Float>?>(null)
    var connectTarget by mutableStateOf<Long?>(null)
    var badge by mutableStateOf<String?>(null)
    var badgeAt by mutableStateOf(0f to 0f)

    /** Element whose icon is being chosen. */
    var iconPickerFor by mutableStateOf<Long?>(null)
    /** Incremented to ask the inspector to focus the text field (double-click). */
    var textEditRequest by mutableStateOf(0)

    // Drag & drop from the component library (window coordinates, px)
    var dragComponent by mutableStateOf<Component?>(null)
    var dragPos by mutableStateOf(Offset.Zero)
    /** Window position of the artboard origin and px per artboard unit (set by the canvas). */
    var artOrigin by mutableStateOf<Offset?>(null)
    var artScale by mutableStateOf(1f)

    private var clipboard: List<FfElement> = emptyList()

    /** Set by the canvas: gives it keyboard focus so shortcuts keep working after toolbar clicks. */
    var focusCanvas: () -> Unit = {}

    private inline fun cmd(block: () -> Unit) { block(); focusCanvas() }

    val ff: Freeform get() = state.doc.freeform ?: Freeform()

    /** Selection limited to elements that still exist. */
    var selection: Set<Long>
        get() { val ids = ff.elements.map { it.id }.toSet(); return selectionRaw.filter { it in ids }.toSet() }
        set(v) { selectionRaw = v }

    val selected: List<FfElement> get() = ff.elements.filter { it.id in selection }

    fun edit(key: String? = null, change: (Freeform) -> Freeform) =
        state.update(key) { d -> d.copy(freeform = change(d.freeform ?: Freeform())) }

    fun select(ids: Set<Long>, additive: Boolean = false) {
        val full = FreeformOps.withGroups(ff, ids)
        selection = if (additive) (selection.toMutableSet().also { s -> full.forEach { if (it in s) s.remove(it) else s.add(it) } }) else full
    }

    fun windowToArt(p: Offset): Pair<Float, Float>? {
        val o = artOrigin ?: return null
        return ((p.x - o.x) / artScale) to ((p.y - o.y) / artScale)
    }

    fun isOverArtboard(p: Offset): Boolean {
        val a = windowToArt(p) ?: return false
        return a.first in -40f..(ff.width + 40f) && a.second in -40f..(ff.height + 40f)
    }

    // ------------------------------------------------------------ Commands

    fun add(c: Component, at: Pair<Float, Float>? = null) {
        val (x, y) = at ?: (ff.width / 2f to ff.height / 2f)
        var e = c.make(x, y)
        if (gridOn && e.kind != FfKind.CONNECTOR) e = e.copy(x = FreeformOps.snapValue(e.x, grid, true), y = FreeformOps.snapValue(e.y, grid, true))
        addElement(e)
    }

    fun addElement(e: FfElement) {
        var added: FfElement? = null
        edit { f -> FreeformOps.add(f, e).also { added = it.elements.last() } }
        added?.let { selection = setOf(it.id) }
        tool = Tool.SELECT
        focusCanvas()
    }

    fun deleteSelection() = cmd { if (selection.isNotEmpty()) { edit { FreeformOps.delete(it, selection) }; selection = emptySet() } }

    fun duplicate() = cmd { duplicateNow() }

    private fun duplicateNow() {
        if (selection.isEmpty()) return
        val copies = FreeformOps.copies(ff, FreeformOps.withGroups(ff, selection).let { s -> ff.elements.filter { it.id in s } })
        edit { it.copy(elements = it.elements + copies) }
        selection = copies.map { it.id }.toSet()
    }

    fun copy() { clipboard = ff.elements.filter { it.id in FreeformOps.withGroups(ff, selection) } }
    fun cut() { copy(); deleteSelection() }
    fun paste() {
        if (clipboard.isEmpty()) return
        val copies = FreeformOps.copies(ff, clipboard, 30f, 30f)
        edit { it.copy(elements = it.elements + copies) }
        clipboard = copies
        selection = copies.map { it.id }.toSet()
    }

    fun selectAll() { selection = ff.elements.filter { !it.hidden && !it.locked }.map { it.id }.toSet() }
    fun group() = cmd { edit { FreeformOps.group(it, selection) } }
    fun ungroup() = cmd { edit { FreeformOps.ungroup(it, selection) } }
    fun reorder(o: Order) = cmd { edit { FreeformOps.reorder(it, selection, o) } }
    fun align(m: AlignMode) = cmd { edit { FreeformOps.align(it, selection, m) } }
    fun distribute(h: Boolean) = cmd { edit { FreeformOps.distribute(it, selection, h) } }
    fun nudge(dx: Float, dy: Float) = cmd { edit("ff-nudge") { FreeformOps.move(it, FreeformOps.withGroups(it, selection), dx, dy) } }
    fun rotateBy(deg: Float) = cmd { rotateNow(deg) }
    private fun rotateNow(deg: Float) = edit("ff-rotate") { f -> FreeformOps.update(f, selection) { if (it.isConnector) it else it.copy(rotation = ((it.rotation + deg) % 360f + 360f) % 360f) } }
    fun setLocked(ids: Set<Long>, v: Boolean) = edit { f -> FreeformOps.update(f, ids) { it.copy(locked = v) } }
    fun setHidden(ids: Set<Long>, v: Boolean) = edit { f -> FreeformOps.update(f, ids) { it.copy(hidden = v) } }
    fun updateSelected(key: String? = null, change: (FfElement) -> FfElement) = edit(key) { f -> FreeformOps.update(f, selection, change) }

    fun clearTransient() {
        guidesX = emptyList(); guidesY = emptyList(); marquee = null
        connectFrom = null; connectTo = null; connectTarget = null; badge = null
    }

    /** Keyboard shortcuts while the canvas has focus. */
    fun onKey(e: KeyEvent): Boolean {
        if (e.type != KeyEventType.KeyDown) return false
        val cmd = e.isCtrlPressed || e.isMetaPressed
        val step = if (e.isShiftPressed) nudgeStep * 5f else nudgeStep
        when {
            e.key == Key.Delete || e.key == Key.Backspace -> deleteSelection()
            e.key == Key.DirectionLeft -> nudge(-step, 0f)
            e.key == Key.DirectionRight -> nudge(step, 0f)
            e.key == Key.DirectionUp -> nudge(0f, -step)
            e.key == Key.DirectionDown -> nudge(0f, step)
            cmd && e.key == Key.D -> duplicate()
            cmd && e.key == Key.G -> if (e.isShiftPressed) ungroup() else group()
            cmd && e.key == Key.A -> selectAll()
            cmd && e.key == Key.C && !e.isShiftPressed -> copy()
            cmd && e.key == Key.X -> cut()
            cmd && e.key == Key.V -> paste()
            e.key == Key.RightBracket -> reorder(if (cmd) Order.FRONT else Order.FORWARD)
            e.key == Key.LeftBracket -> reorder(if (cmd) Order.BACK else Order.BACKWARD)
            e.key == Key.V && !cmd -> tool = Tool.SELECT
            e.key == Key.C && !cmd -> tool = Tool.CONNECT
            e.key == Key.Escape -> return escape()
            else -> return false
        }
        return true
    }

    /** Esc: leave connect mode, then clear the selection. */
    fun escape(): Boolean = when {
        tool != Tool.SELECT -> { tool = Tool.SELECT; true }
        selection.isNotEmpty() -> { selection = emptySet(); true }
        else -> false
    }
}
