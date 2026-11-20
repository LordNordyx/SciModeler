package scimodeler.core

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Layouts inspired by common infographic structures (branch lists, arcs, cones, funnels,
 * timelines, petal diagrams, research frameworks). All share the helpers in Ctx so that
 * models placed side by side read as one family.
 */

private val Ctx.connector: Int get() = theme.connector

private fun Ctx.lineShape(x1: Float, y1: Float, x2: Float, y2: Float, c: Int = connector, w: Float = 5f) =
    shape(PathData.line(x1, y1, x2, y2), null, c, w)

/** Shifts a captured drawing so its left edge is at x = 0. */
private fun Ctx.placeShifted(prims: List<Prim>, minX: Float) = place(prims, -minX, 0f)

private fun Ctx.numberBadge(i: Int, cx: Float, cy: Float, r: Float) {
    val c = color(i)
    shape(PathData.circle(cx, cy, r), Paint.Solid(Ink.white), c, max(3f, r * 0.12f))
    text(listOf("${i + 1}"), cx, cy, r * 0.8f, readable(c), bold = true)
}

private fun Ctx.centerText(cx: Float, cy: Float, w: Float, h: Float, color: Int, start: Float = 26f) {
    if (doc.center.isBlank()) return
    val f = fit(m, doc.center, w, h, start * s, 11f, bold = true)
    text(f.lines, cx, cy, f.size, color, bold = true)
}

// ---------------------------------------------------------------- Branches (core → list)

fun Ctx.branches(): Size {
    val n = items.size
    val coreR = 118f
    val nodeR = 40f
    val textW = 330f
    val trunkX = coreR * 2 + 70f
    val nodeX = trunkX + 56f + nodeR
    val textX = nodeX + nodeR + 24f
    val rowHs = items.indices.map { max(nodeR * 2 + 22f, textBlock(it, 0f, 0f, textW, false) + 22f) }
    val totalH = max(coreR * 2 + 20f, rowHs.sum())
    var y = (totalH - rowHs.sum()) / 2f
    val cys = rowHs.map { h -> val c = y + h / 2f; y += h; c }
    val cx = coreR; val cy = totalH / 2f

    if (n > 0) {
        lineShape(cx + coreR, cy, trunkX, cy)
        lineShape(trunkX, min(cys.first(), cy), trunkX, max(cys.last(), cy))
        cys.forEach { lineShape(trunkX, it, nodeX - nodeR, it) }
    }
    val ring = PathData.circle(cx, cy, coreR)
    shadow(ring)
    shape(ring, Paint.Solid(Ink.white), connector, 12f)
    shape(PathData.circle(cx, cy, coreR - 20f), Paint.Solid(theme.panel))
    centerText(cx, cy, (coreR - 20f) * 1.6f, (coreR - 20f) * 1.3f, theme.hub, 30f)

    for (i in 0 until n) {
        if (icons) iconNode(i, nodeX, cys[i], nodeR, outlined = true) else numberBadge(i, nodeX, cys[i], nodeR)
        val th = textBlock(i, textX, 0f, textW, false)
        textBlock(i, textX, cys[i] - th / 2f, textW, true)
    }
    return Size(textX + textW, totalH)
}

// ---------------------------------------------------------------- Arc list (core with stages on an arc)

fun Ctx.arcList(): Size {
    val n = items.size
    val nodeR = 44f
    val textW = 300f
    val rowH = max(nodeR * 2 + 20f, (items.indices.maxOfOrNull { textBlock(it, 0f, 0f, textW, false) } ?: 0f) + 18f)
    val Ra = max(250f, ((n - 1) * rowH / 2f) / 0.87f)
    val bigR = Ra - nodeR - 18f
    val H = max(2 * bigR + 20f, n * rowH)
    val cx = bigR + 8f; val cy = H / 2f
    val ys = List(n) { cy + (it - (n - 1) / 2f) * rowH }
    val xs = ys.map { cx + sqrt(max(0f, Ra * Ra - (it - cy) * (it - cy))) }

    if (n > 0) {
        val amax = Math.toDegrees(kotlin.math.asin(((ys.last() - cy) / Ra).coerceIn(-1f, 1f).toDouble())).toFloat() + 9f
        shape(PathData().arc(cx, cy, Ra, -amax, amax, move = true), null, connector, 3f)
        listOf(-amax, amax).forEach { a -> shape(PathData.circle(px(cx, Ra, a), py(cy, Ra, a), 6f), Paint.Solid(connector)) }
    }
    val big = PathData.circle(cx, cy, bigR)
    shadow(big, 0.6f)
    shape(big, Paint.Solid(Ink.white), connector, 4f)
    centerText(cx, cy, bigR * 1.5f, bigR * 1.1f, theme.hub, 34f)

    var maxX = cx + bigR
    for (i in 0 until n) {
        if (icons) iconNode(i, xs[i], ys[i], nodeR, outlined = true) else numberBadge(i, xs[i], ys[i], nodeR)
        val tx = xs[i] + nodeR + 22f
        val th = textBlock(i, tx, 0f, textW, false)
        textBlock(i, tx, ys[i] - th / 2f, textW, true)
        maxX = max(maxX, tx + textW)
    }
    return Size(maxX, H)
}

