package scimodeler.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

sealed interface Cmd {
    data class M(val x: Float, val y: Float) : Cmd
    data class L(val x: Float, val y: Float) : Cmd
    data class C(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x: Float, val y: Float) : Cmd
    data object Z : Cmd
}

class PathData(val cmds: MutableList<Cmd> = ArrayList()) {
    fun moveTo(x: Float, y: Float) = apply { cmds += Cmd.M(x, y) }
    fun lineTo(x: Float, y: Float) = apply { cmds += Cmd.L(x, y) }
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) =
        apply { cmds += Cmd.C(x1, y1, x2, y2, x, y) }
    fun close() = apply { cmds += Cmd.Z }

    /** Arc on a circle, angles in degrees (0 = +x, clockwise on screen). */
    fun arc(cx: Float, cy: Float, radius: Float, startDeg: Float, endDeg: Float, move: Boolean) = apply {
        val total = endDeg - startDeg
        val segs = ceil(abs(total) / 90.0).toInt().coerceAtLeast(1)
        val step = Math.toRadians(total.toDouble() / segs)
        var a0 = Math.toRadians(startDeg.toDouble())
        val sx = cx + radius * cos(a0).toFloat()
        val sy = cy + radius * sin(a0).toFloat()
        if (move) moveTo(sx, sy) else lineTo(sx, sy)
        repeat(segs) {
            val a1 = a0 + step
            val k = (4.0 / 3.0 * tan(step / 4)).toFloat()
            val x0 = cx + radius * cos(a0).toFloat(); val y0 = cy + radius * sin(a0).toFloat()
            val x3 = cx + radius * cos(a1).toFloat(); val y3 = cy + radius * sin(a1).toFloat()
            val x1 = x0 - k * radius * sin(a0).toFloat(); val y1 = y0 + k * radius * cos(a0).toFloat()
            val x2 = x3 + k * radius * sin(a1).toFloat(); val y2 = y3 - k * radius * cos(a1).toFloat()
            cubicTo(x1, y1, x2, y2, x3, y3)
            a0 = a1
        }
    }

    fun translated(dx: Float, dy: Float) = transformed { x, y -> (x + dx) to (y + dy) }

    /** Applies an affine point mapping (translate/scale/rotate keep Béziers exact). */
    fun transformed(f: (Float, Float) -> Pair<Float, Float>) = PathData(cmds.mapTo(ArrayList()) {
        when (it) {
            is Cmd.M -> f(it.x, it.y).let { (x, y) -> Cmd.M(x, y) }
            is Cmd.L -> f(it.x, it.y).let { (x, y) -> Cmd.L(x, y) }
            is Cmd.C -> {
                val (a, b) = f(it.x1, it.y1); val (c, d) = f(it.x2, it.y2); val (e, g) = f(it.x, it.y)
                Cmd.C(a, b, c, d, e, g)
            }
            Cmd.Z -> Cmd.Z
        }
    })

    companion object {
        fun circle(cx: Float, cy: Float, radius: Float) =
            PathData().arc(cx, cy, radius, 0f, 360f, move = true).close()

        fun roundRect(x: Float, y: Float, w: Float, h: Float, rad: Float): PathData {
            val rr = rad.coerceAtMost(minOf(w, h) / 2).coerceAtLeast(0f)
            return PathData()
                .arc(x + w - rr, y + rr, rr, -90f, 0f, move = true)
                .arc(x + w - rr, y + h - rr, rr, 0f, 90f, move = false)
                .arc(x + rr, y + h - rr, rr, 90f, 180f, move = false)
                .arc(x + rr, y + rr, rr, 180f, 270f, move = false)
                .close()
        }

        fun sector(cx: Float, cy: Float, r0: Float, r1: Float, a0: Float, a1: Float) = PathData()
            .arc(cx, cy, r1, a0, a1, move = true)
            .arc(cx, cy, r0, a1, a0, move = false)
            .close()

        fun polygon(vararg pts: Pair<Float, Float>): PathData {
            val p = PathData()
            pts.forEachIndexed { i, (x, y) -> if (i == 0) p.moveTo(x, y) else p.lineTo(x, y) }
            return p.close()
        }

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = PathData().moveTo(x1, y1).lineTo(x2, y2)
    }
}

sealed interface Paint {
    data class Solid(val color: Int) : Paint
    data class Linear(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val c1: Int, val c2: Int) : Paint
}

enum class Align { START, CENTER, END }

sealed interface Prim {
    /** A raster image (base64 PNG) drawn into the box (x, y, w, h). The box already has the image's aspect ratio. */
    data class Image(val base64: String, val x: Float, val y: Float, val w: Float, val h: Float) : Prim

    data class Shape(
        val path: PathData,
        val fill: Paint? = null,
        val stroke: Int? = null,
        val strokeWidth: Float = 0f,
        val dash: List<Float>? = null,
    ) : Prim

    /**
     * A block of text lines. (x, y) is the anchor: horizontally according to [align],
     * vertically the centre of the block if [vCenter], otherwise its top edge.
     */
    data class Text(
        val lines: List<String>,
        val x: Float,
        val y: Float,
        val size: Float,
        val color: Int,
        val bold: Boolean = false,
        val align: Align = Align.CENTER,
        val rotation: Float = 0f,
        val lineHeight: Float = 1.22f,
        val vCenter: Boolean = true,
    ) : Prim {
        fun baselines(): List<Float> {
            val lh = size * lineHeight
            val top = if (vCenter) y - lh * lines.size / 2f else y
            return List(lines.size) { i -> top + lh * i + lh / 2f + size * 0.36f }
        }
    }
}

fun Prim.translated(dx: Float, dy: Float): Prim = when (this) {
    is Prim.Shape -> copy(
        path = path.translated(dx, dy),
        fill = when (val f = fill) {
            is Paint.Linear -> f.copy(x1 = f.x1 + dx, y1 = f.y1 + dy, x2 = f.x2 + dx, y2 = f.y2 + dy)
            else -> f
        },
    )
    is Prim.Text -> copy(x = x + dx, y = y + dy)
    is Prim.Image -> copy(x = x + dx, y = y + dy)
}

class Scene(val width: Float, val height: Float, val background: Int?, val prims: List<Prim>, val fontFamily: String)

fun rad(deg: Float): Double = deg * PI / 180.0
fun px(cx: Float, radius: Float, deg: Float) = cx + radius * cos(rad(deg)).toFloat()
fun py(cy: Float, radius: Float, deg: Float) = cy + radius * sin(rad(deg)).toFloat()
