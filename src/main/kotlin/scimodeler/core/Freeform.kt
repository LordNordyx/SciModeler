package scimodeler.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Freeform (custom-built) models: a list of elements on an artboard. Elements are drawn with
 * the same helpers, theme palette, gradients, shadows and typography as the built-in models,
 * so a custom model looks and exports exactly like the others.
 */

enum class FfKind(val label: String) {
    RECT("Rectangle"), ROUNDED("Rounded box"), PILL("Pill"), ELLIPSE("Ellipse"), DIAMOND("Diamond"),
    TRIANGLE("Triangle"), HEXAGON("Hexagon"), CHEVRON("Chevron"), PARALLELOGRAM("Parallelogram"),
    CARD("Card"), NODE("Icon node"), TEXT("Text"), ICON("Icon"), IMAGE("Image"),
    CONNECTOR("Connector"), DOT("Dot"), BADGE("Number badge"), DIVIDER("Divider"),
}

/** Fill styles ("materials"). THEME follows the model's gradient setting. */
enum class Material(val label: String) {
    THEME("Theme"), GRADIENT("Gradient"), SOLID("Solid"), SOFT("Soft"), OUTLINE("Outline"), GLASS("Glass"),
}

enum class Route(val label: String) { STRAIGHT("Straight"), ELBOW("Elbow"), CURVED("Curved") }

/** Special colour slots for neutral colours. */
object Slots {
    const val INK = -1
    const val HUB = -2
    const val NEUTRAL = -3
}

data class FfElement(
    val id: Long = Ids.next(),
    val kind: FfKind,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val rotation: Float = 0f,
    val text: String = "",
    val textSize: Float = 18f,
    val bold: Boolean = true,
    val align: Align = Align.CENTER,
    /** Theme palette slot (or one of [Slots]); used unless [color] is set. */
    val slot: Int = 0,
    val color: Int? = null,
    val material: Material = Material.THEME,
    val corner: Float = 16f,
    val opacity: Float = 1f,
    val icon: String? = null,
    val image: String? = null,
    val bullets: List<String> = emptyList(),
    // Connectors: (x, y) → (x2, y2), unless attached to elements.
    val from: Long? = null,
    val to: Long? = null,
    val x2: Float = 0f,
    val y2: Float = 0f,
    val route: Route = Route.STRAIGHT,
    val headStart: Boolean = false,
    val headEnd: Boolean = true,
    val dashed: Boolean = false,
    val strokeWidth: Float = 4f,
    val group: Long? = null,
    val locked: Boolean = false,
    val hidden: Boolean = false,
    val name: String = "",
) {
    val isConnector get() = kind == FfKind.CONNECTOR
    val cx get() = x + w / 2f
    val cy get() = y + h / 2f
    val displayName: String get() = name.ifBlank { text.lineSequence().firstOrNull()?.take(28)?.ifBlank { null } ?: kind.label }
}