// ---------------------------------------------------------------- Cone (layered wedge, notes alternate sides)

fun Ctx.cone(): Size {
    val n = max(items.size, 1)
    val R = 420f + max(0, n - 5) * 30f
    val half = 33f
    val t = R / n
    val apexY = R + 12f
    val boxW = 360f
    val a0 = -90f - half; val a1 = -90f + half
    val prims = capture {
        // Notes behind the cone
        for (i in items.indices) {
            val rOut = R - i * t
            val rIn = if (i == n - 1) 0f else R - (i + 1) * t
            val rm = if (i == n - 1) rOut * 0.6f else (rIn + rOut) / 2f
            val yc = apexY - rm
            val e = rm * sin(rad(half)).toFloat()
            val side = if (i % 2 == 0) -1 else 1
            val inner = 34f
            val tw = boxW - inner - 44f
            val th = textBlock(i, 0f, 0f, tw, false)
            val bh = max(th + 32f, 70f)
            val x0 = if (side == -1) -e + inner - boxW else e - inner
            shape(PathData.roundRect(x0, yc - bh / 2f, boxW, bh, 18f), Paint.Solid(theme.panel))
            val tx = if (side == -1) x0 + 22f else x0 + inner + 22f
            textBlock(i, tx, yc - th / 2f, tw, true, align = if (side == -1) Align.START else Align.END)
        }
        // Cone bands
        val outline = PathData().arc(0f, apexY, R, a0, a1, move = true).lineTo(0f, apexY).close()
        shadow(outline)
        for (i in items.indices) {
            val c = color(i)
            val rOut = R - i * t
            val path = if (i == n - 1) PathData().arc(0f, apexY, rOut, a0, a1, move = true).lineTo(0f, apexY).close()
                else PathData.sector(0f, apexY, R - (i + 1) * t, rOut, a0, a1)
            val ex = rOut * sin(rad(half)).toFloat()
            shape(path, fill(c, -ex, 0f, ex, 0f), Ink.white, 4f)
            val rm = if (i == n - 1) rOut * 0.58f else rOut - t / 2f
            val yc = apexY - rm
            if (icons) itemIcon(i, 0f, yc, min(t * 0.42f, 42f), onColor(c))
            else text(listOf("${i + 1}"), 0f, yc, min(t * 0.36f, 30f), onColor(c), bold = true)
        }
    }
    val minX = prims.minX(); val maxX = prims.maxX()
    placeShifted(prims, minX)
    return Size(maxX - minX, apexY + 8f)
}

// ---------------------------------------------------------------- Funnel

