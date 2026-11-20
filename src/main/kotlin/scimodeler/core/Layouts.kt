package scimodeler.core

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private fun sideOf(deg: Float): Int {
    val c = cos(rad(deg))
    return when {
        c < -0.05 -> -1
        c > 0.05 -> 1
        else -> 0
    }
}

// ---------------------------------------------------------------- Wheel

fun Ctx.wheel(): Size {
    val n = items.size
    val R = 250f + max(0, n - 6) * 16f
    val r = R * 0.42f
    val ring = R + 24f
    val D = ring
    val cx = D; val cy = D
    val sweep = if (n > 0) 360f / n else 360f
    val start = -90f + style.rotation
    val reqs = ArrayList<CalloutReq>()

    val diagram = capture {
        val ringPath = PathData.circle(cx, cy, ring)
        shadow(ringPath)
        shape(ringPath, Paint.Solid(theme.ring))
        for (i in 0 until n) {
            val a0 = start + i * sweep
            val a1 = a0 + sweep
            val mid = (a0 + a1) / 2f
            val c = color(i)
            val path = PathData.sector(cx, cy, r + 10f, R, a0, a1)
            shape(path, fill(c, px(cx, r, mid), py(cy, r, mid), px(cx, R, mid), py(cy, R, mid)),
                stroke = Ink.white, width = 5f)
            val midR = (r + 10f + R) / 2f
            val chord = if (n > 1) 2f * midR * sin(rad(sweep / 2f)).toFloat() * 0.8f else (R - r)
            // Horizontal room depends on where the segment sits: wide at top/bottom, narrow at the sides.
            val sn = kotlin.math.abs(sin(rad(mid))).toFloat()
            val cs = kotlin.math.abs(cos(rad(mid))).toFloat()
            val maxW = min(chord, sn * chord + cs * (R - r - 10f) * 0.86f)
            val sx = px(cx, midR, mid); val sy = py(cy, midR, mid)
            if (icons) {
                val iz = min(34f, (R - r) * 0.26f) * s.coerceAtMost(1.2f)
                val f = fit(m, items[i].label, maxW, (R - r) * 0.5f, 18f * s, 9.5f, bold = true)
                val th = f.lines.size * f.size * 1.22f
                val top = sy - (iz + 6f + th) / 2f
                itemIcon(i, sx, top + iz / 2f, iz, onColor(c))
                text(f.lines, sx, top + iz + 6f + th / 2f, f.size, onColor(c), bold = true)
            } else {
                val f = fit(m, items[i].label, maxW, (R - r) * 0.72f, 20f * s, 10f, bold = true)
                text(f.lines, sx, sy, f.size, onColor(c), bold = true)
            }
            reqs += CalloutReq(i, px(cx, ring, mid), py(cy, ring, mid), sideOf(mid), py(cy, ring + 40f, mid))
        }
        val hub = PathData.circle(cx, cy, r)
        shadow(hub)
        shape(hub, Paint.Solid(Ink.white))
        if (doc.center.isNotBlank()) {
            val f = fit(m, doc.center, r * 1.55f, r * 1.3f, 24f * s, 11f, bold = true)
            text(f.lines, cx, cy, f.size, Ink.text, bold = true)
        }
    }
    return withCallouts(2 * D, 2 * D, reqs, diagram)
}

// ---------------------------------------------------------------- Cycle & hub

