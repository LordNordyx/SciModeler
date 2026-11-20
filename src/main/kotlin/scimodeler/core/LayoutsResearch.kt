package scimodeler.core

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------- Tree (hierarchy with examples)

fun Ctx.tree(): Size {
    val n = items.size
    val ids = items.map { it.id }.toSet()
    val children = HashMap<Long?, MutableList<Int>>()
    items.forEachIndexed { i, it -> children.getOrPut(it.parent?.takeIf { p -> p in ids && p != it.id }) { ArrayList() } += i }
    val roots = children[null].orEmpty()
    val boxW = 210f; val gap = 26f; val vGap = 58f
    val hasRoot = doc.center.isNotBlank()
    fun depthOf(i: Int): Int { var d = 0; var cur = items[i]; var guard = 0
        while (cur.parent != null && cur.parent in ids && guard++ < 32) { cur = items.first { it.id == cur.parent }; d++ }
        return d }
    fun labelSize(i: Int) = if (depthOf(i) == 0) 19f * s else 17f * s
    fun boxH(i: Int): Float {
        val f = fit(m, items[i].label, boxW - 28f, 200f, labelSize(i), 10f, bold = depthOf(i) == 0)
        return max(62f, f.lines.size * f.size * 1.22f + 28f)
    }
    fun kids(i: Int) = children[items[i].id].orEmpty()
    fun exampleH(i: Int): Float {
        if (!style.showCallouts || kids(i).isNotEmpty() || items[i].bullets.none { it.isNotBlank() }) return 0f
        return bullets(items[i].bullets, 0f, 0f, boxW - 52f, 0, 0, null, false) + 26f
    }
    val sw = HashMap<Int, Float>()
    fun width(i: Int): Float = sw.getOrPut(i) {
        val k = kids(i)
        if (k.isEmpty()) boxW else max(boxW, k.sumOf { width(it).toDouble() }.toFloat() + gap * (k.size - 1))
    }
    val rootH = if (hasRoot) 70f else 0f
    val top0 = if (hasRoot) rootH + vGap else 0f
    val xs = HashMap<Int, Float>(); val ys = HashMap<Int, Float>()
    fun place(i: Int, left: Float, y: Float) {
        ys[i] = y
        val k = kids(i)
        if (k.isEmpty()) { xs[i] = left + (width(i) - boxW) / 2f; return }
        var cx = left + (width(i) - (k.sumOf { width(it).toDouble() }.toFloat() + gap * (k.size - 1))) / 2f
        val childY = y + boxH(i) + vGap
        k.forEach { c -> place(c, cx, childY); cx += width(c) + gap }
        xs[i] = (xs[k.first()]!! + xs[k.last()]!!) / 2f
    }
    var left = 0f
    roots.forEach { r -> place(r, left, top0); left += width(r) + gap }
    val totalW = max(if (n == 0) 260f else left - gap, if (hasRoot) 280f else 0f)
    val conn = theme.connector

    fun bus(px: Float, py: Float, childCenters: List<Pair<Float, Float>>) {
        if (childCenters.isEmpty()) return
        val my = py + vGap / 2f
        shape(PathData.line(px, py, px, my), null, conn, 3f)
        shape(PathData.line(min(px, childCenters.minOf { it.first }), my, max(px, childCenters.maxOf { it.first }), my), null, conn, 3f)
        childCenters.forEach { (cx, cy) -> shape(PathData.line(cx, my, cx, cy), null, conn, 3f) }
    }
    if (hasRoot) bus(totalW / 2f, rootH, roots.map { xs[it]!! + boxW / 2f to ys[it]!! })
    for (i in items.indices) bus(xs[i]!! + boxW / 2f, ys[i]!! + boxH(i), kids(i).map { xs[it]!! + boxW / 2f to ys[it]!! })

    var bottom = if (hasRoot) rootH else 0f
    if (hasRoot) {
        val rw = min(max(280f, m.width(doc.center, 22f * s, true) + 60f), max(totalW, 280f))
        val p = PathData.roundRect(totalW / 2f - rw / 2f, 0f, rw, rootH, 16f)
        shadow(p)
        shape(p, fill(theme.hub, 0f, 0f, 0f, rootH))
        val f = fit(m, doc.center, rw - 30f, rootH - 14f, 22f * s, 11f, bold = true)
        text(f.lines, totalW / 2f, rootH / 2f, f.size, onColor(theme.hub), bold = true)
    }
    for (i in items.indices) {
        val c = color(i)
        val d = depthOf(i)
        val x = xs[i]!!; val y = ys[i]!!; val h = boxH(i)
        val bg = if (d == 0) c else lighten(c, 0.62f)
        val p = PathData.roundRect(x, y, boxW, h, 14f)
        shadow(p, 0.7f)
        shape(p, if (d == 0) fill(c, x, y, x, y + h) else Paint.Solid(bg), if (d == 0) null else lighten(c, 0.35f), 1.5f)
        val f = fit(m, items[i].label, boxW - 28f, h - 16f, labelSize(i), 10f, bold = d == 0)
        text(f.lines, x + boxW / 2f, y + h / 2f, f.size, onColor(bg), bold = d == 0)
        bottom = max(bottom, y + h)
        val eh = exampleH(i)
        if (eh > 0f) {
            val ex = x + 24f; val ey = y + h + 20f; val ew = boxW - 24f
            shape(PathData().moveTo(x + 12f, y + h).lineTo(x + 12f, ey + eh / 2f).lineTo(ex, ey + eh / 2f), null, conn, 3f)
            val card = PathData.roundRect(ex, ey, ew, eh, 12f)
            shape(card, Paint.Solid(lighten(c, 0.9f)), lighten(c, 0.6f), 1.5f)
            bullets(items[i].bullets, ex + 14f, ey + 13f, ew - 28f, Ink.text, c, null, true)
            bottom = max(bottom, ey + eh)
        }
    }
    return Size(totalW, bottom + 4f)
}

