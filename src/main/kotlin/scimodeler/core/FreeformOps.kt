package scimodeler.core

import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

/** Pure editing operations on a [Freeform] (no UI), so they can be tested and reused. */
object FreeformOps {
    const val MIN_SIZE = 12f

    fun ids(ff: Freeform, ids: Set<Long>) = ff.elements.filter { it.id in ids }

    /** Selection widened to whole groups. */
    fun withGroups(ff: Freeform, ids: Set<Long>): Set<Long> {
        val groups = ff.elements.filter { it.id in ids }.mapNotNull { it.group }.toSet()
        return ids + ff.elements.filter { it.group != null && it.group in groups }.map { it.id }
    }

    /** Adds an element, nudging it so it doesn't land exactly on top of another one. */
    fun add(ff: Freeform, e: FfElement): Freeform {
        var el = e
        var guard = 0
        while (guard++ < 30 && ff.elements.any { !it.isConnector && abs(it.x - el.x) < 6f && abs(it.y - el.y) < 6f }) {
            el = el.copy(x = el.x + 24f, y = el.y + 24f, x2 = el.x2 + 24f, y2 = el.y2 + 24f)
        }
        return ff.copy(elements = ff.elements + el)
    }

    fun replace(ff: Freeform, e: FfElement) = ff.copy(elements = ff.elements.map { if (it.id == e.id) e else it })

    fun update(ff: Freeform, ids: Set<Long>, change: (FfElement) -> FfElement) =
        ff.copy(elements = ff.elements.map { if (it.id in ids) change(it) else it })

    /** Moves elements (and free connector ends) by (dx, dy) relative to [base]. */
    fun move(base: Freeform, ids: Set<Long>, dx: Float, dy: Float): Freeform = base.copy(elements = base.elements.map {
        if (it.id !in ids || it.locked) it
        else if (it.isConnector) it.copy(x = it.x + dx, y = it.y + dy, x2 = it.x2 + dx, y2 = it.y2 + dy)
        else it.copy(x = it.x + dx, y = it.y + dy)
    })

    /** Removes elements; connectors attached to them keep their current end points. */
    fun delete(ff: Freeform, ids: Set<Long>): Freeform {
        val gone = ff.elements.filter { it.id in ids && !it.locked }.map { it.id }.toSet()
        return ff.copy(elements = ff.elements.filter { it.id !in gone }.map { e ->
            if (!e.isConnector || (e.from !in gone && e.to !in gone)) e
            else {
                val (s, t) = ff.endpoints(e)
                e.copy(from = if (e.from in gone) null else e.from, to = if (e.to in gone) null else e.to,
                    x = if (e.from in gone) s.first else e.x, y = if (e.from in gone) s.second else e.y,
                    x2 = if (e.to in gone) t.first else e.x2, y2 = if (e.to in gone) t.second else e.y2)
            }
        })
    }

    /** Copies elements with new ids, offset; connectors between copied elements stay connected. */
    fun copies(ff: Freeform, src: List<FfElement>, dx: Float = 24f, dy: Float = 24f): List<FfElement> {
        val map = src.associate { it.id to Ids.next() }
        val groupMap = src.mapNotNull { it.group }.toSet().associateWith { Ids.next() }
        return src.map { e ->
            if (e.isConnector) {
                val (s, t) = ff.endpoints(e)
                e.copy(
                    id = map.getValue(e.id), group = e.group?.let { groupMap[it] },
                    from = e.from?.let { map[it] }, to = e.to?.let { map[it] },
                    x = (if (e.from != null && map[e.from] == null) s.first else e.x) + dx, y = (if (e.from != null && map[e.from] == null) s.second else e.y) + dy,
                    x2 = (if (e.to != null && map[e.to] == null) t.first else e.x2) + dx, y2 = (if (e.to != null && map[e.to] == null) t.second else e.y2) + dy,
                    locked = false,
                )
            } else e.copy(id = map.getValue(e.id), x = e.x + dx, y = e.y + dy, group = e.group?.let { groupMap[it] }, locked = false)
        }
    }

    fun group(ff: Freeform, ids: Set<Long>): Freeform {
        if (ids.size < 2) return ff
        val g = Ids.next()
        return update(ff, ids) { it.copy(group = g) }
    }

    fun ungroup(ff: Freeform, ids: Set<Long>) = update(ff, withGroups(ff, ids)) { it.copy(group = null) }

    enum class Order { FRONT, FORWARD, BACKWARD, BACK }

