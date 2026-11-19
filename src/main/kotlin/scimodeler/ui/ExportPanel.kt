package scimodeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import scimodeler.core.Renderer

@Composable
fun ExportPanel(state: EditorState, actions: Actions) {
    val spec = state.exportSpec
    val scene = remember(state.doc) { Renderer.scene(state.doc) }
    var widthText by remember { mutableStateOf(trimFloat(spec.widthCm)) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 6.dp)) {
        SectionLabel("Size in your document")
        Field(
            "Width (cm)", widthText,
            { v ->
                widthText = v.filter { it.isDigit() || it == '.' || it == ',' }
                widthText.replace(',', '.').toFloatOrNull()?.takeIf { it in 1f..100f }?.let {
                    state.exportSpec = spec.copy(widthCm = it)
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        val presets = listOf("Full page · 16" to 16f, "Half · 8" to 8f, "Landscape · 24" to 24f)
        Segmented(
            presets, presets.firstOrNull { it.second == spec.widthCm } ?: ("" to 0f), { it.first },
            { (_, cm) -> state.exportSpec = spec.copy(widthCm = cm); widthText = trimFloat(cm) },
        )

        SectionLabel("Resolution")
        Segmented(listOf(150, 300, 600), spec.dpi, { "$it dpi" }, { state.exportSpec = spec.copy(dpi = it) })
        Spacer(Modifier.height(6.dp))
        Text("300 dpi is the usual requirement for printed theses.", fontSize = 12.sp, color = Ui.muted)

        Spacer(Modifier.height(14.dp))
        val (pw, ph) = spec.pixels(scene)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ui.accentSoft).padding(14.dp),
        ) {
            Text("${trimFloat(spec.widthCm)} × ${trimFloat(spec.heightCm(scene))} cm", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("$pw × $ph px PNG", fontSize = 13.sp, color = Ui.muted)
        }

        SectionLabel("Export")
        ExportButton(Icons.Outlined.Image, "Export PNG", primary = true, enabled = !actions.busy) { actions.exportPng() }
        Spacer(Modifier.height(8.dp))
        ExportButton(Icons.Outlined.Code, "Export SVG (vector)", enabled = !actions.busy) { actions.exportSvg() }
        Spacer(Modifier.height(8.dp))
        ExportButton(Icons.Outlined.ContentCopy, "Copy image to clipboard", enabled = !actions.busy) { actions.copyImage() }

        SectionLabel("Tips")
        HelpNote("Word / Google Docs", "Insert the PNG, or the SVG for perfectly sharp printing (Word 2019+ / Microsoft 365).")
        HelpNote("LaTeX", "Use the PNG directly, or convert the SVG to PDF (Inkscape) for \\includegraphics.")
        HelpNote("Editing later", "Save the model (.scimodel) so you can change text or layout at any time.")
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HelpNote(title: String, body: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(body, fontSize = 12.sp, color = Ui.muted)
    }
}

@Composable
private fun ExportButton(icon: ImageVector, text: String, primary: Boolean = false, enabled: Boolean, onClick: () -> Unit) {
    val mod = Modifier.fillMaxWidth().height(44.dp)
    val content: @Composable () -> Unit = {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp)
    }
    if (primary) {
        Button(onClick = onClick, enabled = enabled, modifier = mod, shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ui.accent)) { content() }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = mod, shape = RoundedCornerShape(10.dp)) { content() }
    }
}

fun trimFloat(v: Float): String {
    val r = Math.round(v * 10f) / 10f
    return if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString()
}