private fun Ctx.radialNodes(hub: Boolean): Size {
    val n = items.size
    val Rc = (if (hub) 270f else 225f) + max(0, n - 5) * 26f
    val nodeR = if (n <= 1) 80f else (Rc * sin(PI / n).toFloat() * (if (hub) 0.55f else 0.62f)).coerceIn(48f, 84f)
    val D = Rc + nodeR + 6f
    val cx = D; val cy = D
    val start = -90f + style.rotation
    val step = if (n > 0) 360f / n else 360f
    val reqs = ArrayList<CalloutReq>()
    val hubR = 105f

    val diagram = capture {
        if (hub) {
            for (i in 0 until n) {
                val a = start + i * step
                val c = color(i)
                shape(PathData.line(px(cx, hubR, a), py(cy, hubR, a), px(cx, Rc - nodeR, a), py(cy, Rc - nodeR, a)),
                    null, lighten(c, 0.25f), 5f)
            }
            val core = PathData.circle(cx, cy, hubR)
            shadow(core)
            val hc = theme.hub
            shape(core, fill(hc, cx, cy - hubR, cx, cy + hubR))
            if (doc.center.isNotBlank()) {
                val f = fit(m, doc.center, hubR * 1.6f, hubR * 1.35f, 24f * s, 11f, bold = true)
                text(f.lines, cx, cy, f.size, onColor(hc), bold = true)
            }
        } else {
            // Arrows between consecutive nodes along the ring.
            if (n > 1) {
                val off = Math.toDegrees(asin(((nodeR + 14f) / Rc).coerceAtMost(0.99f).toDouble())).toFloat()
                for (i in 0 until n) {
                    val a0 = start + i * step + off
                    val a1 = start + (i + 1) * step - off - 3f
                    if (a1 <= a0) continue
                    val c = color(i)
                    shape(PathData().arc(cx, cy, Rc, a0, a1, move = true), null, lighten(c, 0.2f), 5f)
                    val e = rad(a1).toFloat()
                    val tx = -sin(e); val ty = cos(e)
                    val nx = cos(e); val ny = sin(e)
                    val bx = cx + Rc * nx; val by = cy + Rc * ny
                    shape(PathData.polygon(
                        (bx + tx * 14f) to (by + ty * 14f),
                        (bx + nx * 9f - tx * 2f) to (by + ny * 9f - ty * 2f),
                        (bx - nx * 9f - tx * 2f) to (by - ny * 9f - ty * 2f),
                    ), Paint.Solid(lighten(c, 0.2f)))
                }
            }
            if (doc.center.isNotBlank()) {
                val inner = (Rc - nodeR) * 1.45f
                val f = fit(m, doc.center, inner, inner * 0.8f, 26f * s, 11f, bold = true)
                text(f.lines, cx, cy, f.size, Ink.text, bold = true)
            }
        }
        for (i in 0 until n) {
            val a = start + i * step
            val c = color(i)
            val nx = if (n == 1 && !hub) cx else px(cx, Rc, a)
            val ny = if (n == 1 && !hub) cy - Rc else py(cy, Rc, a)
            val node = PathData.circle(nx, ny, nodeR)
            shadow(node)
            shape(node, fill(c, nx, ny - nodeR, nx, ny + nodeR), Ink.white, 4f)
            if (icons) {
                val iz = nodeR * 0.62f
                if (items[i].image != null) shape(PathData.circle(nx, ny - nodeR * 0.3f, iz * 0.62f), Paint.Solid(Ink.white))
                itemIcon(i, nx, ny - nodeR * 0.3f, if (items[i].image != null) iz * 0.85f else iz, onColor(c))
                val f = fit(m, items[i].label, nodeR * 1.5f, nodeR * 0.6f, 15f * s, 8.5f, bold = true)
                text(f.lines, nx, ny + nodeR * 0.36f, f.size, onColor(c), bold = true)
            } else {
                val f = fit(m, items[i].label, nodeR * 1.62f, nodeR * 1.3f, 18f * s, 9.5f, bold = true)
                text(f.lines, nx, ny, f.size, onColor(c), bold = true)
            }
            reqs += CalloutReq(i, px(nx, nodeR + 4f, a), py(ny, nodeR + 4f, a), sideOf(a), ny, nx, ny, nodeR)
        }
    }
    return withCallouts(2 * D, 2 * D, reqs, diagram)
}

fun Ctx.cycle() = radialNodes(hub = false)
fun Ctx.hub() = radialNodes(hub = true)

// ---------------------------------------------------------------- Concentric (onion)

