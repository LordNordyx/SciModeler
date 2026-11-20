package scimodeler.core

import kotlin.math.cos
import kotlin.math.sin

/** Collects the strokes of one icon, drawn on a 24×24 grid with a 2-unit stroke (Lucide-like style). */
class IconPen {
    val strokes = ArrayList<PathData>()
    val fills = ArrayList<PathData>()

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) { strokes += PathData.line(x1, y1, x2, y2) }

    fun poly(vararg p: Float, closed: Boolean = false) {
        val path = PathData()
        for (i in p.indices step 2) if (i == 0) path.moveTo(p[0], p[1]) else path.lineTo(p[i], p[i + 1])
        if (closed) path.close()
        strokes += path
    }

    fun circle(cx: Float, cy: Float, r: Float) { strokes += PathData.circle(cx, cy, r) }
    fun rect(x: Float, y: Float, w: Float, h: Float, r: Float = 0f) { strokes += PathData.roundRect(x, y, w, h, r) }
    fun arc(cx: Float, cy: Float, r: Float, a0: Float, a1: Float) { strokes += PathData().arc(cx, cy, r, a0, a1, move = true) }
    fun dot(cx: Float, cy: Float, r: Float = 1.2f) { fills += PathData.circle(cx, cy, r) }
    fun path(build: PathData.() -> Unit) { strokes += PathData().apply(build) }

    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float) {
        val k = 0.5523f
        strokes += PathData().moveTo(cx + rx, cy)
            .cubicTo(cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry)
            .cubicTo(cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy)
            .cubicTo(cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry)
            .cubicTo(cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy)
            .close()
    }

    /** Rotates everything drawn in [block] by [deg] around (cx, cy). */
    fun rotated(deg: Float, cx: Float, cy: Float, block: IconPen.() -> Unit) {
        val sub = IconPen().apply(block)
        val a = Math.toRadians(deg.toDouble())
        val c = cos(a).toFloat(); val s = sin(a).toFloat()
        val f: (Float, Float) -> Pair<Float, Float> = { x, y ->
            val dx = x - cx; val dy = y - cy
            (cx + dx * c - dy * s) to (cy + dx * s + dy * c)
        }
        strokes += sub.strokes.map { it.transformed(f) }
        fills += sub.fills.map { it.transformed(f) }
    }
}

class IconDef(val id: String, val name: String, val tags: String, val draw: IconPen.() -> Unit) {
    val pen: IconPen by lazy { IconPen().apply(draw) }
}