    fun reorder(ff: Freeform, ids: Set<Long>, order: Order): Freeform {
        val sel = ff.elements.filter { it.id in ids }
        val rest = ff.elements.filter { it.id !in ids }
        val list = when (order) {
            Order.FRONT -> rest + sel
            Order.BACK -> sel + rest
            Order.FORWARD, Order.BACKWARD -> {
                val l = ff.elements.toMutableList()
                val idx = l.indices.filter { l[it].id in ids }
                if (order == Order.FORWARD) idx.reversed().forEach { i -> if (i < l.lastIndex && l[i + 1].id !in ids) { val t = l[i]; l[i] = l[i + 1]; l[i + 1] = t } }
                else idx.forEach { i -> if (i > 0 && l[i - 1].id !in ids) { val t = l[i]; l[i] = l[i - 1]; l[i - 1] = t } }
                l
            }
        }
        return ff.copy(elements = list)
    }

    enum class AlignMode { LEFT, HCENTER, RIGHT, TOP, VCENTER, BOTTOM }

    fun align(ff: Freeform, ids: Set<Long>, mode: AlignMode): Freeform {
        val sel = ff.elements.filter { it.id in ids && !it.isConnector && !it.locked }
        if (sel.size < 2) return ff
        val l = sel.minOf { it.x }; val r = sel.maxOf { it.x + it.w }; val t = sel.minOf { it.y }; val b = sel.maxOf { it.y + it.h }
        val moved = sel.associate { e ->
            e.id to when (mode) {
                AlignMode.LEFT -> e.copy(x = l)
                AlignMode.HCENTER -> e.copy(x = (l + r) / 2f - e.w / 2f)
                AlignMode.RIGHT -> e.copy(x = r - e.w)
                AlignMode.TOP -> e.copy(y = t)
                AlignMode.VCENTER -> e.copy(y = (t + b) / 2f - e.h / 2f)
                AlignMode.BOTTOM -> e.copy(y = b - e.h)
            }
        }
        return ff.copy(elements = ff.elements.map { moved[it.id] ?: it })
    }

    /** Evenly spaces elements horizontally or vertically (equal gaps). */
    fun distribute(ff: Freeform, ids: Set<Long>, horizontal: Boolean): Freeform {
        val sel = ff.elements.filter { it.id in ids && !it.isConnector && !it.locked }.sortedBy { if (horizontal) it.x else it.y }
        if (sel.size < 3) return ff
        val start = if (horizontal) sel.first().x else sel.first().y
        val end = if (horizontal) sel.last().x + sel.last().w else sel.last().y + sel.last().h
        val total = sel.sumOf { (if (horizontal) it.w else it.h).toDouble() }.toFloat()
        val gap = (end - start - total) / (sel.size - 1)
        var cur = start
        val moved = sel.associate { e ->
            val n = if (horizontal) e.copy(x = cur) else e.copy(y = cur)
            cur += (if (horizontal) e.w else e.h) + gap
            e.id to n
        }
        return ff.copy(elements = ff.elements.map { moved[it.id] ?: it })
    }

    fun matchSize(ff: Freeform, ids: Set<Long>, width: Boolean): Freeform {
        val sel = ff.elements.filter { it.id in ids && !it.isConnector }
        val ref = sel.firstOrNull() ?: return ff
        return update(ff, ids) { if (it.isConnector) it else if (width) it.copy(w = ref.w) else it.copy(h = ref.h) }
    }

    // ------------------------------------------------------------ Snapping

    class Snap(val dx: Float, val dy: Float, val guidesX: List<Float>, val guidesY: List<Float>)