// ---------------------------------------------------------------- Topic map (root → topics → sub-topic pills)

fun Ctx.topicMap(): Size {
    val n = max(items.size, 1)
    val colW = 250f; val gap = 46f
    val totalW = n * colW + (n - 1) * gap
    val rootH = 92f
    val rootW = min(max(320f, m.width(doc.center.ifBlank { " " }, 30f * s, true) + 80f), max(totalW, 320f))
    val headTop = rootH + 120f
    val headH = 72f
    val pillW = colW - 40f
    val conn = theme.connector
    var bottom = headTop + headH

    // Root
    val cx = totalW / 2f
    val root = PathData.roundRect(cx - rootW / 2f, 0f, rootW, rootH, rootH / 2f)
    shadow(root)
    shape(root, Paint.Solid(Ink.white), theme.ring, 2f)
    if (doc.center.isNotBlank()) {
        val f = fit(m, doc.center, rootW - 50f, rootH - 18f, 30f * s, 12f, bold = true)
        text(f.lines, cx, rootH / 2f, f.size, theme.hub, bold = true)
    }
    for (i in items.indices) {
        val c = color(i)
        val x = i * (colW + gap)
        val hx = x + colW / 2f
        shape(PathData().moveTo(cx, rootH).cubicTo(cx, rootH + 80f, hx, headTop - 80f, hx, headTop - 6f), null, conn, 3f)
        shape(PathData.circle(hx, headTop - 6f, 5f), Paint.Solid(conn))
        val head = PathData.roundRect(x, headTop, colW, headH, 14f)
        shadow(head, 0.8f)
        shape(head, fill(c, x, headTop, x + colW, headTop + headH))
        val iz = if (icons) 30f else 0f
        if (icons) itemIcon(i, x + 20f + iz / 2f, headTop + headH / 2f, iz, onColor(c))
        val f = fit(m, items[i].label, colW - 32f - (if (icons) iz + 12f else 0f), headH - 14f, 20f * s, 10f, bold = true)
        if (icons) add(Prim.Text(f.lines, x + 20f + iz + 12f, headTop + headH / 2f, f.size, onColor(c), true, Align.START))
        else text(f.lines, hx, headTop + headH / 2f, f.size, onColor(c), bold = true)
        // Sub-topics
        val subs = if (style.showCallouts) items[i].bullets.map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
        var y = headTop + headH + 22f
        val trunkX = x + 16f
        var lastMid = y
        subs.forEach { sub ->
            val tf = fit(m, sub, pillW - 28f, 200f, 16f * s, 9f, bold = true)
            val ph = max(48f, tf.lines.size * tf.size * 1.22f + 22f)
            val mid = y + ph / 2f
            shape(PathData().moveTo(trunkX, mid - 14f).cubicTo(trunkX, mid - 2f, trunkX + 4f, mid, trunkX + 16f, mid).lineTo(x + 40f, mid), null, conn, 2.5f)
            val pill = PathData.roundRect(x + 40f, y, pillW, ph, 14f)
            shadow(pill, 0.6f)
            shape(pill, Paint.Solid(Ink.white), theme.ring, 1.5f)
            text(tf.lines, x + 40f + pillW / 2f, mid, tf.size, readable(c), bold = true)
            lastMid = mid
            y += ph + 14f
        }
        if (subs.isNotEmpty()) {
            shape(PathData.line(trunkX, headTop + headH, trunkX, lastMid - 14f), null, conn, 2.5f)
            bottom = max(bottom, y - 14f)
        }
    }
    return Size(totalW, bottom + 4f)
}