object IconLibrary {
    val all: List<IconDef> = listOf(
        IconDef("bulb", "Idea", "light bulb insight innovation") {
            arc(12f, 9.5f, 6f, 135f, 405f)
            line(7.76f, 13.74f, 9.2f, 16.5f); line(16.24f, 13.74f, 14.8f, 16.5f)
            line(9.2f, 16.5f, 14.8f, 16.5f); line(10f, 19.5f, 14f, 19.5f)
        },
        IconDef("users", "People", "team group community patients staff") {
            circle(9f, 8f, 3.5f); arc(9f, 20.5f, 6.5f, 180f, 360f)
            circle(17f, 8.5f, 2.6f); arc(17f, 19f, 4.8f, 240f, 360f)
        },
        IconDef("user", "Person", "individual patient client") {
            circle(12f, 8f, 4f); arc(12f, 21f, 7.5f, 180f, 360f)
        },
        IconDef("chart", "Chart", "data statistics analysis bars results") {
            poly(4f, 4f, 4f, 20f, 20f, 20f)
            line(8.5f, 16f, 8.5f, 12f); line(12.5f, 16f, 12.5f, 8f); line(16.5f, 16f, 16.5f, 11f)
        },
        IconDef("trend", "Growth", "trend increase improvement progress") {
            poly(3.5f, 17f, 9f, 11.5f, 13f, 15f, 20.5f, 7.5f); poly(15f, 7.5f, 20.5f, 7.5f, 20.5f, 13f)
        },
        IconDef("target", "Target", "goal objective aim outcome") {
            circle(12f, 12f, 9f); circle(12f, 12f, 5.2f); dot(12f, 12f, 1.8f)
        },
        IconDef("gear", "Process", "settings gear operations method mechanism") {
            circle(12f, 12f, 3f); circle(12f, 12f, 6.8f)
            for (k in 0 until 8) {
                val a = Math.toRadians(k * 45.0)
                line(12f + 6.8f * cos(a).toFloat(), 12f + 6.8f * sin(a).toFloat(),
                    12f + 9.3f * cos(a).toFloat(), 12f + 9.3f * sin(a).toFloat())
            }
        },
        IconDef("book", "Knowledge", "book reading literature theory learning") {
            path { moveTo(12f, 6.5f).cubicTo(10f, 5f, 6.5f, 4.8f, 3.5f, 5.5f).lineTo(3.5f, 18.5f).cubicTo(6.5f, 17.8f, 10f, 18f, 12f, 19.5f) }
            path { moveTo(12f, 6.5f).cubicTo(14f, 5f, 17.5f, 4.8f, 20.5f, 5.5f).lineTo(20.5f, 18.5f).cubicTo(17.5f, 17.8f, 14f, 18f, 12f, 19.5f) }
            line(12f, 6.5f, 12f, 19.5f)
        },
        IconDef("cap", "Education", "graduation training school study") {
            poly(2f, 9.5f, 12f, 5f, 22f, 9.5f, 12f, 14f, closed = true)
            path { moveTo(6.5f, 11.8f).lineTo(6.5f, 16f).cubicTo(9f, 18.5f, 15f, 18.5f, 17.5f, 16f).lineTo(17.5f, 11.8f) }
            line(22f, 9.5f, 22f, 15f)
        },
        IconDef("heart", "Care", "heart love health wellbeing empathy") {
            path {
                moveTo(12f, 20f).cubicTo(5f, 15.5f, 2.5f, 11.5f, 4f, 8f).cubicTo(5.3f, 5f, 9.5f, 4.5f, 12f, 7.5f)
                    .cubicTo(14.5f, 4.5f, 18.7f, 5f, 20f, 8f).cubicTo(21.5f, 11.5f, 19f, 15.5f, 12f, 20f).close()
            }
        },
        IconDef("medical", "Medical", "health hospital clinic cross") {
            rect(3.5f, 3.5f, 17f, 17f, 4f); line(12f, 8f, 12f, 16f); line(8f, 12f, 16f, 12f)
        },
        IconDef("pill", "Medicine", "pill drug therapy pharmacy medication") {
            rotated(-45f, 12f, 12f) { rect(3f, 8.5f, 18f, 7f, 3.5f); line(12f, 8.5f, 12f, 15.5f) }
        },
        IconDef("pulse", "Vital signs", "pulse heartbeat monitoring ecg") {
            poly(2.5f, 12f, 7f, 12f, 9.5f, 6f, 14f, 18f, 16.5f, 12f, 21.5f, 12f)
        },
        IconDef("flask", "Research", "lab experiment science chemistry") {
            path {
                moveTo(9.5f, 3.5f).lineTo(9.5f, 9.5f).lineTo(4.5f, 18.5f).cubicTo(4f, 19.5f, 4.6f, 20.5f, 5.8f, 20.5f)
                    .lineTo(18.2f, 20.5f).cubicTo(19.4f, 20.5f, 20f, 19.5f, 19.5f, 18.5f).lineTo(14.5f, 9.5f).lineTo(14.5f, 3.5f)
            }
            line(8f, 3.5f, 16f, 3.5f); line(7.2f, 15f, 16.8f, 15f)
        },
        IconDef("search", "Analyse", "search find explore investigate magnifier") {
            circle(10.5f, 10.5f, 6f); line(15f, 15f, 20f, 20f)
        },
        IconDef("chat", "Communication", "chat talk dialogue feedback message") {
            rect(3f, 4f, 18f, 13f, 4f); poly(8f, 17f, 7.5f, 20.5f, 12.5f, 17f)
        },
        IconDef("network", "Network", "connections share relations collaboration") {
            circle(6f, 12f, 2.6f); circle(17.5f, 6f, 2.6f); circle(17.5f, 18f, 2.6f)
            line(8.3f, 10.8f, 15.2f, 7.2f); line(8.3f, 13.2f, 15.2f, 16.8f)
        },
        IconDef("shield", "Safety", "security protection quality risk") {
            path {
                moveTo(12f, 3f).lineTo(19.5f, 6f).lineTo(19.5f, 11.5f).cubicTo(19.5f, 16f, 16.5f, 19.3f, 12f, 21f)
                    .cubicTo(7.5f, 19.3f, 4.5f, 16f, 4.5f, 11.5f).lineTo(4.5f, 6f).close()
            }
            poly(9f, 12f, 11.2f, 14.2f, 15.2f, 10f)
        },
        IconDef("check", "Done", "check complete verified approve") {
            circle(12f, 12f, 9f); poly(8f, 12.5f, 11f, 15.5f, 16.5f, 9.5f)
        },
        IconDef("clock", "Time", "clock duration schedule hours") {
            circle(12f, 12f, 9f); poly(12f, 7f, 12f, 12f, 15.5f, 14f)
        },
        IconDef("calendar", "Planning", "calendar date schedule plan") {
            rect(3.5f, 5f, 17f, 15.5f, 2.5f); line(3.5f, 10f, 20.5f, 10f); line(8f, 3f, 8f, 7f); line(16f, 3f, 16f, 7f)
            dot(8f, 14.5f); dot(12f, 14.5f); dot(16f, 14.5f)
        },
        IconDef("flag", "Milestone", "flag milestone start finish") {
            line(5f, 21f, 5f, 4f)
            path { moveTo(5f, 4.5f).cubicTo(9f, 2.5f, 12f, 6.5f, 19f, 4.5f).lineTo(19f, 13f).cubicTo(12f, 15f, 9f, 11f, 5f, 13f) }
        },
        IconDef("globe", "Global", "world international society environment") {
            circle(12f, 12f, 9f); line(3f, 12f, 21f, 12f); ellipse(12f, 12f, 4f, 9f)
        },
        IconDef("home", "Home", "house community family living") {
            poly(3.5f, 11f, 12f, 4f, 20.5f, 11f); poly(6f, 9.5f, 6f, 20f, 18f, 20f, 18f, 9.5f)
            poly(10f, 20f, 10f, 15f, 14f, 15f, 14f, 20f)
        },
        IconDef("leaf", "Environment", "nature ecology sustainability green") {
            path {
                moveTo(4.5f, 19.5f).cubicTo(4.5f, 11f, 10f, 4.5f, 19.5f, 4.5f).cubicTo(19.5f, 13.5f, 13.5f, 19.5f, 4.5f, 19.5f).close()
            }
            line(4.5f, 19.5f, 13f, 11f)
        },
        IconDef("scale", "Balance", "law justice ethics evaluation weighing") {
            line(12f, 4f, 12f, 20f); line(8f, 20f, 16f, 20f); line(5f, 7f, 19f, 7f)
            poly(5f, 7f, 2.5f, 13f); poly(5f, 7f, 7.5f, 13f)
            path { moveTo(2.5f, 13f).cubicTo(3f, 15.5f, 7f, 15.5f, 7.5f, 13f).close() }
            poly(19f, 7f, 16.5f, 13f); poly(19f, 7f, 21.5f, 13f)
            path { moveTo(16.5f, 13f).cubicTo(17f, 15.5f, 21f, 15.5f, 21.5f, 13f).close() }
        },
        IconDef("coins", "Finance", "money cost budget funding economy") {
            ellipse(12f, 7f, 7f, 2.5f)
            path { moveTo(5f, 7f).lineTo(5f, 11f).cubicTo(5f, 14.3f, 19f, 14.3f, 19f, 11f).lineTo(19f, 7f) }
            path { moveTo(5f, 11f).lineTo(5f, 15f).cubicTo(5f, 18.3f, 19f, 18.3f, 19f, 15f).lineTo(19f, 11f) }
        },
        IconDef("document", "Document", "file report paper protocol") {
            path { moveTo(6f, 3f).lineTo(14f, 3f).lineTo(19f, 8f).lineTo(19f, 21f).lineTo(6f, 21f).close() }
            poly(14f, 3f, 14f, 8f, 19f, 8f); line(9f, 12.5f, 16f, 12.5f); line(9f, 16.5f, 16f, 16.5f)
        },
        IconDef("clipboard", "Checklist", "tasks assessment evaluation criteria") {
            rect(5f, 4.5f, 14f, 16.5f, 2.5f); rect(9f, 3f, 6f, 3f, 1f)
            poly(8f, 12f, 9.5f, 13.5f, 12f, 11f); line(13.5f, 12.5f, 16f, 12.5f)
            poly(8f, 17f, 9.5f, 18.5f, 12f, 16f); line(13.5f, 17.5f, 16f, 17.5f)
        },
        IconDef("layers", "Levels", "layers levels stack structure") {
            poly(12f, 3.5f, 21f, 8f, 12f, 12.5f, 3f, 8f, closed = true)
            poly(3f, 12f, 12f, 16.5f, 21f, 12f); poly(3f, 16f, 12f, 20.5f, 21f, 16f)
        },
        IconDef("refresh", "Cycle", "repeat iterate loop continuous improvement") {
            arc(12f, 12f, 7.5f, -150f, 120f)
            poly(4.6f, 17.6f, 8.25f, 18.5f, 9.6f, 15f)
        },
        IconDef("send", "Dissemination", "send share publish deliver") {
            poly(21f, 3f, 3f, 10.5f, 10.5f, 13.5f, 13.5f, 21f, closed = true); line(21f, 3f, 10.5f, 13.5f)
        },
        IconDef("input", "Input", "enter receive supply incoming") {
            poly(10f, 7f, 15f, 12f, 10f, 17f); line(3f, 12f, 15f, 12f)
            path { moveTo(13f, 4f).lineTo(18f, 4f).cubicTo(19.7f, 4f, 21f, 5.3f, 21f, 7f).lineTo(21f, 17f).cubicTo(21f, 18.7f, 19.7f, 20f, 18f, 20f).lineTo(13f, 20f) }
        },
        IconDef("output", "Output", "exit result deliver outgoing") {
            poly(16f, 7f, 21f, 12f, 16f, 17f); line(9f, 12f, 21f, 12f)
            path { moveTo(11f, 4f).lineTo(6f, 4f).cubicTo(4.3f, 4f, 3f, 5.3f, 3f, 7f).lineTo(3f, 17f).cubicTo(3f, 18.7f, 4.3f, 20f, 6f, 20f).lineTo(11f, 20f) }
        },
        IconDef("cube", "Resources", "box supply product materials") {
            poly(12f, 3f, 20f, 7.5f, 20f, 16.5f, 12f, 21f, 4f, 16.5f, 4f, 7.5f, closed = true)
            poly(4f, 7.5f, 12f, 12f, 20f, 7.5f); line(12f, 12f, 12f, 21f)
        },
        IconDef("briefcase", "Work", "job organisation management business") {
            rect(3f, 7.5f, 18f, 12.5f, 2.5f); poly(9f, 7.5f, 9f, 5f, 15f, 5f, 15f, 7.5f); line(3f, 13f, 21f, 13f)
        },
        IconDef("lock", "Privacy", "lock security confidential access") {
            rect(5f, 10.5f, 14f, 10.5f, 2.5f)
            path { moveTo(8f, 10.5f).lineTo(8f, 7.5f).cubicTo(8f, 2.5f, 16f, 2.5f, 16f, 7.5f).lineTo(16f, 10.5f) }
            dot(12f, 15.7f, 1.4f)
        },
        IconDef("eye", "Observation", "see watch awareness vision") {
            path { moveTo(2f, 12f).cubicTo(5f, 6f, 19f, 6f, 22f, 12f).cubicTo(19f, 18f, 5f, 18f, 2f, 12f).close() }
            circle(12f, 12f, 3f)
        },
        IconDef("database", "Data", "database storage records dataset") {
            ellipse(12f, 6f, 7.5f, 2.8f); line(4.5f, 6f, 4.5f, 18f); line(19.5f, 6f, 19.5f, 18f)
            path { moveTo(4.5f, 18f).cubicTo(4.5f, 21.7f, 19.5f, 21.7f, 19.5f, 18f) }
            path { moveTo(4.5f, 12f).cubicTo(4.5f, 15.7f, 19.5f, 15.7f, 19.5f, 12f) }
        },
        IconDef("pin", "Location", "place map setting site") {
            path {
                moveTo(12f, 21f).cubicTo(7f, 15.5f, 5f, 12.5f, 5f, 9.5f).cubicTo(5f, 5.5f, 8.2f, 3f, 12f, 3f)
                    .cubicTo(15.8f, 3f, 19f, 5.5f, 19f, 9.5f).cubicTo(19f, 12.5f, 17f, 15.5f, 12f, 21f).close()
            }
            circle(12f, 9.5f, 2.5f)
        },
        IconDef("star", "Excellence", "star quality favourite value") {
            val pts = ArrayList<Float>()
            for (k in 0 until 10) {
                val r = if (k % 2 == 0) 9.5f else 4.2f
                val a = Math.toRadians(-90.0 + k * 36.0)
                pts += 12f + r * cos(a).toFloat(); pts += 12.8f + r * sin(a).toFloat()
            }
            poly(*pts.toFloatArray(), closed = true)
        },
        IconDef("info", "Information", "info knowledge informed notice") {
            circle(12f, 12f, 9f); line(12f, 11f, 12f, 16.5f); dot(12f, 7.8f, 1.3f)
        },
        IconDef("alert", "Risk", "warning alert danger caution problem") {
            poly(12f, 3.5f, 21.5f, 20f, 2.5f, 20f, closed = true); line(12f, 10f, 12f, 14f); dot(12f, 17f, 1.2f)
        },
        IconDef("arrow", "Next", "arrow forward direction proceed") {
            line(4f, 12f, 20f, 12f); poly(14f, 6f, 20f, 12f, 14f, 18f)
        },
    )