data class Freeform(
    val width: Float = 1200f,
    val height: Float = 760f,
    val elements: List<FfElement> = emptyList(),
) {
    fun byId(id: Long?): FfElement? = id?.let { i -> elements.firstOrNull { it.id == i } }

    /** Resolved connector end points, following attached elements. */
    fun endpoints(e: FfElement): Pair<Pair<Float, Float>, Pair<Float, Float>> {
        val a = byId(e.from)?.takeIf { !it.isConnector }
        val b = byId(e.to)?.takeIf { !it.isConnector }
        val freeStart = e.x to e.y
        val freeEnd = e.x2 to e.y2
        val towardsEnd = b?.let { it.cx to it.cy } ?: freeEnd
        val towardsStart = a?.let { it.cx to it.cy } ?: freeStart
        val s = a?.let { anchor(it, towardsEnd, e.route) } ?: freeStart
        val t = b?.let { anchor(it, towardsStart, e.route) } ?: freeEnd
        return s to t
    }

    /** Point on [el]'s outline in the direction of [p] (side midpoints for elbow routes). */
    fun anchor(el: FfElement, p: Pair<Float, Float>, route: Route): Pair<Float, Float> {
        val dx = p.first - el.cx; val dy = p.second - el.cy
        if (route == Route.ELBOW || route == Route.CURVED) {
            return if (abs(dx) * el.h > abs(dy) * el.w) (if (dx > 0) el.x + el.w + 4f else el.x - 4f) to el.cy
            else el.cx to (if (dy > 0) el.y + el.h + 4f else el.y - 4f)
        }
        if (dx == 0f && dy == 0f) return el.cx to el.cy
        val round = el.kind == FfKind.ELLIPSE || el.kind == FfKind.NODE || el.kind == FfKind.DOT || el.kind == FfKind.BADGE
        val t = if (round) {
            val rx = el.w / 2f; val ry = el.h / 2f
            1f / kotlin.math.sqrt((dx * dx) / (rx * rx) + (dy * dy) / (ry * ry))
        } else min(if (dx != 0f) (el.w / 2f) / abs(dx) else Float.MAX_VALUE, if (dy != 0f) (el.h / 2f) / abs(dy) else Float.MAX_VALUE)
        val gap = 5f / hypot(dx, dy)
        return (el.cx + dx * (t + gap)) to (el.cy + dy * (t + gap))
    }

    /** Polyline points of a connector for its route (curves are returned as a cubic separately). */
    fun routePoints(e: FfElement): List<Pair<Float, Float>> {
        val (s, t) = endpoints(e)
        return when (e.route) {
            Route.STRAIGHT, Route.CURVED -> listOf(s, t)
            Route.ELBOW -> {
                val horizontal = abs(t.first - s.first) >= abs(t.second - s.second)
                if (horizontal) {
                    val mx = (s.first + t.first) / 2f
                    listOf(s, mx to s.second, mx to t.second, t)
                } else {
                    val my = (s.second + t.second) / 2f
                    listOf(s, s.first to my, t.first to my, t)
                }
            }
        }
    }

    /** Bounding box (x, y, w, h) of any element, connectors included. */
    fun bounds(e: FfElement): FloatArray {
        if (!e.isConnector) return floatArrayOf(e.x, e.y, e.w, e.h)
        val pts = routePoints(e)
        val x0 = pts.minOf { it.first }; val y0 = pts.minOf { it.second }
        return floatArrayOf(x0, y0, pts.maxOf { it.first } - x0, pts.maxOf { it.second } - y0)
    }
}

// ---------------------------------------------------------------- Rendering

fun Ctx.freeformLayout(): Size {
    val ff = doc.freeform ?: Freeform()
    for (e in ff.elements) if (!e.hidden) drawElement(ff, e)
    return Size(ff.width, ff.height)
}

fun Ctx.elementColor(e: FfElement): Int = e.color ?: when (e.slot) {
    Slots.INK -> Ink.text
    Slots.HUB -> theme.hub
    Slots.NEUTRAL -> if (e.isConnector || e.kind == FfKind.DIVIDER) darken(theme.connector, 0.12f) else theme.panel
    else -> theme.slot(e.slot)
}

private fun op(c: Int, o: Float) = if (o >= 0.999f) c else withAlpha(c, (a(c) / 255f) * o)

private fun rotate(path: PathData, e: FfElement): PathData {
    if (e.rotation == 0f) return path
    val ang = Math.toRadians(e.rotation.toDouble())
    val co = cos(ang).toFloat(); val si = sin(ang).toFloat()
    return path.transformed { px, py ->
        val dx = px - e.cx; val dy = py - e.cy
        (e.cx + dx * co - dy * si) to (e.cy + dx * si + dy * co)
    }
}

/** Outline of a shape element in its own (unrotated) frame. */
fun shapePath(e: FfElement): PathData {
    val x = e.x; val y = e.y; val w = e.w; val h = e.h
    return when (e.kind) {
        FfKind.RECT -> PathData.roundRect(x, y, w, h, 0f)
        FfKind.ROUNDED, FfKind.CARD -> PathData.roundRect(x, y, w, h, e.corner)
        FfKind.PILL -> PathData.roundRect(x, y, w, h, min(w, h) / 2f)
        FfKind.ELLIPSE, FfKind.NODE, FfKind.DOT, FfKind.BADGE -> ellipse(x + w / 2f, y + h / 2f, w / 2f, h / 2f)
        FfKind.DIAMOND -> PathData.polygon((x + w / 2f) to y, (x + w) to (y + h / 2f), (x + w / 2f) to (y + h), x to (y + h / 2f))
        FfKind.TRIANGLE -> PathData.polygon((x + w / 2f) to y, (x + w) to (y + h), x to (y + h))
        FfKind.HEXAGON -> { val k = min(w * 0.25f, h * 0.5f); PathData.polygon((x + k) to y, (x + w - k) to y, (x + w) to (y + h / 2f), (x + w - k) to (y + h), (x + k) to (y + h), x to (y + h / 2f)) }
        FfKind.CHEVRON -> { val p = min(h * 0.4f, w * 0.3f); PathData.polygon(x to y, (x + w - p) to y, (x + w) to (y + h / 2f), (x + w - p) to (y + h), x to (y + h), (x + p) to (y + h / 2f)) }
        FfKind.PARALLELOGRAM -> { val k = min(w * 0.18f, h * 0.6f); PathData.polygon((x + k) to y, (x + w) to y, (x + w - k) to (y + h), x to (y + h)) }
        else -> PathData.roundRect(x, y, w, h, 0f)
    }
}

