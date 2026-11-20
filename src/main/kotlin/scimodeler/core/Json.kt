package scimodeler.core

/** Small dependency-free JSON reader/writer for .scimodel files. */
object Json {
    fun write(v: Any?): String = StringBuilder().also { w(it, v, 0) }.toString()

    private fun w(sb: StringBuilder, v: Any?, ind: Int) {
        when (v) {
            null -> sb.append("null")
            is String -> str(sb, v)
            is Boolean, is Int, is Long -> sb.append(v.toString())
            is Float -> sb.append(if (v.isFinite()) v.toString() else "0")
            is Double -> sb.append(if (v.isFinite()) v.toString() else "0")
            is Map<*, *> -> {
                sb.append("{")
                var first = true
                for ((k, value) in v) {
                    if (!first) sb.append(",")
                    first = false
                    sb.append('\n').append("  ".repeat(ind + 1))
                    str(sb, k.toString()); sb.append(": ")
                    w(sb, value, ind + 1)
                }
                if (v.isNotEmpty()) sb.append('\n').append("  ".repeat(ind))
                sb.append("}")
            }
            is List<*> -> {
                sb.append("[")
                v.forEachIndexed { i, e ->
                    if (i > 0) sb.append(",")
                    sb.append('\n').append("  ".repeat(ind + 1))
                    w(sb, e, ind + 1)
                }
                if (v.isNotEmpty()) sb.append('\n').append("  ".repeat(ind))
                sb.append("]")
            }
            else -> str(sb, v.toString())
        }
    }

    private fun str(sb: StringBuilder, s: String) {
        sb.append('"')
        for (ch in s) when (ch) {
            '"' -> sb.append("\\\""); '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n"); '\r' -> sb.append("\\r"); '\t' -> sb.append("\\t")
            else -> if (ch < ' ') sb.append("\\u%04x".format(ch.code)) else sb.append(ch)
        }
        sb.append('"')
    }

    fun parse(text: String): Any? = Parser(text).run { val v = value(); ws(); v }

    private class Parser(val s: String) {
        var i = 0
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun value(): Any? {
            ws()
            require(i < s.length) { "Unexpected end of file" }
            return when (val c = s[i]) {
                '{' -> obj(); '[' -> arr(); '"' -> string()
                't' -> { i += 4; true }; 'f' -> { i += 5; false }; 'n' -> { i += 4; null }
                else -> if (c == '-' || c.isDigit()) num() else error("Unexpected '$c' at $i")
            }
        }
        fun obj(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>(); i++; ws()
            if (s[i] == '}') { i++; return m }
            while (true) {
                ws(); val k = string(); ws(); require(s[i] == ':'); i++
                m[k] = value(); ws()
                if (s[i] == ',') { i++; continue }
                require(s[i] == '}'); i++; return m
            }
        }
        fun arr(): List<Any?> {
            val l = ArrayList<Any?>(); i++; ws()
            if (s[i] == ']') { i++; return l }
            while (true) {
                l += value(); ws()
                if (s[i] == ',') { i++; continue }
                require(s[i] == ']'); i++; return l
            }
        }
        fun string(): String {
            require(s[i] == '"'); i++
            val sb = StringBuilder()
            while (s[i] != '"') {
                val c = s[i++]
                if (c == '\\') {
                    when (val e = s[i++]) {
                        'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                        'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                        'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> sb.append(e)
                    }
                } else sb.append(c)
            }
            i++
            return sb.toString()
        }
        fun num(): Number {
            val st = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            val t = s.substring(st, i)
            return if (t.any { it in ".eE" }) t.toDouble() else t.toLong()
        }
    }
}

object DocIO {
    const val EXT = "scimodel"

    fun toJson(d: ModelDoc): String = Json.write(toMap(d))

    fun toMap(d: ModelDoc): Map<String, Any?> = mapOf(
        "format" to "scimodeler", "version" to 1,
        "layout" to d.layout, "title" to d.title, "subtitle" to d.subtitle, "groupNames" to d.groupNames,
        "center" to d.center, "legend" to d.legend,
        "items" to d.items.map {
            mapOf(
                "label" to it.label, "bullets" to it.bullets, "color" to it.color?.let(::toHex),
                "icon" to it.icon, "image" to it.image, "role" to it.role.name,
            )
        },
        "style" to with(d.style) {
            mapOf(
                "theme" to theme, "fontFamily" to fontFamily, "textScale" to textScale,
                "showTitle" to showTitle, "showCallouts" to showCallouts, "showLegend" to showLegend, "showIcons" to showIcons,
                "shadows" to shadows, "gradients" to gradients, "rotation" to rotation,
                "transparentBackground" to transparentBackground,
            )
        },
    )

    @Suppress("UNCHECKED_CAST")
    fun fromJson(text: String): ModelDoc =
        fromMap(Json.parse(text) as? Map<String, Any?> ?: error("Not a SciModeler file"))

    @Suppress("UNCHECKED_CAST")
    fun fromMap(root: Map<String, Any?>): ModelDoc {
        fun str(m: Map<String, Any?>, k: String) = m[k] as? String ?: ""
        val st = root["style"] as? Map<String, Any?> ?: emptyMap()
        val def = Style()
        fun bool(k: String, d: Boolean) = st[k] as? Boolean ?: d
        fun num(k: String, d: Float) = (st[k] as? Number)?.toFloat() ?: d
        val items = (root["items"] as? List<Any?> ?: emptyList()).mapNotNull { e ->
            val m = e as? Map<String, Any?> ?: return@mapNotNull null
            Item(
                label = str(m, "label"),
                bullets = (m["bullets"] as? List<Any?>)?.map { it?.toString() ?: "" } ?: emptyList(),
                color = (m["color"] as? String)?.let { runCatching { hex(it) }.getOrNull() },
                icon = (m["icon"] as? String)?.takeIf { IconLibrary.get(it) != null },
                image = (m["image"] as? String)?.takeIf { it.isNotBlank() },
                role = runCatching { Role.valueOf(m["role"] as? String ?: "AUTO") }.getOrDefault(Role.AUTO),
            )
        }
        return ModelDoc(
            title = str(root, "title"), subtitle = str(root, "subtitle"),
            center = str(root, "center"), legend = str(root, "legend"),
            layout = str(root, "layout").lowercase().takeIf { Models.exists(it) } ?: Models.all.first().id,
            groupNames = (root["groupNames"] as? List<Any?>)?.map { it?.toString() ?: "" } ?: emptyList(),
            items = items,
            style = Style(
                theme = st["theme"] as? String ?: def.theme,
                fontFamily = st["fontFamily"] as? String ?: def.fontFamily,
                textScale = num("textScale", def.textScale),
                showTitle = bool("showTitle", def.showTitle),
                showCallouts = bool("showCallouts", def.showCallouts),
                showLegend = bool("showLegend", def.showLegend),
                showIcons = bool("showIcons", def.showIcons),
                shadows = bool("shadows", def.shadows),
                gradients = bool("gradients", def.gradients),
                rotation = num("rotation", def.rotation),
                transparentBackground = bool("transparentBackground", def.transparentBackground),
            ),
        )
    }
}
