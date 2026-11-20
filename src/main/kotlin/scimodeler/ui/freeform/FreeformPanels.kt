package scimodeler.ui.freeform

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.AlignHorizontalCenter
import androidx.compose.material.icons.outlined.AlignHorizontalLeft
import androidx.compose.material.icons.outlined.AlignHorizontalRight
import androidx.compose.material.icons.outlined.AlignVerticalBottom
import androidx.compose.material.icons.outlined.AlignVerticalCenter
import androidx.compose.material.icons.outlined.AlignVerticalTop
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.FlipToFront
import androidx.compose.material.icons.outlined.Grid4x4
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import scimodeler.core.Align
import scimodeler.core.Component
import scimodeler.core.Components
import scimodeler.core.ElementRaster
import scimodeler.core.FfElement
import scimodeler.core.FfKind
import scimodeler.core.FreeformOps.AlignMode
import scimodeler.core.FreeformOps.Order
import scimodeler.core.Material
import scimodeler.core.ModelDoc
import scimodeler.core.Route
import scimodeler.core.Slots
import scimodeler.core.Themes
import scimodeler.core.hex
import scimodeler.ui.Actions
import scimodeler.ui.EditorState
import scimodeler.ui.components.AppButton
import scimodeler.ui.components.ButtonKind
import scimodeler.ui.components.Chip
import scimodeler.ui.components.Field
import scimodeler.ui.components.GlassPanel
import scimodeler.ui.components.IconBtn
import scimodeler.ui.components.LabeledSlider
import scimodeler.ui.components.SectionHeader
import scimodeler.ui.components.Segmented
import scimodeler.ui.components.Swatch
import scimodeler.ui.components.Tip
import scimodeler.ui.components.ToggleRow
import scimodeler.ui.components.focusRing
import scimodeler.ui.components.pressable
import scimodeler.ui.components.states
import scimodeler.ui.theme.LocalMotion
import scimodeler.ui.theme.Tok
import scimodeler.ui.trimFloat
import kotlin.math.roundToInt

// ------------------------------------------------------------------ Component library

@Composable
fun ComponentLibrary(ui: FreeformUi, doc: ModelDoc, actions: Actions) {
    val styleKey = Triple(doc.style.theme, doc.style.gradients, doc.style.shadows)
    val previews by produceState<Map<String, ImageBitmap>>(emptyMap(), styleKey) {
        withContext(Dispatchers.Default) {
            Components.all.forEach { c ->
                runCatching { ElementRaster.render(doc, c.make(0f, 0f), 160).toComposeImageBitmap() }.getOrNull()?.let { value = value + (c.id to it) }
            }
        }
    }
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().testTag("component-library"),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Field("", query, { query = it }, placeholder = "Search components",
                    leading = { Icon(Icons.Outlined.Search, null, tint = Tok.C.text3, modifier = Modifier.size(18.dp)) })
                Spacer(Modifier.height(6.dp))
                Text("Click to add, or drag onto the canvas.", style = Tok.T.small.copy(color = Tok.C.text3))
            }
        }
        for (cat in Components.categories) {
            val list = Components.all.filter { it.category == cat && (q.isEmpty() || it.name.lowercase().contains(q) || cat.lowercase().contains(q)) }
            if (list.isEmpty()) continue
            item(key = "cat-$cat", span = { GridItemSpan(maxLineSpan) }) {
                Text(cat.uppercase(), style = Tok.T.caption, modifier = Modifier.padding(top = 8.dp))
            }
            items(list, key = { it.id }) { c -> ComponentTile(ui, c, previews[c.id], actions) }
        }
    }
}

