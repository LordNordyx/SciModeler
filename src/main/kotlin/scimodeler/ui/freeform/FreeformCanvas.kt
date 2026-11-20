package scimodeler.ui.freeform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import scimodeler.core.FfElement
import scimodeler.core.Freeform
import scimodeler.core.FreeformOps
import scimodeler.core.Scene
import scimodeler.ui.theme.Tok
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private sealed interface Grip {
    data class Resize(val id: Long, val hx: Int, val hy: Int) : Grip
    data class Rotate(val id: Long) : Grip
    data class End(val id: Long, val start: Boolean) : Grip
}

private const val ROTATE_OFFSET = 30f // px above the element
private val guideColor = Color(0xFFEC4899)

/** World position of a point given in an element's local (centre-relative, unrotated) frame. */
private fun FfElement.world(lx: Float, ly: Float): Pair<Float, Float> {
    val th = Math.toRadians(rotation.toDouble())
    val c = cos(th).toFloat(); val s = sin(th).toFloat()
    return (cx + lx * c - ly * s) to (cy + lx * s + ly * c)
}

private fun grips(ff: Freeform, sel: Set<Long>, scale: Float): List<Pair<Grip, Pair<Float, Float>>> {
    if (sel.size != 1) return emptyList()
    val e = ff.byId(sel.first()) ?: return emptyList()
    if (e.locked) return emptyList()
    if (e.isConnector) {
        val (s, t) = ff.endpoints(e)
        return listOf(Grip.End(e.id, true) to s, Grip.End(e.id, false) to t)
    }
    val out = ArrayList<Pair<Grip, Pair<Float, Float>>>()
    for (hx in -1..1) for (hy in -1..1) if (hx != 0 || hy != 0) out += Grip.Resize(e.id, hx, hy) to e.world(hx * e.w / 2f, hy * e.h / 2f)
    out += Grip.Rotate(e.id) to e.world(0f, -e.h / 2f - ROTATE_OFFSET / scale)
    return out
}

