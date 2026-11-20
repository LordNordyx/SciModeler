package scimodeler.core

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Per-user application data folder (Windows: %APPDATA%, macOS: ~/Library/Application Support). */
object AppDirs {
    private val os = System.getProperty("os.name").orEmpty().lowercase()
    val isMac = os.contains("mac")
    val isWindows = os.contains("win")

    val root: File
        get() {
            System.getProperty("scimodeler.home")?.let { return File(it).also { d -> d.mkdirs() } }
            val home = System.getProperty("user.home")
            val dir = when {
                isWindows -> File(System.getenv("APPDATA") ?: "$home/AppData/Roaming", "SciModeler")
                isMac -> File(home, "Library/Application Support/SciModeler")
                else -> File(System.getenv("XDG_CONFIG_HOME") ?: "$home/.config", "SciModeler")
            }
            return dir.also { it.mkdirs() }
        }

    fun file(name: String) = File(root, name)
}

/** Writes via a temp file so a crash never leaves a half-written file. */
fun File.writeAtomically(text: String) {
    parentFile?.mkdirs()
    val tmp = File(parentFile, ".$name.tmp")
    tmp.writeText(text, Charsets.UTF_8)
    if (!tmp.renameTo(this)) { delete(); if (!tmp.renameTo(this)) { writeText(text, Charsets.UTF_8); tmp.delete() } }
}

enum class MotionPref(val label: String) { SYSTEM("Follow system"), REDUCED("Reduced"), FULL("Full") }

data class AppSettings(
    val motion: MotionPref = MotionPref.SYSTEM,
    val exportWidthCm: Float = 16f,
    val exportDpi: Int = 300,
)

object SettingsStore {
    private val file get() = AppDirs.file("settings.json")

    fun load(): AppSettings = runCatching {
        @Suppress("UNCHECKED_CAST")
        val m = Json.parse(file.readText()) as Map<String, Any?>
        AppSettings(
            motion = runCatching { MotionPref.valueOf(m["motion"] as String) }.getOrDefault(MotionPref.SYSTEM),
            exportWidthCm = (m["exportWidthCm"] as? Number)?.toFloat() ?: 16f,
            exportDpi = (m["exportDpi"] as? Number)?.toInt() ?: 300,
        )
    }.getOrDefault(AppSettings())

    fun save(s: AppSettings) = runCatching {
        file.writeAtomically(Json.write(mapOf("motion" to s.motion.name, "exportWidthCm" to s.exportWidthCm, "exportDpi" to s.exportDpi)))
    }
}

/** The document that was open when the app last ran; restored on start. */
data class Session(val doc: ModelDoc, val file: File?, val dirty: Boolean)

object SessionStore {
    val file get() = AppDirs.file("session.json")

    fun save(s: Session) = runCatching {
        file.writeAtomically(Json.write(mapOf(
            "session" to 1, "file" to s.file?.absolutePath, "dirty" to s.dirty, "doc" to DocIO.toMap(s.doc),
        )))
    }

    @Suppress("UNCHECKED_CAST")
    fun load(): Session? = runCatching {
        if (!file.isFile) return null
        val m = Json.parse(file.readText()) as Map<String, Any?>
        Session(DocIO.fromMap(m["doc"] as Map<String, Any?>), (m["file"] as? String)?.let(::File), m["dirty"] as? Boolean ?: false)
    }.getOrNull()
}

/** Custom icons the user has imported, kept so they can be reused in other models. */
object IconStore {
    private val dir get() = File(AppDirs.root, "icons").also { it.mkdirs() }

    /** Saves [base64] PNG to the library (deduplicated by content). */
    fun remember(base64: String) {
        runCatching {
            val name = sha1(base64).take(16) + ".b64"
            val f = File(dir, name)
            if (!f.exists()) f.writeAtomically(base64) else f.setLastModified(System.currentTimeMillis())
        }
    }

    fun recent(limit: Int = 18): List<String> = runCatching {
        dir.listFiles { f -> f.name.endsWith(".b64") }.orEmpty()
            .sortedByDescending { it.lastModified() }.take(limit).map { it.readText().trim() }
    }.getOrDefault(emptyList())

    fun forget(base64: String) { runCatching { File(dir, sha1(base64).take(16) + ".b64").delete() } }

    private fun sha1(s: String) = MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

/** Reads the operating system's "reduce motion" preference where it is available. */
object SystemMotion {
    val prefersReduced: Boolean by lazy {
        runCatching {
            when {
                AppDirs.isMac -> run("defaults", "read", "com.apple.universalaccess", "reduceMotion").trim() == "1"
                AppDirs.isWindows -> {
                    // "Animation effects" off in Settings → Accessibility → Visual effects clears this value.
                    val out = run("reg", "query", "HKCU\\Control Panel\\Desktop\\WindowMetrics", "/v", "MinAnimate")
                    Regex("MinAnimate\\s+REG_SZ\\s+(\\d)").find(out)?.groupValues?.get(1) == "0"
                }
                else -> false
            }
        }.getOrDefault(false)
    }

    private fun run(vararg cmd: String): String {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        if (!p.waitFor(1500, TimeUnit.MILLISECONDS)) { p.destroy(); return "" }
        return p.inputStream.bufferedReader().readText()
    }
}
