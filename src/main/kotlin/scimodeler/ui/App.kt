package scimodeler.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import scimodeler.core.LayoutType
import scimodeler.core.ModelDoc
import scimodeler.core.Renderer
import scimodeler.core.Scene
import scimodeler.core.Templates
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun App(state: EditorState, actions: Actions) {
    MaterialTheme(colorScheme = Ui.colors) {
        Box(Modifier.fillMaxSize().background(Ui.bg)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(state, actions)
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    LayoutGallery(state)
                    VerticalDivider(color = Ui.line)
                    CanvasArea(state, Modifier.weight(1f))
                    VerticalDivider(color = Ui.line)
                    Inspector(state, actions)
                }
            }
            SnackbarHost(actions.snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
        }
        actions.pending?.let { next ->
            AlertDialog(
                onDismissRequest = { actions.pending = null },
                title = { Text("Save changes to “${state.displayName}”?") },
                text = { Text("Your changes will be lost if you don't save them.") },
                confirmButton = {
                    Button(onClick = {
                        actions.pending = null
                        if (actions.save()) next()
                    }) { Text("Save") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { actions.pending = null }) { Text("Cancel") }
                        TextButton(onClick = { actions.pending = null; next() }) { Text("Don't save") }
                    }
                },
            )
        }
    }
}

// ------------------------------------------------------------------ Top bar

@Composable
private fun TopBar(state: EditorState, actions: Actions) {
    Surface(color = Ui.surface, modifier = Modifier.fillMaxWidth().height(58.dp)) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF06B6D4)))),
                contentAlignment = Alignment.Center,
            ) { Text("S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(Modifier.width(10.dp))
            Text("SciModeler", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(16.dp))
            VerticalDivider(Modifier.height(24.dp), color = Ui.line)
            Spacer(Modifier.width(16.dp))
            Text(state.displayName, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false))
            if (state.dirty) {
                Spacer(Modifier.width(8.dp))
                Text("Edited", fontSize = 11.sp, color = Ui.muted,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFF2F4F7))
                        .padding(horizontal = 7.dp, vertical = 2.dp))
            }
            Spacer(Modifier.weight(1f))

            IconBtn(Icons.AutoMirrored.Outlined.Undo, "Undo", enabled = state.canUndo) { state.undo() }
            IconBtn(Icons.AutoMirrored.Outlined.Redo, "Redo", enabled = state.canRedo) { state.redo() }
            Spacer(Modifier.width(8.dp))
            VerticalDivider(Modifier.height(24.dp), color = Ui.line)
            Spacer(Modifier.width(8.dp))

            var newMenu by remember { mutableStateOf(false) }
            Box {
                IconBtn(Icons.Outlined.Add, "New from template") { newMenu = true }
                DropdownMenu(expanded = newMenu, onDismissRequest = { newMenu = false }) {
                    Text("New from template", fontSize = 11.sp, color = Ui.muted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    Templates.all.forEach { t ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(t.name, fontSize = 14.sp)
                                    Text(t.description, fontSize = 12.sp, color = Ui.muted)
                                }
                            },
                            onClick = { newMenu = false; actions.guard { actions.newFrom(t) } },
                        )
                    }
                }
            }
            IconBtn(Icons.Outlined.FolderOpen, "Open…") { actions.guard { actions.open() } }
            IconBtn(Icons.Outlined.Save, "Save") { actions.save() }
            Spacer(Modifier.width(10.dp))

            var exportMenu by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { exportMenu = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ui.accent),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp),
                ) {
                    Icon(Icons.Outlined.FileDownload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export", fontSize = 14.sp)
                }
                DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                    DropdownMenuItem(text = { Text("PNG image") }, onClick = { exportMenu = false; actions.exportPng() })
                    DropdownMenuItem(text = { Text("SVG vector") }, onClick = { exportMenu = false; actions.exportSvg() })
                    HorizontalDivider(color = Ui.line)
                    DropdownMenuItem(text = { Text("Copy to clipboard") }, onClick = { exportMenu = false; actions.copyImage() })
                }
            }
        }
    }
    HorizontalDivider(color = Ui.line)
    if (actions.busy) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = Ui.accent)
}

// ------------------------------------------------------------------ Layout gallery

