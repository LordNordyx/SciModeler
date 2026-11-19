package scimodeler.core

import kotlin.math.max

/** Drawing context shared by all layouts. Coordinates are in "points" at 1× scale. */
class Ctx(val doc: ModelDoc, val m: Measure) {
    val style = doc.style
    val theme = Themes.get(style.theme)
    val s = style.textScale.coerceIn(0.6f, 1.8f)
    val items: List<Item> = doc.items.take(doc.layout.maxItems)
    var prims = ArrayList<Prim>()

    fun color(i: Int): Int = items[i].color ?: theme.colors[i % theme.colors.size]

    fun add(p: Prim) { prims += p }

    fun capture(block: () -> Unit): List<Prim> {
        val saved = prims
        prims = ArrayList()
        block()
        val out = prims
        prims = saved
        return out
    }

    fun place(list: List<Prim>, dx: Float, dy: Float) { list.forEach { prims += it.translated(dx, dy) } }

    /** Colour fill, optionally as a soft gradient from (x1,y1) to (x2,y2). */
    fun fill(c: Int, x1: Float, y1: Float, x2: Float, y2: Float): Paint =
        if (style.gradients) Paint.Linear(x1, y1, x2, y2, lighten(c, 0.14f), darken(c, 0.10f)) else Paint.Solid(c)

    fun shadow(path: PathData, strength: Float = 1f) {
        if (!style.shadows) return
        val p = path.translated(0f, 4f)
        add(Prim.Shape(p, stroke = withAlpha(0xFF0F172A.toInt(), 0.035f * strength), strokeWidth = 16f))
        add(Prim.Shape(p, stroke = withAlpha(0xFF0F172A.toInt(), 0.05f * strength), strokeWidth = 8f))
        add(Prim.Shape(p, fill = Paint.Solid(withAlpha(0xFF0F172A.toInt(), 0.08f * strength)),
            stroke = withAlpha(0xFF0F172A.toInt(), 0.06f * strength), strokeWidth = 2f))
    }

    fun shape(path: PathData, fill: Paint?, stroke: Int? = null, width: Float = 0f, dash: List<Float>? = null) =
        add(Prim.Shape(path, fill, stroke, width, dash))

    fun text(
        lines: List<String>, x: Float, y: Float, size: Float, color: Int,
        bold: Boolean = false, align: Align = Align.CENTER, vCenter: Boolean = true, rotation: Float = 0f,
    ) = add(Prim.Text(lines, x, y, size, color, bold, align, rotation, vCenter = vCenter))

    fun dottedLine(x1: Float, y1: Float, x2: Float, y2: Float, c: Int) =
        add(Prim.Shape(PathData.line(x1, y1, x2, y2), stroke = c, strokeWidth = 3f, dash = listOf(0.1f, 7f)))

    fun dot(x: Float, y: Float, c: Int, radius: Float = 9f) {
        shape(PathData.circle(x, y, radius + 3f), Paint.Solid(Ink.white))
        shape(PathData.circle(x, y, radius), fill(c, x, y - radius, x, y + radius))
    }

    // ---------- Bullet lists ----------

    val bulletSize get() = 15f * s
    private val bulletLh = 1.25f

    /**
     * Measures (and if [draw] draws) a bullet list in a column of width [w] starting at (x, y).
     * Returns the height used.
     */
    fun bullets(
        list: List<String>, x: Float, y: Float, w: Float, textColor: Int, dotColor: Int,
        separator: Int?, draw: Boolean, size: Float = bulletSize,
    ): Float {
        val clean = list.map { it.trim() }.filter { it.isNotEmpty() }
        if (clean.isEmpty()) return 0f
        val indent = size * 1.05f
        val gap = size * 0.55f
        var cy = y
        clean.forEachIndexed { i, b ->
            val lines = wrap(m, b, w - indent, size, false)
            val h = lines.size * size * bulletLh
            if (draw) {
                add(Prim.Text(listOf("•"), x + size * 0.15f, cy, size * 1.15f, dotColor, bold = true,
                    align = Align.START, lineHeight = bulletLh * size / (size * 1.15f), vCenter = false))
                add(Prim.Text(lines, x + indent, cy, size, textColor, align = Align.START,
                    lineHeight = bulletLh, vCenter = false))
            }
            cy += h
            if (i < clean.lastIndex) {
                if (separator != null && draw) {
                    val sy = cy + gap / 2f
                    add(Prim.Shape(PathData.line(x, sy, x + w, sy), stroke = separator, strokeWidth = 1.6f,
                        dash = listOf(0.1f, 4f)))
                }
                cy += gap
            }
        }
        return cy - y
    }

    // ---------- Callout cards ----------

    val calloutW get() = (300f * max(1f, s * 0.95f))
    private val calloutPad = 18f