fun Ctx.funnel(): Size {
    val n = items.size
    val h = 92f; val gap = 14f
    val Wt = 700f; val Wb = 230f
    val Htot = max(1f, n * h + (n - 1) * gap)
    fun w(y: Float) = Wt - (Wt - Wb) * (y / Htot)
    val tw = 290f
    val cx = tw + 40f + Wt / 2f
    var tail = 0f
    for (i in 0 until n) {
        val c = color(i)
        val y0 = i * (h + gap); val y1 = y0 + h
        val p = PathData.polygon(
            (cx - w(y0) / 2f) to y0, (cx + w(y0) / 2f) to y0,
            (cx + w(y1) / 2f) to y1, (cx - w(y1) / 2f) to y1,
        )
        shadow(p, 0.8f)
        shape(p, fill(c, cx - w(y0) / 2f, y0, cx + w(y0) / 2f, y1))
        val f = fit(m, items[i].label, w(y1) * 0.8f, h * 0.62f, 21f * s, 10f, bold = true)
        text(f.lines, cx, y0 + h / 2f - 4f, f.size, onColor(c), bold = true)
        // Marker on the lower edge and a leader line to the note
        val side = if (i % 2 == 0) -1 else 1
        val mx = cx + side * w(y1) * 0.3f
        val my = y1
        val tx = if (side == -1) cx - Wt / 2f - 40f - tw else cx + Wt / 2f + 40f
        val lx = if (side == -1) tx + tw + 12f else tx - 12f
        lineShape(mx, my, lx, my, lighten(c, 0.1f), 2.5f)
        shape(PathData.circle(lx, my, 4f), Paint.Solid(c))
        shape(PathData.circle(mx, my, 19f), Paint.Solid(c), Ink.white, 5f)
        shape(PathData.circle(mx, my, 5.5f), Paint.Solid(Ink.white))
        val th = textBlock(i, tx, 0f, tw, false)
        textBlock(i, tx, my - th / 2f, tw, true, align = if (side == -1) Align.END else Align.START)
        if (i == n - 1) tail = max(19f, th / 2f)
    }
    return Size(2 * cx, Htot + tail + 6f)
}

// ---------------------------------------------------------------- Fan-out

fun Ctx.fanOut(): Size {
    val n = items.size
    val coreR = 92f
    val nodeR = 34f
    val barW = 220f; val barH = 44f
    val noteW = 290f
    val rowH = items.indices.maxOfOrNull {
        max(nodeR * 2 + 22f, bullets(items[it].bullets, 0f, 0f, noteW, 0, 0, null, false).takeIf { style.showCallouts } ?: 0f) + 16f
    } ?: 80f
    val Ra = max(250f, ((n - 1) * rowH / 2f) / 0.82f)
    val arcsOuter = coreR + 14f * 4
    val H = max(2 * arcsOuter + 30f, n * rowH)
    val cx = arcsOuter + 4f; val cy = H / 2f
    val ys = List(n) { cy + (it - (n - 1) / 2f) * rowH }
    val xs = ys.map { cx + sqrt(max(0f, Ra * Ra - (it - cy) * (it - cy))) }
    val barX = cx + Ra + nodeR + 70f
    val arcC = lighten(theme.hub, 0.55f)

    for (k in 1..4) shape(PathData().arc(cx, cy, coreR + 14f * k, 55f, 305f, move = true), null, arcC, 2.5f)
    for (i in 0 until n) {
        val c = color(i)
        val th = rad(-38f + if (n > 1) 76f * i / (n - 1) else 38f)
        val sx = cx + coreR * cos(th).toFloat(); val sy = cy + coreR * sin(th).toFloat()
        val ex = xs[i] - nodeR - 4f; val ey = ys[i]
        shape(PathData().moveTo(sx, sy).cubicTo(sx + 70f, sy, ex - 90f, ey, ex, ey), null, lighten(c, 0.35f), 3f)
        arrow(listOf((xs[i] + nodeR + 8f) to ys[i], (barX - 10f) to ys[i]), lighten(c, 0.2f), 3f, 12f)
    }
    val core = PathData.circle(cx, cy, coreR)
    shadow(core)
    shape(core, fill(theme.hub, cx, cy - coreR, cx, cy + coreR))
    centerText(cx, cy, coreR * 1.55f, coreR * 1.3f, onColor(theme.hub), 24f)
    for (i in 0 until n) {
        val c = color(i)
        if (icons) iconNode(i, xs[i], ys[i], nodeR, outlined = false) else numberBadge(i, xs[i], ys[i], nodeR)
        val bar = PathData.roundRect(barX, ys[i] - barH / 2f, barW, barH, 12f)
        shadow(bar, 0.6f)
        shape(bar, fill(c, barX, 0f, barX + barW, 0f))
        val f = fit(m, items[i].label, barW - 24f, barH - 8f, 17f * s, 9f, bold = true)
        text(f.lines, barX + barW / 2f, ys[i], f.size, onColor(c), bold = true)
        if (style.showCallouts) {
            val bh = bullets(items[i].bullets, 0f, 0f, noteW, 0, 0, null, false)
            bullets(items[i].bullets, barX + barW + 18f, ys[i] - bh / 2f, noteW, Ink.subtle, c, null, true)
        }
    }
    val right = barX + barW + if (style.showCallouts && items.any { it.bullets.any(String::isNotBlank) }) 18f + noteW else 0f
    return Size(right, H)
}

// ---------------------------------------------------------------- Zigzag

