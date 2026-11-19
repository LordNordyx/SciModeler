package scimodeler.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import scimodeler.core.Item
import scimodeler.core.Themes
import scimodeler.core.hex

private val extraColors = listOf(
    "#0F172A", "#475569", "#94A3B8", "#DC2626", "#EA580C", "#CA8A04",
    "#16A34A", "#0D9488", "#0284C7", "#4F46E5", "#9333EA", "#DB2777",
).map(::hex)

@Composable
fun ContentPanel(state: EditorState) {
    val doc = state.doc
    val theme = Themes.get(doc.style.theme)
    LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
        item {
            SectionLabel("Text")
            Field("Title", doc.title, { v -> state.update("title") { it.copy(title = v) } })
            Spacer(Modifier.height(8.dp))
            Field("Subtitle", doc.subtitle, { v -> state.update("subtitle") { it.copy(subtitle = v) } },
                placeholder = "Optional")
            Spacer(Modifier.height(8.dp))
            Field(
                "Central concept", doc.center, { v -> state.update("center") { it.copy(center = v) } },
                singleLine = false,
                hint = if (!doc.layout.usesCenter) "Not shown in the ${doc.layout.title} layout" else null,
            )
        }
        item {
            SectionLabel("Items · ${doc.items.size}") {
                TextButton(onClick = {
                    state.update { d -> d.copy(items = d.items + Item(label = "New item")) }
                }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add item", fontSize = 13.sp)
                }
            }
            if (doc.items.size > doc.layout.maxItems) {
                Text(
                    "The ${doc.layout.title} layout shows the first ${doc.layout.maxItems} items.",
                    fontSize = 12.sp, color = Ui.muted, modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        itemsIndexed(doc.items, key = { _, it -> it.id }) { index, item ->
            ItemCard(
                state = state, item = item, index = index, count = doc.items.size,
                autoColor = theme.colors[index % theme.colors.size], themeColors = theme.colors,
            )
            Spacer(Modifier.height(10.dp))
        }
        item {
            SectionLabel("Footnote")
            Field(
                "Legend / source", doc.legend, { v -> state.update("legend") { it.copy(legend = v) } },
                singleLine = false, minLines = 2,
                placeholder = "e.g. S – collaboration, O – responsibility … Source: Author (2025)",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ItemCard(
    state: EditorState, item: Item, index: Int, count: Int, autoColor: Int, themeColors: List<Int>,
) {
    var pickColor by remember { mutableStateOf(false) }
    val color = item.color ?: autoColor
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Ui.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, Ui.line, RoundedCornerShape(14.dp)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Tip("Change colour") { Swatch(argbColor(color), 24) { pickColor = true } }
                    DropdownMenu(expanded = pickColor, onDismissRequest = { pickColor = false }) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Theme", fontSize = 11.sp, color = Ui.muted)
                            Spacer(Modifier.height(6.dp))
                            ColorGrid(themeColors, item.color) {
                                state.updateItem(item.id) { i -> i.copy(color = it) }; pickColor = false
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("More", fontSize = 11.sp, color = Ui.muted)
                            Spacer(Modifier.height(6.dp))
                            ColorGrid(extraColors, item.color) {
                                state.updateItem(item.id) { i -> i.copy(color = it) }; pickColor = false
                            }
                            Spacer(Modifier.height(6.dp))
                            TextButton(onClick = {
                                state.updateItem(item.id) { it.copy(color = null) }; pickColor = false
                            }) { Text("Use theme colour automatically", fontSize = 12.sp) }
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("${index + 1}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ui.muted)
                Spacer(Modifier.weight(1f))
                SmallIconBtn(Icons.Outlined.KeyboardArrowUp, "Move up", enabled = index > 0) { state.moveItem(index, -1) }
                SmallIconBtn(Icons.Outlined.KeyboardArrowDown, "Move down", enabled = index < count - 1) { state.moveItem(index, 1) }
                SmallIconBtn(Icons.Outlined.ContentCopy, "Duplicate") {
                    state.update { d ->
                        val list = d.items.toMutableList()
                        list.add(index + 1, item.copy(id = scimodeler.core.Ids.next()))
                        d.copy(items = list)
                    }
                }
                SmallIconBtn(Icons.Outlined.Delete, "Delete") {
                    state.update { d -> d.copy(items = d.items.filterNot { it.id == item.id }) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Field("Label", item.label, { v -> state.updateItem(item.id, "label") { it.copy(label = v) } }, singleLine = false)
            Spacer(Modifier.height(8.dp))
            Field(
                "Description points", item.bullets.joinToString("\n"),
                { v -> state.updateItem(item.id, "bullets") { it.copy(bullets = v.split('\n')) } },
                singleLine = false, minLines = 2, placeholder = "One point per line",
            )
        }
    }
}

@Composable
private fun ColorGrid(colors: List<Int>, selected: Int?, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c -> Swatch(argbColor(c), 24, selected = c == selected) { onPick(c) } }
            }
        }
    }
}

fun EditorState.moveItem(index: Int, delta: Int) = update { d ->
    val list = d.items.toMutableList()
    val target = index + delta
    if (target !in list.indices) return@update d
    val it = list.removeAt(index)
    list.add(target, it)
    d.copy(items = list)
}