    /**
     * Snaps a move of [ids] by (dx, dy): first to edges/centres of other elements and the artboard
     * (smart guides, within [threshold]), otherwise to the grid.
     */
    fun snapMove(base: Freeform, ids: Set<Long>, dx: Float, dy: Float, grid: Float, gridOn: Boolean, guidesOn: Boolean, threshold: Float): Snap {
        val moving = base.elements.filter { it.id in ids }
        if (moving.isEmpty()) return Snap(dx, dy, emptyList(), emptyList())
        val boxes = moving.map { base.bounds(it) }
        val l = boxes.minOf { it[0] } + dx; val t = boxes.minOf { it[1] } + dy
        val r = boxes.maxOf { it[0] + it[2] } + dx; val b = boxes.maxOf { it[1] + it[3] } + dy
        val xs = listOf(l, (l + r) / 2f, r); val ys = listOf(t, (t + b) / 2f, b)
        val targetsX = ArrayList<Float>(); val targetsY = ArrayList<Float>()
        if (guidesOn) {
            targetsX += listOf(0f, base.width / 2f, base.width); targetsY += listOf(0f, base.height / 2f, base.height)
            base.elements.filter { it.id !in ids && !it.hidden && !it.isConnector }.forEach {
                targetsX += listOf(it.x, it.cx, it.x + it.w); targetsY += listOf(it.y, it.cy, it.y + it.h)
            }
        }
        fun best(src: List<Float>, tgt: List<Float>): Pair<Float, Float>? {
            var bestD = threshold + 1f; var res: Pair<Float, Float>? = null
            for (s in src) for (g in tgt) { val d = abs(g - s); if (d < bestD) { bestD = d; res = (g - s) to g } }
            return if (bestD <= threshold) res else null
        }
        var ndx = dx; var ndy = dy
        val gx = ArrayList<Float>(); val gy = ArrayList<Float>()
        val bx = best(xs, targetsX)
        if (bx != null) { ndx += bx.first; gx += bx.second }
        else if (gridOn && grid > 0f) ndx += round(l / grid) * grid - l
        val by = best(ys, targetsY)
        if (by != null) { ndy += by.first; gy += by.second }
        else if (gridOn && grid > 0f) ndy += round(t / grid) * grid - t
        return Snap(ndx, ndy, gx, gy)
    }

    fun snapValue(v: Float, grid: Float, on: Boolean) = if (on && grid > 0f) round(v / grid) * grid else v

    // ------------------------------------------------------------ Transforms

    /**
     * Resizes [e] by dragging handle (hx, hy) ∈ {-1, 0, 1}² to world point [p], keeping the opposite
     * side fixed. Works for rotated elements.
     */
    fun resize(e: FfElement, hx: Int, hy: Int, p: Pair<Float, Float>, keepAspect: Boolean, grid: Float, gridOn: Boolean): FfElement {
        val th = Math.toRadians(e.rotation.toDouble())
        val c = cos(th).toFloat(); val s = sin(th).toFloat()
        val px = p.first - e.cx; val py = p.second - e.cy
        val lx = px * c + py * s; val ly = -px * s + py * c
        val ax = -hx * e.w / 2f; val ay = -hy * e.h / 2f
        var nw = if (hx != 0) max(MIN_SIZE, (lx - ax) * hx) else e.w
        var nh = if (hy != 0) max(MIN_SIZE, (ly - ay) * hy) else e.h
        if (e.rotation == 0f && gridOn) {
            if (hx != 0) nw = max(MIN_SIZE, snapValue(nw, grid, true))
            if (hy != 0) nh = max(MIN_SIZE, snapValue(nh, grid, true))
        }
        if (keepAspect && hx != 0 && hy != 0) {
            val k = max(nw / e.w, nh / e.h); nw = e.w * k; nh = e.h * k
        }
        val clx = if (hx != 0) ax + hx * nw / 2f else 0f
        val cly = if (hy != 0) ay + hy * nh / 2f else 0f
        val ncx = e.cx + clx * c - cly * s
        val ncy = e.cy + clx * s + cly * c
        return e.copy(x = ncx - nw / 2f, y = ncy - nh / 2f, w = nw, h = nh)
    }

    /** Rotation (degrees) of [e] so its top points towards [p]; [fine] disables snapping. */
    fun rotation(e: FfElement, p: Pair<Float, Float>, step15: Boolean): Float {
        var a = Math.toDegrees(atan2((p.second - e.cy).toDouble(), (p.first - e.cx).toDouble())).toFloat() + 90f
        a = ((a % 360f) + 360f) % 360f
        if (step15) a = round(a / 15f) * 15f
        else listOf(0f, 90f, 180f, 270f, 360f).firstOrNull { abs(it - a) < 3f }?.let { a = it }
        return if (a >= 360f) a - 360f else a
    }

    // ------------------------------------------------------------ Hit testing

    fun contains(ff: Freeform, e: FfElement, p: Pair<Float, Float>, tolerance: Float): Boolean {
        if (e.isConnector) {
            val pts = ff.routePoints(e)
            return pts.zipWithNext().any { (a, b) -> segDist(p, a, b) <= tolerance + e.strokeWidth / 2f }
        }
        val th = Math.toRadians(-e.rotation.toDouble())
        val dx = p.first - e.cx; val dy = p.second - e.cy
        val lx = dx * cos(th).toFloat() - dy * sin(th).toFloat()
        val ly = dx * sin(th).toFloat() + dy * cos(th).toFloat()
        val extraBottom = if (e.kind == FfKind.NODE && e.text.isNotBlank()) 40f else 0f
        return abs(lx) <= e.w / 2f + tolerance && ly >= -e.h / 2f - tolerance && ly <= e.h / 2f + tolerance + extraBottom
    }