fun Ctx.onion(): Size {
    val n = items.size
    val r0 = 95f
    val t = min(78f, (420f - r0) / max(n, 1))
    val D = r0 + n * t + 4f
    val cx = D; val cy = D
    val reqs = ArrayList<CalloutReq>()
    val diagram = capture {
        val outer = PathData.circle(cx, cy, r0 + n * t)
        shadow(outer)
        for (i in n - 1 downTo 0) {
            val rr = r0 + (i + 1) * t
            val c = color(i)
            shape(PathData.circle(cx, cy, rr), fill(c, cx, cy - rr, cx, cy + rr), Ink.white, 3f)
        }
        for (i in 0 until n) {
            val c = color(i)
            val bandMid = r0 + i * t + t / 2f
            val y = cy - bandMid
            val rOut = r0 + (i + 1) * t
            // Shrink until the text block's top corners stay inside the band.
            var size = 18f * s
            var f: Fitted
            while (true) {
                val lines1 = wrap(m, items[i].label, 10000f, size, true).size.coerceAtLeast(1)
                var maxW = 0f
                var lines = lines1
                repeat(3) {
                    val hh = lines * size * 1.22f
                    maxW = 2f * sqrt(max(0f, rOut * rOut - (bandMid + hh / 2f) * (bandMid + hh / 2f))) * 0.92f
                    lines = wrap(m, items[i].label, maxW, size, true).size.coerceAtLeast(1)
                }
                f = Fitted(wrap(m, items[i].label, maxW, size, true), size)
                val ok = f.lines.size * size * 1.22f <= t * 0.82f &&
                    f.lines.all { m.width(it, size, true) <= maxW + 0.5f } && f.lines.none { it.endsWith("-") }
                if (ok || size <= 9f) break
                size -= 0.5f
            }
            text(f.lines, cx, y, f.size, onColor(c), bold = true)
            val span = if (n > 1) 70f / (n - 1) else 0f
            val ang = -35f + i * span + style.rotation
            reqs += CalloutReq(i, px(cx, bandMid, ang), py(cy, bandMid, ang), 1, py(cy, bandMid, ang))
        }
        val core = PathData.circle(cx, cy, r0)
        val hc = theme.hub
        shape(core, fill(hc, cx, cy - r0, cx, cy + r0), Ink.white, 3f)
        if (doc.center.isNotBlank()) {
            val f = fit(m, doc.center, r0 * 1.55f, r0 * 1.3f, 22f * s, 10f, bold = true)
            text(f.lines, cx, cy, f.size, onColor(hc), bold = true)
        }
        // Anchor dots on the rings.
        if (style.showCallouts) for (rq in reqs) if (calloutHeight(rq.item) > 0f) {
            shape(PathData.circle(rq.ax, rq.ay, 6f), Paint.Solid(Ink.white))
        }
    }
    return withCallouts(2 * D, 2 * D, reqs, diagram)
}

// ---------------------------------------------------------------- Pyramid

fun Ctx.pyramid(): Size {
    val n = items.size
    val h = 96f
    val gap = 7f
    val Wb = 640f + max(0, n - 5) * 40f
    val Hp = n * h
    val wTop = Wb * 0.2f
    fun wAt(y: Float) = wTop + (Wb - wTop) * (y / max(Hp, 1f))
    val cx = Wb / 2f
    val reqs = ArrayList<CalloutReq>()
    val diagram = capture {
        for (i in 0 until n) {
            val y0 = i * h
            val y1 = y0 + h - gap
            val w0 = wAt(y0); val w1 = wAt(y1)
            val c = color(i)
            val p = PathData.polygon(
                (cx - w0 / 2f) to y0, (cx + w0 / 2f) to y0,
                (cx + w1 / 2f) to y1, (cx - w1 / 2f) to y1,
            )
            shadow(p, 0.8f)
            shape(p, fill(c, cx, y0, cx, y1))
            val f = fit(m, items[i].label, w0 * 0.86f, (h - gap) * 0.8f, 20f * s, 10f, bold = true)
            text(f.lines, cx, (y0 + y1) / 2f, f.size, onColor(c), bold = true)
            val ym = (y0 + y1) / 2f
            reqs += CalloutReq(i, cx + wAt(ym) / 2f + 6f, ym, 1, ym)
        }
    }
    return withCallouts(Wb, Hp, reqs, diagram)
}

// ---------------------------------------------------------------- Venn

fun Ctx.venn(): Size {
    val n = items.size
    val r = 190f
    val dist = when (n) { 1 -> 0f; 2 -> 118f; 3 -> 122f; else -> 128f }
    val angles = when (n) {
        1 -> listOf(-90f)
        2 -> listOf(180f, 0f)
        3 -> listOf(-90f, 30f, 150f)
        else -> listOf(-135f, -45f, 45f, 135f)
    }.map { it + style.rotation }
    val ext = dist + r + 8f
    val cx = ext; val cy = ext
    val reqs = ArrayList<CalloutReq>()
    val diagram = capture {
        for (i in 0 until n) {
            val c = color(i)
            val x = px(cx, dist, angles[i]); val y = py(cy, dist, angles[i])
            shape(PathData.circle(x, y, r), Paint.Solid(withAlpha(c, 0.42f)), c, 3f)
        }
        for (i in 0 until n) {
            val c = color(i)
            val labelR = if (n == 1) 0f else dist + r * 0.5f
            val lx = px(cx, labelR, angles[i]); val ly = py(cy, labelR, angles[i])
            val f = fit(m, items[i].label, r * 0.85f, r * 0.6f, 20f * s, 10f, bold = true)
            text(f.lines, lx, ly, f.size, darken(c, 0.45f), bold = true)
            val ax = px(cx, dist + r + 4f, angles[i]); val ay = py(cy, dist + r + 4f, angles[i])
            reqs += CalloutReq(i, ax, ay, sideOf(angles[i]), ay)
        }
        if (doc.center.isNotBlank() && n >= 2) {
            val f = fit(m, doc.center, r * 0.62f, r * 0.5f, 17f * s, 9f, bold = true)
            val pill = maxOf(f.lines.maxOf { m.width(it, f.size, true) } + 28f, 60f)
            val ph = f.lines.size * f.size * 1.22f + 16f
            val p = PathData.roundRect(cx - pill / 2f, cy - ph / 2f, pill, ph, ph / 2f)
            shadow(p, 0.7f)
            shape(p, Paint.Solid(Ink.white))
            text(f.lines, cx, cy, f.size, Ink.text, bold = true)
        }
    }
    return withCallouts(2 * ext, 2 * ext, reqs, diagram)
}

