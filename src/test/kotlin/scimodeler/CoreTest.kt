package scimodeler

import scimodeler.core.DocIO
import scimodeler.core.ExportSpec
import scimodeler.core.Exporter
import scimodeler.core.IconLibrary
import scimodeler.core.Images
import scimodeler.core.Item
import scimodeler.core.ModelDoc
import scimodeler.core.Models
import scimodeler.core.Renderer
import scimodeler.core.Role
import scimodeler.core.Templates
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoreTest {
    private val shots = File(System.getProperty("scimodeler.screenshots") ?: "build/screenshots", "models").apply { mkdirs() }

    @Test
    fun everyModelRendersEveryTemplate() {
        for (t in Templates.all) for (spec in Models.all) {
            val scene = Renderer.scene(t.doc().copy(layout = spec.id))
            assertTrue(scene.width > 0 && scene.height > 0, "${t.name}/${spec.id}")
            assertTrue(Renderer.image(scene, 0.2f).width > 0)
            assertTrue(Renderer.svg(scene).startsWith("<svg"))
        }
    }

    @Test
    fun templatesRenderToFiles() {
        Templates.all.forEachIndexed { k, t ->
            val img = Renderer.image(Renderer.scene(t.doc()), 0.7f)
            ImageIO.write(img, "png", File(shots, "%02d-%s.png".format(k, t.doc().layout)))
        }
    }

    @Test
    fun edgeCasesDoNotCrash() {
        val base = ModelDoc()
        for (n in listOf(0, 1, 2, 3, 7, 12)) for (spec in Models.all) {
            val items = List(n) { Item(label = "Item $it with a fairly long label čšž", bullets = listOf("a", "", "b"), role = Role.entries[it % 4]) }
            Renderer.image(Renderer.scene(base.copy(items = items, layout = spec.id)), 0.2f)
        }
    }

    @Test
    fun saveAndLoadRoundTrip() {
        val d = Templates.all.first().doc().let { doc ->
            doc.copy(items = doc.items.mapIndexed { i, x -> if (i == 0) x.copy(color = 0xFF112233.toInt(), icon = "flask", role = Role.OUTPUT) else x })
        }
        val back = DocIO.fromJson(DocIO.toJson(d))
        assertEquals(d.title, back.title)
        assertEquals(d.layout, back.layout)
        assertEquals(d.style, back.style)
        assertEquals(d.items.map { listOf(it.label, it.bullets, it.color, it.icon, it.role) }, back.items.map { listOf(it.label, it.bullets, it.color, it.icon, it.role) })
    }

    @Test
    fun opensFilesFromVersion1() {
        val v1 = """{"format":"scimodeler","version":1,"layout":"PYRAMID","title":"Old","items":[{"label":"A","bullets":["x"],"color":null}],"style":{"theme":"soft"}}"""
        val d = DocIO.fromJson(v1)
        assertEquals("pyramid", d.layout)
        assertEquals("A", d.items.single().label)
    }

    @Test
    fun customImageKeepsProportions() {
        val src = BufferedImage(600, 300, BufferedImage.TYPE_INT_ARGB)
        val f = File.createTempFile("icon", ".png")
        ImageIO.write(src, "png", f)
        val img = assertNotNull(Images.decode(Images.import(f)))
        assertEquals(256, img.width)
        assertEquals(128, img.height)
        f.delete()
    }

    @Test
    fun iconLibraryIsComplete() {
        assertTrue(IconLibrary.all.size >= 40)
        assertEquals(IconLibrary.all.size, IconLibrary.all.map { it.id }.toSet().size)
        IconLibrary.all.forEach { assertTrue(it.pen.strokes.isNotEmpty() || it.pen.fills.isNotEmpty(), it.id) }
    }

    @Test
    fun pngExportHasRequestedSize() {
        val scene = Renderer.scene(Templates.all.first().doc())
        val f = File.createTempFile("scimodeler", ".png")
        Exporter.png(scene, ExportSpec(10f, 300), f)
        assertEquals(Math.round(10f / 2.54f * 300), ImageIO.read(f).width)
        f.delete()
    }
}
