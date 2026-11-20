package scimodeler.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role as SemRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import scimodeler.core.Fonts
import scimodeler.core.IconLibrary
import scimodeler.core.Ids
import scimodeler.core.Item
import scimodeler.core.Models
import scimodeler.core.Renderer
import scimodeler.core.Role
import scimodeler.core.Themes
import scimodeler.core.hex
import scimodeler.core.lighten
import scimodeler.ui.components.AppButton
import scimodeler.ui.components.ButtonKind
import scimodeler.ui.components.Field
import scimodeler.ui.components.GlassPanel
import scimodeler.ui.components.IconBtn
import scimodeler.ui.components.IconGlyph
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
import kotlin.math.roundToInt

private enum class Tab(val title: String) { CONTENT("Content"), STYLE("Style"), EXPORT("Export") }

@Composable
fun Inspector(state: EditorState, actions: Actions, ui: UiState, modifier: Modifier) {
    val motion = LocalMotion.current
    var tab by remember { mutableStateOf(Tab.CONTENT) }
    GlassPanel(modifier) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.padding(14.dp)) {
                Segmented(Tab.entries.toList(), tab, { it.title }, { tab = it }, tagPrefix = "tab")
            }
            HorizontalDivider(color = Tok.C.line)
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    if (!motion.enabled) EnterTransition.None togetherWith ExitTransition.None
                    else {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (fadeIn(motion.tween(Tok.Motion.base)) + slideInHorizontally(motion.tween(Tok.Motion.base)) { dir * it / 10 }) togetherWith
                            fadeOut(motion.tween(Tok.Motion.fast))
                    }
                },
                modifier = Modifier.weight(1f),
                label = "inspector-tab",
            ) { t ->
                when (t) {
                    Tab.CONTENT -> ContentPanel(state, ui)
                    Tab.STYLE -> StylePanel(state)
                    Tab.EXPORT -> ExportPanel(state, actions)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Content

private val extraColors = listOf(
    "#0F172A", "#475569", "#94A3B8", "#DC2626", "#EA580C", "#CA8A04",
    "#16A34A", "#0D9488", "#0284C7", "#4F46E5", "#9333EA", "#DB2777",
).map(::hex)

@Composable
private fun ContentPanel(state: EditorState, ui: UiState) {
    val doc = state.doc
    val spec = doc.spec
    val theme = Themes.get(doc.style.theme)
    val motion = LocalMotion.current
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), modifier = Modifier.testTag("content-list")) {
        item(key = "text") {
            SectionHeader("Text")
            Field("Title", doc.title, { v -> state.update("title") { it.copy(title = v) } }, tag = "field-title")
            Spacer(Modifier.height(8.dp))
            Field("Subtitle", doc.subtitle, { v -> state.update("subtitle") { it.copy(subtitle = v) } }, placeholder = "Optional")
            Spacer(Modifier.height(8.dp))
            Field(
                "Central concept", doc.center, { v -> state.update("center") { it.copy(center = v) } }, singleLine = false,
                hint = if (!spec.usesCenter) "Not shown in the ${spec.title} layout" else null,
            )
        }
        if (spec.groups.isNotEmpty()) item(key = "groups") {
            SectionHeader("Group names")
            spec.groups.forEachIndexed { gi, def ->
                Field(
                    def, doc.groupNames.getOrElse(gi) { "" },
                    { v ->
                        state.update("group$gi") { d ->
                            val names = (0 until spec.groups.size).map { k -> if (k == gi) v else d.groupNames.getOrElse(k) { "" } }
                            d.copy(groupNames = names)
                        }
                    },
                    placeholder = "Caption for “$def”",
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        item(key = "items-header") {
            SectionHeader("Items · ${doc.items.size}") {
                AppButton("Add", { state.update { d -> d.copy(items = d.items + Item(label = "New item")) } },
                    kind = ButtonKind.GHOST, icon = Icons.Outlined.Add, height = 30.dp, tag = "add-item")
            }
            if (doc.items.size > spec.maxItems) {
                Text("${spec.title} shows the first ${spec.maxItems} items.", style = Tok.T.small, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (!spec.usesIcons) {
                Text("Icons appear in layouts such as Branches, Arc list, Petals and Milestones.", style = Tok.T.small.copy(color = Tok.C.text3), modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        itemsIndexed(doc.items, key = { _, it -> it.id }) { index, item ->
            Box(
                if (motion.enabled) Modifier.animateItem() else Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null),
            ) {
                ItemCard(state, ui, item, index, doc.items.size, item.color ?: theme.colors[index % theme.colors.size], theme.colors, spec.usesRoles, spec.usesIcons)
            }
        }
        item(key = "footnote") {
            SectionHeader("Footnote")
            Field(
                "Legend / source", doc.legend, { v -> state.update("legend") { it.copy(legend = v) } }, singleLine = false, minLines = 2,
                placeholder = "e.g. S – collaboration, O – responsibility … Source: Author (2025)",
            )
        }
    }
}

@Composable
private fun ItemCard(
    state: EditorState, ui: UiState, item: Item, index: Int, count: Int, color: Int, themeColors: List<Int>,
    roles: Boolean, iconsShown: Boolean,
) {
    var pickColor by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    val st = src.states()
    val motion = LocalMotion.current
    val tileBg by animateColorAsState(if (st.hovered) Color(lighten(color, 0.78f)) else Color(lighten(color, 0.86f)), motion.tween(Tok.Motion.fast))
    val tileShape = RoundedCornerShape(Tok.R.lg)
    Column(
        Modifier.padding(bottom = 10.dp).fillMaxWidth().clip(RoundedCornerShape(Tok.R.xl)).background(Tok.C.surface)
            .border(1.dp, Tok.C.line, RoundedCornerShape(Tok.R.xl)).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tip(if (iconsShown) "Change icon" else "Change icon (shown in icon layouts)") {
                Box(
                    Modifier.testTag("item-icon-$index").size(46.dp).scale(if (st.pressed) 0.94f else 1f)
                        .clip(tileShape).background(tileBg).focusRing(st.focused, tileShape)
                        .pressable(src) { ui.iconPickerFor = item.id },
                    contentAlignment = Alignment.Center,
                ) {
                    IconGlyph(
                        if (item.image == null) (IconLibrary.get(item.icon) ?: IconLibrary.defaultFor(index)) else null,
                        item.image, Color(color), 26.dp,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Field("Label ${index + 1}", item.label, { v -> state.updateItem(item.id, "label") { it.copy(label = v) } }, modifier = Modifier.weight(1f), tag = "item-label-$index")
        }
        Spacer(Modifier.height(8.dp))
        Field(
            "Description points", item.bullets.joinToString("\n"),
            { v -> state.updateItem(item.id, "bullets") { it.copy(bullets = v.split('\n')) } },
            singleLine = false, minLines = 2, placeholder = "One point per line",
        )
        if (roles) {
            Spacer(Modifier.height(8.dp))
            Segmented(Role.entries.toList(), item.role, { it.label }, { r -> state.updateItem(item.id) { it.copy(role = r) } })
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Swatch(Color(color), 22.dp, tip = "Item colour") { pickColor = true }
                GlassMenu(pickColor, { pickColor = false }) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Text("Theme", style = Tok.T.caption)
                        Spacer(Modifier.height(6.dp))
                        ColorGrid(themeColors, item.color) { c -> state.updateItem(item.id) { it.copy(color = c) }; pickColor = false }
                        Spacer(Modifier.height(10.dp))
                        Text("More", style = Tok.T.caption)
                        Spacer(Modifier.height(6.dp))
                        ColorGrid(extraColors, item.color) { c -> state.updateItem(item.id) { it.copy(color = c) }; pickColor = false }
                    }
                    DropdownMenuItem(text = { Text("Use theme colour automatically", style = Tok.T.body) }, onClick = {
                        state.updateItem(item.id) { it.copy(color = null) }; pickColor = false
                    })
                }
            }
            Spacer(Modifier.weight(1f))
            IconBtn(Icons.Outlined.KeyboardArrowUp, "Move up", { state.moveItem(index, -1) }, enabled = index > 0, size = 30.dp, iconSize = 17.dp)
            IconBtn(Icons.Outlined.KeyboardArrowDown, "Move down", { state.moveItem(index, 1) }, enabled = index < count - 1, size = 30.dp, iconSize = 17.dp)
            IconBtn(Icons.Outlined.ContentCopy, "Duplicate", {
                state.update { d -> d.copy(items = d.items.toMutableList().also { it.add(index + 1, item.copy(id = Ids.next())) }) }
            }, size = 30.dp, iconSize = 16.dp)
            IconBtn(Icons.Outlined.Delete, "Delete", { state.update { d -> d.copy(items = d.items.filterNot { it.id == item.id }) } }, size = 30.dp, iconSize = 17.dp)
        }
    }
}

@Composable
private fun ColorGrid(colors: List<Int>, selected: Int?, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c -> Swatch(Color(c), 24.dp, selected = c == selected) { onPick(c) } }
            }
        }
    }
}

// ------------------------------------------------------------------ Style

private val suggestedFonts = listOf(
    "Inter", "Segoe UI", "Calibri", "Aptos", "Arial", "Helvetica Neue", "Helvetica", "Roboto", "Open Sans",
    "Lato", "Source Sans 3", "Montserrat", "Times New Roman", "Georgia", "Cambria", "Garamond",
)

private val rotatable = setOf("wheel", "cycle", "hub", "onion", "venn", "flower")

@Composable
private fun StylePanel(state: EditorState) {
    val style = state.doc.style
    val motion = LocalMotion.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        SectionHeader("Colour theme")
        Themes.all.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                row.forEach { t ->
                    val sel = t.id == style.theme
                    val src = remember(t.id) { MutableInteractionSource() }
                    val st = src.states()
                    val shape = RoundedCornerShape(Tok.R.lg)
                    val bg by animateColorAsState(when { sel -> Tok.C.accentSoft; st.hovered -> Color.White; else -> Tok.C.surface }, motion.tween(Tok.Motion.fast))
                    val bd by animateColorAsState(if (sel) Tok.C.accent else if (st.hovered) Tok.C.lineStrong else Tok.C.line, motion.tween(Tok.Motion.fast))
                    Column(
                        Modifier.weight(1f).scale(if (st.pressed) 0.97f else 1f).clip(shape).background(bg)
                            .border(if (sel) 2.dp else 1.dp, bd, shape).focusRing(st.focused, shape)
                            .testTag("theme-${t.id}")
                            .pressable(src, role = SemRole.RadioButton) { state.updateStyle { it.copy(theme = t.id) } }
                            .padding(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { t.colors.take(6).forEach { Swatch(Color(it), 15.dp) } }
                        Spacer(Modifier.height(6.dp))
                        Text(t.name, style = Tok.T.label.copy(fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        SectionHeader("Typography")
        var fontMenu by remember { mutableStateOf(false) }
        val installed = style.fontFamily in Fonts.available || Fonts.available.isEmpty()
        Box {
            Field(
                "Font", style.fontFamily, { v -> state.updateStyle("font") { it.copy(fontFamily = v) } },
                hint = if (!installed) "Not installed on this computer — a fallback is used" else null,
                trailing = { IconBtn(Icons.Outlined.ExpandMore, "Choose font", { fontMenu = true }, size = 30.dp) },
            )
            GlassMenu(fontMenu, { fontMenu = false }) {
                suggestedFonts.filter { it in Fonts.available }.forEach { f ->
                    DropdownMenuItem(text = { Text(f, style = Tok.T.body) }, onClick = { state.updateStyle { it.copy(fontFamily = f) }; fontMenu = false })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LabeledSlider("Text size", "${(style.textScale * 100).roundToInt()} %", style.textScale, 0.7f..1.5f) { v ->
            state.updateStyle("scale") { it.copy(textScale = (v * 20f).roundToInt() / 20f) }
        }
        if (state.doc.layout in rotatable) {
            LabeledSlider("Rotation", "${style.rotation.roundToInt()}°", style.rotation, -180f..180f) { v ->
                state.updateStyle("rotation") { it.copy(rotation = (v / 5f).roundToInt() * 5f) }
            }
        }

        SectionHeader("Show")
        ToggleRow("Title & subtitle", style.showTitle, { v -> state.updateStyle { it.copy(showTitle = v) } })
        ToggleRow("Descriptions", style.showCallouts, { v -> state.updateStyle { it.copy(showCallouts = v) } }, hint = "Each item's description points")
        ToggleRow("Icons", style.showIcons, { v -> state.updateStyle { it.copy(showIcons = v) } }, hint = "In layouts that support icons", tag = "toggle-icons")
        ToggleRow("Footnote", style.showLegend, { v -> state.updateStyle { it.copy(showLegend = v) } })

        SectionHeader("Effects")
        ToggleRow("Soft shadows", style.shadows, { v -> state.updateStyle { it.copy(shadows = v) } })
        ToggleRow("Gradients", style.gradients, { v -> state.updateStyle { it.copy(gradients = v) } })
        ToggleRow("Transparent background", style.transparentBackground, { v -> state.updateStyle { it.copy(transparentBackground = v) } }, hint = "PNG and SVG without white fill")
    }
}

// ------------------------------------------------------------------ Export

@Composable
private fun ExportPanel(state: EditorState, actions: Actions) {
    val spec = state.exportSpec
    val scene = remember(state.doc) { Renderer.scene(state.doc) }
    var widthText by remember { mutableStateOf(trimFloat(spec.widthCm)) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        SectionHeader("Size in your document")
        Field("Width (cm)", widthText, { v ->
            widthText = v.filter { it.isDigit() || it == '.' || it == ',' }
            widthText.replace(',', '.').toFloatOrNull()?.takeIf { it in 1f..100f }?.let { actions.setExportSpec(spec.copy(widthCm = it)) }
        })
        Spacer(Modifier.height(8.dp))
        val presets = listOf("Full · 16" to 16f, "Half · 8" to 8f, "Wide · 24" to 24f)
        Segmented(presets, presets.firstOrNull { it.second == spec.widthCm }, { it.first }, { (_, cm) ->
            actions.setExportSpec(spec.copy(widthCm = cm)); widthText = trimFloat(cm)
        })
        SectionHeader("Resolution")
        Segmented(listOf(150, 300, 600), spec.dpi, { "$it dpi" }, { actions.setExportSpec(spec.copy(dpi = it)) })
        Spacer(Modifier.height(6.dp))
        Text("300 dpi is the usual requirement for printed theses.", style = Tok.T.small)
        Spacer(Modifier.height(14.dp))
        val (pw, ph) = spec.pixels(scene)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Tok.R.lg)).background(Tok.C.accentSoft).padding(14.dp)) {
            Text("${trimFloat(spec.widthCm)} × ${trimFloat(spec.heightCm(scene))} cm", style = Tok.T.title)
            Text("$pw × $ph px PNG", style = Tok.T.small)
        }
        SectionHeader("Export")
        AppButton("Export PNG", { actions.exportPng() }, Modifier.fillMaxWidth(), ButtonKind.PRIMARY, Icons.Outlined.Image, enabled = !actions.busy, height = 42.dp)
        Spacer(Modifier.height(8.dp))
        AppButton("Export SVG (vector)", { actions.exportSvg() }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Code, enabled = !actions.busy, height = 42.dp)
        Spacer(Modifier.height(8.dp))
        AppButton("Copy image to clipboard", { actions.copyImage() }, Modifier.fillMaxWidth(), icon = Icons.Outlined.ContentCopy, enabled = !actions.busy, height = 42.dp)
        SectionHeader("Tips")
        Note("Word / Google Docs", "Insert the PNG, or the SVG for perfectly sharp printing.")
        Note("LaTeX", "Use the PNG directly, or convert the SVG to PDF (Inkscape) for \\includegraphics.")
        Note("Editing later", "Save the model (.scimodel) so you can change text or layout at any time.")
    }
}

@Composable
private fun Note(title: String, body: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(title, style = Tok.T.label.copy(fontWeight = FontWeight.SemiBold))
        Text(body, style = Tok.T.small)
    }
}

fun trimFloat(v: Float): String {
    val r = Math.round(v * 10f) / 10f
    return if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString()
}