// ---------------------------------------------------------------- Mediation / moderation

private class PathRoles(val iv: List<Int>, val dv: List<Int>, val med: List<Int>, val mod: List<Int>)

private fun Ctx.pathRoles(): PathRoles {
    val roles = doc.resolvedRoles()
    val iv = ArrayList<Int>(); val dv = ArrayList<Int>(); val med = ArrayList<Int>(); val mod = ArrayList<Int>()
    items.indices.forEach {
        when (roles[it]) {
            Role.INPUT -> iv += it
            Role.OUTPUT -> dv += it
            Role.MODERATOR, Role.CONTEXT -> mod += it
            else -> med += it
        }
    }
    return PathRoles(iv, dv, med, mod)
}

private fun Ctx.pathLabel(text: String, x: Float, y: Float) {
    if (!style.showLabels) return
    val sz = 17f * s
    val w = m.width(text, sz, true) + 16f
    shape(PathData.roundRect(x - w / 2f, y - 13f, w, 26f, 13f), Paint.Solid(withAlpha(theme.background, 0.92f)))
    text(listOf(text), x, y, sz, Ink.text, bold = true)
}

/** Point on the segment midpoint, shifted perpendicular to it (for path labels). */
private fun beside(x1: Float, y1: Float, x2: Float, y2: Float, off: Float): Pair<Float, Float> {
    val len = hypot(x2 - x1, y2 - y1).coerceAtLeast(1f)
    val nx = -(y2 - y1) / len; val ny = (x2 - x1) / len
    val sgn = if (ny > 0) -1f else 1f
    return ((x1 + x2) / 2f + nx * off * sgn) to ((y1 + y2) / 2f + ny * off * sgn)
}

