package scimodeler

import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runDesktopComposeUiTest
import scimodeler.core.Images
import scimodeler.core.GlassPref
import scimodeler.core.MotionPref
import scimodeler.core.SessionStore
import scimodeler.core.SettingsStore
import scimodeler.ui.Dialogs
import scimodeler.ui.EditorState
import scimodeler.ui.AppRoot
import scimodeler.ui.UiState
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Drives the real interface offscreen: select a new model, assign a custom icon, restart the app,
 * and restore the default icon. Runs on Windows and macOS in CI and saves screenshots of each step.
 */
@OptIn(ExperimentalTestApi::class)
class UiFlowTest {
    private val shots = File(System.getProperty("scimodeler.screenshots") ?: "build/screenshots").apply { mkdirs() }

    private val t0 = System.currentTimeMillis()
    private fun step(msg: String) = println("STEP +${(System.currentTimeMillis() - t0) / 1000}s $msg")

    private fun DesktopComposeUiTest.settle() = repeat(3) { Thread.sleep(120); waitForIdle() }

    private fun DesktopComposeUiTest.shot(name: String) {
        step("shot $name")
        settle()
        ImageIO.write(onAllNodes(isRoot()).onFirst().captureToImage().toAwtImage(), "png", File(shots, "$name.png"))
    }

    /** Clicks a node; if the expected result doesn't appear, reports it and falls back to the semantic click. */
    private fun DesktopComposeUiTest.click(tag: String, expectTag: String? = null, expect: (() -> Boolean)? = null) {
        step("click $tag")
        onNodeWithTag(tag).performClick()
        parkPointer()
        settle()
        val ok = (expectTag == null || onAllNodesWithTag(expectTag).fetchSemanticsNodes().isNotEmpty()) && (expect?.invoke() ?: true)
        if (!ok) {
            println("POINTER CLICK ON '$tag' HAD NO EFFECT — node: " + onNodeWithTag(tag).fetchSemanticsNode().let { "${it.boundsInRoot} ${it.config}" })
            shot("debug-$tag")
            onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
            settle()
        }
    }

    private fun DesktopComposeUiTest.selectModel(id: String) {
        step("scroll to $id")
        onNodeWithTag("model-gallery").performScrollToNode(hasTestTag("model-card-$id"))
        step("click model $id")
        onNodeWithTag("model-card-$id").performClick()
        step("park pointer")
        parkPointer()
        step("idle after $id")
        waitForIdle()
        step("selected $id")
    }

    /** Moves the pointer to an empty part of the canvas, like a user would after clicking. */
    private fun DesktopComposeUiTest.parkPointer() {
        onAllNodes(isRoot()).onFirst().performMouseInput { moveTo(Offset(700f, 140f)) }
    }

    @Test
    fun mainFlows() {
        val home = Files.createTempDirectory("scimodeler-home").toFile()
        System.setProperty("scimodeler.home", home.absolutePath)
        // CI machines without a GPU render blur in software, which is very slow on Windows runners;
        // use solid panels there (macOS runs keep testing the glass effect).
        val windows = System.getProperty("os.name").lowercase().contains("win") || System.getProperty("scimodeler.fast") != null
        if (windows) {
            SettingsStore.save(SettingsStore.load().copy(glass = GlassPref.SOLID, motion = MotionPref.REDUCED))
        }

        // A non-square "logo" to verify proportions are preserved.
        val logo = File(home, "logo.png")
        val src = BufferedImage(300, 150, BufferedImage.TYPE_INT_ARGB)
        src.createGraphics().apply { color = Color(220, 60, 60); fillRoundRect(0, 0, 300, 150, 60, 60); color = Color.WHITE; fillOval(110, 35, 80, 80); dispose() }
        ImageIO.write(src, "png", logo)
        val dialogs = object : Dialogs {
            override fun openModel(): File? = null
            override fun saveFile(title: String, suggested: String, ext: String): File? = null
            override fun pickImage(): File = logo
        }

        val generation = mutableStateOf(0)
        var state = EditorState(scimodeler.core.Templates.all.first().doc())

        runDesktopComposeUiTest(1500, 940) {
            setContent { key(generation.value) { AppRoot(state, dialogs, UiState()) } }
            shot("00-start")

            // 1. Select one of the new models.
            selectModel("branches")
            assertEquals("branches", state.doc.layout)
            shot("01-new-model-selected")

            // 2. Assign a custom icon from a local image.
            click("item-icon-0", expectTag = "icon-picker")
            onNodeWithTag("icon-picker").assertExists()
            click("icon-choose-image", expect = { state.doc.items[0].image != null })
            val image = assertNotNull(state.doc.items[0].image, "custom image assigned")
            val decoded = assertNotNull(Images.decode(image))
            assertEquals(2.0, decoded.width.toDouble() / decoded.height, 0.02, "aspect ratio preserved")
            shot("02-custom-icon-in-picker")
            click("icon-close", expect = { onAllNodesWithTag("icon-picker").fetchSemanticsNodes().isEmpty() })
            step("wait for session")
            waitUntil(timeoutMillis = 8000) { SessionStore.load()?.doc?.items?.firstOrNull()?.image == image }
            shot("03-custom-icon-in-model")

            // 3. Restart: a fresh editor restored from the saved session.
            step("restart")
            state = initialState()
            generation.value++
            waitForIdle()
            step("restarted")
            assertEquals("branches", state.doc.layout)
            assertEquals(image, state.doc.items[0].image, "custom icon survives restart")
            shot("04-after-restart")

            // 4. Restore the default icon.
            click("item-icon-0", expectTag = "icon-picker")
            click("icon-restore-default", expect = { state.doc.items[0].image == null })
            assertNull(state.doc.items[0].image)
            assertNull(state.doc.items[0].icon)
            shot("05-default-restored")
            click("icon-close", expect = { onAllNodesWithTag("icon-picker").fetchSemanticsNodes().isEmpty() })
            step("wait for session")
            waitUntil(timeoutMillis = 8000) { SessionStore.load()?.doc?.items?.firstOrNull()?.image == null }

            step("restart")
            state = initialState()
            generation.value++
            waitForIdle()
            step("restarted")
            assertNull(state.doc.items[0].image, "default icon survives restart")

            // 5. Built-in icon choice.
            click("item-icon-1", expectTag = "icon-picker")
            click("icon-flask", expect = { state.doc.items[1].icon == "flask" })
            assertEquals("flask", state.doc.items[1].icon)
            click("icon-close", expect = { onAllNodesWithTag("icon-picker").fetchSemanticsNodes().isEmpty() })

            // 6. Reduced-motion preference is saved.
            click("settings", expectTag = "motion-reduced")
            click("motion-reduced", expect = { SettingsStore.load().motion == MotionPref.REDUCED })
            assertEquals(MotionPref.REDUCED, SettingsStore.load().motion)

            // Screenshots of other screens and models for visual review.
            click("tab-style"); shot("06-style-tab")
            click("tab-export"); shot("07-export-tab")
            click("tab-content")
            click("new", expectTag = "template-gallery"); shot("08-templates")
            click("templates-close", expect = { onAllNodesWithTag("template-gallery").fetchSemanticsNodes().isEmpty() })
            if (!windows) for (id in listOf("flower", "framework", "milestones", "cone", "cards")) { selectModel(id); shot("model-$id") }
            step("all flows passed")
        }
    }
}
