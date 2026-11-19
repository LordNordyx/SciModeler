package scimodeler.core

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode
import kotlin.math.roundToInt

data class ExportSpec(val widthCm: Float = 16f, val dpi: Int = 300) {
    fun scaleFor(scene: Scene): Float = (widthCm / 2.54f * dpi) / scene.width
    fun pixels(scene: Scene): Pair<Int, Int> {
        val sc = scaleFor(scene)
        return (scene.width * sc).roundToInt() to (scene.height * sc).roundToInt()
    }
    fun heightCm(scene: Scene) = widthCm * scene.height / scene.width
}

object Exporter {
    fun png(scene: Scene, spec: ExportSpec, file: File) {
        val img = Renderer.image(scene, spec.scaleFor(scene))
        writePng(img, file, spec.dpi)
    }

    fun svg(scene: Scene, spec: ExportSpec, file: File) = file.writeText(Renderer.svg(scene, spec.widthCm), Charsets.UTF_8)

    /** Writes a PNG with pHYs metadata so Word/LibreOffice place it at the intended physical size. */
    fun writePng(img: BufferedImage, file: File, dpi: Int) {
        val writer = ImageIO.getImageWritersByFormatName("png").next()
        try {
            val param = writer.defaultWriteParam
            val meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), param)
            val ppm = (dpi / 0.0254).roundToInt().toString()
            val root = IIOMetadataNode("javax_imageio_png_1.0")
            val phys = IIOMetadataNode("pHYs")
            phys.setAttribute("pixelsPerUnitXAxis", ppm)
            phys.setAttribute("pixelsPerUnitYAxis", ppm)
            phys.setAttribute("unitSpecifier", "meter")
            root.appendChild(phys)
            meta.mergeTree("javax_imageio_png_1.0", root)
            ImageIO.createImageOutputStream(file).use { ios ->
                writer.output = ios
                writer.write(null, IIOImage(img, null, meta), param)
            }
        } finally {
            writer.dispose()
        }
    }

    /** Opaque copy for the clipboard (Windows apps show transparent pixels as black). */
    fun flatten(img: BufferedImage): BufferedImage {
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, img.width, img.height)
        g.drawImage(img, 0, 0, null)
        g.dispose()
        return out
    }
}