fun Ctx.mediation(): Size {
    val r = pathRoles()
    val R = 92f
    val pillW = 240f; val pillH = 64f
    val ink = Ink.text
    val ivs = r.iv.ifEmpty { listOf(-1) }; val dvs = r.dv.ifEmpty { listOf(-1) }
    val span = 620f
    val medTop = 0f
    val medH = if (r.med.isNotEmpty()) pillH + 120f else 30f
    val stackH = max(ivs.size, dvs.size) * (2 * R + 30f) - 30f
    val cy = medTop + medH + stackH / 2f
    val ivX = R + 6f; val dvX = ivX + span
    val W = dvX + R + 6f
    fun stackY(k: Int, count: Int) = cy + (k - (count - 1) / 2f) * (2 * R + 30f)

    fun ringNode(i: Int, x: Float, y: Float) {
        val c = if (i >= 0) color(i) else theme.hub
        shadow(PathData.circle(x, y, R), 0.8f)
        shape(PathData.circle(x, y, R), Paint.Solid(Ink.white))
        shape(PathData().arc(x, y, R - 3f, 180f, 360f, move = true), null, c, 5f)
        shape(PathData.sector(x, y, R - 16f, R, 0f, 180f), fill(c, x, y, x, y + R))
        shape(PathData.circle(x, y, R - 24f), Paint.Solid(theme.panel))
        val label = if (i >= 0) items[i].label else ""
        val f = fit(m, label, (R - 28f) * 1.6f, (R - 28f) * 1.3f, 19f * s, 9f, bold = true)
        text(f.lines, x, y, f.size, ink, bold = true)
    }
    // Direct paths
    ivs.forEachIndexed { a, _ -> dvs.forEachIndexed { b, _ ->
        val y1 = stackY(a, ivs.size); val y2 = stackY(b, dvs.size)
        arrow(listOf((ivX + R + 18f) to y1, (dvX - R - 18f) to y2), darken(theme.hub, 0f), 9f, 30f)
        if (a == 0 && b == 0) pathLabel("c′", (ivX + dvX) / 2f, (y1 + y2) / 2f - 24f)
    } }
    // Mediators above
    val medY = medTop + pillH / 2f
    r.med.forEachIndexed { k, i ->
        val px = (ivX + dvX) / 2f + (k - (r.med.size - 1) / 2f) * (pillW + 30f)
        val c = color(i)
        val sx = ivX + R * 0.62f; val sy = stackY(0, ivs.size) - R * 0.86f
        val ex = dvX - R * 0.62f; val ey = stackY(0, dvs.size) - R * 0.86f
        arrow(listOf(sx to sy, (px - pillW / 2f - 8f) to (medY + pillH * 0.35f)), ink, 3f, 13f)
        arrow(listOf((px + pillW / 2f + 8f) to (medY + pillH * 0.35f), ex to ey), ink, 3f, 13f)
        if (k == 0) {
            val (lx, ly) = beside(sx, sy, px - pillW / 2f, medY, 20f); pathLabel("a", lx, ly)
            val (mx, my) = beside(px + pillW / 2f, medY, ex, ey, 20f); pathLabel("b", mx, my)
        }
        val pill = PathData.roundRect(px - pillW / 2f, medY - pillH / 2f, pillW, pillH, pillH / 2f)
        shadow(pill, 0.7f)
        shape(pill, Paint.Solid(lighten(c, theme.cardTint)), lighten(c, 0.5f), 2f)
        val f = fit(m, items[i].label, pillW - 30f, pillH - 12f, 18f * s, 9f, bold = true)
        text(f.lines, px, medY, f.size, readable(c), bold = true)
    }
    // Moderators below, pointing at the direct path
    val lineY = (stackY(0, ivs.size) + stackY(0, dvs.size)) / 2f
    val modY = cy + stackH / 2f + 110f
    r.mod.forEachIndexed { k, i ->
        val px = (ivX + dvX) / 2f + (k - (r.mod.size - 1) / 2f) * (pillW + 30f)
        val c = color(i)
        arrow(listOf(px to (modY - pillH / 2f - 8f), px to (lineY + 16f)), ink, 3f, 13f)
        val pill = PathData.roundRect(px - pillW / 2f, modY - pillH / 2f, pillW, pillH, pillH / 2f)
        shadow(pill, 0.7f)
        shape(pill, Paint.Solid(lighten(c, theme.cardTint)), lighten(c, 0.5f), 2f)
        val f = fit(m, items[i].label, pillW - 30f, pillH - 12f, 18f * s, 9f, bold = true)
        text(f.lines, px, modY, f.size, readable(c), bold = true)
    }
    ivs.forEachIndexed { a, i -> ringNode(i, ivX, stackY(a, ivs.size)) }
    dvs.forEachIndexed { b, i -> ringNode(i, dvX, stackY(b, dvs.size)) }
    val bottom = if (r.mod.isNotEmpty()) modY + pillH / 2f else cy + stackH / 2f + R
    return Size(W, bottom + 4f)
}

// ---------------------------------------------------------------- Path model with hypotheses

