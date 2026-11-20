package scimodeler.core

import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.roundToInt

object Renderer {
    private const val MARGIN = 44f

    fun scene(doc: ModelDoc, m: Measure = AwtMeasure.of(doc.style.fontFamily)): Scene {
        val ctx = Ctx(doc, m)
        val s = ctx.s
        var body = Size(0f, 0f)
        val bodyPrims = ctx.capture {
            body = doc.spec.render(ctx)
        }
        val contentW = max(body.w, 520f)
        val W = contentW + MARGIN * 2
        var y = MARGIN

        val out = ArrayList<Prim>()
        if (doc.style.showTitle && doc.title.isNotBlank()) {
            val ts = 40f * s
            val lines = wrap(m, doc.title, contentW, ts, true)
            out += Prim.Text(lines, W / 2f, y, ts, Ink.text, bold = true, vCenter = false, lineHeight = 1.15f)
            y += lines.size * ts * 1.15f
            if (doc.subtitle.isNotBlank()) {
                val ss = 19f * s
                val sl = wrap(m, doc.subtitle, contentW, ss, false)
                y += 6f
                out += Prim.Text(sl, W / 2f, y, ss, Ink.subtle, vCenter = false)
                y += sl.size * ss * 1.22f
            }
            y += 36f
        }
        val bx = MARGIN + (contentW - body.w) / 2f
        bodyPrims.forEach { out += it.translated(bx, y) }
        y += body.h

        if (doc.style.showLegend && doc.legend.isNotBlank()) {
            y += 34f
            val ls = 15f * s
            val maxW = minOf(contentW * 0.8f, 900f)
            val lines = wrap(m, doc.legend, maxW - 40f, ls, false)
            val textW = lines.maxOf { m.width(it, ls, false) }
            val bw = textW + 48f
            val bh = lines.size * ls * 1.3f + 30f
            out += Prim.Shape(PathData.roundRect(W / 2f - bw / 2f, y, bw, bh, 18f), Paint.Solid(Ink.panel))
            out += Prim.Text(lines, W / 2f, y + bh / 2f, ls, Ink.subtle, lineHeight = 1.3f)
            y += bh
        }
        val H = y + MARGIN
        val bg = if (doc.style.transparentBackground) null else Ink.white
        return Scene(W, H, bg, out, doc.style.fontFamily)
    }

    // ------------------------------------------------------------ AWT / PNG

    fun image(scene: Scene, scale: Float): BufferedImage {
        val w = (scene.width * scale).roundToInt().coerceAtLeast(1)
        val h = (scene.height * scale).roundToInt().coerceAtLeast(1)
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.scale(scale.toDouble(), scale.toDouble())
            scene.background?.let { g.color = Color(it, true); g.fillRect(0, 0, scene.width.toInt() + 1, scene.height.toInt() + 1) }
            val measure = AwtMeasure.of(scene.fontFamily)
            for (p in scene.prims) when (p) {
                is Prim.Shape -> {
                    val path = toAwt(p.path)
                    when (val f = p.fill) {
                        is Paint.Solid -> { g.paint = Color(f.color, true); g.fill(path) }
                        is Paint.Linear -> {
                            g.paint = GradientPaint(f.x1, f.y1, Color(f.c1, true), f.x2, f.y2, Color(f.c2, true))
                            g.fill(path)
                        }
                        null -> {}
                    }
                    if (p.stroke != null && p.strokeWidth > 0f) {
                        g.paint = Color(p.stroke, true)
                        g.stroke = if (p.dash != null) BasicStroke(p.strokeWidth, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND, 10f, p.dash.toFloatArray(), 0f)
                        else BasicStroke(p.strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                        g.draw(path)
                    }
                }
                is Prim.Image -> {
                    Images.decode(p.base64)?.let { img ->
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                        val at = AffineTransform(p.w / img.width, 0f, 0f, p.h / img.height, p.x, p.y)
                        g.drawImage(img, at, null)
                    }
                }
                is Prim.Text -> {
                    val saved: AffineTransform = g.transform
                    if (p.rotation != 0f) g.rotate(rad(p.rotation), p.x.toDouble(), p.y.toDouble())
                    g.font = measure.font(p.size, p.bold)
                    g.color = Color(p.color, true)
                    val bases = p.baselines()
                    p.lines.forEachIndexed { i, line ->
                        val lw = measure.width(line, p.size, p.bold)
                        val lx = when (p.align) { Align.START -> p.x; Align.CENTER -> p.x - lw / 2f; Align.END -> p.x - lw }
                        g.drawString(line, lx, bases[i])
                    }
                    g.transform = saved
                }
            }
        } finally {
            g.dispose()
        }
        return img
    }

