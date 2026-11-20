package scimodeler.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import scimodeler.core.MotionPref
import scimodeler.core.Renderer
import scimodeler.core.SystemMotion
import scimodeler.core.Templates
import scimodeler.ui.components.AppButton
import scimodeler.ui.components.ButtonKind
import scimodeler.ui.components.Chip
import scimodeler.ui.components.GlassPanel
import scimodeler.ui.components.IconBtn
import scimodeler.ui.components.IconPicker
import scimodeler.ui.components.LocalHaze
import scimodeler.ui.components.PreviewCard
import scimodeler.ui.components.glass
import scimodeler.ui.theme.LocalMotion
import scimodeler.ui.theme.MotionSpec
import scimodeler.ui.theme.Tok
import kotlin.math.max

/** Interface-only state (which overlay is open, etc.). */
class UiState {
    var iconPickerFor by mutableStateOf<Long?>(null)
    var templatesOpen by mutableStateOf(false)

    /** Closes the top-most overlay; returns true if something was closed. */
    fun dismissTop(actions: Actions?): Boolean = when {
        actions?.pending != null -> { actions.pending = null; true }
        iconPickerFor != null -> { iconPickerFor = null; true }
        templatesOpen -> { templatesOpen = false; true }
        else -> false
    }
}

@Composable
fun AppRoot(state: EditorState, dialogs: Dialogs, ui: UiState, onReady: (Actions) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val actions = remember { Actions(state, dialogs, scope, snackbar) }
    LaunchedEffect(actions) { onReady(actions) }
    val reduced = when (actions.settings.motion) {
        MotionPref.SYSTEM -> SystemMotion.prefersReduced
        MotionPref.REDUCED -> true
        MotionPref.FULL -> false
    }
    val haze = remember { HazeState() }
    val canvas = remember { CanvasState() }

    // Autosave the session so the document (including custom icons) survives a restart.
    LaunchedEffect(state.doc, state.file, state.dirty) {
        delay(350)
        withContext(Dispatchers.IO) { actions.saveSession() }
    }

    CompositionLocalProvider(LocalMotion provides MotionSpec(!reduced), LocalHaze provides haze) {
        MaterialTheme(colorScheme = Tok.colorScheme, typography = Tok.typography) {
            Box(Modifier.fillMaxSize().background(Tok.C.canvasBrush)) {
                val g = Tok.S.gutter
                val panelTop = g + Tok.Layout.topBar + g
                CanvasView(
                    state, canvas,
                    Modifier.fillMaxSize().hazeSource(haze),
                    padStart = g + Tok.Layout.gallery, padEnd = g + Tok.Layout.inspector, padTop = panelTop,
                )
                TopBar(state, actions, ui, Modifier.align(Alignment.TopCenter).padding(g).fillMaxWidth().height(Tok.Layout.topBar))
                ModelGallery(
                    state,
                    Modifier.align(Alignment.TopStart).padding(start = g, top = panelTop, bottom = g)
                        .width(Tok.Layout.gallery).fillMaxHeight(),
                )
                Inspector(
                    state, actions, ui,
                    Modifier.align(Alignment.TopEnd).padding(end = g, top = panelTop, bottom = g)
                        .width(Tok.Layout.inspector).fillMaxHeight(),
                )
                ZoomBar(
                    canvas,
                    Modifier.align(Alignment.BottomCenter)
                        .padding(start = g + Tok.Layout.gallery, end = g + Tok.Layout.inspector, bottom = 20.dp),
                )
                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp),
                ) { data ->
                    Snackbar(
                        data, shape = RoundedCornerShape(Tok.R.lg), containerColor = Tok.C.tooltip,
                        contentColor = Color.White, actionColor = Color(0xFFA5B4FC),
                    )
                }

                Overlay(ui.templatesOpen, onDismiss = { ui.templatesOpen = false }) {
                    TemplateGallery(onPick = { t -> ui.templatesOpen = false; actions.guard { actions.newFrom(t) } }, onClose = { ui.templatesOpen = false })
                }
                val pickId = ui.iconPickerFor
                Overlay(pickId != null, onDismiss = { ui.iconPickerFor = null }) {
                    val idx = pickId?.let { state.index(it) } ?: -1
                    if (pickId != null && idx >= 0) {
                        IconPicker(
                            doc = state.doc, itemIndex = idx, recentImages = actions.recentImages,
                            onPickIcon = { actions.setIcon(pickId, it) },
                            onChooseImage = { actions.chooseImage(pickId) },
                            onPickImage = { actions.setImage(pickId, it) },
                            onRestoreDefault = { actions.restoreDefaultIcon(pickId) },
                            onClose = { ui.iconPickerFor = null },
                        )
                    }
                }
                val next = actions.pending
                Overlay(next != null, onDismiss = { actions.pending = null }) {
                    Column(Modifier.width(420.dp).padding(24.dp)) {
                        Text("Save changes to “${state.displayName}”?", style = Tok.T.display)
                        Spacer(Modifier.height(8.dp))
                        Text("Your changes will be lost if you don't save them.", style = Tok.T.body.copy(color = Tok.C.text2))
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppButton("Don't save", { actions.pending = null; next?.invoke() }, kind = ButtonKind.GHOST)
                            Spacer(Modifier.weight(1f))
                            AppButton("Cancel", { actions.pending = null })
                            AppButton("Save", { actions.pending = null; if (actions.save()) next?.invoke() }, kind = ButtonKind.PRIMARY)
                        }
                    }
                }
            }
        }
    }
}

