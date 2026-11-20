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
    MEDIATOR("Mediator"),
    MODERATOR("Moderator"),
}

data class Item(
    val id: Long = Ids.next(),
    val label: String,
    val bullets: List<String> = emptyList(),
    /** Custom ARGB colour; overrides everything else. */
    val color: Int? = null,
    /** Theme palette slot; the item follows theme changes. Null with no [color] = automatic. */
    val slot: Int? = null,
    /** Built-in icon id ([IconLibrary]); null = the default icon for this position. */
    val icon: String? = null,
    /** Custom icon as base64-encoded PNG; overrides [icon]. */
    val image: String? = null,
    val role: Role = Role.AUTO,
    /** Parent item for hierarchical models (tree); null = top level. */
    val parent: Long? = null,
)

data class Style(
    val theme: String = "vivid",
    val fontFamily: String = Fonts.default,
    val textScale: Float = 1f,
    val showTitle: Boolean = true,
    val showCallouts: Boolean = true,
    val showLegend: Boolean = true,
    val showIcons: Boolean = true,
    /** Labels on arrows, e.g. hypotheses H1, H2 … in path models. */
    val showLabels: Boolean = true,
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
    /** Content of a freeform (custom-built) model. */
    val freeform: Freeform? = null,
) {
    val spec: ModelSpec get() = Models.get(layout)

    fun groupName(index: Int): String =
        groupNames.getOrNull(index)?.takeIf { it.isNotBlank() } ?: spec.groups.getOrElse(index) { "" }
}