// ---------------------------------------------------------------- Process

fun Ctx.process(): Size {
    val n = max(items.size, 1)
    val stepW = (1320f / n).coerceIn(200f, 300f)
    val p = 30f
    val h = 104f
    val bannerH = if (doc.center.isNotBlank()) 64f else 0f
    val top = if (bannerH > 0f) bannerH + 34f else 18f
    val totalW = n * stepW + p
    val cardW = stepW - p - 4f
    val cardTop = top + h + 26f
    var maxCard = 0f
    if (doc.center.isNotBlank()) {
        val b = PathData.roundRect(0f, 0f, totalW, bannerH, bannerH / 2f)
        shadow(b, 0.6f)
        shape(b, fill(theme.hub, 0f, 0f, totalW, 0f))
        val f = fit(m, doc.center, totalW - 60f, bannerH - 14f, 22f * s, 11f, bold = true)
        text(f.lines, totalW / 2f, bannerH / 2f, f.size, onColor(theme.hub), bold = true)
    }
    for (i in items.indices) {
        val L = i * stepW
        val R = L + stepW - 6f + p
        val c = color(i)
        val path = PathData()
        path.moveTo(L, top).lineTo(R - p, top).lineTo(R, top + h / 2f).lineTo(R - p, top + h).lineTo(L, top + h)
        if (i > 0) path.lineTo(L + p, top + h / 2f)
        path.close()
        shadow(path, 0.8f)
        shape(path, fill(c, L, top, R, top + h))
        val innerL = if (i > 0) L + p else L + 12f
        val innerW = (R - p) - innerL
        val f = fit(m, items[i].label, innerW - 8f, h - 26f, 19f * s, 10f, bold = true)
        text(f.lines, innerL + innerW / 2f, top + h / 2f + 6f, f.size, onColor(c), bold = true)
        // Step number badge
        val bx = innerL + innerW / 2f
        shape(PathData.circle(bx, top, 16f), Paint.Solid(Ink.white), c, 3f)
        text(listOf("${i + 1}"), bx, top, 14f, darken(c, 0.2f), bold = true)
        if (style.showCallouts && items[i].bullets.any { it.isNotBlank() }) {
            val cx0 = (if (i > 0) L + p else L) + 2f
            val ch = calloutHeightFor(i, cardW)
            callout(i, cx0, cardTop, cardW, ch, 0)
            maxCard = max(maxCard, ch)
        }
    }
    return Size(totalW, cardTop + (if (maxCard > 0f) maxCard else -26f))
}

private fun Ctx.calloutHeightFor(i: Int, w: Float): Float =
    bullets(items[i].bullets, 0f, 0f, w - 18f * 2 - 6f, 0, 0, null, draw = false) + 18f * 2 + 6f

// ---------------------------------------------------------------- Matrix / grid

