package scimodeler

import scimodeler.core.DocIO
import scimodeler.core.ExportSpec
import scimodeler.core.Exporter
import scimodeler.core.Item
import scimodeler.core.LayoutType
import scimodeler.core.ModelDoc
import scimodeler.core.Renderer
import scimodeler.core.Templates
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreTest {
    @Test
    fun everyLayoutRendersEveryTemplate() {
        for (t in Templates.all) for (lt in LayoutType.entries) {
            val scene = Renderer.scene(t.doc().copy(layout = lt))
            assertTrue(scene.width > 0 && scene.height > 0, "${t.name}/$lt")
            val img = Renderer.image(scene, 0.25f)
            assertTrue(img.width > 0)
            assertTrue(Renderer.svg(scene).startsWith("<svg"))
        }
    }

    @Test
    fun edgeCasesDoNotCrash() {
        val base = ModelDoc(title = "", center = "")
        for (n in listOf(0, 1, 2, 3, 7, 12)) for (lt in LayoutType.entries) {
            val items = List(n) { Item(label = "Item $it with a fairly long label čšž", bullets = listOf("a", "", "b")) }
            Renderer.image(Renderer.scene(base.copy(items = items, layout = lt)), 0.2f)
        }
    }

    @Test
    fun saveAndLoadRoundTrip() {
        val d = Templates.all.first().doc().let { it.copy(items = it.items.mapIndexed { i, x -> if (i == 0) x.copy(color = 0xFF112233.toInt()) else x }) }
        val back = DocIO.fromJson(DocIO.toJson(d))
        assertEquals(d.title, back.title)
        assertEquals(d.layout, back.layout)
        assertEquals(d.style, back.style)
        assertEquals(d.items.map { Triple(it.label, it.bullets, it.color) }, back.items.map { Triple(it.label, it.bullets, it.color) })
    }

    @Test
    fun pngExportHasRequestedSize() {
        val scene = Renderer.scene(Templates.all.first().doc())
        val f = File.createTempFile("scimodeler", ".png")
        Exporter.png(scene, ExportSpec(10f, 300), f)
        val img = javax.imageio.ImageIO.read(f)
        assertEquals((10f / 2.54f * 300).toInt(), img.width, "width", )
        f.delete()
    }
}