fun Ctx.zigzag(): Size {
    val n = items.size
    val sq = 126f; val step = 138f; val off = 76f
    val textW = 330f
    val half = off + sq / 2f + 40f + textW
    val cx = half
    val top = 40f
    val cys = List(n) { top + 30f + sq / 2f + it * step }
    val endY = (cys.lastOrNull() ?: top) + sq / 2f + 54f
    // Spine
    shape(PathData.circle(cx, top, 7f), Paint.Solid(Ink.subtle))
    arrow(listOf(cx to top, cx to endY), Ink.subtle, 3f, 13f)
    for (i in 0 until n) {
        val c = color(i)
        val side = if (i % 2 == 0) 1 else -1
        val sx = cx + side * off
        val y = cys[i]
        lineShape(cx, y, sx - side * sq / 2f, y, Ink.subtle, 3f)
        val box = PathData.roundRect(sx - sq / 2f, y - sq / 2f, sq, sq, 24f)
        shadow(box)
        shape(box, fill(c, sx - sq / 2f, y - sq / 2f, sx + sq / 2f, y + sq / 2f))
        val pill = PathData.roundRect(sx - sq * 0.34f, y - 22f, sq * 0.68f, 44f, 14f)
        shape(pill, null, withAlpha(onColor(c), 0.85f), 2.5f)
        if (icons) itemIcon(i, sx, y, 30f, onColor(c))
        else text(listOf(items[i].label.trim().take(1).uppercase()), sx, y, 26f, onColor(c), bold = true)
        // Note with rules above and below
        val tx = if (side == 1) sx + sq / 2f + 40f else sx - sq / 2f - 40f - textW
        val th = textBlock(i, tx, 0f, textW, false)
        val ty = y - th / 2f
        val r0 = min(ty - 12f, y - sq / 2f + 6f); val r1 = max(ty + th + 12f, y + sq / 2f - 6f)
        val lx0 = if (side == 1) sx + sq / 2f + 14f else tx; val lx1 = if (side == 1) tx + textW else sx - sq / 2f - 14f
        lineShape(lx0, r0, lx1, r0, theme.ring, 2f)
        lineShape(lx0, r1, lx1, r1, theme.ring, 2f)
        textBlock(i, tx, ty, textW, true, align = if (side == 1) Align.START else Align.END)
    }
    return Size(2 * half, endY + 6f)
}

// ---------------------------------------------------------------- Milestones (chevron timeline)

fun Ctx.milestones(): Size {
    val n = max(items.size, 1)
    val stepW = (1240f / n).coerceIn(190f, 270f)
    val p = 24f
    val lineY = 22f
    val iconY = lineY + 92f
    val iconR = 58f
    val chevTop = iconY + iconR + 34f
    val chevH = 70f
    val textTop = chevTop + chevH + 24f
    val textW = stepW - 34f
    val maxText = if (style.showCallouts)
        items.indices.maxOfOrNull { bullets(items[it].bullets, 0f, 0f, textW, 0, 0, null, false) } ?: 0f else 0f
    val bottom = textTop + maxText + 22f
    val totalW = n * stepW + p
    // Frames alternate between enclosing the icon (top) and the note (bottom)
    for (i in items.indices) {
        val L = i * stepW + 8f; val R = L + stepW - 16f + p * 0.4f
        val mid = chevTop + chevH / 2f
        val (y0, y1) = if (i % 2 == 0) lineY to mid else mid to bottom
        shape(PathData.roundRect(L, y0, R - L, y1 - y0, 28f), null, hex("#CBD2DC"), 2f)
    }
    arrow(listOf(0f to lineY, (totalW + 6f) to lineY), hex("#AEB6C2"), 2f, 12f)
    for (i in items.indices) {
        val c = color(i)
        val L = i * stepW
        val R = L + stepW - 6f + p
        shape(PathData.circle(L + 8f, lineY, 8f), Paint.Solid(c), Ink.white, 3f)
        // icon
        val icx = L + (stepW - 6f) / 2f + 6f
        shape(PathData.circle(icx, iconY, iconR), Paint.Solid(Ink.white), lighten(c, 0.25f), 2f, listOf(6f, 6f))
        if (icons) itemIcon(i, icx, iconY, 54f, readable(c)) else text(listOf("${i + 1}"), icx, iconY, 34f, readable(c), bold = true)
        // chevron
        val path = PathData()
        path.moveTo(L, chevTop).lineTo(R - p, chevTop).lineTo(R, chevTop + chevH / 2f).lineTo(R - p, chevTop + chevH).lineTo(L, chevTop + chevH)
        if (i > 0) path.lineTo(L + p, chevTop + chevH / 2f)
        path.close()
        shadow(path, 0.7f)
        shape(path, fill(c, L, chevTop, R, chevTop + chevH))
        val innerL = if (i > 0) L + p else L + 10f
        val f = fit(m, items[i].label, (R - p) - innerL - 6f, chevH - 14f, 18f * s, 9.5f, bold = true)
        text(f.lines, (innerL + R - p) / 2f, chevTop + chevH / 2f, f.size, onColor(c), bold = true)
        if (style.showCallouts) bullets(items[i].bullets, L + 18f, textTop, textW, Ink.subtle, c, null, true)
    }
    return Size(totalW + 10f, bottom + 4f)
}

