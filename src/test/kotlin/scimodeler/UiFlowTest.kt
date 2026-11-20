package scimodeler

import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runDesktopComposeUiTest
import scimodeler.core.Images
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

    private fun DesktopComposeUiTest.settle() = repeat(8) { Thread.sleep(150); waitForIdle() }

    private fun DesktopComposeUiTest.shot(name: String) {
        settle()
        ImageIO.write(onAllNodes(isRoot()).onFirst().captureToImage().toAwtImage(), "png", File(shots, "$name.png"))
    }

    private fun DesktopComposeUiTest.selectModel(id: String) {
        onNodeWithTag("model-gallery").performScrollToNode(hasTestTag("model-card-$id"))
        onNodeWithTag("model-card-$id").performClick()
        waitForIdle()
    }

    @Test
    fun mainFlows() {
        val home = Files.createTempDirectory("scimodeler-home").toFile()
        System.setProperty("scimodeler.home", home.absolutePath)

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
            onNodeWithTag("item-icon-0").performClick()
            waitForIdle()
            onNodeWithTag("icon-picker").assertExists()
            onNodeWithTag("icon-choose-image").performClick()
            waitForIdle()
            val image = assertNotNull(state.doc.items[0].image, "custom image assigned")
            val decoded = assertNotNull(Images.decode(image))
            assertEquals(2.0, decoded.width.toDouble() / decoded.height, 0.02, "aspect ratio preserved")
            shot("02-custom-icon-in-picker")
            onNodeWithTag("icon-close").performClick()
            waitUntil(timeoutMillis = 8000) { SessionStore.load()?.doc?.items?.firstOrNull()?.image == image }
            shot("03-custom-icon-in-model")

            // 3. Restart: a fresh editor restored from the saved session.
            state = initialState()
            generation.value++
            waitForIdle()
            assertEquals("branches", state.doc.layout)
            assertEquals(image, state.doc.items[0].image, "custom icon survives restart")
            shot("04-after-restart")

            // 4. Restore the default icon.
            onNodeWithTag("item-icon-0").performClick()
            waitForIdle()
            onNodeWithTag("icon-restore-default").performClick()
            waitForIdle()
            assertNull(state.doc.items[0].image)
            assertNull(state.doc.items[0].icon)
            shot("05-default-restored")
            onNodeWithTag("icon-close").performClick()
            waitUntil(timeoutMillis = 8000) { SessionStore.load()?.doc?.items?.firstOrNull()?.image == null }

            state = initialState()
            generation.value++
            waitForIdle()
            assertNull(state.doc.items[0].image, "default icon survives restart")

            // 5. Built-in icon choice.
            onNodeWithTag("item-icon-1").performClick()
            waitForIdle()
            onNodeWithTag("icon-flask").performClick()
            waitForIdle()
            assertEquals("flask", state.doc.items[1].icon)
            onNodeWithTag("icon-close").performClick()

            // 6. Reduced-motion preference is saved.
            onNodeWithTag("settings").performClick()
            waitForIdle()
            onNodeWithTag("motion-reduced").performClick()
            waitForIdle()
            assertEquals(MotionPref.REDUCED, SettingsStore.load().motion)

            // Screenshots of other screens and models for visual review.
            onNodeWithTag("tab-style").performClick(); shot("06-style-tab")
            onNodeWithTag("tab-export").performClick(); shot("07-export-tab")
            onNodeWithTag("tab-content").performClick()
            onNodeWithTag("new").performClick(); shot("08-templates")
            onNodeWithTag("templates-close").performClick()
            for (id in listOf("flower", "framework", "milestones", "cone", "cards")) { selectModel(id); shot("model-$id") }
        }
    }
}
