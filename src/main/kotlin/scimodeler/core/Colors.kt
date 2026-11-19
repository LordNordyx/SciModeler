package scimodeler.core

import kotlin.math.pow
import kotlin.math.roundToInt

data class Theme(val id: String, val name: String, val colors: List<Int>, val hub: Int)

fun hex(s: String): Int {
    val v = s.removePrefix("#").toLong(16)
    return if (s.removePrefix("#").length <= 6) (0xFF000000 or v).toInt() else v.toInt()
}

object Themes {
    val all: List<Theme> = listOf(
        Theme("vivid", "Vivid", listOf(
            "#1E9BD7", "#E8343B", "#F59E0B", "#22A06B", "#8E3FB0", "#1D4F9C", "#E85D9A", "#14B8A6",
        ).map(::hex), hex("#1F2A44")),
        Theme("soft", "Soft", listOf(
            "#6C8EF5", "#F47C7C", "#F2B261", "#4FBF9A", "#A98AEF", "#4FA8DA", "#EE8FB8", "#86C97F",
        ).map(::hex), hex("#3B4A6B")),
        Theme("earth", "Earth", listOf(
            "#264653", "#2A9D8F", "#E9C46A", "#F4A261", "#E76F51", "#6D597A", "#355070", "#B56576",
        ).map(::hex), hex("#22333B")),
        Theme("ocean", "Ocean", listOf(
            "#023E8A", "#0077B6", "#0096C7", "#00B4D8", "#2A6F97", "#014F86", "#48CAE4", "#468FAF",
        ).map(::hex), hex("#03045E")),
        Theme("sunset", "Sunset", listOf(
            "#F94144", "#F3722C", "#F8961E", "#F9C74F", "#90BE6D", "#43AA8B", "#577590", "#9B5DE5",
        ).map(::hex), hex("#2B2D42")),
        Theme("forest", "Forest", listOf(
            "#1B4332", "#2D6A4F", "#40916C", "#52B788", "#74C69D", "#95D5B2", "#386641", "#6A994E",
        ).map(::hex), hex("#081C15")),
        Theme("academic", "Academic", listOf(
            "#1F3A5F", "#3E5C76", "#748CAB", "#9A8C98", "#4A4E69", "#22577A", "#38A3A5", "#57CC99",
        ).map(::hex), hex("#0D1B2A")),
        Theme("mono", "Print (greyscale)", listOf(
            "#262626", "#4D4D4D", "#737373", "#999999", "#3A3A3A", "#606060", "#868686", "#ADADAD",
        ).map(::hex), hex("#111111")),
    )

    fun get(id: String): Theme = all.firstOrNull { it.id == id } ?: all.first()
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
