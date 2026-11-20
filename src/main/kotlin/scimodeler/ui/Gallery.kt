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
    val d = doc.copy(layout = id, style = doc.style.copy(showTitle = false, showLegend = false, shadows = false))
    val scene = Renderer.scene(d)
    return Renderer.image(scene, 240f / max(scene.width, scene.height)).toComposeImageBitmap()
}

@Composable
fun ModelGallery(state: EditorState, modifier: Modifier) {
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

    GlassPanel(modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(start = 18.dp, end = 14.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Models", style = Tok.T.title, modifier = Modifier.weight(1f))
                Chip("${Models.all.size}")
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
                for (cat in Category.entries) {
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