fun Ctx.matrix(): Size {
    val n = max(items.size, 1)
    val cols = when { n <= 4 -> 2; n <= 9 -> 3; else -> 4 }
    val rows = ceil(n / cols.toDouble()).toInt()
    val cellW = 340f
    val gap = 14f
    val pad = 24f
    val quad = n == 4 && doc.center.isNotBlank()
    val bannerH = if (!quad && doc.center.isNotBlank()) 64f else 0f
    val top = if (bannerH > 0f) bannerH + 22f else 0f
    // Uniform cell height = tallest content.
    var cellH = 200f
    for (i in items.indices) {
        val lab = fit(m, items[i].label, cellW - pad * 2, 200f, 21f * s, 11f, bold = true)
        val lh = lab.lines.size * lab.size * 1.22f
        val bh = if (style.showCallouts) bullets(items[i].bullets, 0f, 0f, cellW - pad * 2, 0, 0, null, false) else 0f
        cellH = max(cellH, pad * 2 + lh + (if (bh > 0f) 14f + bh else 0f) + (if (quad) 30f else 0f))
    }
    val totalW = cols * cellW + (cols - 1) * gap
    if (bannerH > 0f) {
        val b = PathData.roundRect(0f, 0f, totalW, bannerH, bannerH / 2f)
        shadow(b, 0.6f)
        shape(b, fill(theme.hub, 0f, 0f, totalW, 0f))
        val f = fit(m, doc.center, totalW - 60f, bannerH - 14f, 22f * s, 11f, bold = true)
        text(f.lines, totalW / 2f, bannerH / 2f, f.size, onColor(theme.hub), bold = true)
    }
    for (i in items.indices) {
        val col = i % cols; val row = i / cols
        val x = col * (cellW + gap); val y = top + row * (cellH + gap)
        val c = color(i)
        val cell = PathData.roundRect(x, y, cellW, cellH, 20f)
        shadow(cell, 0.8f)
        shape(cell, fill(c, x, y, x + cellW, y + cellH))
        val fg = onColor(c)
        // In the 2×2 quadrant variant, push text away from the centre circle.
        val alignRight = quad && col == 1
        val tx = if (alignRight) x + cellW - pad else x + pad
        val ty = if (quad && row == 1) y + pad + 30f else y + pad
        val lab = fit(m, items[i].label, cellW - pad * 2 - (if (icons) 44f else 0f), 200f, 21f * s, 11f, bold = true)
        if (icons) {
            val ix = if (alignRight) x + pad + 18f else x + cellW - pad - 18f
            itemIcon(i, ix, ty + 16f, 34f, withAlpha(fg, 0.9f))
        }
        add(Prim.Text(lab.lines, tx, ty, lab.size, fg, bold = true,
            align = if (alignRight) Align.END else Align.START, vCenter = false))
        if (style.showCallouts) {
            val by = ty + lab.lines.size * lab.size * 1.22f + 14f
            bullets(items[i].bullets, x + pad, by, cellW - pad * 2, fg, withAlpha(fg, 0.8f), withAlpha(fg, 0.35f), true)
        }
    }
    val totalH = top + rows * cellH + (rows - 1) * gap
    if (quad) {
        val cx = totalW / 2f; val cy = top + cellH + gap / 2f
        val rr = 84f
        val circle = PathData.circle(cx, cy, rr)
        shadow(circle)
        shape(circle, Paint.Solid(Ink.white))
        val f = fit(m, doc.center, rr * 1.55f, rr * 1.3f, 20f * s, 10f, bold = true)
        text(f.lines, cx, cy, f.size, Ink.text, bold = true)
    }
    return Size(totalW, totalH)
}

// ---------------------------------------------------------------- Layers

fun Ctx.layers(): Size {
    val n = items.size
    val totalW = 1120f
    val labelW = 300f
    val pad = 20f
    val gap = 12f
    var y = 0f
    for (i in 0 until n) {
        val c = color(i)
        val descW = totalW - labelW - pad * 2
        val bh = if (style.showCallouts) bullets(items[i].bullets, 0f, 0f, descW, 0, 0, null, false) else 0f
        val lab = fit(m, items[i].label, labelW - pad * 2, 400f, 21f * s, 11f, bold = true)
        val labH = lab.lines.size * lab.size * 1.22f
        val h = max(if (icons) 112f else 84f, max(bh, labH + (if (icons) 38f else 0f)) + pad * 2)
        val whole = PathData.roundRect(0f, y, totalW, h, 18f)
        shadow(whole, 0.7f)
        shape(whole, Paint.Solid(lighten(c, 0.88f)))
        // Coloured label block: rounded on the left, square on the right.
        val lp = PathData()
            .arc(18f, y + 18f, 18f, 180f, 270f, move = true)
            .lineTo(labelW, y).lineTo(labelW, y + h)
            .arc(18f, y + h - 18f, 18f, 90f, 180f, move = false)
            .close()
        shape(lp, fill(c, 0f, y, labelW, y + h))
        if (icons && h >= 84f) {
            val iz = 30f
            val total = iz + 8f + labH
            itemIcon(i, labelW / 2f, y + (h - total) / 2f + iz / 2f, iz, onColor(c))
            text(lab.lines, labelW / 2f, y + (h - total) / 2f + iz + 8f + labH / 2f, lab.size, onColor(c), bold = true)
        } else {
            text(lab.lines, labelW / 2f, y + h / 2f, lab.size, onColor(c), bold = true)
        }
        if (bh > 0f) {
            bullets(items[i].bullets, labelW + pad, y + (h - bh) / 2f, descW, Ink.text, c, lighten(c, 0.45f), true)
        }
        y += h + gap
    }
    return Size(totalW, max(0f, y - gap))
}
