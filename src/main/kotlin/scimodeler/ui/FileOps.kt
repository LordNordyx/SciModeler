package scimodeler.ui

import scimodeler.core.DocIO
import scimodeler.core.Exporter
import scimodeler.core.ModelDoc
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.File

object FileOps {
    fun chooseOpen(owner: Frame): File? {
        val d = FileDialog(owner, "Open model", FileDialog.LOAD)
        d.setFilenameFilter { _, name -> name.endsWith(".${DocIO.EXT}") }
        d.isVisible = true
        val name = d.file ?: return null
        return File(d.directory, name)
    }

    fun chooseSave(owner: Frame, title: String, suggested: String, ext: String): File? {
        val d = FileDialog(owner, title, FileDialog.SAVE)
        d.file = "$suggested.$ext"
        d.isVisible = true
        val name = d.file ?: return null
        val f = File(d.directory, name)
        return if (f.extension.equals(ext, ignoreCase = true)) f else File(f.parentFile, "${f.name}.$ext")
    }

    fun read(file: File): ModelDoc = DocIO.fromJson(file.readText(Charsets.UTF_8))

    fun write(file: File, doc: ModelDoc) {
        val tmp = File(file.parentFile, ".${file.name}.tmp")
        tmp.writeText(DocIO.toJson(doc), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.writeText(DocIO.toJson(doc), Charsets.UTF_8)
            tmp.delete()
        }
    }

    fun slug(s: String): String =
        s.trim().replace(Regex("[\\\\/:*?\"<>|]"), "").replace(Regex("\\s+"), "-").take(60).ifBlank { "model" }

    fun copyImage(img: BufferedImage) {
        val flat = Exporter.flatten(img)
        val t = object : Transferable {
            override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
            override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.imageFlavor
            override fun getTransferData(flavor: DataFlavor): Any {
                if (flavor != DataFlavor.imageFlavor) throw UnsupportedFlavorException(flavor)
                return flat as Image
            }
        }
        Toolkit.getDefaultToolkit().systemClipboard.setContents(t, null)
    }

    val isMac: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("mac")
}