private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float): PathData {
    val k = 0.5523f
    return PathData().moveTo(cx + rx, cy)
        .cubicTo(cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry)
        .cubicTo(cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy)
        .cubicTo(cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry)
        .cubicTo(cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy).close()
}

fun Ctx.drawElement(ff: Freeform, e: FfElement) {
    val c = op(elementColor(e), e.opacity)
    when (e.kind) {
        FfKind.CONNECTOR -> drawConnector(ff, e, c)
        FfKind.TEXT -> {
            val size = e.textSize * s
            val lines = wrap(m, e.text, max(10f, e.w), size, e.bold)
            val ax = when (e.align) { Align.START -> e.x; Align.CENTER -> e.cx; Align.END -> e.x + e.w }
            add(Prim.Text(lines, ax, e.cy, size, c, e.bold, e.align, e.rotation, lineHeight = 1.22f))
        }
        FfKind.ICON -> {
            val sz = min(e.w, e.h)
            val img = e.image
            if (img != null) { val (iw, ih) = Images.fit(img, sz); add(Prim.Image(img, e.cx - iw / 2f, e.cy - ih / 2f, iw, ih, e.rotation)) }
            else glyph(IconLibrary.get(e.icon) ?: IconLibrary.defaultFor(0), e.cx, e.cy, sz, c, rotation = e.rotation)
        }
        FfKind.IMAGE -> {
            val img = e.image
            if (img == null) {
                val p = rotate(PathData.roundRect(e.x, e.y, e.w, e.h, 12f), e)
                shape(p, Paint.Solid(theme.panel), theme.connector, 2f, listOf(8f, 6f))
                glyph(IconLibrary.get("document")!!, e.cx, e.cy, min(e.w, e.h) * 0.3f, Ink.subtle)
            } else {
                // Image keeps its proportions inside the element box.
                val sc = min(e.w / Images.size(img).first, e.h / Images.size(img).second)
                val iw = Images.size(img).first * sc; val ih = Images.size(img).second * sc
                add(Prim.Image(img, e.cx - iw / 2f, e.cy - ih / 2f, iw, ih, e.rotation))
            }
        }
        FfKind.DIVIDER -> {
            val p = rotate(PathData.line(e.x, e.cy, e.x + e.w, e.cy), e)
            add(Prim.Shape(p, stroke = c, strokeWidth = e.strokeWidth, dash = if (e.dashed) listOf(0.1f, e.strokeWidth * 2.2f) else null))
        }
        FfKind.DOT -> {
            val r = min(e.w, e.h) / 2f
            shape(PathData.circle(e.cx, e.cy, r), Paint.Solid(op(Ink.white, e.opacity)))
            shape(PathData.circle(e.cx, e.cy, r * 0.72f), fill(c, e.cx, e.cy - r, e.cx, e.cy + r))
        }
        FfKind.BADGE -> {
            val r = min(e.w, e.h) / 2f
            shape(PathData.circle(e.cx, e.cy, r), Paint.Solid(op(Ink.white, e.opacity)), c, max(2.5f, r * 0.12f))
            text(listOf(e.text.ifBlank { "1" }.take(3)), e.cx, e.cy, r * 0.85f, readable(c), bold = true)
        }
        FfKind.NODE -> {
            val r = min(e.w, e.h) / 2f
            val disc = PathData.circle(e.cx, e.cy, r)
            val outlined = e.material == Material.OUTLINE || e.material == Material.SOFT || e.image != null
            if (e.material != Material.OUTLINE) shadow(disc, 0.7f)
            if (outlined) shape(disc, Paint.Solid(op(Ink.white, e.opacity)), c, max(3f, r * 0.12f))
            else shape(disc, materialFill(e, c), Ink.white, 3f)
            val fg = if (outlined) readable(c) else onColor(elementColor(e))
            val img = e.image
            if (img != null) picture(img, e.cx, e.cy, r * 1.05f)
            else glyph(IconLibrary.get(e.icon) ?: IconLibrary.defaultFor(0), e.cx, e.cy, r * 1.02f, fg, rotation = e.rotation)
            if (e.text.isNotBlank()) {
                val size = e.textSize * s
                val lines = wrap(m, e.text, max(r * 3f, 160f), size, e.bold)
                add(Prim.Text(lines, e.cx, e.y + e.h + 10f, size, readable(c), e.bold, Align.CENTER, vCenter = false))
            }
        }
        FfKind.CARD -> {
            val p = rotate(shapePath(e), e)
            shadow(p, 0.6f)
            shape(p, Paint.Solid(op(lighten(elementColor(e), theme.cardTint), e.opacity)))
            shape(rotate(PathData.roundRect(e.x + 14f, e.y, e.w - 28f, 6f, 3f), e), Paint.Solid(c))
            val pad = 18f
            val size = e.textSize * s
            val title = wrap(m, e.text, e.w - pad * 2, size, true)
            var ty = e.y + pad
            if (e.text.isNotBlank()) {
                add(Prim.Text(title, e.x + pad, ty, size, readable(elementColor(e)), true, Align.START, vCenter = false))
                ty += title.size * size * 1.22f + 8f
            }
            if (e.bullets.any { it.isNotBlank() }) bullets(e.bullets, e.x + pad, ty, e.w - pad * 2, Ink.text, c, lighten(c, 0.45f), true)
        }
        else -> drawShape(e, c)
    }
}