    fun calloutHeight(i: Int): Float {
        val h = bullets(items[i].bullets, 0f, 0f, calloutW - calloutPad * 2 - 6f, 0, 0, null, draw = false)
        return if (h == 0f) 0f else h + calloutPad * 2
    }

    /** Draws a tinted card with an accent bar on [accentSide] (-1 = left, 1 = right, 0 = top). */
    fun callout(i: Int, x: Float, y: Float, w: Float, h: Float, accentSide: Int) {
        val c = color(i)
        val card = PathData.roundRect(x, y, w, h, 16f)
        shadow(card, 0.6f)
        shape(card, Paint.Solid(lighten(c, 0.87f)))
        val bar = when (accentSide) {
            -1 -> PathData.roundRect(x, y, 6f, h, 3f)
            1 -> PathData.roundRect(x + w - 6f, y, 6f, h, 3f)
            else -> PathData.roundRect(x, y, w, 6f, 3f)
        }
        shape(bar, Paint.Solid(c))
        val inset = if (accentSide == -1) 6f else 0f
        val top = if (accentSide == 0) 6f else 0f
        bullets(items[i].bullets, x + calloutPad + inset, y + calloutPad + top,
            w - calloutPad * 2 - 6f, Ink.text, c, lighten(c, 0.45f), draw = true)
    }
}

data class Size(val w: Float, val h: Float)

class CalloutReq(
    val item: Int, var ax: Float, var ay: Float, var side: Int, val idealY: Float,
    /** For round nodes: lets a top/bottom node re-anchor on the side its card ends up on. */
    val nodeX: Float = Float.NaN, val nodeY: Float = Float.NaN, val nodeR: Float = Float.NaN,
)

/**
 * Places a diagram (drawn in its own local space [diagW]×[diagH]) between columns of callout cards.
 * Anchor coordinates in [reqs] are in the diagram's local space.
 */
fun Ctx.withCallouts(diagW: Float, diagH: Float, reqs: List<CalloutReq>, diagram: List<Prim>): Size {
    val active = if (style.showCallouts) reqs.filter { calloutHeight(it.item) > 0f } else emptyList()
    val heights = active.associate { it.item to calloutHeight(it.item) }
    val gap = 18f
    // Balance near-vertical anchors to the emptier side.
    fun total(side: Int) = active.filter { it.side == side }.sumOf { (heights[it.item]!! + gap).toDouble() }.toFloat()
    active.filter { it.side == 0 }.forEach {
        it.side = if (total(-1) <= total(1)) -1 else 1
        if (!it.nodeR.isNaN()) { it.ax = it.nodeX + it.side * (it.nodeR + 4f); it.ay = it.nodeY }
    }

    val colGap = 64f
    val boxW = calloutW
    val hasL = active.any { it.side == -1 }
    val hasR = active.any { it.side == 1 }
    val leftW = if (hasL) boxW + colGap else 0f
    val rightW = if (hasR) boxW + colGap else 0f
    val H = maxOf(diagH, total(-1) - gap, total(1) - gap)
    val ox = leftW
    val oy = (H - diagH) / 2f
    place(diagram, ox, oy)

    for (side in listOf(-1, 1)) {
        val col = active.filter { it.side == side }.sortedBy { it.idealY }
        if (col.isEmpty()) continue
        // Greedy top-down placement around the ideal positions, then pull back inside the bounds.
        val tops = FloatArray(col.size)
        var cursor = 0f
        col.forEachIndexed { k, rq ->
            val h = heights[rq.item]!!
            val ideal = oy + rq.idealY - h / 2f
            tops[k] = maxOf(ideal, cursor)
            cursor = tops[k] + h + gap
        }
        if (cursor - gap > H) {
            for (k in col.indices.reversed()) {
                val h = heights[col[k].item]!!
                val limit = if (k == col.lastIndex) H - h else tops[k + 1] - gap - h
                tops[k] = minOf(tops[k], limit)
            }
        }
        val bx = if (side == -1) 0f else leftW + diagW + colGap
        col.forEachIndexed { k, rq ->
            val h = heights[rq.item]!!
            val c = color(rq.item)
            val ex = if (side == -1) bx + boxW else bx
            val ey = tops[k] + h / 2f
            dottedLine(ox + rq.ax, oy + rq.ay, ex, ey, lighten(c, 0.15f))
        }
        col.forEachIndexed { k, rq ->
            val h = heights[rq.item]!!
            callout(rq.item, bx, tops[k], boxW, h, if (side == -1) 1 else -1)
            val ex = if (side == -1) bx + boxW else bx
            dot(ex, tops[k] + h / 2f, color(rq.item))
        }
    }
    return Size(leftW + diagW + rightW, H)
}