// ---------------------------------------------------------------- Petals (ring flower)

fun Ctx.flower(): Size {
    val n = items.size
    val r = when { n <= 3 -> 165f; n <= 5 -> 150f; n <= 6 -> 135f; else -> 118f }
    val d = when { n <= 1 -> 0f; n == 2 -> r * 0.75f; else -> (r / sin(PI / n).toFloat()) * 0.8f }
    val ext = d + r + 12f
    val cx = ext; val cy = ext
    val reqs = ArrayList<CalloutReq>()
    val angles = List(n) { -90f + it * 360f / max(n, 1) + style.rotation }
    val diagram = capture {
        for (i in 0 until n) shape(PathData.circle(px(cx, d, angles[i]), py(cy, d, angles[i]), r), Paint.Solid(withAlpha(color(i), 0.08f)))
        for (i in 0 until n) shape(PathData.circle(px(cx, d, angles[i]), py(cy, d, angles[i]), r), null, color(i), 11f)
        for (i in 0 until n) {
            val c = color(i)
            val x = px(cx, d, angles[i]); val y = py(cy, d, angles[i])
            val ix = px(x, r * 0.22f, angles[i]); val iy = py(y, r * 0.22f, angles[i])
            if (icons) {
                shape(PathData.circle(ix, iy, r * 0.36f), Paint.Solid(lighten(c, 0.86f)))
                itemIcon(i, ix, iy, r * 0.38f, readable(c))
            } else {
                val f = fit(m, items[i].label, r * 0.95f, r * 0.6f, 19f * s, 9.5f, bold = true)
                text(f.lines, ix, iy, f.size, readable(c), bold = true)
            }
            val ax = px(x, r + 10f, angles[i]); val ay = py(y, r + 10f, angles[i])
            reqs += CalloutReq(i, ax, ay, sideOf(angles[i]), ay, x, y, r + 6f)
        }
        if (doc.center.isNotBlank() && n >= 2) {
            val f = fit(m, doc.center, r * 0.72f, r * 0.55f, 17f * s, 9f, bold = true)
            val pw = max(f.lines.maxOf { m.width(it, f.size, true) } + 30f, 64f)
            val ph = f.lines.size * f.size * 1.22f + 18f
            val pill = PathData.roundRect(cx - pw / 2f, cy - ph / 2f, pw, ph, ph / 2f)
            shadow(pill, 0.7f)
            shape(pill, Paint.Solid(Ink.white))
            text(f.lines, cx, cy, f.size, Ink.text, bold = true)
        }
    }
    return withCallouts(2 * ext, 2 * ext, reqs, diagram, plain = true)
}

private fun sideOf(deg: Float): Int {
    val c = cos(rad(deg))
    return when { c < -0.05 -> -1; c > 0.05 -> 1; else -> 0 }
}

// ---------------------------------------------------------------- Card flow with feedback loop