private fun Ctx.materialFill(e: FfElement, c: Int): Paint = when (e.material) {
    Material.THEME -> fill(c, e.x, e.y, e.x + e.w, e.y + e.h)
    Material.GRADIENT -> Paint.Linear(e.x, e.y, e.x + e.w, e.y + e.h, lighten(c, 0.14f), darken(c, 0.10f))
    Material.SOLID -> Paint.Solid(c)
    Material.SOFT -> Paint.Solid(op(lighten(c, theme.cardTint), e.opacity))
    Material.OUTLINE -> Paint.Solid(op(Ink.white, e.opacity))
    Material.GLASS -> Paint.Linear(e.x, e.y, e.x, e.y + e.h, withAlpha(lighten(c, 0.35f), 0.75f * e.opacity), withAlpha(c, 0.45f * e.opacity))
}

private fun Ctx.drawShape(e: FfElement, c: Int) {
    val p = rotate(shapePath(e), e)
    val base = elementColor(e)
    when (e.material) {
        Material.OUTLINE -> shape(p, materialFill(e, c), c, 3f)
        Material.SOFT -> shape(p, materialFill(e, c))
        Material.GLASS -> {
            shadow(p, 0.5f)
            shape(p, materialFill(e, c), withAlpha(Ink.white, 0.85f), 2f)
        }
        else -> {
            shadow(p, 0.8f)
            shape(p, materialFill(e, c))
        }
    }
    val icon = e.icon ?: e.image?.let { "" }
    val pad = 14f
    var textLeft = e.x + pad
    var textW = e.w - pad * 2
    val fg = when (e.material) {
        Material.SOFT, Material.OUTLINE -> readable(base)
        Material.GLASS -> darken(base, 0.45f)
        else -> onColor(base)
    }
    if (icon != null && e.text.isNotBlank()) {
        val iz = min(e.h * 0.5f, 40f)
        itemLikeIcon(e, e.x + pad + iz / 2f, e.cy, iz, fg)
        textLeft += iz + 10f; textW -= iz + 10f
    } else if (icon != null) {
        itemLikeIcon(e, e.cx, e.cy, min(e.w, e.h) * 0.5f, fg)
    }
    if (e.text.isNotBlank()) {
        val f = fit(m, e.text, max(20f, textW), max(14f, e.h - pad), e.textSize * s, 8f, e.bold)
        val ax = when { icon != null -> textLeft; e.align == Align.START -> textLeft; e.align == Align.END -> textLeft + textW; else -> textLeft + textW / 2f }
        add(Prim.Text(f.lines, ax, e.cy, f.size, fg, e.bold, if (icon != null) Align.START else e.align, e.rotation))
    }
}

