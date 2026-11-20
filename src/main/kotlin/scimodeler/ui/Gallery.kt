package scimodeler.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import scimodeler.core.Category
import scimodeler.core.FreeformOps
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenuItem
import scimodeler.ui.components.IconBtn
import scimodeler.ui.components.Segmented
import scimodeler.core.ModelDoc
import scimodeler.core.Models
import scimodeler.core.Renderer
import scimodeler.ui.components.Bitmaps
import scimodeler.ui.components.Chip
import scimodeler.ui.components.Field
import scimodeler.ui.components.GlassPanel
import scimodeler.ui.components.PreviewCard
import scimodeler.ui.theme.Tok
import kotlin.math.max

/** Live thumbnails of the user's own content in each model; static resources take precedence. */
private fun renderThumb(doc: ModelDoc, id: String): ImageBitmap {
    val ff = if (id == "freeform") doc.freeform ?: FreeformOps.fromItems(doc) else doc.freeform
    val d = doc.copy(layout = id, freeform = ff, style = doc.style.copy(showTitle = false, showLegend = false, shadows = false))
    val scene = Renderer.scene(d)
    return Renderer.image(scene, 240f / max(scene.width, scene.height)).toComposeImageBitmap()
}

@Composable
fun ModelGallery(state: EditorState, modifier: Modifier, actions: Actions, ffui: scimodeler.ui.freeform.FreeformUi) {
    val doc = state.doc
    val key = doc.copy(layout = "wheel")
    val thumbs by produceState<Map<String, ImageBitmap>>(emptyMap(), key) {
        delay(200)
        withContext(Dispatchers.Default) {
            for (spec in Models.all) {
                val img = Bitmaps.thumbnail(spec) ?: runCatching { renderThumb(key, spec.id) }.getOrNull() ?: continue
                value = value + (spec.id to img)
            }
        }
    }
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val visible = Models.all.filter { q.isEmpty() || it.title.lowercase().contains(q) || it.description.lowercase().contains(q) }

    val freeform = doc.spec.freeform
    val presetThumbs by produceState<Map<String, ImageBitmap>>(emptyMap(), actions.presets, doc.style.theme) {
        withContext(Dispatchers.Default) {
            actions.presets.forEach { p ->
                val d = p.doc.copy(style = p.doc.style.copy(theme = p.doc.style.theme))
                runCatching { renderThumb(d, d.layout) }.getOrNull()?.let { value = value + (p.id to it) }
            }
        }
    }
    GlassPanel(modifier) {
        Column(Modifier.fillMaxSize()) {
            if (freeform) {
                Box(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp)) {
                    Segmented(scimodeler.ui.freeform.LeftTab.entries.toList(), ffui.leftTab, { it.label }, { ffui.leftTab = it }, tagPrefix = "left")
                }
                Spacer(Modifier.height(10.dp))
                when (ffui.leftTab) {
                    scimodeler.ui.freeform.LeftTab.COMPONENTS -> { scimodeler.ui.freeform.ComponentLibrary(ffui, doc, actions); return@Column }
                    scimodeler.ui.freeform.LeftTab.LAYERS -> { scimodeler.ui.freeform.LayersPanel(ffui); return@Column }
                    else -> {}
                }
            }
            Row(Modifier.padding(start = 18.dp, end = 10.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Models", style = Tok.T.title, modifier = Modifier.weight(1f))
                Chip("${Models.all.size + actions.presets.size}")
                IconBtn(Icons.Outlined.BookmarkAdd, "Save current model to My models", { actions.requestSavePreset() }, size = 32.dp, tag = "gallery-save-preset")
            }
            Field(
                "", query, { query = it }, placeholder = "Search models",
                leading = { Icon(Icons.Outlined.Search, null, tint = Tok.C.text3, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), tag = "model-search",
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).testTag("model-gallery"),
            ) {
                // My models: the freeform builder and saved presets
                val presets = actions.presets.filter { q.isEmpty() || it.name.lowercase().contains(q) }
                item(key = "cat-custom", span = { GridItemSpan(maxLineSpan) }) {
                    Text(Category.CUSTOM.title.uppercase(), style = Tok.T.caption, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
                }
                visible.filter { it.category == Category.CUSTOM }.forEach { spec ->
                    item(key = spec.id) {
                        PreviewCard(
                            title = spec.title, description = spec.description, thumbnail = thumbs[spec.id],
                            selected = spec.id == doc.layout,
                            onClick = { state.update { it.copy(layout = spec.id, freeform = it.freeform ?: FreeformOps.fromItems(it)) } },
                            tag = "model-card-${spec.id}",
                        )
                    }
                }
                items(presets, key = { it.id }) { p ->
                    Box {
                        PreviewCard(
                            title = p.name, description = "Saved model · ${Models.get(p.doc.layout).title}", thumbnail = presetThumbs[p.id],
                            selected = false, onClick = { actions.openPreset(p) }, tag = "model-card-${p.id}",
                        )
                        var menu by remember(p.id) { mutableStateOf(false) }
                        Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                            IconBtn(Icons.Outlined.MoreHoriz, "More", { menu = true }, size = 26.dp, iconSize = 16.dp, tag = "preset-menu-${p.id}")
                            GlassMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text("Open", style = Tok.T.body) }, onClick = { menu = false; actions.openPreset(p) })
                                DropdownMenuItem(text = { Text("Rename…", style = Tok.T.body) }, onClick = { menu = false; actions.renamePreset = p })
                                DropdownMenuItem(text = { Text("Duplicate", style = Tok.T.body) }, onClick = { menu = false; actions.duplicatePreset(p) })
                                DropdownMenuItem(text = { Text("Delete", style = Tok.T.body.copy(color = Tok.C.danger)) }, onClick = { menu = false; actions.deletePreset(p) })
                            }
                        }
                    }
                }
                for (cat in Category.entries) {
                    if (cat == Category.CUSTOM) continue
                    val specs = visible.filter { it.category == cat }
                    if (specs.isEmpty()) continue
                    item(key = "cat-${cat.name}", span = { GridItemSpan(maxLineSpan) }) {
                        Text(cat.title.uppercase(), style = Tok.T.caption, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
                    }
                    items(specs, key = { it.id }) { spec ->
                        PreviewCard(
                            title = spec.title, description = spec.description, thumbnail = thumbs[spec.id],
                            selected = spec.id == doc.layout,
                            onClick = { state.update { it.copy(layout = spec.id) } },
                            tag = "model-card-${spec.id}",
                        )
                    }
                }
                if (visible.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("No models match “$query”.", style = Tok.T.small) }
                }
            }
        }
    }
}
