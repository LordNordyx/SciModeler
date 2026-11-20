package scimodeler.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.Dp
import scimodeler.core.IconDef
import scimodeler.core.IconRaster
import scimodeler.core.ModelDoc
import scimodeler.core.ModelSpec
import kotlin.math.roundToInt

/** Small LRU cache for rendered UI bitmaps. */
object Bitmaps {
    private val cache = object : LinkedHashMap<String, ImageBitmap>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 400
    }

    @Synchronized
    fun glyph(def: IconDef?, base64: String?, color: Int, px: Int): ImageBitmap =
        cache.getOrPut("g|${def?.id}|${base64?.hashCode()}|${base64?.length}|$color|$px") {
            IconRaster.glyph(def, base64, color, px).toComposeImageBitmap()
        }

    @Synchronized
    fun resource(path: String): ImageBitmap? = cache[path] ?: runCatching {
        Bitmaps::class.java.classLoader.getResourceAsStream(path)?.use { loadImageBitmap(it) }
    }.getOrNull()?.also { cache[path] = it }

    /** Optional static thumbnail for a model: the spec's own path, or thumbnails/<id>.png by convention. */
    fun thumbnail(spec: ModelSpec): ImageBitmap? = spec.thumbnail?.let(::resource) ?: resource("thumbnails/${spec.id}.png")

    fun node(doc: ModelDoc, index: Int, outlined: Boolean, px: Int): ImageBitmap =
        IconRaster.node(doc, index, outlined, px).toComposeImageBitmap()
}

/** A built-in icon or custom image, sharp at any display density. */
@Composable
fun IconGlyph(def: IconDef?, base64: String?, color: Color, size: Dp, modifier: Modifier = Modifier) {
    val px = (size.value * LocalDensity.current.density).roundToInt().coerceAtLeast(8)
    val argb = color.toArgb()
    val bmp = remember(def?.id, base64, argb, px) { Bitmaps.glyph(def, base64, argb, px) }
    Image(bmp, def?.name ?: "Custom icon", modifier.size(size), contentScale = ContentScale.Fit)
}
