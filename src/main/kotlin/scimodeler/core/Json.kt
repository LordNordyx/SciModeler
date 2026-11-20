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
                "slot" to it.slot, "id" to it.id, "parent" to it.parent,
            )
        },
        "style" to with(d.style) {
            mapOf(
                "theme" to theme, "fontFamily" to fontFamily, "textScale" to textScale,
                "showTitle" to showTitle, "showCallouts" to showCallouts, "showLegend" to showLegend, "showIcons" to showIcons, "showLabels" to showLabels,
                "shadows" to shadows, "gradients" to gradients, "rotation" to rotation,
                "transparentBackground" to transparentBackground,
            )
        },
        "freeform" to d.freeform?.let { FreeformIO.toMap(it) },
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
                slot = (m["slot"] as? Number)?.toInt(),
                id = (m["id"] as? Number)?.toLong() ?: Ids.next(),
                parent = (m["parent"] as? Number)?.toLong(),
            )
        }
        return ModelDoc(
            title = str(root, "title"), subtitle = str(root, "subtitle"),
            center = str(root, "center"), legend = str(root, "legend"),
            layout = str(root, "layout").lowercase().takeIf { Models.exists(it) } ?: Models.all.first().id,
            groupNames = (root["groupNames"] as? List<Any?>)?.map { it?.toString() ?: "" } ?: emptyList(),
            freeform = (root["freeform"] as? Map<String, Any?>)?.let { FreeformIO.fromMap(it) },
            items = items,
            style = Style(
                theme = st["theme"] as? String ?: def.theme,
                fontFamily = st["fontFamily"] as? String ?: def.fontFamily,
                textScale = num("textScale", def.textScale),
                showTitle = bool("showTitle", def.showTitle),
                showCallouts = bool("showCallouts", def.showCallouts),
                showLegend = bool("showLegend", def.showLegend),
                showIcons = bool("showIcons", def.showIcons),
                showLabels = bool("showLabels", def.showLabels),
                shadows = bool("shadows", def.shadows),
                gradients = bool("gradients", def.gradients),
                rotation = num("rotation", def.rotation),
                transparentBackground = bool("transparentBackground", def.transparentBackground),
            ),
        )
    }
}


object FreeformIO {
    fun toMap(f: Freeform): Map<String, Any?> = mapOf(
        "width" to f.width, "height" to f.height,
        "elements" to f.elements.map { e ->
            mapOf(
                "id" to e.id, "kind" to e.kind.name, "x" to e.x, "y" to e.y, "w" to e.w, "h" to e.h,
                "rotation" to e.rotation, "text" to e.text, "textSize" to e.textSize, "bold" to e.bold,
                "align" to e.align.name, "slot" to e.slot, "color" to e.color?.let(::toHex),
                "material" to e.material.name, "corner" to e.corner, "opacity" to e.opacity,
                "icon" to e.icon, "image" to e.image, "bullets" to e.bullets,
                "from" to e.from, "to" to e.to, "x2" to e.x2, "y2" to e.y2, "route" to e.route.name,
                "headStart" to e.headStart, "headEnd" to e.headEnd, "dashed" to e.dashed, "strokeWidth" to e.strokeWidth,
                "group" to e.group, "locked" to e.locked, "hidden" to e.hidden, "name" to e.name,
            )
        },
    )

    @Suppress("UNCHECKED_CAST")
    fun fromMap(m: Map<String, Any?>): Freeform {
        fun f(e: Map<String, Any?>, k: String, d: Float) = (e[k] as? Number)?.toFloat() ?: d
        fun b(e: Map<String, Any?>, k: String, d: Boolean) = e[k] as? Boolean ?: d
        val els = (m["elements"] as? List<Any?>).orEmpty().mapNotNull { raw ->
            val e = raw as? Map<String, Any?> ?: return@mapNotNull null
            val kind = runCatching { FfKind.valueOf(e["kind"] as String) }.getOrNull() ?: return@mapNotNull null
            FfElement(
                id = (e["id"] as? Number)?.toLong() ?: Ids.next(), kind = kind,
                x = f(e, "x", 0f), y = f(e, "y", 0f), w = f(e, "w", 100f), h = f(e, "h", 60f),
                rotation = f(e, "rotation", 0f), text = e["text"] as? String ?: "", textSize = f(e, "textSize", 18f),
                bold = b(e, "bold", true), align = runCatching { Align.valueOf(e["align"] as String) }.getOrDefault(Align.CENTER),
                slot = (e["slot"] as? Number)?.toInt() ?: 0, color = (e["color"] as? String)?.let { runCatching { hex(it) }.getOrNull() },
                material = runCatching { Material.valueOf(e["material"] as String) }.getOrDefault(Material.THEME),
                corner = f(e, "corner", 16f), opacity = f(e, "opacity", 1f),
                icon = e["icon"] as? String, image = (e["image"] as? String)?.takeIf { it.isNotBlank() },
                bullets = (e["bullets"] as? List<Any?>)?.map { it?.toString() ?: "" } ?: emptyList(),
                from = (e["from"] as? Number)?.toLong(), to = (e["to"] as? Number)?.toLong(),
                x2 = f(e, "x2", 0f), y2 = f(e, "y2", 0f),
                route = runCatching { Route.valueOf(e["route"] as String) }.getOrDefault(Route.STRAIGHT),
                headStart = b(e, "headStart", false), headEnd = b(e, "headEnd", true), dashed = b(e, "dashed", false),
                strokeWidth = f(e, "strokeWidth", 4f), group = (e["group"] as? Number)?.toLong(),
                locked = b(e, "locked", false), hidden = b(e, "hidden", false), name = e["name"] as? String ?: "",
            )
        }
        return Freeform((m["width"] as? Number)?.toFloat() ?: 1200f, (m["height"] as? Number)?.toFloat() ?: 760f, els)
    }
}
