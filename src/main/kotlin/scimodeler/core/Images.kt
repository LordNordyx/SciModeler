package scimodeler.core

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

/** Decoding and importing of custom icon images. Icons are stored as base64 PNG, at most [MAX_SIDE] px. */
object Images {
    const val MAX_SIDE = 256
    val extensions = setOf("png", "jpg", "jpeg", "gif", "bmp")

    private val cache = object : LinkedHashMap<String, BufferedImage>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BufferedImage>?) = size > 64
    }

    @Synchronized
    fun decode(base64: String): BufferedImage? = cache[base64] ?: runCatching {
        ImageIO.read(ByteArrayInputStream(Base64.getDecoder().decode(base64)))
    }.getOrNull()?.also { cache[base64] = it }

    /** Pixel size of an encoded image (1×1 if it can't be decoded). */
    fun size(base64: String): Pair<Float, Float> = decode(base64)?.let { it.width.toFloat() to it.height.toFloat() } ?: (1f to 1f)

    /** Size of the largest box with the image's aspect ratio that fits in [side]×[side]. */
    fun fit(base64: String, side: Float): Pair<Float, Float> {
        val img = decode(base64) ?: return side to side
        val sc = side / max(img.width, img.height)
        return img.width * sc to img.height * sc
    }

    /** Reads an image file, scales it down (keeping proportions) and returns base64 PNG. */
    fun import(file: File): String {
        require(file.isFile) { "File not found" }
        require(file.length() < 25L * 1024 * 1024) { "Image is larger than 25 MB" }
        val src = ImageIO.read(file) ?: error("Unsupported image format (use PNG, JPG, GIF or BMP)")
        return encode(scaleDown(src, MAX_SIDE))
    }

    fun scaleDown(src: BufferedImage, maxSide: Int): BufferedImage {
        val sc = minOf(1.0, maxSide.toDouble() / max(src.width, src.height))
        val w = (src.width * sc).roundToInt().coerceAtLeast(1)
        val h = (src.height * sc).roundToInt().coerceAtLeast(1)
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(src, 0, 0, w, h, null)
        g.dispose()
        return out
    }

    fun encode(img: BufferedImage): String {
        val bos = ByteArrayOutputStream()
        ImageIO.write(img, "png", bos)
        return Base64.getEncoder().encodeToString(bos.toByteArray())
    }
}