@Composable
private fun LayoutGallery(state: EditorState) {
    val doc = state.doc
    // Thumbnails depend on everything except the chosen layout.
    val thumbKey = doc.copy(layout = LayoutType.WHEEL)
    val thumbs by produceState<Map<LayoutType, ImageBitmap>>(emptyMap(), thumbKey) {
        delay(220)
        value = withContext(Dispatchers.Default) { renderThumbs(thumbKey) }
    }
    Column(Modifier.width(248.dp).fillMaxHeight().background(Ui.surface)) {
        Text("Layout", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 2.dp))
        Text("Same content, different shape", fontSize = 12.sp, color = Ui.muted,
            modifier = Modifier.padding(start = 16.dp, bottom = 10.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(LayoutType.entries.toList()) { lt ->
                val sel = lt == doc.layout
                Tip(lt.description) {
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .clickable { state.update { it.copy(layout = lt) } }
                            .padding(2.dp),
                    ) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1.15f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) Ui.accentSoft else Color(0xFFF8F9FB))
                                .border(if (sel) BorderStroke(2.dp, Ui.accent) else BorderStroke(1.dp, Ui.line), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            thumbs[lt]?.let { Image(it, lt.title, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                        }
                        Text(lt.title, fontSize = 12.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (sel) Ui.accent else Ui.text, modifier = Modifier.padding(top = 5.dp, start = 2.dp))
                    }
                }
            }
        }
    }
}

private fun renderThumbs(doc: ModelDoc): Map<LayoutType, ImageBitmap> = LayoutType.entries.associateWith { lt ->
    val d = doc.copy(layout = lt, style = doc.style.copy(showTitle = false, showLegend = false, shadows = false))
    val scene = Renderer.scene(d)
    val scale = 220f / max(scene.width, scene.height)
    Renderer.image(scene, scale).toComposeImageBitmap()
}

// ------------------------------------------------------------------ Canvas

@Composable
private fun CanvasArea(state: EditorState, modifier: Modifier) {
    val doc = state.doc
    var fit by remember { mutableStateOf(true) }
    var zoom by remember { mutableStateOf(1f) }
    val scene by produceState<Scene?>(null, doc) {
        value = withContext(Dispatchers.Default) { Renderer.scene(doc) }
    }
    val density = LocalDensity.current.density

    BoxWithConstraints(modifier.fillMaxHeight().background(Ui.canvas)) {
        val sc = scene
        if (sc == null) return@BoxWithConstraints
        val boxW = maxWidth
        val boxH = maxHeight
        val availW = maxWidth.value - 80f
        val availH = maxHeight.value - 110f
        val fitZoom = min(availW / sc.width, availH / sc.height).coerceIn(0.1f, 2f)
        val z = if (fit) fitZoom else zoom
        // Re-render only when the effective pixel scale changes noticeably.
        val renderScale = ((z * density).coerceIn(0.5f, 3f) * 4f).roundToInt() / 4f
        val bitmap by produceState<ImageBitmap?>(null, sc, renderScale) {
            value = withContext(Dispatchers.Default) { Renderer.image(sc, renderScale).toComposeImageBitmap() }
        }
        val vs = rememberScrollState()
        val hs = rememberScrollState()
        Box(Modifier.fillMaxSize().verticalScroll(vs).horizontalScroll(hs)) {
            Box(
                Modifier.defaultMinSize(minWidth = boxW, minHeight = boxH).padding(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size((sc.width * z).dp, (sc.height * z).dp)
                        .shadow(10.dp, RoundedCornerShape(4.dp))
                        .background(if (doc.style.transparentBackground) Color(0xFFF7F7F9) else Color.White),
                ) {
                    bitmap?.let {
                        Image(it, "Model preview", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    }
                }
            }
        }

        // Floating zoom controls
        Surface(
            shape = RoundedCornerShape(12.dp), color = Ui.surface, shadowElevation = 6.dp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
        ) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBtn(Icons.Outlined.ZoomOut, "Zoom out") { zoom = (z / 1.2f).coerceAtLeast(0.1f); fit = false }
                Text("${(z * 100).roundToInt()} %", fontSize = 13.sp, modifier = Modifier.width(52.dp).clickable {
                    zoom = 1f; fit = false
                }, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconBtn(Icons.Outlined.ZoomIn, "Zoom in") { zoom = (z * 1.2f).coerceAtMost(4f); fit = false }
                IconBtn(Icons.Outlined.FitScreen, "Fit to window", tint = if (fit) Ui.accent else Ui.text) { fit = true }
                VerticalDivider(Modifier.height(20.dp).padding(horizontal = 4.dp), color = Ui.line)
                Text("${sc.width.roundToInt()} × ${sc.height.roundToInt()}", fontSize = 12.sp, color = Ui.muted,
                    modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

// ------------------------------------------------------------------ Inspector

private enum class Tab(val title: String) { CONTENT("Content"), STYLE("Style"), EXPORT("Export") }

@Composable
private fun Inspector(state: EditorState, actions: Actions) {
    var tab by remember { mutableStateOf(Tab.CONTENT) }
    Column(Modifier.width(400.dp).fillMaxHeight().background(Ui.bg)) {
        Box(Modifier.background(Ui.surface).padding(14.dp)) {
            Segmented(Tab.entries.toList(), tab, { it.title }, { tab = it })
        }
        HorizontalDivider(color = Ui.line)
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.CONTENT -> ContentPanel(state)
                Tab.STYLE -> StylePanel(state)
                Tab.EXPORT -> ExportPanel(state, actions)
            }
        }
    }
}
