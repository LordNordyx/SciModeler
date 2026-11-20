package scimodeler.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import scimodeler.core.Renderer
import scimodeler.core.Scene
import scimodeler.ui.components.GlassPanel
import scimodeler.ui.components.IconBtn
import scimodeler.ui.theme.LocalMotion
import scimodeler.ui.theme.Tok
import kotlin.math.min
import kotlin.math.roundToInt

class CanvasState {
    var fit by mutableStateOf(true)
    var zoom by mutableStateOf(1f)
    /** Zoom actually in use (fit-to-window or manual). */
    var effective by mutableStateOf(1f)
    var sceneW by mutableStateOf(0f)
    var sceneH by mutableStateOf(0f)

    fun zoomBy(f: Float) { zoom = (effective * f).coerceIn(0.1f, 4f); fit = false }
}

private class Rendered(val layout: String, val bitmap: ImageBitmap)

@Composable
fun CanvasView(state: EditorState, cs: CanvasState, modifier: Modifier, padStart: Dp, padEnd: Dp, padTop: Dp) {
    val doc = state.doc
    val motion = LocalMotion.current
    val scene by produceState<Scene?>(null, doc) { value = withContext(Dispatchers.Default) { Renderer.scene(doc) } }
    val density = LocalDensity.current.density

    BoxWithConstraints(modifier.testTag("canvas")) {
        val sc = scene ?: return@BoxWithConstraints
        val boxW = maxWidth; val boxH = maxHeight
        val availW = (maxWidth - padStart - padEnd).value - 56f
        val availH = (maxHeight - padTop).value - 96f
        val fitZoom = min(availW / sc.width, availH / sc.height).coerceIn(0.08f, 2f)
        val z = if (cs.fit) fitZoom else cs.zoom
        LaunchedEffect(z, sc) { cs.effective = z; cs.sceneW = sc.width; cs.sceneH = sc.height }
        val renderScale = ((z * density).coerceIn(0.5f, 3f) * 4f).roundToInt() / 4f
        val rendered by produceState<Rendered?>(null, sc, renderScale) {
            value = withContext(Dispatchers.Default) { Rendered(doc.layout, Renderer.image(sc, renderScale).toComposeImageBitmap()) }
        }
        // Gentle fade/scale when the model type changes.
        val appear = remember { Animatable(1f) }
        val shownLayout = rendered?.layout
        LaunchedEffect(shownLayout) {
            if (shownLayout != null && motion.enabled) {
                appear.snapTo(0f)
                appear.animateTo(1f, motion.tween(Tok.Motion.slow))
            }
        }
        val vs = rememberScrollState()
        val hs = rememberScrollState()
        Box(Modifier.fillMaxSize().verticalScroll(vs).horizontalScroll(hs)) {
            Box(
                Modifier.defaultMinSize(minWidth = boxW, minHeight = boxH)
                    .padding(start = padStart + 28.dp, end = padEnd + 28.dp, top = padTop + 20.dp, bottom = 84.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size((sc.width * z).dp, (sc.height * z).dp)
                        .graphicsLayer {
                            alpha = 0.35f + 0.65f * appear.value
                            val k = 0.975f + 0.025f * appear.value
                            scaleX = k; scaleY = k
                        }
                        .shadow(22.dp, RoundedCornerShape(10.dp), ambientColor = Tok.C.glassShadow, spotColor = Tok.C.glassShadow)
                        .background(if (doc.style.transparentBackground) Color(0xFFF7F8FA) else Color.White, RoundedCornerShape(10.dp)),
                ) {
                    rendered?.let {
                        Image(it.bitmap, "Model preview", Modifier.fillMaxSize().testTag("preview-image"), contentScale = ContentScale.FillBounds)
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomBar(cs: CanvasState, modifier: Modifier) {
    GlassPanel(modifier.height(44.dp), RoundedCornerShape(Tok.R.lg)) {
        Row(Modifier.padding(horizontal = 6.dp).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.Outlined.ZoomOut, "Zoom out", { cs.zoomBy(1 / 1.2f) }, size = 32.dp)
            Text(
                "${(cs.effective * 100).roundToInt()} %", style = Tok.T.label, textAlign = TextAlign.Center,
                modifier = Modifier.width(54.dp),
            )
            IconBtn(Icons.Outlined.ZoomIn, "Zoom in", { cs.zoomBy(1.2f) }, size = 32.dp)
            IconBtn(Icons.Outlined.FitScreen, "Fit to window", { cs.fit = true }, active = cs.fit, size = 32.dp)
            VerticalDivider(Modifier.height(18.dp).padding(horizontal = 6.dp), color = Tok.C.line)
            Text("${cs.sceneW.roundToInt()} × ${cs.sceneH.roundToInt()}", style = Tok.T.small, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}