fun Ctx.cardFlow(): Size {
    val n = max(items.size, 1)
    val cardW = 290f; val gapX = 74f; val pad = 22f
    val headH = 50f
    val bodyW = cardW - pad * 2
    val bodyH = if (style.showCallouts) items.indices.maxOfOrNull {
        bullets(items[it].bullets, 0f, 0f, bodyW, 0, 0, null, false)
    } ?: 0f else 0f
    val cardH = 7f + pad + headH + (if (bodyH > 0f) 16f + bodyH else 0f) + pad
    val loop = doc.center.isNotBlank() && items.size >= 2
    val totalW = n * cardW + (n - 1) * gapX
    for (i in items.indices) {
        val c = color(i)
        val x = i * (cardW + gapX)
        val card = PathData.roundRect(x, 0f, cardW, cardH, 22f)
        shadow(card)
        shape(card, Paint.Solid(Ink.white), theme.ring, 1.5f)
        shape(PathData.roundRect(x + 16f, 0f, cardW - 32f, 7f, 3.5f), Paint.Solid(c))
        val hy = 7f + pad + headH / 2f
        if (icons) {
            shape(PathData.circle(x + pad + 22f, hy, 22f), Paint.Solid(lighten(c, 0.86f)))
            itemIcon(i, x + pad + 22f, hy, 24f, readable(c))
        } else numberBadge(i, x + pad + 22f, hy, 20f)
        val lx = x + pad + 58f
        val f = fit(m, items[i].label, cardW - pad - 58f - pad, headH, 21f * s, 11f, bold = true)
        add(Prim.Text(f.lines, lx, hy, f.size, Ink.text, bold = true, align = Align.START))
        if (bodyH > 0f) bullets(items[i].bullets, x + pad, 7f + pad + headH + 16f, bodyW, Ink.subtle, c, theme.ring, true)
        if (i < items.lastIndex) {
            val ax = x + cardW + 10f
            arrow(listOf(ax to cardH / 2f, (ax + gapX - 20f) to cardH / 2f), lighten(color(i + 1), 0.25f), 8f, 26f)
        }
    }
    if (!loop) return Size(totalW, cardH)
    val c = lighten(theme.colors[0], 0.1f)
    val lastX = (items.size - 1) * (cardW + gapX) + cardW / 2f
    val firstX = cardW / 2f
    val ly = cardH + 70f
    arrow(listOf(lastX to (cardH + 8f), lastX to ly, firstX to ly, firstX to (cardH + 10f)), c, 4f, 18f)
    val f = fit(m, doc.center, min(totalW * 0.5f, 520f), 60f, 17f * s, 10f, bold = true)
    val pw = f.lines.maxOf { m.width(it, f.size, true) } + 40f
    val ph = f.lines.size * f.size * 1.22f + 20f
    val pill = PathData.roundRect(totalW / 2f - pw / 2f, ly - ph / 2f, pw, ph, ph / 2f)
    shape(pill, Paint.Solid(Ink.white), c, 2.5f)
    text(f.lines, totalW / 2f, ly, f.size, readable(c), bold = true)
    return Size(totalW, ly + ph / 2f + 6f)
}

// ---------------------------------------------------------------- Group resolution for role-based models

/** Splits the visible items into (inputs, outputs, context) by their resolved role. */
fun Ctx.groupsByRole(): Triple<List<Int>, List<Int>, List<Int>> {
    val roles = doc.resolvedRoles()
    val inputs = ArrayList<Int>(); val outputs = ArrayList<Int>(); val ctx = ArrayList<Int>()
    items.indices.forEach {
        when (roles[it]) {
            Role.OUTPUT, Role.MEDIATOR -> outputs += it
            Role.CONTEXT, Role.MODERATOR -> ctx += it
            else -> inputs += it
        }
    }
    return Triple(inputs, outputs, ctx)
}

// ---------------------------------------------------------------- Variables (research model)