    private val byId = all.associateBy { it.id }

    private val defaults = listOf("bulb", "users", "chart", "target", "gear", "book", "heart", "shield", "chat", "flag", "globe", "check")

    fun get(id: String?): IconDef? = id?.let { byId[it] }

    fun defaultFor(index: Int): IconDef = byId.getValue(defaults[index % defaults.size])

    fun search(q: String): List<IconDef> {
        val t = q.trim().lowercase()
        if (t.isEmpty()) return all
        return all.filter { it.name.lowercase().contains(t) || it.tags.contains(t) || it.id.contains(t) }
    }
}


/** Renders icons and icon nodes to images for the user interface. */
object IconRaster {
    /** A glyph (or custom image) in [color], [px] pixels square. */
    fun glyph(def: IconDef?, base64: String?, color: Int, px: Int): java.awt.image.BufferedImage {
        val prims = ArrayList<Prim>()
        val size = 24f
        if (base64 != null) {
            val (w, h) = Images.fit(base64, size)
            prims += Prim.Image(base64, (size - w) / 2f, (size - h) / 2f, w, h)
        } else if (def != null) {
            def.pen.strokes.forEach { prims += Prim.Shape(it, stroke = color, strokeWidth = 2f) }
            def.pen.fills.forEach { prims += Prim.Shape(it, fill = Paint.Solid(color)) }
        }
        return Renderer.image(Scene(size, size, null, prims, Fonts.default), px / size)
    }

    /** The item's icon node exactly as layouts draw it. */
    fun node(doc: ModelDoc, index: Int, outlined: Boolean, px: Int): java.awt.image.BufferedImage {
        val probe = Ctx(doc, AwtMeasure.of(doc.style.fontFamily))
        val item = doc.items[index].copy(color = probe.color(index))
        val d = doc.copy(items = listOf(item), layout = "branches", style = doc.style.copy(shadows = false))
        val ctx = Ctx(d, AwtMeasure.of(d.style.fontFamily))
        val prims = ctx.capture { ctx.iconNode(0, 50f, 50f, 46f, outlined) }
        return Renderer.image(Scene(100f, 100f, null, prims, d.style.fontFamily), px / 100f)
    }
}