private fun Ctx.itemLikeIcon(e: FfElement, cx: Float, cy: Float, size: Float, color: Int) {
    val img = e.image
    if (img != null) picture(img, cx, cy, size) else glyph(IconLibrary.get(e.icon) ?: IconLibrary.defaultFor(0), cx, cy, size, color)
}

private fun Ctx.drawConnector(ff: Freeform, e: FfElement, c: Int) {
    val dash = if (e.dashed) listOf(e.strokeWidth * 2.5f, e.strokeWidth * 2f) else null
    val head = e.strokeWidth * 3.4f + 4f
    if (e.route == Route.CURVED) {
        val (s0, t0) = ff.endpoints(e)
        val horizontal = abs(t0.first - s0.first) >= abs(t0.second - s0.second)
        val c1 = if (horizontal) ((s0.first + t0.first) / 2f to s0.second) else (s0.first to (s0.second + t0.second) / 2f)
        val c2 = if (horizontal) ((s0.first + t0.first) / 2f to t0.second) else (t0.first to (s0.second + t0.second) / 2f)
        // Sample the curve so arrowheads follow the tangent.
        val pts = (0..24).map { k ->
            val t = k / 24f; val u = 1 - t
            (u * u * u * s0.first + 3 * u * u * t * c1.first + 3 * u * t * t * c2.first + t * t * t * t0.first) to
                (u * u * u * s0.second + 3 * u * u * t * c1.second + 3 * u * t * t * c2.second + t * t * t * t0.second)
        }
        drawPolyArrow(pts, c, e, dash, head)
    } else drawPolyArrow(ff.routePoints(e), c, e, dash, head)
}

private fun Ctx.drawPolyArrow(pts: List<Pair<Float, Float>>, c: Int, e: FfElement, dash: List<Float>?, head: Float) {
    if (pts.size < 2) return
    if (e.headEnd) arrow(if (e.headStart) pts.drop(0) else pts, c, e.strokeWidth, head, dash)
    else add(Prim.Shape(polyline(pts), stroke = c, strokeWidth = e.strokeWidth, dash = dash))
    if (e.headStart) {
        val rev = pts.reversed()
        val (x1, y1) = rev[rev.size - 2]; val (x2, y2) = rev.last()
        val len = hypot(x2 - x1, y2 - y1).coerceAtLeast(0.01f)
        val ux = (x2 - x1) / len; val uy = (y2 - y1) / len
        val bx = x2 - ux * head; val by = y2 - uy * head
        shape(PathData.polygon(x2 to y2, (bx - uy * head * 0.62f) to (by + ux * head * 0.62f), (bx + uy * head * 0.62f) to (by - ux * head * 0.62f)), Paint.Solid(c))
    }
}

private fun polyline(pts: List<Pair<Float, Float>>): PathData {
    val p = PathData()
    pts.forEachIndexed { i, (x, y) -> if (i == 0) p.moveTo(x, y) else p.lineTo(x, y) }
    return p
}

// ---------------------------------------------------------------- Component library

class Component(val id: String, val name: String, val category: String, val make: (Float, Float) -> FfElement)

object Components {
    val categories = listOf("Shapes", "Text", "Connectors", "Nodes & icons", "Cards", "Decorations")

    private fun box(kind: FfKind, w: Float, h: Float, text: String = "", material: Material = Material.THEME, slot: Int = 0) =
        { cx: Float, cy: Float -> FfElement(kind = kind, x = cx - w / 2f, y = cy - h / 2f, w = w, h = h, text = text, material = material, slot = slot) }