/**
 * Interactive layer drawn over the rendered freeform model: selection, handles, guides, marquee,
 * connector creation and keyboard shortcuts. The model itself is drawn by the normal renderer
 * underneath, so what you edit is exactly what you export.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FreeformOverlay(ui: FreeformUi, scene: Scene, zoom: Float, modifier: Modifier) {
    val density = LocalDensity.current.density
    val scale = zoom * density
    val geo = rememberUpdatedState(Triple(scale, scene.bodyX, scene.bodyY))
    val focus = remember { FocusRequester() }
    val measurer = rememberTextMeasurer()
    val last = remember { longArrayOf(0L, 0L) } // last click time, id
    androidx.compose.runtime.SideEffect { ui.focusCanvas = { runCatching { focus.requestFocus() } } }

    fun toArt(o: Offset): Pair<Float, Float> { val (k, bx, by) = geo.value; return (o.x / k - bx) to (o.y / k - by) }

    Box(
        modifier.testTag("freeform-canvas")
            .onGloballyPositioned { c ->
                val (k, bx, by) = geo.value
                val w = c.positionInWindow()
                ui.artOrigin = Offset(w.x + bx * k, w.y + by * k); ui.artScale = k
            }
            .focusRequester(focus).focusable()
            .onPreviewKeyEvent { ui.onKey(it) }
            .pointerHoverIcon(if (ui.tool == Tool.CONNECT) PointerIcon.Crosshair else PointerIcon.Default)
            .onPointerEvent(PointerEventType.Move) { ev ->
                val p = toArt(ev.changes.first().position)
                ui.hover = FreeformOps.hit(ui.ff, p, 5f / geo.value.first)?.id
            }
            .onPointerEvent(PointerEventType.Exit) { ui.hover = null }
            .pointerInput(ui) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    focus.requestFocus()
                    val mods = currentEvent.keyboardModifiers
                    val additive = mods.isShiftPressed || mods.isCtrlPressed || mods.isMetaPressed
                    val k = geo.value.first
                    val p0 = toArt(down.position)
                    val base = ui.ff
                    val grip = grips(base, ui.selection, k).firstOrNull { (_, at) -> hypot(at.first - p0.first, at.second - p0.second) * k <= 9f }?.first
                    val hit = if (grip == null) FreeformOps.hit(base, p0, 5f / k) else null
                    val now = System.currentTimeMillis()
                    if (hit != null && hit.id == last[1] && now - last[0] < 420) ui.textEditRequest++
                    last[0] = now; last[1] = hit?.id ?: 0L

                    val connecting = grip == null && ui.tool == Tool.CONNECT
                    if (grip == null && !connecting) {
                        when {
                            hit != null && additive -> ui.select(setOf(hit.id), additive = true)
                            hit != null && hit.id !in ui.selection -> ui.select(setOf(hit.id))
                            hit == null && !additive -> ui.selection = emptySet()
                        }
                    }
                    val moving = FreeformOps.withGroups(base, ui.selection).filter { id -> base.byId(id)?.locked == false }.toSet()
                    if (connecting) ui.connectFrom = hit?.let { it.cx to it.cy } ?: p0
                    var dragged = false
                    var p = p0
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull() ?: break
                        if (!ch.pressed) break
                        p = toArt(ch.position)
                        if (!dragged && hypot(p.first - p0.first, p.second - p0.second) * k < 3f) continue
                        dragged = true
                        ch.consume()
                        val shift = ev.keyboardModifiers.isShiftPressed
                        val noSnap = ev.keyboardModifiers.isAltPressed
                        when {
                            grip is Grip.Resize -> {
                                val e0 = base.byId(grip.id) ?: break
                                val ne = FreeformOps.resize(e0, grip.hx, grip.hy, p, shift, ui.grid, ui.gridOn && !noSnap)
                                ui.edit("ff-resize") { FreeformOps.replace(base, ne) }
                                ui.badge = "${ne.w.roundToInt()} × ${ne.h.roundToInt()}"; ui.badgeAt = p
                            }
                            grip is Grip.Rotate -> {
                                val e0 = base.byId(grip.id) ?: break
                                val a = FreeformOps.rotation(e0, p, shift)
                                ui.edit("ff-rotate") { FreeformOps.replace(base, e0.copy(rotation = a)) }
                                ui.badge = "${a.roundToInt()}°"; ui.badgeAt = p
                            }
                            grip is Grip.End -> {
                                val e0 = base.byId(grip.id) ?: break
                                val target = FreeformOps.hit(base, p, 4f / k)?.takeIf { !it.isConnector && it.id != e0.id }
                                ui.connectTarget = target?.id
                                val ne = if (grip.start) e0.copy(x = p.first, y = p.second, from = null) else e0.copy(x2 = p.first, y2 = p.second, to = null)
                                ui.edit("ff-end") { FreeformOps.replace(base, ne) }
                            }
                            connecting -> {
                                ui.connectTo = p
                                ui.connectTarget = FreeformOps.hit(base, p, 4f / k)?.takeIf { !it.isConnector && it.id != hit?.id }?.id
                            }
                            hit != null && moving.isNotEmpty() -> {
                                var dx = p.first - p0.first; var dy = p.second - p0.second
                                if (shift) { if (abs(dx) > abs(dy)) dy = 0f else dx = 0f }
                                val snap = FreeformOps.snapMove(base, moving, dx, dy, ui.grid, ui.gridOn && !noSnap, ui.guidesOn && !noSnap, 6f / k)
                                ui.edit("ff-move") { FreeformOps.move(base, moving, snap.dx, snap.dy) }
                                ui.guidesX = snap.guidesX; ui.guidesY = snap.guidesY
                                val first = base.byId(moving.first())
                                if (first != null && !first.isConnector) { ui.badge = "${(first.x + snap.dx).roundToInt()}, ${(first.y + snap.dy).roundToInt()}"; ui.badgeAt = p }
                            }
                            else -> ui.marquee = floatArrayOf(p0.first, p0.second, p.first, p.second)
                        }
                    }
                    // Release
                    when {
                        grip is Grip.End && ui.connectTarget != null -> {
                            val t = ui.connectTarget
                            ui.edit("ff-end") { f -> FreeformOps.update(f, setOf(grip.id)) { if (grip.start) it.copy(from = t) else it.copy(to = t) } }
                        }
                        connecting && dragged -> {
                            val c = FfElement(
                                kind = scimodeler.core.FfKind.CONNECTOR, x = p0.first, y = p0.second, w = 0f, h = 0f, x2 = p.first, y2 = p.second,
                                from = hit?.id, to = ui.connectTarget, slot = scimodeler.core.Slots.INK,
                            )
                            ui.edit { it.copy(elements = it.elements + c) }
                            ui.selection = setOf(c.id)
                        }
                        ui.marquee != null -> {
                            val m = ui.marquee!!
                            val inside = base.elements.filter { !it.hidden && !it.locked && FreeformOps.inRect(base, it, m[0], m[1], m[2], m[3]) }.map { it.id }.toSet()
                            ui.select(inside, additive)
                        }
                        !dragged && hit != null && !additive && grip == null && !connecting -> ui.select(setOf(hit.id))
                    }
                    ui.clearTransient()
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) { drawOverlay(ui, scene, scale, measurer) }
    }
}

private fun DrawScope.drawOverlay(ui: FreeformUi, scene: Scene, k: Float, measurer: androidx.compose.ui.text.TextMeasurer) {
    val ff = ui.ff
    val bx = scene.bodyX; val by = scene.bodyY
    fun px(p: Pair<Float, Float>) = Offset((p.first + bx) * k, (p.second + by) * k)
    val accent = Tok.C.accent

    // Artboard and grid
    val origin = px(0f to 0f)
    drawRect(Tok.C.lineStrong, origin, Size(ff.width * k, ff.height * k), style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
    if (ui.showGrid && ui.grid > 0f) {
        var step = ui.grid * k
        while (step < 9f) step *= 5f
        val pts = ArrayList<Offset>()
        var x = origin.x
        while (x <= origin.x + ff.width * k + 0.5f) {
            var y = origin.y
            while (y <= origin.y + ff.height * k + 0.5f) { pts += Offset(x, y); y += step }
            x += step
        }
        drawPoints(pts, PointMode.Points, Color(0x330B1220), strokeWidth = 1.6f, cap = StrokeCap.Round)
    }

    fun outline(e: FfElement, color: Color, width: Float) {
        if (e.isConnector) {
            val pts = ff.routePoints(e).map(::px)
            val path = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
            drawPath(path, color.copy(alpha = 0.35f), style = Stroke(width + 6f, cap = StrokeCap.Round))
            return
        }
        val c = px(e.cx to e.cy)
        rotate(e.rotation, c) {
            drawRoundRect(color, Offset(c.x - e.w * k / 2f - 3f, c.y - e.h * k / 2f - 3f), Size(e.w * k + 6f, e.h * k + 6f), CornerRadius(4f), style = Stroke(width))
        }
    }

    ui.hover?.let { id -> if (id !in ui.selection) ff.byId(id)?.let { outline(it, accent.copy(alpha = 0.45f), 1.5f) } }
    ui.connectTarget?.let { id -> ff.byId(id)?.let { outline(it, Color(0xFF10B981), 3f) } }
    val sel = ui.selection
    ff.elements.filter { it.id in sel }.forEach { outline(it, if (it.locked) Tok.C.text3 else accent, 2f) }

    // Handles
    for ((g, at) in grips(ff, sel, k)) {
        val o = px(at)
        when (g) {
            is Grip.Rotate -> {
                val e = ff.byId(g.id) ?: continue
                drawLine(accent, px(e.world(0f, -e.h / 2f)), o, 1.5f)
                drawCircle(Color.White, 6.5f, o); drawCircle(accent, 6.5f, o, style = Stroke(2f))
            }
            is Grip.End -> { drawCircle(Color.White, 7f, o); drawCircle(accent, 7f, o, style = Stroke(2.5f)) }
            is Grip.Resize -> {
                drawRect(Color.White, Offset(o.x - 5f, o.y - 5f), Size(10f, 10f))
                drawRect(accent, Offset(o.x - 5f, o.y - 5f), Size(10f, 10f), style = Stroke(1.6f))
            }
        }
    }

    // Smart guides
    ui.guidesX.forEach { gx -> drawLine(guideColor, px(gx to -20f), px(gx to ff.height + 20f), 1.2f) }
    ui.guidesY.forEach { gy -> drawLine(guideColor, px(-20f to gy), px(ff.width + 20f to gy), 1.2f) }

    // Marquee
    ui.marquee?.let { m ->
        val a = px(minOf(m[0], m[2]) to minOf(m[1], m[3]))
        val sz = Size(abs(m[2] - m[0]) * k, abs(m[3] - m[1]) * k)
        drawRect(accent.copy(alpha = 0.08f), a, sz)
        drawRect(accent, a, sz, style = Stroke(1.2f))
    }

    // Connector being drawn
    val cf = ui.connectFrom; val ct = ui.connectTo
    if (cf != null && ct != null) {
        drawLine(accent, px(cf), px(ct), 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        drawCircle(accent, 5f, px(ct))
    }

    // Size / position badge
    ui.badge?.let { text ->
        val layout = measurer.measure(text, TextStyle(fontFamily = Tok.inter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
        val o = px(ui.badgeAt) + Offset(14f, 14f)
        drawRoundRect(Tok.C.tooltip, o, Size(layout.size.width + 14f, layout.size.height + 8f), CornerRadius(6f))
        drawText(layout, topLeft = o + Offset(7f, 4f))
    }
}