    fun hit(ff: Freeform, p: Pair<Float, Float>, tolerance: Float, includeLocked: Boolean = false): FfElement? =
        ff.elements.asReversed().firstOrNull { !it.hidden && (includeLocked || !it.locked) && contains(ff, it, p, tolerance) }

    fun segDist(p: Pair<Float, Float>, a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
        val vx = b.first - a.first; val vy = b.second - a.second
        val len2 = vx * vx + vy * vy
        val t = if (len2 == 0f) 0f else (((p.first - a.first) * vx + (p.second - a.second) * vy) / len2).coerceIn(0f, 1f)
        return hypot(p.first - (a.first + vx * t), p.second - (a.second + vy * t))
    }

    fun inRect(ff: Freeform, e: FfElement, x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val b = ff.bounds(e)
        return b[0] >= min(x0, x1) && b[1] >= min(y0, y1) && b[0] + b[2] <= max(x0, x1) && b[1] + b[3] <= max(y0, y1)
    }

    /** Builds a freeform model from the current items, so switching to Freeform keeps the content. */
    fun fromItems(doc: ModelDoc): Freeform {
        val items = doc.items.take(16)
        if (items.isEmpty()) return starterFreeform()
        val cols = if (items.size <= 4) items.size else if (items.size <= 9) 3 else 4
        val boxW = 250f; val boxH = 100f; val gx = 60f; val gy = 60f
        val hasCore = doc.center.isNotBlank()
        val top = if (hasCore) 210f else 40f
        val rows = (items.size + cols - 1) / cols
        val width = max(1000f, cols * boxW + (cols - 1) * gx + 160f)
        val left = (width - (cols * boxW + (cols - 1) * gx)) / 2f
        val els = ArrayList<FfElement>()
        val core = if (hasCore) FfElement(kind = FfKind.PILL, x = width / 2f - 180f, y = 40f, w = 360f, h = 90f, text = doc.center, slot = Slots.HUB, textSize = 22f) else null
        core?.let { els += it }
        items.forEachIndexed { i, it ->
            val e = FfElement(
                kind = FfKind.ROUNDED, x = left + (i % cols) * (boxW + gx), y = top + (i / cols) * (boxH + gy), w = boxW, h = boxH,
                text = it.label, icon = it.icon ?: IconLibrary.defaultFor(i).id, image = it.image, material = Material.SOFT,
                slot = i, color = it.color, textSize = 18f,
            )
            els += e
            if (core != null) els += FfElement(kind = FfKind.CONNECTOR, x = 0f, y = 0f, w = 0f, h = 0f, from = core.id, to = e.id, slot = Slots.NEUTRAL, strokeWidth = 3f, route = Route.CURVED)
        }
        return Freeform(width, top + rows * (boxH + gy) + 20f, els)
    }
}

/** Custom models saved by the user ("My models"), stored as .scimodel files in the data folder. */
object PresetStore {
    private val dir get() = File(AppDirs.root, "models").also { it.mkdirs() }

    class Preset(val file: File, val name: String, val doc: ModelDoc) {
        val id: String get() = "preset-" + file.nameWithoutExtension.lowercase().replace(Regex("[^a-z0-9]+"), "-")
    }

    private fun safe(name: String) = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "").take(60).ifBlank { "My model" }

    fun list(): List<Preset> = runCatching {
        dir.listFiles { f -> f.extension == DocIO.EXT }.orEmpty().sortedByDescending { it.lastModified() }.mapNotNull { f ->
            runCatching { Preset(f, f.nameWithoutExtension, DocIO.fromJson(f.readText())) }.getOrNull()
        }
    }.getOrDefault(emptyList())

    fun save(name: String, doc: ModelDoc): Preset {
        var f = File(dir, safe(name) + "." + DocIO.EXT)
        f.writeAtomically(DocIO.toJson(doc))
        return Preset(f, f.nameWithoutExtension, doc)
    }

    fun exists(name: String) = File(dir, safe(name) + "." + DocIO.EXT).exists()

    fun delete(p: Preset) { p.file.delete() }

    fun rename(p: Preset, name: String): Preset {
        val target = File(dir, safe(name) + "." + DocIO.EXT)
        if (target != p.file) { target.writeAtomically(p.file.readText()); p.file.delete() }
        return Preset(target, target.nameWithoutExtension, p.doc)
    }

    fun duplicate(p: Preset): Preset {
        var n = 2
        while (exists("${p.name} $n")) n++
        return save("${p.name} $n", p.doc)
    }
}
