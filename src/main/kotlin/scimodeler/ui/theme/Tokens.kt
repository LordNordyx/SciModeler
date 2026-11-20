package scimodeler.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens. Every colour, type style, spacing step, radius and animation timing used by the
 * interface is defined here, so the look can be changed in one place.
 */
object Tok {
    object C {
        val accent = Color(0xFF4F46E5)
        val accentHover = Color(0xFF4338CA)
        val accentPressed = Color(0xFF3730A3)
        val accentSoft = Color(0xFFEEF0FF)
        val onAccent = Color.White

        val canvasTop = Color(0xFFEFF2F7)
        val canvasBottom = Color(0xFFE2E6EE)
        val canvasBrush = Brush.verticalGradient(listOf(canvasTop, canvasBottom))

        /** Floating glass panels: translucent white over a blurred canvas. */
        val glass = Color(0xB8FFFFFF)
        val glassStrong = Color(0xF2FFFFFF)
        val glassBorder = Color(0xCCFFFFFF)
        val glassShadow = Color(0x330F172A)

        val surface = Color(0xE6FFFFFF)
        val surfaceSolid = Color.White
        val surfaceSunken = Color(0x0A0B1220)
        val hover = Color(0x0D0B1220)
        val pressed = Color(0x170B1220)

        val text = Color(0xFF0B1220)
        val text2 = Color(0xFF475467)
        val text3 = Color(0xFF8A94A6)
        val line = Color(0x170B1220)
        val lineStrong = Color(0x290B1220)
        val focus = Color(0x804F46E5)
        val danger = Color(0xFFDC2626)
        val scrim = Color(0x470B1220)
        val tooltip = Color(0xF0101828)
    }

    val inter = FontFamily(
        Font("fonts/Inter-Regular.ttf", FontWeight.Normal),
        Font("fonts/Inter-Medium.ttf", FontWeight.Medium),
        Font("fonts/Inter-SemiBold.ttf", FontWeight.SemiBold),
        Font("fonts/Inter-Bold.ttf", FontWeight.Bold),
    )

    object T {
        val display = TextStyle(fontFamily = inter, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, color = C.text)
        val title = TextStyle(fontFamily = inter, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp, color = C.text)
        val body = TextStyle(fontFamily = inter, fontSize = 14.sp, lineHeight = 20.sp, color = C.text)
        val bodyStrong = body.copy(fontWeight = FontWeight.Medium)
        val label = TextStyle(fontFamily = inter, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = C.text)
        val small = TextStyle(fontFamily = inter, fontSize = 12.sp, lineHeight = 16.sp, color = C.text2)
        val caption = TextStyle(fontFamily = inter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp, color = C.text3)
        val tiny = TextStyle(fontFamily = inter, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = C.text2)
    }

    object S {
        val xxs = 2.dp; val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp
        val xl = 20.dp; val xxl = 24.dp; val xxxl = 32.dp
        /** Gap between floating panels and the window edge. */
        val gutter = 12.dp
    }

    object R {
        val sm = 8.dp; val md = 10.dp; val lg = 14.dp; val xl = 18.dp; val panel = 22.dp
    }

    object Layout {
        val topBar = 58.dp
        val gallery = 276.dp
        val inspector = 392.dp
    }

    object Motion {
        const val fast = 120
        const val base = 220
        const val slow = 360
        val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val standard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    }

    val colorScheme: ColorScheme = lightColorScheme(
        primary = C.accent, onPrimary = C.onAccent,
        primaryContainer = C.accentSoft, onPrimaryContainer = C.accent,
        background = C.canvasTop, surface = C.surfaceSolid, onSurface = C.text, onSurfaceVariant = C.text2,
        outline = C.lineStrong, outlineVariant = C.line, surfaceVariant = Color(0xFFF2F4F7),
    )

    val typography = Typography().let { t ->
        Typography(
            bodyLarge = t.bodyLarge.copy(fontFamily = inter), bodyMedium = t.bodyMedium.copy(fontFamily = inter),
            bodySmall = t.bodySmall.copy(fontFamily = inter), labelLarge = t.labelLarge.copy(fontFamily = inter),
            labelMedium = t.labelMedium.copy(fontFamily = inter), labelSmall = t.labelSmall.copy(fontFamily = inter),
            titleLarge = t.titleLarge.copy(fontFamily = inter), titleMedium = t.titleMedium.copy(fontFamily = inter),
            titleSmall = t.titleSmall.copy(fontFamily = inter), headlineSmall = t.headlineSmall.copy(fontFamily = inter),
        )
    }
}

/** Animation specs that collapse to instant changes when reduced motion is on. */
class MotionSpec(val enabled: Boolean) {
    fun <T> tween(ms: Int = Tok.Motion.base, easing: Easing = Tok.Motion.emphasized): FiniteAnimationSpec<T> =
        if (enabled) androidx.compose.animation.core.tween(ms, easing = easing) else snap()

    fun <T> spring(): FiniteAnimationSpec<T> = if (enabled) spring(dampingRatio = 0.62f, stiffness = 650f) else snap()

    fun popIn(): EnterTransition =
        if (enabled) fadeIn(tween(Tok.Motion.base)) + scaleIn(tween(Tok.Motion.base), initialScale = 0.94f) else EnterTransition.None

    fun popOut(): ExitTransition =
        if (enabled) fadeOut(tween(Tok.Motion.fast)) + scaleOut(tween(Tok.Motion.fast), targetScale = 0.97f) else ExitTransition.None

    fun fade(): EnterTransition = if (enabled) fadeIn(tween(Tok.Motion.base)) else EnterTransition.None
    fun fadeOutT(): ExitTransition = if (enabled) fadeOut(tween(Tok.Motion.fast)) else ExitTransition.None
}

val LocalMotion = staticCompositionLocalOf { MotionSpec(true) }
