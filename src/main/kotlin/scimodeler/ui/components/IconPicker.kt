package scimodeler.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import scimodeler.core.IconDef
import scimodeler.core.IconLibrary
import scimodeler.core.ModelDoc
import scimodeler.core.colorOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.toComposeImageBitmap
import scimodeler.ui.theme.LocalMotion
import scimodeler.ui.theme.Tok
import kotlin.math.roundToInt

/**
 * Icon chooser for one item: built-in icon grid with search, custom image import with a live
 * preview of how the node will look, previously imported images, and "restore default".
 */
@Composable
fun IconPicker(
    doc: ModelDoc,
    itemIndex: Int,
    recentImages: List<String>,
    onPickIcon: (String) -> Unit,
    onChooseImage: () -> Unit,
    onPickImage: (String) -> Unit,
    onRestoreDefault: () -> Unit,
    onClose: () -> Unit,
) {
    val item = doc.items.getOrNull(itemIndex) ?: return
    val density = LocalDensity.current.density
    val key = Triple(item.icon, item.image?.hashCode(), doc.colorOf(itemIndex))
    IconChooser(
        title = "Icon", subtitle = item.label.ifBlank { "Item ${itemIndex + 1}" },
        icon = item.icon, image = item.image, defaultIcon = IconLibrary.defaultFor(itemIndex),
        recentImages = recentImages,
        onPickIcon = onPickIcon, onChooseImage = onChooseImage, onPickImage = onPickImage, onRestoreDefault = onRestoreDefault, onClose = onClose,
    ) {
        listOf(true to 84.dp, false to 84.dp, true to 30.dp).forEach { (outlined, size) ->
            val px = (size.value * density).roundToInt()
            val bmp = remember(key, outlined, px, doc.style.theme, doc.style.gradients) { Bitmaps.node(doc, itemIndex, outlined, px) }
            PreviewSlot(bmp, size, if (size < 40.dp) "Small" else if (outlined) "Outlined" else "Filled")
        }
    }
}

@Composable
private fun PreviewSlot(bmp: androidx.compose.ui.graphics.ImageBitmap, size: Dp, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 18.dp)) {
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Image(bmp, null, Modifier.size(size), contentScale = ContentScale.Fit)
        }
        Text(label, style = Tok.T.tiny)
    }
}

/** Icon chooser for a freeform element, previewing the element itself. */
@Composable
fun ElementIconPicker(
    doc: ModelDoc,
    element: scimodeler.core.FfElement,
    recentImages: List<String>,
    onPickIcon: (String) -> Unit,
    onChooseImage: () -> Unit,
    onPickImage: (String) -> Unit,
    onRestoreDefault: () -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current.density
    IconChooser(
        title = "Icon", subtitle = element.displayName, icon = element.icon, image = element.image,
        defaultIcon = IconLibrary.defaultFor(0), recentImages = recentImages,
        onPickIcon = onPickIcon, onChooseImage = onChooseImage, onPickImage = onPickImage, onRestoreDefault = onRestoreDefault, onClose = onClose,
    ) {
        val px = (150 * density).roundToInt()
        val bmp = remember(element, doc.style) {
            scimodeler.core.ElementRaster.render(doc, element, px).toComposeImageBitmap()
        }
        Box(Modifier.size(width = 260.dp, height = 110.dp), contentAlignment = Alignment.Center) {
            Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

/**
 * Shared icon chooser: built-in icon grid with search, custom image import with a live
 * preview, previously imported images, and "restore default".
 */
@Composable
fun IconChooser(
    title: String,
    subtitle: String,
    icon: String?,
    image: String?,
    defaultIcon: IconDef,
    recentImages: List<String>,
    onPickIcon: (String) -> Unit,
    onChooseImage: () -> Unit,
    onPickImage: (String) -> Unit,
    onRestoreDefault: () -> Unit,
    onClose: () -> Unit,
    previews: @Composable () -> Unit,
) {
    val current: IconDef = IconLibrary.get(icon) ?: defaultIcon
    val isDefault = icon == null && image == null
    var query by remember { mutableStateOf("") }
    val results = remember(query) { IconLibrary.search(query) }

    Column(Modifier.width(660.dp).height(640.dp).padding(22.dp).testTag("icon-picker")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = Tok.T.display)
                Text(subtitle, style = Tok.T.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconBtn(Icons.Outlined.Close, "Close (Esc)", onClose, tag = "icon-close")
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Tok.R.xl)).background(Tok.C.surface)
                .border(1.dp, Tok.C.line, RoundedCornerShape(Tok.R.xl)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            previews()
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton("Choose image…", onChooseImage, icon = Icons.Outlined.FileUpload, kind = ButtonKind.PRIMARY, tag = "icon-choose-image")
                AppButton("Restore default", onRestoreDefault, icon = Icons.Outlined.RestartAlt, kind = ButtonKind.GHOST, enabled = !isDefault, tag = "icon-restore-default")
                Text(
                    when { image != null -> "Custom image"; icon != null -> current.name; else -> "Default · ${current.name}" },
                    style = Tok.T.tiny,
                )
            }
        }

        if (recentImages.isNotEmpty()) {
            SectionHeader("Your images")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentImages) { b64 ->
                    Tile(selected = image == b64, size = 52.dp, tip = "Use this image", onClick = { onPickImage(b64) }) {
                        IconGlyph(null, b64, Color.Black, 34.dp)
                    }
                }
            }
        }

        SectionHeader("Built-in icons")
        Field(
            "", query, { query = it }, placeholder = "Search icons (e.g. team, data, care)",
            leading = { Icon(Icons.Outlined.Search, null, tint = Tok.C.text3, modifier = Modifier.size(18.dp)) },
            tag = "icon-search",
        )
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(76.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(results, key = { it.id }) { def ->
                val sel = image == null && def.id == current.id
                Tile(selected = sel, size = 76.dp, tip = def.name, tag = "icon-${def.id}", onClick = { onPickIcon(def.id) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconGlyph(def, null, if (sel) Tok.C.accent else Tok.C.text, 26.dp)
                        Spacer(Modifier.height(5.dp))
                        Text(def.name, style = Tok.T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (results.isEmpty()) Text("No icons match “$query”.", style = Tok.T.small)
    }
}

@Composable
private fun Tile(selected: Boolean, size: Dp, tip: String, onClick: () -> Unit, tag: String? = null, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    val src = remember { MutableInteractionSource() }
    val st = src.states()
    val shape = RoundedCornerShape(Tok.R.lg)
    val bg by animateColorAsState(
        when { selected -> Tok.C.accentSoft; st.pressed -> Tok.C.pressed; st.hovered -> Color.White; else -> Tok.C.surface },
        motion.tween(Tok.Motion.fast),
    )
    val border by animateColorAsState(if (selected) Tok.C.accent else if (st.hovered) Tok.C.lineStrong else Tok.C.line, motion.tween(Tok.Motion.fast))
    val scale by animateFloatAsState(if (st.pressed) 0.94f else 1f, motion.spring())
    Tip(tip) {
        Box(
            Modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier)
                .size(size).scale(scale).clip(shape).background(bg)
                .border(if (selected) 2.dp else 1.dp, border, shape)
                .focusRing(st.focused, shape, 3.dp)
                .pressable(src, role = Role.RadioButton, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