fun Ctx.variables(): Size {
    val (ins0, outs, ctxs) = groupsByRole()
    val ins = ins0 + ctxs
    val r = 56f
    val rowH = 2 * r + 36f
    val rows = max(max(ins.size, outs.size), 1)
    val capH = 44f
    val colW = 230f; val gap = 170f
    val bodyTop = capH + 16f
    val bodyH = rows * rowH + 20f
    val inX = colW / 2f
    val outX = if (outs.isEmpty()) inX else colW + gap + colW / 2f
    val depX = outX + colW / 2f + gap + 70f
    val depY = bodyTop + bodyH / 2f
    fun ysFor(k: Int) = List(k) { bodyTop + bodyH / 2f + (it - (k - 1) / 2f) * rowH }
    val inY = ysFor(ins.size); val outY = ysFor(outs.size)
    val frame = hex("#C9D0DA")

    fun column(x: Float, caption: String) {
        shape(PathData.roundRect(x - colW / 2f, bodyTop, colW, bodyH, 28f), null, frame, 2f)
        val f = wrap(m, caption, colW, 16f * s, true)
        text(f, x, capH / 2f + 4f, 16f * s, Ink.text, bold = true)
    }
    column(inX, doc.groupName(0))
    if (outs.isNotEmpty()) column(outX, doc.groupName(1))
    text(wrap(m, doc.groupName(2), 200f, 16f * s, true), depX, capH / 2f + 4f, 16f * s, Ink.text, bold = true)
    // Arrows
    fun shortened(x1: Float, y1: Float, x2: Float, y2: Float, r1: Float, r2: Float): List<Pair<Float, Float>> {
        val len = hypot(x2 - x1, y2 - y1); val ux = (x2 - x1) / len; val uy = (y2 - y1) / len
        return listOf((x1 + ux * r1) to (y1 + uy * r1), (x2 - ux * r2) to (y2 - uy * r2))
    }
    if (outs.isNotEmpty()) {
        ins.forEachIndexed { a, i -> outs.indices.forEach { b ->
            arrow(shortened(inX, inY[a], outX, outY[b], r + 4f, r + 6f), withAlpha(color(i), 0.85f), 2.5f, 12f)
        } }
        outs.forEachIndexed { b, o -> arrow(shortened(outX, outY[b], depX, depY, r + 4f, 70f), withAlpha(color(o), 0.9f), 3.5f, 14f) }
    } else ins.forEachIndexed { a, i -> arrow(shortened(inX, inY[a], depX, depY, r + 4f, 70f), withAlpha(color(i), 0.9f), 3f, 14f) }

    fun node(i: Int, x: Float, y: Float) {
        val c = color(i)
        val disc = PathData.circle(x, y, r)
        shadow(disc, 0.8f)
        shape(disc, fill(c, x - r, y - r, x + r, y + r), Ink.white, 3f)
        val f = fit(m, items[i].label, r * 1.55f, r * 1.25f, 16f * s, 8.5f, bold = true)
        text(f.lines, x, y, f.size, onColor(c), bold = true)
    }
    ins.forEachIndexed { a, i -> node(i, inX, inY[a]) }
    outs.forEachIndexed { b, o -> node(o, outX, outY[b]) }
    val dep = PathData.circle(depX, depY, 68f)
    shadow(dep)
    shape(dep, fill(theme.hub, depX, depY - 68f, depX, depY + 68f), Ink.white, 4f)
    val label = doc.center.ifBlank { doc.groupName(2) }
    val f = fit(m, label, 68f * 1.55f, 68f * 1.25f, 18f * s, 9f, bold = true)
    text(f.lines, depX, depY, f.size, onColor(theme.hub), bold = true)
    return Size(depX + 72f, bodyTop + bodyH + 4f)
}

// ---------------------------------------------------------------- Conceptual framework

