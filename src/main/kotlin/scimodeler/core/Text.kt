package scimodeler.core

import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.font.FontRenderContext

interface Measure {
    fun width(s: String, size: Float, bold: Boolean): Float
}

object Fonts {
    /** Registers the bundled Inter font with Java2D so diagrams look the same on every computer. */
    private val bundled: Unit by lazy {
        listOf("Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf").forEach { name ->
            runCatching {
                Fonts::class.java.getResourceAsStream("/fonts/$name")?.use {
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Font.createFont(Font.TRUETYPE_FONT, it))
                }
            }
        }
    }

    val available: List<String> by lazy {
        bundled
        runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toList() }
            .getOrDefault(emptyList())
    }

    val default: String by lazy {
        val preferred = listOf("Inter", "Segoe UI", "Helvetica Neue", "Helvetica", "Arial", "DejaVu Sans")
        preferred.firstOrNull { it in available } ?: "SansSerif"
    }
}

class AwtMeasure(family: String) : Measure {
    private val frc = FontRenderContext(null, true, true)
    private val regular = Font(family, Font.PLAIN, 100)
    private val bold = Font(family, Font.BOLD, 100)
    private val cache = HashMap<String, Float>()

    fun font(size: Float, isBold: Boolean): Font = (if (isBold) bold else regular).deriveFont(size)

    override fun width(s: String, size: Float, bold: Boolean): Float {
        val key = (if (bold) "b|" else "r|") + s
        val w100 = cache.getOrPut(key) {
            (if (bold) this.bold else regular).getStringBounds(s, frc).width.toFloat()
        }
        return w100 * size / 100f
    }

    companion object {
        private val instances = HashMap<String, AwtMeasure>()
        @Synchronized
        fun of(family: String): AwtMeasure = instances.getOrPut(family) { AwtMeasure(family) }
    }
}

/** Word-wrap [text] to [maxW]. Explicit newlines are kept. */
fun wrap(m: Measure, text: String, maxW: Float, size: Float, bold: Boolean): List<String> {
    val out = ArrayList<String>()
    for (para in text.split('\n')) {
        val words = para.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) { if (text.contains('\n')) out += ""; continue }
        var line = ""
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (m.width(candidate, size, bold) <= maxW) {
                line = candidate
            } else {
                if (line.isNotEmpty()) out += line
                // Break words that are longer than a whole line.
                var w = word
                while (m.width(w, size, bold) > maxW && w.length > 1) {
                    var cut = w.length - 1
                    while (cut > 1 && m.width(w.substring(0, cut) + "-", size, bold) > maxW) cut--
                    out += w.substring(0, cut) + "-"
                    w = w.substring(cut)
                }
                line = w
            }
        }
        if (line.isNotEmpty()) out += line
    }
    while (out.isNotEmpty() && out.last().isEmpty()) out.removeAt(out.lastIndex)
    return out
}

data class Fitted(val lines: List<String>, val size: Float)

/** Largest font size (between [min] and [start]) at which [text] fits the box. */
fun fit(
    m: Measure, text: String, maxW: Float, maxH: Float,
    start: Float, min: Float, bold: Boolean, lineHeight: Float = 1.22f,
): Fitted {
    var size = start
    while (true) {
        val lines = wrap(m, text, maxW, size, bold)
        val fitsH = lines.size * size * lineHeight <= maxH
        val fitsW = lines.all { m.width(it, size, bold) <= maxW + 0.5f }
        val unbroken = lines.none { it.endsWith("-") } || size <= min
        if ((fitsH && fitsW && unbroken) || size <= min) return Fitted(lines, size)
        size -= 0.5f
    }
}