    val all: List<Component> = listOf(
        Component("rounded", "Rounded box", "Shapes", box(FfKind.ROUNDED, 220f, 96f, "Concept")),
        Component("rect", "Rectangle", "Shapes", box(FfKind.RECT, 220f, 96f, "Concept", slot = 1)),
        Component("pill", "Pill", "Shapes", box(FfKind.PILL, 220f, 70f, "Label", slot = 2)),
        Component("ellipse", "Ellipse", "Shapes", box(FfKind.ELLIPSE, 180f, 120f, "Idea", slot = 3)),
        Component("circle", "Circle", "Shapes", box(FfKind.ELLIPSE, 140f, 140f, "Core", slot = 4)),
        Component("diamond", "Diamond", "Shapes", box(FfKind.DIAMOND, 170f, 130f, "Decision", slot = 5)),
        Component("hexagon", "Hexagon", "Shapes", box(FfKind.HEXAGON, 190f, 110f, "Factor", slot = 6)),
        Component("triangle", "Triangle", "Shapes", box(FfKind.TRIANGLE, 160f, 140f, "", slot = 7)),
        Component("chevron", "Chevron", "Shapes", box(FfKind.CHEVRON, 210f, 80f, "Step", slot = 0)),
        Component("parallelogram", "Parallelogram", "Shapes", box(FfKind.PARALLELOGRAM, 210f, 80f, "Input", slot = 1)),
        Component("soft", "Soft box", "Shapes", box(FfKind.ROUNDED, 220f, 96f, "Concept", Material.SOFT, 2)),
        Component("outline", "Outline box", "Shapes", box(FfKind.ROUNDED, 220f, 96f, "Concept", Material.OUTLINE, 3)),
        Component("glass", "Glass box", "Shapes", box(FfKind.ROUNDED, 220f, 96f, "Concept", Material.GLASS, 0)),
        Component("heading", "Heading", "Text") { cx, cy -> FfElement(kind = FfKind.TEXT, x = cx - 180f, y = cy - 24f, w = 360f, h = 48f, text = "Heading", textSize = 30f, slot = Slots.INK) },
        Component("body", "Body text", "Text") { cx, cy -> FfElement(kind = FfKind.TEXT, x = cx - 150f, y = cy - 30f, w = 300f, h = 60f, text = "Write a short explanation here.", textSize = 16f, bold = false, slot = Slots.INK) },
        Component("label", "Colour label", "Text") { cx, cy -> FfElement(kind = FfKind.TEXT, x = cx - 100f, y = cy - 16f, w = 200f, h = 32f, text = "Label", textSize = 18f, slot = 0) },
        Component("arrow", "Arrow", "Connectors") { cx, cy -> FfElement(kind = FfKind.CONNECTOR, x = cx - 110f, y = cy, w = 0f, h = 0f, x2 = cx + 110f, y2 = cy, slot = Slots.INK) },
        Component("double", "Two-way arrow", "Connectors") { cx, cy -> FfElement(kind = FfKind.CONNECTOR, x = cx - 110f, y = cy, w = 0f, h = 0f, x2 = cx + 110f, y2 = cy, headStart = true, slot = Slots.INK) },
        Component("elbow", "Elbow arrow", "Connectors") { cx, cy -> FfElement(kind = FfKind.CONNECTOR, x = cx - 110f, y = cy - 50f, w = 0f, h = 0f, x2 = cx + 110f, y2 = cy + 50f, route = Route.ELBOW, slot = Slots.INK) },
        Component("curved", "Curved arrow", "Connectors") { cx, cy -> FfElement(kind = FfKind.CONNECTOR, x = cx - 110f, y = cy - 50f, w = 0f, h = 0f, x2 = cx + 110f, y2 = cy + 50f, route = Route.CURVED, slot = Slots.INK) },
        Component("dashed", "Dashed arrow", "Connectors") { cx, cy -> FfElement(kind = FfKind.CONNECTOR, x = cx - 110f, y = cy, w = 0f, h = 0f, x2 = cx + 110f, y2 = cy, dashed = true, strokeWidth = 3f, slot = Slots.INK) },
        Component("line", "Line", "Connectors") { cx, cy -> FfElement(kind = FfKind.CONNECTOR, x = cx - 110f, y = cy, w = 0f, h = 0f, x2 = cx + 110f, y2 = cy, headEnd = false, slot = Slots.NEUTRAL, strokeWidth = 5f) },
        Component("node", "Icon node", "Nodes & icons") { cx, cy -> FfElement(kind = FfKind.NODE, x = cx - 46f, y = cy - 46f, w = 92f, h = 92f, icon = "bulb", slot = 0) },
        Component("node-outline", "Outlined node", "Nodes & icons") { cx, cy -> FfElement(kind = FfKind.NODE, x = cx - 46f, y = cy - 46f, w = 92f, h = 92f, icon = "users", material = Material.OUTLINE, slot = 1) },
        Component("icon", "Icon", "Nodes & icons") { cx, cy -> FfElement(kind = FfKind.ICON, x = cx - 32f, y = cy - 32f, w = 64f, h = 64f, icon = "star", slot = 2) },
        Component("icon-box", "Box with icon", "Nodes & icons") { cx, cy -> FfElement(kind = FfKind.ROUNDED, x = cx - 130f, y = cy - 48f, w = 260f, h = 96f, text = "Concept", icon = "chart", material = Material.SOFT, slot = 3) },
        Component("image", "Image", "Nodes & icons") { cx, cy -> FfElement(kind = FfKind.IMAGE, x = cx - 90f, y = cy - 70f, w = 180f, h = 140f, slot = Slots.NEUTRAL) },
        Component("card", "Note card", "Cards") { cx, cy -> FfElement(kind = FfKind.CARD, x = cx - 150f, y = cy - 80f, w = 300f, h = 160f, text = "Title", bullets = listOf("First point", "Second point"), slot = 0, textSize = 18f) },
        Component("card2", "Tinted card", "Cards") { cx, cy -> FfElement(kind = FfKind.CARD, x = cx - 150f, y = cy - 70f, w = 300f, h = 140f, text = "Finding", bullets = listOf("Key result"), slot = 4, textSize = 18f) },
        Component("dot", "Dot", "Decorations") { cx, cy -> FfElement(kind = FfKind.DOT, x = cx - 12f, y = cy - 12f, w = 24f, h = 24f, slot = 0) },
        Component("badge", "Number badge", "Decorations") { cx, cy -> FfElement(kind = FfKind.BADGE, x = cx - 22f, y = cy - 22f, w = 44f, h = 44f, text = "1", slot = 1) },
        Component("divider", "Divider", "Decorations") { cx, cy -> FfElement(kind = FfKind.DIVIDER, x = cx - 150f, y = cy - 6f, w = 300f, h = 12f, slot = Slots.NEUTRAL, strokeWidth = 3f) },
        Component("dotted", "Dotted line", "Decorations") { cx, cy -> FfElement(kind = FfKind.DIVIDER, x = cx - 150f, y = cy - 6f, w = 300f, h = 12f, slot = 0, strokeWidth = 3f, dashed = true) },
        Component("panel", "Background panel", "Decorations") { cx, cy -> FfElement(kind = FfKind.ROUNDED, x = cx - 220f, y = cy - 150f, w = 440f, h = 300f, slot = Slots.NEUTRAL, material = Material.SOLID, corner = 28f) },
    )