fun Ctx.framework(): Size {
    val (ins, outs, ctxs) = groupsByRole()
    val boxW = 270f; val gap = 150f; val R = 128f
    val pad = 18f
    fun boxH(i: Int, w: Float): Float {
        val lab = fit(m, items[i].label, w - pad * 2, 400f, 19f * s, 10f, bold = true)
        val lh = lab.lines.size * lab.size * 1.22f
        val bh = if (style.showCallouts) bullets(items[i].bullets, 0f, 0f, w - pad * 2, 0, 0, null, false) else 0f
        return max(88f, lh + (if (bh > 0f) 10f + bh else 0f) + pad * 2)
    }
    val colGap = 20f
    val leftH = ins.sumOf { (boxH(it, boxW) + colGap).toDouble() }.toFloat() - colGap
    val rightH = outs.sumOf { (boxH(it, boxW) + colGap).toDouble() }.toFloat() - colGap
    val ctxTop = ctxs.take(ceil(ctxs.size / 2.0).toInt())
    val ctxBottom = ctxs.drop(ctxTop.size)
    val ctxW = 220f
    val ctxRowH = (ctxs.maxOfOrNull { boxH(it, ctxW) } ?: 0f)
    val topBand = if (ctxTop.isNotEmpty()) ctxRowH + 70f else 0f
    val bottomBand = if (ctxBottom.isNotEmpty()) ctxRowH + 70f else 0f
    val bodyH = maxOf(leftH, rightH, 2 * R + 40f)
    val W = boxW * 2 + gap * 2 + R * 2
    val cx = W / 2f; val cy = topBand + bodyH / 2f

    fun drawBox(i: Int, x: Float, y: Float, w: Float, h: Float, c: Int) {
        val box = PathData.roundRect(x, y, w, h, 14f)
        shadow(box, 0.7f)
        shape(box, fill(c, x, y, x + w, y + h))
        val fg = onColor(c)
        val lab = fit(m, items[i].label, w - pad * 2, 400f, 19f * s, 10f, bold = true)
        val lh = lab.lines.size * lab.size * 1.22f
        val bh = if (style.showCallouts) bullets(items[i].bullets, 0f, 0f, w - pad * 2, 0, 0, null, false) else 0f
        if (bh > 0f) {
            text(lab.lines, x + w / 2f, y + pad + lh / 2f, lab.size, fg, bold = true)
            bullets(items[i].bullets, x + pad, y + pad + lh + 10f, w - pad * 2, withAlpha(fg, 0.92f), withAlpha(fg, 0.8f), null, true)
        } else text(lab.lines, x + w / 2f, y + h / 2f, lab.size, fg, bold = true)
    }
    fun edge(px0: Float, py0: Float, extra: Float): Pair<Float, Float> {
        val dx = px0 - cx; val dy = py0 - cy; val len = hypot(dx, dy).coerceAtLeast(1f)
        return (cx + dx / len * (R + extra)) to (cy + dy / len * (R + extra))
    }
    // Inputs (left)
    var y = topBand + (bodyH - leftH) / 2f
    for (i in ins) {
        val h = boxH(i, boxW)
        val c = color(i)
        val by = y + h / 2f
        val mid = (boxW + 46f) to by
        arrow(listOf((boxW + 2f) to by, mid, edge(mid.first, mid.second, 10f)), lighten(c, 0.05f), 9f, 28f)
        drawBox(i, 0f, y, boxW, h, c)
        y += h + colGap
    }
    // Outputs (right)
    y = topBand + (bodyH - rightH) / 2f
    for (o in outs) {
        val h = boxH(o, boxW)
        val c = color(o)
        val by = y + h / 2f
        val bx = W - boxW
        val mid = (bx - 46f) to by
        arrow(listOf(edge(mid.first, mid.second, 6f), mid, (bx - 3f) to by), c, 9f, 28f)
        drawBox(o, bx, y, boxW, h, c)
        y += h + colGap
    }
    // Context (top/bottom)
    fun ctxRow(list: List<Int>, top: Boolean) {
        if (list.isEmpty()) return
        val total = list.size * ctxW + (list.size - 1) * 24f
        var x = cx - total / 2f
        for (k in list) {
            val c = color(k)
            val by = if (top) 0f else topBand + bodyH + 70f
            val ax = x + ctxW / 2f
            val start = if (top) ax to (by + ctxRowH + 2f) else ax to (by - 2f)
            arrow(listOf(start, edge(start.first, start.second, 10f)), c, 9f, 28f)
            drawBox(k, x, by, ctxW, ctxRowH, c)
            x += ctxW + 24f
        }
    }
    ctxRow(ctxTop, true)
    ctxRow(ctxBottom, false)
    // Central circle
    val circle = PathData.circle(cx, cy, R)
    shadow(circle)
    shape(circle, fill(theme.hub, cx, cy - R, cx, cy + R))
    shape(PathData.circle(cx, cy, R - 14f), null, Ink.white, 5f)
    centerText(cx, cy, (R - 20f) * 1.55f, (R - 20f) * 1.3f, onColor(theme.hub), 26f)
    return Size(W, topBand + bodyH + bottomBand)
}

// ---------------------------------------------------------------- bounds helpers

internal fun List<Prim>.minX(): Float = minOfOrNull { p ->
    when (p) {
        is Prim.Shape -> p.path.cmds.minOfOrNull { c -> when (c) { is Cmd.M -> c.x; is Cmd.L -> c.x; is Cmd.C -> minOf(c.x1, c.x2, c.x); Cmd.Z -> Float.MAX_VALUE } } ?: 0f
        is Prim.Text -> p.x - 400f * 0f
        is Prim.Image -> p.x
    }
} ?: 0f

internal fun List<Prim>.maxX(): Float = maxOfOrNull { p ->
    when (p) {
        is Prim.Shape -> p.path.cmds.maxOfOrNull { c -> when (c) { is Cmd.M -> c.x; is Cmd.L -> c.x; is Cmd.C -> maxOf(c.x1, c.x2, c.x); Cmd.Z -> -Float.MAX_VALUE } } ?: 0f
        is Prim.Text -> p.x
        is Prim.Image -> p.x + p.w
    }
} ?: 0f
