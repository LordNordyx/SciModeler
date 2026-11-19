package scimodeler.core

import java.util.concurrent.atomic.AtomicLong

enum class LayoutType(
    val title: String,
    val description: String,
    val usesCenter: Boolean,
    val maxItems: Int = 12,
) {
    WHEEL("Wheel", "Segments around a central concept", usesCenter = true),
    CYCLE("Cycle", "Stages that repeat in a loop", usesCenter = true),
    HUB("Hub & spoke", "Factors linked to a core concept", usesCenter = true),
    ONION("Concentric", "Nested levels, e.g. socio-ecological", usesCenter = true),
    PROCESS("Process", "Sequential steps, left to right", usesCenter = true),
    PYRAMID("Pyramid", "Hierarchy from top to base", usesCenter = false),
    VENN("Venn", "Overlapping concepts (2–4)", usesCenter = true, maxItems = 4),
    MATRIX("Matrix", "Grid or 2×2 of categories", usesCenter = true),
    LAYERS("Layers", "Stacked levels with descriptions", usesCenter = false),
}

object Ids {
    private val counter = AtomicLong(System.currentTimeMillis())
    fun next(): Long = counter.incrementAndGet()
}

data class Item(
    val id: Long = Ids.next(),
    val label: String,
    val bullets: List<String> = emptyList(),
    /** ARGB colour override; null = take colour from the theme. */
    val color: Int? = null,
)

data class Style(
    val theme: String = "vivid",
    val fontFamily: String = Fonts.default,
    val textScale: Float = 1f,
    val showTitle: Boolean = true,
    val showCallouts: Boolean = true,
    val showLegend: Boolean = true,
    val shadows: Boolean = true,
    val gradients: Boolean = true,
    /** Rotation in degrees for circular layouts. */
    val rotation: Float = 0f,
    val transparentBackground: Boolean = false,
)

data class ModelDoc(
    val title: String = "",
    val subtitle: String = "",
    val center: String = "",
    val items: List<Item> = emptyList(),
    val legend: String = "",
    val layout: LayoutType = LayoutType.WHEEL,
    val style: Style = Style(),
)