/** Modal overlay: dimmed backdrop and a frosted card that pops in. */
@Composable
fun Overlay(visible: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    AnimatedVisibility(visible, enter = motion.fade(), exit = motion.fadeOutT()) {
        Box(
            Modifier.fillMaxSize().background(Tok.C.scrim)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.animateEnterExit(enter = motion.popIn(), exit = motion.popOut())
                    .glass(RoundedCornerShape(26.dp), Tok.C.glassStrong, elevation = 30.dp)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
            ) { content() }
        }
    }
}

// ------------------------------------------------------------------ Top bar

@Composable
private fun TopBar(state: EditorState, actions: Actions, ui: UiState, modifier: Modifier) {
    val motion = LocalMotion.current
    GlassPanel(modifier, RoundedCornerShape(Tok.R.xl)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF06B6D4)))),
                contentAlignment = Alignment.Center,
            ) { Text("S", style = Tok.T.title.copy(color = Color.White, fontWeight = FontWeight.Bold)) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("SciModeler", style = Tok.T.tiny.copy(color = Tok.C.text3))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.displayName, style = Tok.T.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    AnimatedVisibility(state.dirty, enter = motion.popIn(), exit = motion.popOut()) {
                        Chip("Edited", Modifier.padding(start = 8.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            IconBtn(Icons.AutoMirrored.Outlined.Undo, "Undo", { state.undo() }, enabled = state.canUndo, tag = "undo")
            IconBtn(Icons.AutoMirrored.Outlined.Redo, "Redo", { state.redo() }, enabled = state.canRedo, tag = "redo")
            VerticalDivider(Modifier.height(22.dp).padding(horizontal = 8.dp), color = Tok.C.line)
            IconBtn(Icons.Outlined.Add, "New from template", { ui.templatesOpen = true }, tag = "new")
            IconBtn(Icons.Outlined.FolderOpen, "Open…", { actions.guard { actions.open() } })
            IconBtn(Icons.Outlined.Save, "Save", { actions.save() })
            var settingsMenu by remember { mutableStateOf(false) }
            Box {
                IconBtn(Icons.Outlined.Settings, "Settings", { settingsMenu = true }, active = settingsMenu, tag = "settings")
                GlassMenu(settingsMenu, { settingsMenu = false }) {
                    Text("Motion", style = Tok.T.caption, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    MotionPref.entries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.label, style = Tok.T.body) },
                            leadingIcon = {
                                RadioButton(actions.settings.motion == p, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Tok.C.accent))
                            },
                            onClick = { actions.updateSettings { it.copy(motion = p) }; settingsMenu = false },
                            modifier = Modifier.testTag("motion-${p.name.lowercase()}"),
                        )
                    }
                    HorizontalDivider(color = Tok.C.line, modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(text = { Text("Open data folder", style = Tok.T.body) }, onClick = { settingsMenu = false; actions.openDataFolder() })
                }
            }
            Spacer(Modifier.width(8.dp))
            var exportMenu by remember { mutableStateOf(false) }
            Box {
                AppButton("Export", { exportMenu = true }, kind = ButtonKind.PRIMARY, icon = Icons.Outlined.FileDownload, tag = "export")
                GlassMenu(exportMenu, { exportMenu = false }) {
                    DropdownMenuItem(text = { Text("PNG image", style = Tok.T.body) }, leadingIcon = { Icon(Icons.Outlined.Image, null) }, onClick = { exportMenu = false; actions.exportPng() })
                    DropdownMenuItem(text = { Text("SVG vector", style = Tok.T.body) }, leadingIcon = { Icon(Icons.Outlined.Code, null) }, onClick = { exportMenu = false; actions.exportSvg() })
                    HorizontalDivider(color = Tok.C.line, modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(text = { Text("Copy to clipboard", style = Tok.T.body) }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { exportMenu = false; actions.copyImage() })
                }
            }
        }
        if (actions.busy) {
            LinearProgressIndicator(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp), color = Tok.C.accent, trackColor = Color.Transparent)
        }
    }
}

/** Dropdown menu styled as frosted glass. */
@Composable
fun GlassMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Tok.R.lg), containerColor = Tok.C.glassStrong,
        shadowElevation = 16.dp, tonalElevation = 0.dp,
    ) { content() }
}

// ------------------------------------------------------------------ Template gallery

@Composable
private fun TemplateGallery(onPick: (scimodeler.core.Template) -> Unit, onClose: () -> Unit) {
    val thumbs by produceState<Map<String, ImageBitmap>>(emptyMap()) {
        withContext(Dispatchers.Default) {
            Templates.all.forEach { t ->
                val d = t.doc().let { it.copy(style = it.style.copy(showTitle = false, showLegend = false, shadows = false)) }
                val sc = Renderer.scene(d)
                val bmp = Renderer.image(sc, 300f / max(sc.width, sc.height)).toComposeImageBitmap()
                value = value + (t.name to bmp)
            }
        }
    }
    Column(Modifier.width(920.dp).height(640.dp).padding(24.dp).testTag("template-gallery")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Start from a template", style = Tok.T.display)
                Text("Every template can switch to any layout afterwards.", style = Tok.T.small)
            }
            IconBtn(Icons.Outlined.Close, "Close (Esc)", onClose, tag = "templates-close")
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(196.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(Templates.all, key = { it.name }) { t ->
                PreviewCard(
                    title = t.name, description = t.description, thumbnail = thumbs[t.name], selected = false,
                    onClick = { onPick(t) }, tag = "template-${t.name.lowercase().replace(' ', '-')}",
                )
            }
        }
    }
}