@Composable
private fun ComponentTile(ui: FreeformUi, c: Component, preview: ImageBitmap?, actions: Actions) {
    val motion = LocalMotion.current
    val src = remember { MutableInteractionSource() }
    val st = src.states()
    val shape = RoundedCornerShape(Tok.R.lg)
    var origin by remember { mutableStateOf(Offset.Zero) }
    val bg by animateColorAsState(if (st.hovered) Color.White else Tok.C.surface, motion.tween(Tok.Motion.fast))
    val scale by animateFloatAsState(if (st.pressed) 0.94f else if (st.hovered) 1.03f else 1f, motion.spring())
    Tip(c.name) {
        Column(
            Modifier.testTag("component-${c.id}").scale(scale)
                .onGloballyPositioned { origin = it.positionInWindow() }
                .pointerDrag(ui, c) { origin }
                .pressable(src) {
                    if (c.id == "image") actions.addImageElement(ui) else ui.add(c)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.15f).clip(shape).background(bg)
                    .border(1.dp, if (st.hovered) Tok.C.lineStrong else Tok.C.line, shape).focusRing(st.focused, shape).padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (preview != null) Image(preview, c.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Text(c.name, style = Tok.T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Drag a library tile onto the canvas. */
private fun Modifier.pointerDrag(ui: FreeformUi, c: Component, origin: () -> Offset): Modifier =
    this.pointerInput(c.id) {
        detectDragGestures(
            onDragStart = { off -> ui.dragComponent = c; ui.dragPos = origin() + off },
            onDrag = { ch, amount -> ch.consume(); ui.dragPos += amount },
            onDragEnd = {
                val at = ui.windowToArt(ui.dragPos)
                if (at != null && ui.isOverArtboard(ui.dragPos)) ui.add(c, at)
                ui.dragComponent = null
            },
            onDragCancel = { ui.dragComponent = null },
        )
    }

// ------------------------------------------------------------------ Layers

private fun kindIcon(e: FfElement): ImageVector = when (e.kind) {
    FfKind.TEXT -> Icons.Outlined.TextFields
    FfKind.CONNECTOR, FfKind.DIVIDER -> Icons.AutoMirrored.Outlined.ArrowRightAlt
    FfKind.IMAGE -> Icons.Outlined.Image
    FfKind.ELLIPSE, FfKind.NODE, FfKind.DOT, FfKind.BADGE -> Icons.Outlined.Circle
    else -> Icons.Outlined.CropSquare
}

@Composable
fun LayersPanel(ui: FreeformUi) {
    val ff = ui.ff
    val sel = ui.selection
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${ff.elements.size} elements · front first", style = Tok.T.small, modifier = Modifier.weight(1f))
            IconBtn(Icons.Outlined.KeyboardArrowUp, "Bring forward", { ui.reorder(Order.FORWARD) }, enabled = sel.isNotEmpty(), size = 30.dp)
            IconBtn(Icons.Outlined.KeyboardArrowDown, "Send backward", { ui.reorder(Order.BACKWARD) }, enabled = sel.isNotEmpty(), size = 30.dp)
        }
        LazyColumn(contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 18.dp), modifier = Modifier.testTag("layers")) {
            items(ff.elements.asReversed(), key = { it.id }) { e -> LayerRow(ui, e, e.id in sel) }
        }
    }
}

@Composable
private fun LayerRow(ui: FreeformUi, e: FfElement, selected: Boolean) {
    val motion = LocalMotion.current
    val src = remember { MutableInteractionSource() }
    val st = src.states()
    val shape = RoundedCornerShape(Tok.R.md)
    val bg by animateColorAsState(when { selected -> Tok.C.accentSoft; st.hovered -> Tok.C.hover; else -> Color.Transparent }, motion.tween(Tok.Motion.fast))
    Row(
        Modifier.fillMaxWidth().clip(shape).background(bg).focusRing(st.focused, shape)
            .testTag("layer-${e.id}")
            .pressable(src) { ui.select(setOf(e.id)) }
            .padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(kindIcon(e), null, tint = if (selected) Tok.C.accent else Tok.C.text2, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            e.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = Tok.T.label.copy(color = if (e.hidden) Tok.C.text3 else Tok.C.text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            modifier = Modifier.weight(1f),
        )
        if (e.group != null) Chip("Group", Modifier.padding(end = 4.dp))
        IconBtn(if (e.hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (e.hidden) "Show" else "Hide",
            { ui.setHidden(setOf(e.id), !e.hidden) }, size = 28.dp, iconSize = 15.dp)
        IconBtn(if (e.locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, if (e.locked) "Unlock" else "Lock",
            { ui.setLocked(setOf(e.id), !e.locked) }, active = e.locked, size = 28.dp, iconSize = 15.dp)
    }
}

// ------------------------------------------------------------------ Transform toolbar

@Composable
fun TransformBar(ui: FreeformUi, modifier: Modifier) {
    val sel = ui.selection.isNotEmpty()
    GlassPanel(modifier.height(48.dp), RoundedCornerShape(Tok.R.lg)) {
        Row(Modifier.padding(horizontal = 6.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.Outlined.NorthWest, "Select (V)", { ui.tool = Tool.SELECT; ui.focusCanvas() }, active = ui.tool == Tool.SELECT, size = 32.dp, tipBelow = true, tag = "tool-select")
            IconBtn(Icons.AutoMirrored.Outlined.TrendingFlat, "Connect: drag from one shape to another (C)", { ui.tool = Tool.CONNECT; ui.focusCanvas() }, active = ui.tool == Tool.CONNECT, size = 32.dp, tipBelow = true, tag = "tool-connect")
            Divider()
            IconBtn(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Move left", { ui.nudge(-ui.nudgeStep, 0f) }, enabled = sel, size = 30.dp, tipBelow = true)
            IconBtn(Icons.Outlined.KeyboardArrowUp, "Move up", { ui.nudge(0f, -ui.nudgeStep) }, enabled = sel, size = 30.dp, tipBelow = true)
            IconBtn(Icons.Outlined.KeyboardArrowDown, "Move down", { ui.nudge(0f, ui.nudgeStep) }, enabled = sel, size = 30.dp, tipBelow = true)
            IconBtn(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Move right", { ui.nudge(ui.nudgeStep, 0f) }, enabled = sel, size = 30.dp, tipBelow = true, tag = "nudge-right")
            Tip("Step for arrows and arrow keys", below = true) {
                Text(
                    "${ui.nudgeStep.roundToInt()} px", style = Tok.T.label,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).pressable(remember { MutableInteractionSource() }) {
                        val steps = listOf(1f, 5f, 10f, 20f, 50f)
                        ui.nudgeStep = steps[(steps.indexOf(ui.nudgeStep) + 1) % steps.size]
                    }.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            Divider()
            IconBtn(Icons.Outlined.RotateLeft, "Rotate −15°", { ui.rotateBy(-15f) }, enabled = sel, size = 30.dp, tipBelow = true)
            IconBtn(Icons.Outlined.RotateRight, "Rotate +15°", { ui.rotateBy(15f) }, enabled = sel, size = 30.dp, tipBelow = true)
            IconBtn(Icons.Outlined.FlipToFront, "Bring to front", { ui.reorder(Order.FRONT) }, enabled = sel, size = 30.dp, tipBelow = true)
            IconBtn(Icons.Outlined.FlipToBack, "Send to back", { ui.reorder(Order.BACK) }, enabled = sel, size = 30.dp, tipBelow = true)
            IconBtn(Icons.Outlined.ContentCopy, "Duplicate (Ctrl/Cmd+D)", { ui.duplicate() }, enabled = sel, size = 30.dp, tipBelow = true, tag = "ff-duplicate")
            IconBtn(Icons.Outlined.Delete, "Delete", { ui.deleteSelection() }, enabled = sel, size = 30.dp, tipBelow = true, tag = "ff-delete")
            Divider()
            IconBtn(Icons.Outlined.Grid4x4, "Snap to grid", { ui.gridOn = !ui.gridOn }, active = ui.gridOn, size = 30.dp, tipBelow = true, tag = "toggle-grid")
            IconBtn(Icons.Outlined.Straighten, "Smart guides", { ui.guidesOn = !ui.guidesOn }, active = ui.guidesOn, size = 30.dp, tipBelow = true)
        }
    }
}

@Composable
private fun Divider() = VerticalDivider(Modifier.height(20.dp).padding(horizontal = 4.dp), color = Tok.C.line)

// ------------------------------------------------------------------ Inspector

private val customColors = listOf("#0F172A", "#475569", "#94A3B8", "#DC2626", "#EA580C", "#CA8A04", "#16A34A", "#0D9488", "#0284C7", "#4F46E5", "#9333EA", "#DB2777").map(::hex)

@Composable
fun FreeformInspector(state: EditorState, ui: FreeformUi, actions: Actions) {
    val sel = ui.selected
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).testTag("ff-inspector")) {
        when {
            sel.isEmpty() -> ArtboardSection(state, ui, actions)
            sel.size == 1 -> ElementSection(state, ui, actions, sel.first())
            else -> MultiSection(ui, sel)
        }
    }
}

@Composable
private fun ArtboardSection(state: EditorState, ui: FreeformUi, actions: Actions) {
    val ff = ui.ff
    val doc = state.doc
    SectionHeader("Text")
    Field("Title", doc.title, { v -> state.update("title") { it.copy(title = v) } })
    Spacer(Modifier.height(8.dp))
    Field("Footnote", doc.legend, { v -> state.update("legend") { it.copy(legend = v) } }, singleLine = false)
    SectionHeader("Artboard")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumField("Width", ff.width, Modifier.weight(1f)) { v -> ui.edit("ff-size") { it.copy(width = v.coerceIn(200f, 6000f)) } }
        NumField("Height", ff.height, Modifier.weight(1f)) { v -> ui.edit("ff-size") { it.copy(height = v.coerceIn(150f, 6000f)) } }
    }
    Spacer(Modifier.height(8.dp))
    val presets = listOf("4:3" to (1200f to 900f), "16:9" to (1440f to 810f), "A4 ↔" to (1400f to 990f), "A4 ↕" to (990f to 1400f))
    Segmented(presets, presets.firstOrNull { it.second.first == ff.width && it.second.second == ff.height }, { it.first },
        { (_, wh) -> ui.edit { it.copy(width = wh.first, height = wh.second) } })
    SectionHeader("Snapping")
    ToggleRow("Snap to grid", ui.gridOn, { ui.gridOn = it }, hint = "Hold Alt while dragging to move freely")
    Segmented(listOf(5f, 10f, 20f, 40f), ui.grid, { "${it.roundToInt()} px" }, { ui.grid = it })
    Spacer(Modifier.height(4.dp))
    ToggleRow("Show grid", ui.showGrid, { ui.showGrid = it })
    ToggleRow("Smart guides", ui.guidesOn, { ui.guidesOn = it }, hint = "Align to edges and centres of other shapes")
    SectionHeader("Reuse")
    AppButton("Save to My models", { actions.requestSavePreset() }, Modifier.fillMaxWidth(), ButtonKind.PRIMARY, Icons.Outlined.BookmarkAdd, height = 40.dp, tag = "save-preset")
    Spacer(Modifier.height(6.dp))
    Text("Saved models appear in the model library and keep every style option.", style = Tok.T.small)
    SectionHeader("Shortcuts")
    Text(
        "Drag to move · Shift = straight line · Alt = no snapping\nArrows nudge · Del deletes · Ctrl/Cmd+D duplicate\nCtrl/Cmd+G group · Shift+Ctrl/Cmd+G ungroup\nV select tool · C connect tool · [ ] layer order",
        style = Tok.T.small,
    )
}

@Composable
private fun ElementSection(state: EditorState, ui: FreeformUi, actions: Actions, e: FfElement) {
    val theme = Themes.get(state.doc.style.theme)
    val hasText = e.kind !in setOf(FfKind.CONNECTOR, FfKind.DIVIDER, FfKind.DOT, FfKind.ICON, FfKind.IMAGE)
    val isShape = e.kind in setOf(FfKind.RECT, FfKind.ROUNDED, FfKind.PILL, FfKind.ELLIPSE, FfKind.DIAMOND, FfKind.TRIANGLE, FfKind.HEXAGON, FfKind.CHEVRON, FfKind.PARALLELOGRAM)
    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(kindIcon(e), null, tint = Tok.C.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(e.kind.label, style = Tok.T.title, modifier = Modifier.weight(1f))
        if (e.group != null) Chip("In group")
    }
    Spacer(Modifier.height(8.dp))
    Field("Layer name", e.name, { v -> ui.updateSelected("ff-name") { it.copy(name = v) } }, placeholder = e.displayName)

    if (!e.isConnector) {
        SectionHeader("Position & size")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("X", e.x, Modifier.weight(1f), "ff-x") { v -> ui.updateSelected("ff-pos") { it.copy(x = v) } }
            NumField("Y", e.y, Modifier.weight(1f), "ff-y") { v -> ui.updateSelected("ff-pos") { it.copy(y = v) } }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("W", e.w, Modifier.weight(1f), "ff-w") { v -> ui.updateSelected("ff-size") { it.copy(w = v.coerceAtLeast(12f)) } }
            NumField("H", e.h, Modifier.weight(1f), "ff-h") { v -> ui.updateSelected("ff-size") { it.copy(h = v.coerceAtLeast(12f)) } }
            NumField("Rotate °", e.rotation, Modifier.weight(1f)) { v -> ui.updateSelected("ff-rot") { it.copy(rotation = ((v % 360f) + 360f) % 360f) } }
        }
    }

    if (hasText) {
        SectionHeader(if (e.kind == FfKind.CARD) "Title & points" else "Text")
        val fr = remember { FocusRequester() }
        LaunchedEffect(ui.textEditRequest) { if (ui.textEditRequest > 0) runCatching { fr.requestFocus() } }
        Field("Text", e.text, { v -> ui.updateSelected("ff-text-${e.id}") { it.copy(text = v) } }, singleLine = false,
            modifier = Modifier.focusRequester(fr), tag = "ff-text")
        if (e.kind == FfKind.CARD) {
            Spacer(Modifier.height(8.dp))
            Field("Points", e.bullets.joinToString("\n"), { v -> ui.updateSelected("ff-bullets-${e.id}") { it.copy(bullets = v.split('\n')) } },
                singleLine = false, minLines = 2, placeholder = "One point per line")
        }
        LabeledSlider("Text size", "${e.textSize.roundToInt()}", e.textSize, 9f..60f) { v -> ui.updateSelected("ff-ts") { it.copy(textSize = v.roundToInt().toFloat()) } }
        ToggleRow("Bold", e.bold, { v -> ui.updateSelected { it.copy(bold = v) } })
        if (e.kind == FfKind.TEXT || isShape) {
            Segmented(listOf(Align.START, Align.CENTER, Align.END), e.align, { when (it) { Align.START -> "Left"; Align.CENTER -> "Centre"; Align.END -> "Right" } },
                { a -> ui.updateSelected { it.copy(align = a) } })
        }
    }

    SectionHeader("Colour")
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        theme.colors.forEachIndexed { k, c -> Swatch(Color(c), 22.dp, selected = e.color == null && e.slot == k, tag = "ff-slot-$k") { ui.updateSelected { it.copy(slot = k, color = null) } } }
    }
    Spacer(Modifier.height(7.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        Swatch(Color(scimodeler.core.Ink.text), 22.dp, selected = e.color == null && e.slot == Slots.INK, tip = "Ink") { ui.updateSelected { it.copy(slot = Slots.INK, color = null) } }
        Swatch(Color(theme.hub), 22.dp, selected = e.color == null && e.slot == Slots.HUB, tip = "Theme dark") { ui.updateSelected { it.copy(slot = Slots.HUB, color = null) } }
        Swatch(Color(theme.panel), 22.dp, selected = e.color == null && e.slot == Slots.NEUTRAL, tip = "Neutral") { ui.updateSelected { it.copy(slot = Slots.NEUTRAL, color = null) } }
        Spacer(Modifier.width(6.dp))
        customColors.take(6).forEach { c -> Swatch(Color(c), 18.dp, selected = e.color == c) { ui.updateSelected { it.copy(color = c) } } }
    }
    Text(if (e.color != null) "Custom colour" else "Theme colour — follows theme changes", style = Tok.T.tiny.copy(color = Tok.C.text3), modifier = Modifier.padding(top = 6.dp))
    if (isShape || e.kind == FfKind.NODE) {
        SectionHeader("Material")
        val mats = if (e.kind == FfKind.NODE) listOf(Material.THEME, Material.SOLID, Material.OUTLINE) else Material.entries.toList()
        Segmented(mats, e.material, { it.label }, { m -> ui.updateSelected { it.copy(material = m) } }, tagPrefix = "material")
    }
    if (e.kind == FfKind.ROUNDED || e.kind == FfKind.CARD) {
        LabeledSlider("Corner radius", "${e.corner.roundToInt()}", e.corner, 0f..60f) { v -> ui.updateSelected("ff-corner") { it.copy(corner = v.roundToInt().toFloat()) } }
    }
    LabeledSlider("Opacity", "${(e.opacity * 100).roundToInt()} %", e.opacity, 0.1f..1f) { v -> ui.updateSelected("ff-op") { it.copy(opacity = (v * 20f).roundToInt() / 20f) } }

    if (isShape || e.kind == FfKind.NODE || e.kind == FfKind.ICON || e.kind == FfKind.IMAGE) {
        SectionHeader(if (e.kind == FfKind.IMAGE) "Image" else "Icon")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (e.kind != FfKind.IMAGE) AppButton("Change icon…", { ui.iconPickerFor = e.id }, Modifier.weight(1f), tag = "ff-change-icon")
            if (e.kind == FfKind.IMAGE) AppButton("Choose image…", { actions.chooseElementImage(ui, e.id) }, Modifier.weight(1f), ButtonKind.PRIMARY)
            if (isShape && (e.icon != null || e.image != null)) AppButton("Remove", { ui.updateSelected { it.copy(icon = null, image = null) } }, kind = ButtonKind.GHOST)
        }
    }

    if (e.isConnector) {
        SectionHeader("Connector")
        Segmented(Route.entries.toList(), e.route, { it.label }, { r -> ui.updateSelected { it.copy(route = r) } }, tagPrefix = "route")
        Spacer(Modifier.height(4.dp))
        ToggleRow("Arrow at end", e.headEnd, { v -> ui.updateSelected { it.copy(headEnd = v) } })
        ToggleRow("Arrow at start", e.headStart, { v -> ui.updateSelected { it.copy(headStart = v) } })
        ToggleRow("Dashed", e.dashed, { v -> ui.updateSelected { it.copy(dashed = v) } })
        LabeledSlider("Thickness", "${e.strokeWidth.roundToInt()}", e.strokeWidth, 1f..14f) { v -> ui.updateSelected("ff-sw") { it.copy(strokeWidth = v.roundToInt().toFloat()) } }
        val ff = ui.ff
        Text(
            "From: ${ff.byId(e.from)?.displayName ?: "free point"}   ·   To: ${ff.byId(e.to)?.displayName ?: "free point"}",
            style = Tok.T.small, modifier = Modifier.padding(top = 4.dp),
        )
        Text("Drag an end point onto a shape to attach it. Attached arrows follow the shape.", style = Tok.T.tiny.copy(color = Tok.C.text3))
    } else if (e.kind == FfKind.DIVIDER) {
        ToggleRow("Dashed", e.dashed, { v -> ui.updateSelected { it.copy(dashed = v) } })
        LabeledSlider("Thickness", "${e.strokeWidth.roundToInt()}", e.strokeWidth, 1f..14f) { v -> ui.updateSelected("ff-sw") { it.copy(strokeWidth = v.roundToInt().toFloat()) } }
    }

    SectionHeader("Arrange")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppButton(if (e.locked) "Unlock" else "Lock", { ui.setLocked(setOf(e.id), !e.locked) }, Modifier.weight(1f), icon = if (e.locked) Icons.Outlined.LockOpen else Icons.Outlined.Lock)
        AppButton(if (e.hidden) "Show" else "Hide", { ui.setHidden(setOf(e.id), !e.hidden) }, Modifier.weight(1f), icon = if (e.hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff)
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppButton("Duplicate", { ui.duplicate() }, Modifier.weight(1f), icon = Icons.Outlined.ContentCopy)
        AppButton("Delete", { ui.deleteSelection() }, Modifier.weight(1f), icon = Icons.Outlined.Delete)
    }
}

@Composable
private fun MultiSection(ui: FreeformUi, sel: List<FfElement>) {
    Text("${sel.size} elements selected", style = Tok.T.title, modifier = Modifier.padding(top = 16.dp))
    SectionHeader("Align")
    Row {
        IconBtn(Icons.Outlined.AlignHorizontalLeft, "Align left", { ui.align(AlignMode.LEFT) }, tag = "align-left")
        IconBtn(Icons.Outlined.AlignHorizontalCenter, "Align centres", { ui.align(AlignMode.HCENTER) })
        IconBtn(Icons.Outlined.AlignHorizontalRight, "Align right", { ui.align(AlignMode.RIGHT) })
        IconBtn(Icons.Outlined.AlignVerticalTop, "Align top", { ui.align(AlignMode.TOP) }, tag = "align-top")
        IconBtn(Icons.Outlined.AlignVerticalCenter, "Align middles", { ui.align(AlignMode.VCENTER) })
        IconBtn(Icons.Outlined.AlignVerticalBottom, "Align bottom", { ui.align(AlignMode.BOTTOM) })
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppButton("Distribute ↔", { ui.distribute(true) }, Modifier.weight(1f), enabled = sel.size >= 3)
        AppButton("Distribute ↕", { ui.distribute(false) }, Modifier.weight(1f), enabled = sel.size >= 3)
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppButton("Same width", { ui.edit { scimodeler.core.FreeformOps.matchSize(it, ui.selection, true) } }, Modifier.weight(1f))
        AppButton("Same height", { ui.edit { scimodeler.core.FreeformOps.matchSize(it, ui.selection, false) } }, Modifier.weight(1f))
    }
    SectionHeader("Group")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppButton("Group", { ui.group() }, Modifier.weight(1f), icon = Icons.Outlined.Workspaces, tag = "ff-group")
        AppButton("Ungroup", { ui.ungroup() }, Modifier.weight(1f))
    }
    SectionHeader("Material for all")
    Segmented(Material.entries.toList(), sel.map { it.material }.distinct().singleOrNull(), { it.label }, { m -> ui.updateSelected { if (it.isConnector) it else it.copy(material = m) } })
    Spacer(Modifier.height(10.dp))
    AppButton("Delete ${sel.size} elements", { ui.deleteSelection() }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Delete)
}

/** Numeric field that commits valid numbers and resyncs when the value changes elsewhere (e.g. dragging). */
@Composable
fun NumField(label: String, value: Float, modifier: Modifier = Modifier, tag: String? = null, onCommit: (Float) -> Unit) {
    var text by remember(value) { mutableStateOf(trimFloat(value)) }
    Field(label, text, { v ->
        text = v.filter { it.isDigit() || it == '.' || it == '-' || it == ',' }
        text.replace(',', '.').toFloatOrNull()?.let { if (!text.endsWith(".") && text != "-") onCommit(it) }
    }, modifier, tag = tag)
}
