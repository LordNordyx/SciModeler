package scimodeler.core

import java.util.concurrent.atomic.AtomicLong

object Ids {
    private val counter = AtomicLong(System.currentTimeMillis())
    fun next(): Long = counter.incrementAndGet()
}

/** Where an item sits in models that distinguish groups (e.g. independent vs dependent variables). */
enum class Role(val label: String) {
    AUTO("Auto"),
    INPUT("Input"),
    OUTPUT("Output"),
    CONTEXT("Context"),
}

data class Item(
    val id: Long = Ids.next(),
    val label: String,
    val bullets: List<String> = emptyList(),
    /** ARGB colour override; null = take colour from the theme. */
    val color: Int? = null,
    /** Built-in icon id ([IconLibrary]); null = the default icon for this position. */
    val icon: String? = null,
    /** Custom icon as base64-encoded PNG; overrides [icon]. */
    val image: String? = null,
    val role: Role = Role.AUTO,
)

data class Style(
    val theme: String = "vivid",
    val fontFamily: String = Fonts.default,
    val textScale: Float = 1f,
    val showTitle: Boolean = true,
    val showCallouts: Boolean = true,
    val showLegend: Boolean = true,
    val showIcons: Boolean = true,
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
    /** Id of a [ModelSpec] in [Models]. */
    val layout: String = "wheel",
    /** Optional custom captions for item groups (see [ModelSpec.groups]). */
    val groupNames: List<String> = emptyList(),
    val style: Style = Style(),
) {
    val spec: ModelSpec get() = Models.get(layout)

    fun groupName(index: Int): String =
        groupNames.getOrNull(index)?.takeIf { it.isNotBlank() } ?: spec.groups.getOrElse(index) { "" }
}
