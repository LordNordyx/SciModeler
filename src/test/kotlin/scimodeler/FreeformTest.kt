package scimodeler

import scimodeler.core.DocIO
import scimodeler.core.FfElement
import scimodeler.core.FfKind
import scimodeler.core.Freeform
import scimodeler.core.FreeformOps
import scimodeler.core.Models
import scimodeler.core.Renderer
import scimodeler.core.Templates
import scimodeler.core.Themes
import scimodeler.core.applyTheme
import scimodeler.core.colorOf
import scimodeler.core.Ctx
import scimodeler.core.AwtMeasure
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FreeformTest {
    private fun box(x: Float, y: Float) = FfElement(kind = FfKind.ROUNDED, x = x, y = y, w = 100f, h = 60f)

    @Test
    fun colourResolverMatchesWhatLayoutsDraw() {
        for (t in Templates.all) for (spec in Models.all) {
            val doc = t.doc().copy(layout = spec.id)
            val ctx = Ctx(doc, AwtMeasure.of(doc.style.fontFamily))
            ctx.items.indices.forEach { assertEquals(doc.colorOf(it), ctx.color(it), "${t.name}/${spec.id}/$it") }
        }
    }

    @Test
    fun themeIsACompletePreset() {
        val doc = Templates.all.first().doc().let { d -> d.copy(items = d.items.mapIndexed { i, it -> if (i % 2 == 0) it.copy(color = 0xFF123456.toInt()) else it.copy(slot = 5) }) }
        val flat = doc.applyTheme("flat")
        val t = Themes.get("flat")
        assertEquals("flat", flat.style.theme)
        assertEquals(t.gradients, flat.style.gradients)
        assertEquals(t.shadows, flat.style.shadows)
        assertTrue(flat.items.all { it.color == null && it.slot == null })
        flat.items.indices.forEach { assertEquals(t.slot(it), flat.colorOf(it)) }
    }

    @Test
    fun slotColoursFollowTheTheme() {
        val doc = Templates.all.first().doc().let { d -> d.copy(items = d.items.mapIndexed { i, it -> if (i == 0) it.copy(slot = 3) else it }) }
        assertEquals(Themes.get(doc.style.theme).slot(3), doc.colorOf(0))
        val other = doc.copy(style = doc.style.copy(theme = "ocean"))
        assertEquals(Themes.get("ocean").slot(3), other.colorOf(0))
    }

    @Test
    fun snappingToGridAndGuides() {
        val ff = Freeform(1000f, 600f, listOf(box(103f, 47f), box(400f, 300f)))
        val a = ff.elements[0]
        val grid = FreeformOps.snapMove(ff, setOf(a.id), 20f, 20f, 10f, gridOn = true, guidesOn = false, threshold = 6f)
        assertEquals(0f, (a.x + grid.dx) % 10f)
        assertEquals(0f, (a.y + grid.dy) % 10f)
        // Within 6 units of the other box's left edge → snaps to it and reports a guide
        val guide = FreeformOps.snapMove(ff, setOf(a.id), 400f - 103f + 4f, 0f, 10f, gridOn = true, guidesOn = true, threshold = 6f)
        assertEquals(400f, a.x + guide.dx)
        assertTrue(400f in guide.guidesX)
    }

    @Test
    fun editingOperations() {
        var ff = Freeform(1000f, 600f, listOf(box(0f, 0f), box(200f, 50f), box(500f, 120f)))
        val ids = ff.elements.map { it.id }.toSet()
        ff = FreeformOps.align(ff, ids, FreeformOps.AlignMode.TOP)
        assertTrue(ff.elements.all { it.y == 0f })
        ff = FreeformOps.distribute(ff, ids, true)
        val xs = ff.elements.sortedBy { it.x }
        assertEquals(xs[1].x - (xs[0].x + xs[0].w), xs[2].x - (xs[1].x + xs[1].w), 0.01f)
        ff = FreeformOps.group(ff, ids)
        assertEquals(1, ff.elements.map { it.group }.toSet().size)
        assertEquals(ids, FreeformOps.withGroups(ff, setOf(ff.elements[0].id)))
        val conn = FfElement(kind = FfKind.CONNECTOR, x = 0f, y = 0f, w = 0f, h = 0f, from = ff.elements[0].id, to = ff.elements[1].id)
        ff = ff.copy(elements = ff.elements + conn)
        val copies = FreeformOps.copies(ff, ff.elements)
        val cc = copies.first { it.isConnector }
        assertTrue(cc.from in copies.map { it.id } && cc.to in copies.map { it.id }, "copied connector stays attached to the copies")
        ff = FreeformOps.delete(ff, setOf(ff.elements[0].id))
        val c2 = ff.elements.first { it.isConnector }
        assertNull(c2.from)
        assertNotNull(c2.to)
        ff = FreeformOps.reorder(ff, setOf(ff.elements.last().id), FreeformOps.Order.BACK)
        assertTrue(ff.elements.first().isConnector)
    }

    @Test
    fun resizeKeepsOppositeCornerFixed() {
        val e = FfElement(kind = FfKind.RECT, x = 100f, y = 100f, w = 100f, h = 50f, rotation = 30f)
        fun corner(el: FfElement, hx: Int, hy: Int): Pair<Float, Float> {
            val th = Math.toRadians(el.rotation.toDouble()); val c = Math.cos(th).toFloat(); val s = Math.sin(th).toFloat()
            val lx = hx * el.w / 2f; val ly = hy * el.h / 2f
            return (el.cx + lx * c - ly * s) to (el.cy + lx * s + ly * c)
        }
        val before = corner(e, -1, -1)
        val r = FreeformOps.resize(e, 1, 1, 260f to 230f, keepAspect = false, grid = 10f, gridOn = false)
        val after = corner(r, -1, -1)
        assertTrue(abs(before.first - after.first) < 0.01f && abs(before.second - after.second) < 0.01f)
        assertTrue(r.w > e.w)
    }

    @Test
    fun freeformSavesAndRenders() {
        val doc = Templates.all.first { it.doc().layout == "freeform" }.doc()
        val back = DocIO.fromJson(DocIO.toJson(doc))
        assertEquals(doc.freeform, back.freeform)
        val scene = Renderer.scene(back)
        assertTrue(scene.prims.size > 10)
        // the artboard defines the body size
        assertTrue(scene.width >= doc.freeform!!.width)
        val fromItems = FreeformOps.fromItems(Templates.all.first().doc())
        assertEquals(Templates.all.first().doc().items.size, fromItems.elements.count { it.kind == FfKind.ROUNDED })
    }
}
