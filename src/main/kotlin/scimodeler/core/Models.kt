package scimodeler.core

enum class Category(val title: String) {
    CUSTOM("My models"),
    CIRCULAR("Circular"),
    FLOW("Flow & process"),
    HIERARCHY("Hierarchy & funnels"),
    RELATIONS("Relations & frameworks"),
    RESEARCH("Research designs"),
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
    /** Roles the user can assign to items (empty = roles not used), with their labels. */
    val roles: List<Pair<Role, String>> = emptyList(),
    /** Roles given to items left on AUTO, by position (n = item count). */
    val autoRoles: ((Int) -> List<Role>)? = null,
    /** Role for AUTO items once the user has assigned roles explicitly. */
    val autoFallback: Role = Role.INPUT,
    /** Default captions for groups, editable per document. */
    val groups: List<String> = emptyList(),
    /** Items can be nested under each other (tree). */
    val hierarchical: Boolean = false,
    /** Model is drawn from freeform elements instead of items. */
    val freeform: Boolean = false,
    /** Colour of an item that has no explicit colour (null = theme palette by position). */
    val autoColor: ((ModelDoc, Theme, Int) -> Int?)? = null,
    /** Optional classpath resource used as gallery thumbnail. */
    val thumbnail: String? = null,
    val render: Ctx.() -> Size,
) {
    val usesRoles: Boolean get() = roles.isNotEmpty()
    fun roleLabel(r: Role): String = roles.firstOrNull { it.first == r }?.second ?: r.label
}

private val halfSplit: (Int) -> List<Role> = { n -> List(n) { if (it < (n + 1) / 2) Role.INPUT else Role.OUTPUT } }
private val mediationSplit: (Int) -> List<Role> = { n ->
    List(n) { when (it) { 0 -> Role.INPUT; 1 -> Role.OUTPUT; 3 -> Role.MODERATOR; else -> Role.MEDIATOR } }
}

/** Colours by group so that a group reads as one unit. */
private fun byRole(input: Int, output: Int?, mediator: Int, moderator: Int, context: Int): (ModelDoc, Theme, Int) -> Int? = { doc, t, i ->
    when (doc.resolvedRoles().getOrNull(i)) {
        Role.INPUT -> t.slot(input)
        Role.OUTPUT -> output?.let { t.slot(it) } ?: t.hub
        Role.MEDIATOR -> t.slot(mediator)
        Role.MODERATOR -> t.slot(moderator)
        Role.CONTEXT -> t.slot(context)
        else -> null
    }
}

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
        ModelSpec("tree", "Tree", "Hierarchy of categories with examples", Category.HIERARCHY, maxItems = 24, hierarchical = true,
            autoColor = { doc, t, i -> t.slot(doc.topLevelIndex(i)) }) { tree() },
        ModelSpec("topics", "Topic map", "Main topics with sub-topic lists", Category.HIERARCHY, usesIcons = true, maxItems = 6) { topicMap() },
        ModelSpec("pyramid", "Pyramid", "Hierarchy from top to base", Category.HIERARCHY, usesCenter = false) { pyramid() },
        ModelSpec("funnel", "Funnel", "Narrowing stages, top to bottom", Category.HIERARCHY, usesCenter = false, maxItems = 8) { funnel() },
        ModelSpec("cone", "Cone", "Layered wedge with side notes", Category.HIERARCHY, usesCenter = false, usesIcons = true, maxItems = 7) { cone() },
        ModelSpec("layers", "Layers", "Stacked levels with descriptions", Category.HIERARCHY, usesCenter = false, usesIcons = true) { layers() },
        ModelSpec("matrix", "Matrix", "Grid or 2×2 of categories", Category.HIERARCHY, usesIcons = true) { matrix() },
        // ---- Relations
        ModelSpec("branches", "Branches", "Core concept branching into a list", Category.RELATIONS, usesIcons = true, maxItems = 8) { branches() },
        ModelSpec("fanout", "Fan-out", "One source spreading into outcomes", Category.RELATIONS, usesIcons = true, maxItems = 8) { fanOut() },
        // ---- Research designs
        ModelSpec(
            "framework", "Framework", "Inputs → research question → outcomes", Category.RESEARCH,
            roles = listOf(Role.INPUT to "Input", Role.OUTPUT to "Outcome", Role.CONTEXT to "Context"),
            autoRoles = halfSplit, groups = listOf("Inputs", "Outcomes", "Context"),
            autoColor = byRole(0, null, 2, 3, 1),
        ) { framework() },
        ModelSpec(
            "variables", "Variables", "Independent variables, measures, dependent variable", Category.RESEARCH,
            roles = listOf(Role.INPUT to "Independent", Role.OUTPUT to "Measure"),
            autoRoles = halfSplit, groups = listOf("Independent variables", "Measures", "Dependent variable"),
        ) { variables() },
        ModelSpec(
            "mediation", "Mediation", "Independent → dependent with mediator and moderator", Category.RESEARCH, usesCenter = false,
            roles = listOf(Role.INPUT to "Independent", Role.OUTPUT to "Dependent", Role.MEDIATOR to "Mediator", Role.MODERATOR to "Moderator"),
            autoRoles = mediationSplit, autoFallback = Role.MEDIATOR, maxItems = 8,
            autoColor = byRole(0, 5, 2, 3, 4),
        ) { mediation() },
        ModelSpec(
            "path", "Path model", "Hypothesised paths (H1, H2 …) between variables", Category.RESEARCH, usesCenter = false, usesIcons = true,
            roles = listOf(Role.INPUT to "Independent", Role.OUTPUT to "Dependent", Role.MEDIATOR to "Mediator", Role.MODERATOR to "Moderator"),
            autoRoles = mediationSplit, autoFallback = Role.MEDIATOR, maxItems = 8,
            autoColor = byRole(7, 0, 2, 7, 4),
        ) { pathModel() },
        // ---- Custom
        ModelSpec(
            "freeform", "Freeform", "Build your own model from shapes, icons and arrows", Category.CUSTOM,
            usesCenter = false, usesIcons = true, freeform = true, maxItems = 0,
        ) { freeformLayout() },
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): ModelSpec = byId[id.lowercase()] ?: all.first()

    fun exists(id: String) = byId.containsKey(id.lowercase())
}

/** Role of every item after resolving AUTO according to the current model. */
fun ModelDoc.resolvedRoles(): List<Role> {
    val s = spec
    val auto = s.autoRoles ?: return items.map { if (it.role == Role.AUTO) Role.INPUT else it.role }
    val explicit = items.any { it.role != Role.AUTO }
    val byPos = auto(items.size)
    return items.mapIndexed { i, it -> if (it.role != Role.AUTO) it.role else if (explicit) s.autoFallback else byPos[i] }
}

/** Index of the item's top-level ancestor among top-level items (for tree colouring). */
fun ModelDoc.topLevelIndex(i: Int): Int {
    val byId = items.associateBy { it.id }
    var cur = items.getOrNull(i) ?: return i
    var guard = 0
    while (cur.parent != null && byId[cur.parent] != null && guard++ < 64) cur = byId.getValue(cur.parent!!)
    val roots = items.filter { it.parent == null || byId[it.parent] == null }
    return roots.indexOf(cur).coerceAtLeast(0)
}
