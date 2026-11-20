package scimodeler

import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import scimodeler.core.FfKind
import scimodeler.core.GlassPref
import scimodeler.core.MotionPref
import scimodeler.core.PresetStore
import scimodeler.core.Renderer
import scimodeler.core.SessionStore
import scimodeler.core.SettingsStore
import scimodeler.core.Templates
import scimodeler.core.Themes
import scimodeler.core.colorOf
import scimodeler.ui.AppRoot
import scimodeler.ui.Dialogs
import scimodeler.ui.EditorState
import scimodeler.ui.UiState
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Theme colours, new models and the freeform builder, driven through the real interface. */
@OptIn(ExperimentalTestApi::class)
class BuilderFlowTest {
    private val shots = File(System.getProperty("scimodeler.screenshots") ?: "build/screenshots", "builder").apply { mkdirs() }
    private val t0 = System.currentTimeMillis()
    private fun step(msg: String) = println("STEP +${(System.currentTimeMillis() - t0) / 1000}s builder: $msg")

    private fun DesktopComposeUiTest.settle() = repeat(3) { Thread.sleep(120); waitForIdle() }
    private fun DesktopComposeUiTest.shot(name: String) {
        step("shot $name"); settle()
        ImageIO.write(onAllNodes(isRoot()).onFirst().captureToImage().toAwtImage(), "png", File(shots, "$name.png"))
    }
    private fun DesktopComposeUiTest.park() = onAllNodes(isRoot()).onFirst().performMouseInput { moveTo(Offset(760f, 140f)) }
    private fun DesktopComposeUiTest.click(tag: String) {
        step("click $tag")
        onNodeWithTag(tag, useUnmergedTree = true).performClick()
        park(); settle()
    }
    private fun DesktopComposeUiTest.model(id: String) {
        step("model $id")
        onNodeWithTag("model-gallery").performScrollToNode(hasTestTag("model-card-$id"))
        onNodeWithTag("model-card-$id").performClick()
        park(); settle()
    }

