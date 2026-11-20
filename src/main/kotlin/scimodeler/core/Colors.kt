package scimodeler.core

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * A complete visual preset: palette plus the styling that goes with it. Applying a theme
 * ([applyTheme]) recolours every item and sets these style values together.
 */
data class Theme(
    val id: String,
    val name: String,
    val colors: List<Int>,
    val hub: Int,
    val gradients: Boolean = true,
    val shadows: Boolean = true,
    /** How light the tinted description cards are (0 = item colour, 1 = white). */
    val cardTint: Float = 0.87f,
    val background: Int = 0xFFFFFFFF.toInt(),
    /** Neutral panels: legends, side notes, outer rings. */
    val panel: Int = hex("#F1F4F8"),
    val ring: Int = hex("#EBEEF3"),
    val connector: Int = hex("#D3D9E2"),
) {
    fun slot(i: Int): Int = colors[((i % colors.size) + colors.size) % colors.size]
}

fun hex(s: String): Int {
    val v = s.removePrefix("#").toLong(16)
    return if (s.removePrefix("#").length <= 6) (0xFF000000 or v).toInt() else v.toInt()
}

private fun pal(vararg c: String) = c.map(::hex)

object Themes {
    val all: List<Theme> = listOf(
        Theme("vivid", "Vivid", pal("#1E9BD7", "#E8343B", "#F59E0B", "#22A06B", "#8E3FB0", "#1D4F9C", "#E85D9A", "#14B8A6"), hex("#1F2A44")),
        Theme("soft", "Soft", pal("#6C8EF5", "#F47C7C", "#F2B261", "#4FBF9A", "#A98AEF", "#4FA8DA", "#EE8FB8", "#86C97F"), hex("#3B4A6B"),
            cardTint = 0.9f),
        Theme("flat", "Flat", pal("#2563EB", "#DC2626", "#F59E0B", "#16A34A", "#9333EA", "#0891B2", "#DB2777", "#65A30D"), hex("#111827"),
            gradients = false, shadows = false, cardTint = 0.9f, panel = hex("#F3F4F6"), ring = hex("#E5E7EB")),
        Theme("earth", "Earth", pal("#264653", "#2A9D8F", "#E9C46A", "#F4A261", "#E76F51", "#6D597A", "#355070", "#B56576"), hex("#22333B"),
            cardTint = 0.86f, panel = hex("#F4F1EA"), ring = hex("#ECE6DA"), connector = hex("#D9D0BF")),
        Theme("paper", "Paper", pal("#3D5A80", "#C0603F", "#D9A441", "#5B8E7D", "#8B6F9E", "#4F7CAC", "#B5838D", "#7A9E5B"), hex("#2F3E46"),
            gradients = false, shadows = true, cardTint = 0.88f, background = hex("#FFFCF5"),
            panel = hex("#F5EFE3"), ring = hex("#EFE7D8"), connector = hex("#D8CCB6")),
        Theme("ocean", "Ocean", pal("#023E8A", "#0077B6", "#0096C7", "#00B4D8", "#2A6F97", "#014F86", "#48CAE4", "#468FAF"), hex("#03045E"),
            panel = hex("#EEF5FA"), ring = hex("#E3EEF6"), connector = hex("#C6DAE8")),
        Theme("sunset", "Sunset", pal("#F94144", "#F3722C", "#F8961E", "#F9C74F", "#90BE6D", "#43AA8B", "#577590", "#9B5DE5"), hex("#2B2D42")),
        Theme("forest", "Forest", pal("#1B4332", "#2D6A4F", "#40916C", "#52B788", "#74C69D", "#95D5B2", "#386641", "#6A994E"), hex("#081C15"),
            panel = hex("#EFF5F0"), ring = hex("#E4EEE6"), connector = hex("#C8DBCC")),
        Theme("academic", "Academic", pal("#1F3A5F", "#3E5C76", "#748CAB", "#9A8C98", "#4A4E69", "#22577A", "#38A3A5", "#57CC99"), hex("#0D1B2A"),
            gradients = false, cardTint = 0.9f),
        Theme("mono", "Print (greyscale)", pal("#262626", "#4D4D4D", "#737373", "#999999", "#3A3A3A", "#606060", "#868686", "#ADADAD"), hex("#111111"),
            gradients = false, shadows = false, cardTint = 0.9f, panel = hex("#F2F2F2"), ring = hex("#E6E6E6"), connector = hex("#BDBDBD")),
    )

    fun get(id: String): Theme = all.firstOrNull { it.id == id } ?: all.first()
}

/** Applies a theme as a complete preset: palette, effects, and every item's colour. */
fun ModelDoc.applyTheme(id: String): ModelDoc {
    val t = Themes.get(id)
    return copy(
        style = style.copy(theme = t.id, gradients = t.gradients, shadows = t.shadows),
        items = items.map { it.copy(color = null, slot = null) },
        freeform = freeform?.let { ff -> ff.copy(elements = ff.elements.map { it.copy(color = null) }) },
    )
}

/**
 * The colour an item is actually drawn with. Every colour swatch in the interface uses this,
 * so swatches always match the model.
 */
fun ModelDoc.colorOf(index: Int): Int {
    val theme = Themes.get(style.theme)
    val item = items.getOrNull(index) ?: return theme.slot(index)
    item.color?.let { return it }
    item.slot?.let { return theme.slot(it) }
    return spec.autoColor?.invoke(this, theme, index) ?: theme.slot(index)
}

object Ink {
    val text = hex("#1F2937")
    val subtle = hex("#6B7280")
    val panel = hex("#F1F4F8")
    val ring = hex("#EBEEF3")
    const val white = 0xFFFFFFFF.toInt()
}

fun a(c: Int) = (c ushr 24) and 0xFF
fun r(c: Int) = (c shr 16) and 0xFF
fun g(c: Int) = (c shr 8) and 0xFF
fun b(c: Int) = c and 0xFF
fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

fun mix(c1: Int, c2: Int, t: Float): Int {
    fun l(x: Int, y: Int) = (x + (y - x) * t).roundToInt().coerceIn(0, 255)
    return argb(l(a(c1), a(c2)), l(r(c1), r(c2)), l(g(c1), g(c2)), l(b(c1), b(c2)))
}

fun lighten(c: Int, t: Float) = mix(c, Ink.white, t)
fun darken(c: Int, t: Float) = mix(c, 0xFF000000.toInt(), t)
fun withAlpha(c: Int, alpha: Float) = (c and 0x00FFFFFF) or ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24)

fun luminance(c: Int): Double {
    fun ch(v: Int): Double {
        val s = v / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * ch(r(c)) + 0.7152 * ch(g(c)) + 0.0722 * ch(b(c))
}

/** Readable text colour on top of [bg]. */
fun onColor(bg: Int): Int = if (luminance(bg) > 0.42) Ink.text else Ink.white

fun toHex(c: Int): String = "#%02X%02X%02X".format(r(c), g(c), b(c))
