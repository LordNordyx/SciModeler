package scimodeler.core

enum class Category(val title: String) {
    CIRCULAR("Circular"),
    FLOW("Flow & process"),
    HIERARCHY("Hierarchy & funnels"),
    RELATIONS("Relations & frameworks"),
}

/**
 * Everything the app needs to know about one model type.
 *
 * To add a new model: write a `Ctx.myLayout(): Size` function and add ONE entry to [Models.all].
 * Optionally drop a thumbnail at `src/main/resources/thumbnails/<id>.png`; without one the
 * gallery renders a live preview of the user's own content.
 */
class ModelSpec(
    val id: String,
    val title: String,
    val description: String,
    val category: Category,
    val usesCenter: Boolean = true,
    val maxItems: Int = 12,
    /** Whether the layout shows item icons. */
    val usesIcons: Boolean = false,
    /** Whether items can be assigned to groups via [Role]. */
    val usesRoles: Boolean = false,
    /** Default captions for groups, editable per document. */
    val groups: List<String> = emptyList(),
    /** Optional classpath resource used as gallery thumbnail. */
    val thumbnail: String? = null,
    val render: Ctx.() -> Size,
)

object Models {
    val all: List<ModelSpec> = listOf(
        // ---- Circular
        ModelSpec("wheel", "Wheel", "Segments around a central concept", Category.CIRCULAR, usesIcons = true) { wheel() },
        ModelSpec("cycle", "Cycle", "Stages that repeat in a loop", Category.CIRCULAR, usesIcons = true) { cycle() },
        ModelSpec("hub", "Hub & spoke", "Factors linked to a core concept", Category.CIRCULAR, usesIcons = true) { hub() },
        ModelSpec("onion", "Concentric", "Nested levels, e.g. socio-ecological", Category.CIRCULAR) { onion() },
        ModelSpec("flower", "Petals", "Overlapping rings around a core", Category.CIRCULAR, usesIcons = true, maxItems = 8) { flower() },
        ModelSpec("venn", "Venn", "Overlapping concepts (2–4)", Category.CIRCULAR, maxItems = 4) { venn() },
        // ---- Flow
        ModelSpec("process", "Process", "Sequential steps, left to right", Category.FLOW) { process() },
        ModelSpec("milestones", "Milestones", "Chevron timeline with icons", Category.FLOW, usesIcons = true, maxItems = 8) { milestones() },
        ModelSpec("cards", "Card flow", "Stage cards with a feedback loop", Category.FLOW, usesIcons = true, maxItems = 6) { cardFlow() },
        ModelSpec("zigzag", "Zigzag", "Stepwise path, alternating sides", Category.FLOW, usesIcons = true, maxItems = 8) { zigzag() },
        ModelSpec("arc", "Arc list", "Stages along an arc beside a core", Category.FLOW, usesIcons = true, maxItems = 8) { arcList() },
        // ---- Hierarchy
        ModelSpec("pyramid", "Pyramid", "Hierarchy from top to base", Category.HIERARCHY, usesCenter = false) { pyramid() },
        ModelSpec("funnel", "Funnel", "Narrowing stages, top to bottom", Category.HIERARCHY, usesCenter = false, maxItems = 8) { funnel() },
        ModelSpec("cone", "Cone", "Layered wedge with side notes", Category.HIERARCHY, usesCenter = false, usesIcons = true, maxItems = 7) { cone() },
        ModelSpec("layers", "Layers", "Stacked levels with descriptions", Category.HIERARCHY, usesCenter = false, usesIcons = true) { layers() },
        ModelSpec("matrix", "Matrix", "Grid or 2×2 of categories", Category.HIERARCHY, usesIcons = true) { matrix() },
        // ---- Relations
        ModelSpec("branches", "Branches", "Core concept branching into a list", Category.RELATIONS, usesIcons = true, maxItems = 8) { branches() },
        ModelSpec("fanout", "Fan-out", "One source spreading into outcomes", Category.RELATIONS, usesIcons = true, maxItems = 8) { fanOut() },
        ModelSpec(
            "framework", "Framework", "Inputs → research question → outcomes", Category.RELATIONS,
            usesRoles = true, groups = listOf("Inputs", "Outcomes", "Context"),
        ) { framework() },
        ModelSpec(
            "variables", "Variables", "Independent variables, measures, dependent variable", Category.RELATIONS,
            usesRoles = true, groups = listOf("Independent variables", "Measures", "Dependent variable"),
        ) { variables() },
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): ModelSpec = byId[id.lowercase()] ?: all.first()

    fun exists(id: String) = byId.containsKey(id.lowercase())
}