    @Test
    fun builderFlows() {
        val home = Files.createTempDirectory("scimodeler-builder").toFile()
        System.setProperty("scimodeler.home", home.absolutePath)
        val fast = System.getProperty("os.name").lowercase().contains("win") || System.getProperty("scimodeler.fast") != null
        if (fast) SettingsStore.save(SettingsStore.load().copy(glass = GlassPref.SOLID, motion = MotionPref.REDUCED))
        val dialogs = object : Dialogs {
            override fun openModel(): File? = null
            override fun saveFile(title: String, suggested: String, ext: String): File? = null
            override fun pickImage(): File? = null
        }
        val generation = mutableStateOf(0)
        var state = EditorState(Templates.all.first().doc())

        runDesktopComposeUiTest(1500, 940) {
            setContent { key(generation.value) { AppRoot(state, dialogs, UiState()) } }
            settle()

            // 1. Colour selector reflects and sets the colour the model uses.
            click("item-color-0")
            click("slot-3")
            assertEquals(3, state.doc.items[0].slot)
            assertEquals(Themes.get(state.doc.style.theme).slot(3), state.doc.colorOf(0))

            // 2. A theme restyles everything at once.
            click("tab-style")
            onNodeWithTag("theme-flat").performClick(); park(); settle()
            val flat = Themes.get("flat")
            assertEquals("flat", state.doc.style.theme)
            assertEquals(false, state.doc.style.gradients)
            assertTrue(state.doc.items.indices.all { state.doc.colorOf(it) == flat.slot(it) }, "every item recoloured")
            shot("01-theme-flat")
            click("tab-content")

            // 3. New models.
            for (id in listOf("tree", "topics", "mediation", "path")) { model(id); if (!fast) shot("model-$id") }
            assertEquals("path", state.doc.layout)

            // 4. Freeform builder, starting from the current content.
            model("freeform")
            val ff0 = assertNotNull(state.doc.freeform)
            assertTrue(ff0.elements.isNotEmpty())
            shot("02-freeform-from-items")

            // Add a component from the library.
            val before = state.doc.freeform!!.elements.size
            onNodeWithTag("component-library").performScrollToNode(hasTestTag("component-hexagon"))
            click("component-hexagon")
            val added = state.doc.freeform!!.elements.last()
            assertEquals(before + 1, state.doc.freeform!!.elements.size)
            assertEquals(FfKind.HEXAGON, added.kind)

            // Drag it on the canvas; position snaps to the 10 px grid or a smart guide.
            val scene = Renderer.scene(state.doc)
            val node = onNodeWithTag("freeform-canvas").fetchSemanticsNode()
            val k = node.size.width / scene.width
            fun px(x: Float, y: Float) = Offset((x + scene.bodyX) * k, (y + scene.bodyY) * k)
            step("drag")
            onNodeWithTag("freeform-canvas").performMouseInput {
                moveTo(px(added.cx, added.cy)); press()
                for (i in 1..10) moveTo(px(added.cx + 5.3f * i, added.cy + 2.7f * i))
                release()
            }
            park(); settle()
            val moved = state.doc.freeform!!.elements.first { it.id == added.id }
            assertTrue(abs(moved.x - (added.x + 53f)) <= 10f && abs(moved.y - (added.y + 27f)) <= 10f, "moved with the pointer: $moved")
            val others = state.doc.freeform!!.elements.filter { it.id != added.id && !it.isConnector }
            val snapped = moved.x % 10f == 0f || others.any { o -> listOf(o.x, o.cx, o.x + o.w).any { abs(it - moved.x) < 0.01f || abs(it - moved.cx) < 0.01f || abs(it - (moved.x + moved.w)) < 0.01f } }
            assertTrue(snapped, "x snapped: ${moved.x}")
            shot("03-dragged")

            // Delete with the keyboard, then undo.
            onNodeWithTag("freeform-canvas").performKeyInput { pressKey(Key.Delete) }
            settle()
            assertTrue(state.doc.freeform!!.elements.none { it.id == added.id })
            state.undo(); settle()
            assertTrue(state.doc.freeform!!.elements.any { it.id == added.id })

            // Select everything and group it.
            onNodeWithTag("freeform-canvas").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.A) } }
            settle()
            click("ff-group")
            val groups = state.doc.freeform!!.elements.map { it.group }.toSet()
            assertEquals(1, groups.size); assertNotNull(groups.first())
            shot("04-grouped")
            onNodeWithTag("freeform-canvas").performKeyInput { withKeyDown(Key.CtrlLeft) { withKeyDown(Key.ShiftLeft) { pressKey(Key.G) } } }
            settle()
            assertTrue(state.doc.freeform!!.elements.all { it.group == null })

            // Connect two shapes with the connect tool.
            val boxes = state.doc.freeform!!.elements.filter { !it.isConnector }.take(2)
            click("tool-connect")
            val connectorsBefore = state.doc.freeform!!.elements.count { it.isConnector }
            onNodeWithTag("freeform-canvas").performMouseInput {
                moveTo(px(boxes[0].cx, boxes[0].cy)); press()
                for (i in 1..12) moveTo(px(boxes[0].cx + (boxes[1].cx - boxes[0].cx) * i / 12f, boxes[0].cy + (boxes[1].cy - boxes[0].cy) * i / 12f))
                release()
            }
            park(); settle()
            val conn = state.doc.freeform!!.elements.filter { it.isConnector }
            assertEquals(connectorsBefore + 1, conn.size)
            assertEquals(boxes[0].id to boxes[1].id, conn.last().from to conn.last().to)
            onNodeWithTag("freeform-canvas").performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("freeform-canvas").performKeyInput { pressKey(Key.Escape) }
            settle()

            // 5. Save as a custom model and find it in the library.
            click("save-preset")
            onNodeWithTag("save-preset-dialog-name").performTextReplacement("Research framework")
            click("save-preset-dialog-confirm")
            val preset = PresetStore.list().firstOrNull { it.name == "Research framework" }
            assertNotNull(preset)
            click("left-models")
            onNodeWithTag("model-gallery").performScrollToNode(hasTestTag("model-card-${preset.id}"))
            shot("05-my-models")

            // 6. Restart: the freeform model is restored from the session.
            val saved = state.doc.freeform
            waitUntil(timeoutMillis = 8000) { SessionStore.load()?.doc?.freeform == saved }
            state = initialState()
            generation.value++
            settle()
            assertEquals(saved, state.doc.freeform)
            shot("06-after-restart")
            step("builder flows passed")
        }
    }
}