    private fun toAwt(p: PathData): Path2D.Float {
        val path = Path2D.Float()
        for (c in p.cmds) when (c) {
            is Cmd.M -> path.moveTo(c.x, c.y)
            is Cmd.L -> path.lineTo(c.x, c.y)
            is Cmd.C -> path.curveTo(c.x1, c.y1, c.x2, c.y2, c.x, c.y)
            Cmd.Z -> path.closePath()
        }
        return path
    }

    // ------------------------------------------------------------ SVG

    fun svg(scene: Scene, widthCm: Float? = null): String {
        val sb = StringBuilder()
        val defs = StringBuilder()
        var gid = 0
        fun n(v: Float): String {
            val r = (v * 100f).roundToInt() / 100f
            return if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString()
        }
        fun col(c: Int) = toHex(c)
        fun opacity(attr: String, c: Int) = if (a(c) < 255) " $attr=\"${n(a(c) / 255f)}\"" else ""
        fun pathD(p: PathData) = p.cmds.joinToString(" ") {
            when (it) {
                is Cmd.M -> "M${n(it.x)} ${n(it.y)}"
                is Cmd.L -> "L${n(it.x)} ${n(it.y)}"
                is Cmd.C -> "C${n(it.x1)} ${n(it.y1)} ${n(it.x2)} ${n(it.y2)} ${n(it.x)} ${n(it.y)}"
                Cmd.Z -> "Z"
            }
        }
        val family = esc(scene.fontFamily)
        val (sw, sh) = if (widthCm != null) "${n(widthCm)}cm" to "${n(widthCm * scene.height / scene.width)}cm"
            else n(scene.width) to n(scene.height)
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="$sw" height="$sh" """)
        sb.append("""viewBox="0 0 ${n(scene.width)} ${n(scene.height)}" font-family="'$family', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif">""")
        sb.append('\n')
        val bodySb = StringBuilder()
        scene.background?.let { bodySb.append("""<rect width="100%" height="100%" fill="${col(it)}"/>""").append('\n') }
        for (p in scene.prims) when (p) {
            is Prim.Shape -> {
                bodySb.append("<path d=\"").append(pathD(p.path)).append('"')
                when (val f = p.fill) {
                    is Paint.Solid -> bodySb.append(" fill=\"${col(f.color)}\"").append(opacity("fill-opacity", f.color))
                    is Paint.Linear -> {
                        val id = "g${gid++}"
                        defs.append("""<linearGradient id="$id" gradientUnits="userSpaceOnUse" x1="${n(f.x1)}" y1="${n(f.y1)}" x2="${n(f.x2)}" y2="${n(f.y2)}">""")
                        defs.append("""<stop offset="0" stop-color="${col(f.c1)}"${opacity("stop-opacity", f.c1)}/>""")
                        defs.append("""<stop offset="1" stop-color="${col(f.c2)}"${opacity("stop-opacity", f.c2)}/></linearGradient>""").append('\n')
                        bodySb.append(" fill=\"url(#$id)\"")
                    }
                    null -> bodySb.append(" fill=\"none\"")
                }
                if (p.stroke != null && p.strokeWidth > 0f) {
                    bodySb.append(" stroke=\"${col(p.stroke)}\"").append(opacity("stroke-opacity", p.stroke))
                    bodySb.append(" stroke-width=\"${n(p.strokeWidth)}\" stroke-linecap=\"round\" stroke-linejoin=\"round\"")
                    p.dash?.let { d -> bodySb.append(" stroke-dasharray=\"${d.joinToString(" ") { n(it) }}\"") }
                }
                bodySb.append("/>\n")
            }
            is Prim.Image -> {
                val href = "data:image/png;base64,${p.base64}"
                bodySb.append("""<image x="${n(p.x)}" y="${n(p.y)}" width="${n(p.w)}" height="${n(p.h)}" preserveAspectRatio="xMidYMid meet" href="$href" xlink:href="$href"/>""").append('\n')
            }
            is Prim.Text -> {
                val anchor = when (p.align) { Align.START -> "start"; Align.CENTER -> "middle"; Align.END -> "end" }
                val rot = if (p.rotation != 0f) """ transform="rotate(${n(p.rotation)} ${n(p.x)} ${n(p.y)})"""" else ""
                val weight = if (p.bold) " font-weight=\"700\"" else ""
                val bases = p.baselines()
                p.lines.forEachIndexed { i, line ->
                    bodySb.append("""<text x="${n(p.x)}" y="${n(bases[i])}" font-size="${n(p.size)}"$weight fill="${col(p.color)}"${opacity("fill-opacity", p.color)} text-anchor="$anchor"$rot>""")
                    bodySb.append(esc(line)).append("</text>\n")
                }
            }
        }
        if (defs.isNotEmpty()) sb.append("<defs>\n").append(defs).append("</defs>\n")
        sb.append(bodySb).append("</svg>\n")
        return sb.toString()
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
