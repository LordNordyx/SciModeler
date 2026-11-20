package scimodeler.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import scimodeler.ui.theme.Tok

/** The blur source (the canvas) shared by all glass surfaces. Null = draw a solid fallback. */
val LocalHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * Frosted-glass surface: blurs whatever is behind it (via Haze), tints it white, and adds a soft
 * shadow and hairline highlight. Content on top stays fully crisp.
 */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(Tok.R.panel),
    tint: Color = Tok.C.glass,
    elevation: Dp = 18.dp,
): Modifier {
    val haze = LocalHaze.current
    val base = this
        .shadow(elevation, shape, clip = false, ambientColor = Tok.C.glassShadow, spotColor = Tok.C.glassShadow)
        .clip(shape)
    val fill = if (haze != null) {
        base.hazeEffect(state = haze) {
            backgroundColor = Tok.C.canvasTop
            tints = listOf(HazeTint(tint))
            blurRadius = 30.dp
            noiseFactor = 0.05f
        }
    } else base.background(tint.copy(alpha = maxOf(tint.alpha, 0.94f)))
    return fill.border(1.dp, Tok.C.glassBorder, shape)
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Tok.R.panel),
    tint: Color = Tok.C.glass,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.glass(shape, tint), content = content)
}
