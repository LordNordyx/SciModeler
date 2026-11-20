package scimodeler.ui

import scimodeler.core.AppDirs
import scimodeler.core.DocIO
import scimodeler.core.Exporter
import scimodeler.core.Images
import scimodeler.core.ModelDoc
import scimodeler.core.writeAtomically
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.File

/** Native file choosers. An interface so tests can supply files without a dialog. */
interface Dialogs {
    fun openModel(): File?
    fun saveFile(title: String, suggested: String, ext: String): File?
    fun pickImage(): File?
}

/** AWT FileDialog: the native chooser on both Windows and macOS. */
class AwtDialogs(private val owner: Frame) : Dialogs {
    override fun openModel(): File? = choose("Open model", FileDialog.LOAD) { it.endsWith(".${DocIO.EXT}", true) }

    override fun pickImage(): File? = choose("Choose an icon image", FileDialog.LOAD) { name ->
        Images.extensions.any { name.endsWith(".$it", ignoreCase = true) }
    }

    override fun saveFile(title: String, suggested: String, ext: String): File? {
        val d = FileDialog(owner, title, FileDialog.SAVE)
        d.file = "$suggested.$ext"
        d.isVisible = true
        val name = d.file ?: return null
        val f = File(d.directory, name)
        return if (f.extension.equals(ext, ignoreCase = true)) f else File(f.parentFile, "${f.name}.$ext")
    }

    private fun choose(title: String, mode: Int, accept: (String) -> Boolean): File? {
        val d = FileDialog(owner, title, mode)
        d.setFilenameFilter { _, name -> accept(name) }
        if (AppDirs.isWindows) d.file = "*.*"
        d.isVisible = true
        val name = d.file ?: return null
        return File(d.directory, name).takeIf { it.isFile }
    }
}

object FileOps {
    fun read(file: File): ModelDoc = DocIO.fromJson(file.readText(Charsets.UTF_8))

    fun write(file: File, doc: ModelDoc) = file.writeAtomically(DocIO.toJson(doc))

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

    val isMac: Boolean get() = AppDirs.isMac
}