    fun get(id: String) = all.firstOrNull { it.id == id }
}

/** A starting freeform model so the builder never opens empty. */
fun starterFreeform(): Freeform {
    val a = FfElement(kind = FfKind.ROUNDED, x = 110f, y = 300f, w = 240f, h = 104f, text = "Independent variable", icon = "users", material = Material.SOFT, slot = 0)
    val b = FfElement(kind = FfKind.ROUNDED, x = 480f, y = 120f, w = 240f, h = 104f, text = "Mediator", icon = "bulb", material = Material.SOFT, slot = 2)
    val c = FfElement(kind = FfKind.ROUNDED, x = 850f, y = 300f, w = 240f, h = 104f, text = "Outcome", icon = "target", material = Material.SOFT, slot = 1)
    val note = FfElement(kind = FfKind.CARD, x = 450f, y = 470f, w = 300f, h = 150f, text = "Notes", bullets = listOf("Drag shapes from the library", "Connect them with arrows"), slot = 3)
    return Freeform(1200f, 700f, listOf(
        a, b, c, note,
        FfElement(kind = FfKind.CONNECTOR, x = 0f, y = 0f, w = 0f, h = 0f, from = a.id, to = b.id, slot = Slots.INK),
        FfElement(kind = FfKind.CONNECTOR, x = 0f, y = 0f, w = 0f, h = 0f, from = b.id, to = c.id, slot = Slots.INK),
        FfElement(kind = FfKind.CONNECTOR, x = 0f, y = 0f, w = 0f, h = 0f, from = a.id, to = c.id, slot = Slots.INK, strokeWidth = 5f),
    ))
}