fun Ctx.pathModel(): Size {
    val r = pathRoles()
    val boxW = 300f; val boxH = 108f
    val medW = 340f; val medH = 104f; val medGap = 26f
    val colGap = 150f
    val ivs = r.iv.ifEmpty { listOf(-1) }; val dvs = r.dv.ifEmpty { listOf(-1) }
    val medTotal = if (r.med.isEmpty()) 0f else r.med.size * medH + (r.med.size - 1) * medGap
    val mainTop = if (r.med.isEmpty()) 0f else medTotal + 40f
    val mainRows = max(ivs.size, dvs.size)
    val mainH = mainRows * boxH + (mainRows - 1) * 24f
    val ivX = 0f; val medX = boxW + colGap; val dvX = medX + medW + colGap
    val W = dvX + boxW
    val ink = Ink.text
    var h = 1

    fun box(i: Int, x: Float, y: Float, w: Float, hh: Float) {
        val c = if (i >= 0) color(i) else theme.hub
        val p = PathData.roundRect(x, y, w, hh, 18f)
        shadow(p, 0.6f)
        shape(p, Paint.Solid(lighten(c, 0.84f)), darken(c, 0.25f), 2.5f)
        val label = if (i >= 0) items[i].label else ""
        val iz = if (icons && i >= 0) 50f else 0f
        if (iz > 0f) itemIcon(i, x + 22f + iz / 2f, y + hh / 2f, iz, darken(c, 0.1f))
        val tx = x + 22f + (if (iz > 0f) iz + 16f else 0f)
        val tw = w - (tx - x) - 20f
        val f = fit(m, label, tw, hh - 20f, 20f * s, 10f, bold = true)
        add(Prim.Text(f.lines, if (iz > 0f) tx else x + w / 2f, y + hh / 2f, f.size, darken(c, 0.55f), true, if (iz > 0f) Align.START else Align.CENTER))
    }
    fun ivY(a: Int) = mainTop + a * (boxH + 24f) + (mainH - (ivs.size * boxH + (ivs.size - 1) * 24f)) / 2f
    fun dvY(b: Int) = mainTop + b * (boxH + 24f) + (mainH - (dvs.size * boxH + (dvs.size - 1) * 24f)) / 2f
    fun medY(k: Int) = k * (medH + medGap)
    val labels = ArrayList<Triple<String, Float, Float>>()

    // H1: direct path(s)
    ivs.forEachIndexed { a, _ -> dvs.forEachIndexed { b, _ ->
        val y1 = ivY(a) + boxH / 2f; val y2 = dvY(b) + boxH / 2f
        arrow(listOf((ivX + boxW + 8f) to y1, (dvX - 10f) to y2), ink, 3f, 14f)
        val (lx, ly) = beside(ivX + boxW, y1, dvX, y2, 22f); labels += Triple("H${h++}", lx, ly)
    } }
    // IV → mediators
    r.med.forEachIndexed { k, _ ->
        val sx = ivX + boxW * 0.62f; val sy = ivY(0) - 6f
        val ex = medX - 10f; val ey = medY(k) + medH / 2f
        arrow(listOf(sx to sy, ex to ey), ink, 3f, 14f)
        val (lx, ly) = beside(sx, sy, ex, ey, 22f); labels += Triple("H${h++}", lx, ly)
    }
    // mediators → DV
    r.med.forEachIndexed { k, _ ->
        val sx = medX + medW + 8f; val sy = medY(k) + medH / 2f
        val ex = dvX + boxW * 0.38f; val ey = dvY(0) - 8f
        arrow(listOf(sx to sy, ex to ey), ink, 3f, 14f)
        val (lx, ly) = beside(sx, sy, ex, ey, 22f); labels += Triple("H${h++}", lx, ly)
    }
    // moderators → direct path (dashed)
    val lineY = (ivY(0) + dvY(0)) / 2f + boxH / 2f
    val modTop = mainTop + mainH + 90f
    r.mod.forEachIndexed { k, i ->
        val mx = (ivX + boxW + dvX) / 2f + (k - (r.mod.size - 1) / 2f) * (medW * 0.85f + 30f)
        arrow(listOf(mx to modTop - 8f, mx to (lineY + 10f)), ink, 2.5f, 13f, listOf(9f, 7f))
        labels += Triple("H${h++}", mx + 30f, (modTop + lineY) / 2f + 6f)
        box(i, mx - medW * 0.85f / 2f, modTop, medW * 0.85f, boxH * 0.95f)
    }
    ivs.forEachIndexed { a, i -> box(i, ivX, ivY(a), boxW, boxH) }
    dvs.forEachIndexed { b, i -> box(i, dvX, dvY(b), boxW, boxH) }
    r.med.forEachIndexed { k, i -> box(i, medX, medY(k), medW, medH) }
    labels.forEach { (t, x, y) -> pathLabel(t, x, y) }
    val bottom = if (r.mod.isNotEmpty()) modTop + boxH * 0.95f else mainTop + mainH
    return Size(W, bottom + 4f)
}
