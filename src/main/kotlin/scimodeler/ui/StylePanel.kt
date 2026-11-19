package scimodeler.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import scimodeler.core.Fonts
import scimodeler.core.LayoutType
import scimodeler.core.Themes
import kotlin.math.roundToInt

private val suggestedFonts = listOf(
    "Inter", "Segoe UI", "Calibri", "Arial", "Helvetica Neue", "Helvetica", "Roboto", "Open Sans",
    "Lato", "Source Sans 3", "Source Sans Pro", "Montserrat", "Poppins", "Aptos",
    "Times New Roman", "Georgia", "Cambria", "Garamond",
)

private val rotatable = setOf(LayoutType.WHEEL, LayoutType.CYCLE, LayoutType.HUB, LayoutType.ONION, LayoutType.VENN)

@Composable
fun StylePanel(state: EditorState) {
    val style = state.doc.style
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 6.dp)) {
        SectionLabel("Colour theme")
        Themes.all.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                row.forEach { t ->
                    val sel = t.id == style.theme
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sel) Ui.accentSoft else Ui.surface,
                        modifier = Modifier.weight(1f)
                            .border(if (sel) 2.dp else 1.dp, if (sel) Ui.accent else Ui.line, RoundedCornerShape(12.dp))
                            .clickable { state.updateStyle { it.copy(theme = t.id) } },
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                t.colors.take(6).forEach { Swatch(argbColor(it), 16) }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(t.name, fontSize = 12.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        SectionLabel("Typography")
        var fontMenu by remember { mutableStateOf(false) }
        val installed = style.fontFamily in Fonts.available || Fonts.available.isEmpty()
        Box {
            Field(
                "Font", style.fontFamily, { v -> state.updateStyle("font") { it.copy(fontFamily = v) } },
                hint = if (!installed) "Not installed on this computer – a fallback is used" else null,
                trailing = {
                    IconButton(onClick = { fontMenu = true }) { Icon(Icons.Outlined.ExpandMore, "Choose font") }
                },
            )
            DropdownMenu(expanded = fontMenu, onDismissRequest = { fontMenu = false }) {
                suggestedFonts.filter { it in Fonts.available }.forEach { f ->
                    DropdownMenuItem(text = { Text(f) }, onClick = {
                        state.updateStyle { it.copy(fontFamily = f) }; fontMenu = false
                    })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LabeledSlider("Text size", "${(style.textScale * 100).roundToInt()} %", style.textScale, 0.7f..1.5f, 0) { v ->
            state.updateStyle("scale") { it.copy(textScale = (v * 20f).roundToInt() / 20f) }
        }

        if (state.doc.layout in rotatable) {
            LabeledSlider("Rotation", "${style.rotation.roundToInt()}°", style.rotation, -180f..180f, 0) { v ->
                state.updateStyle("rotation") { it.copy(rotation = (v / 5f).roundToInt() * 5f) }
            }
        }

        SectionLabel("Show")
        ToggleRow("Title & subtitle", style.showTitle, { v -> state.updateStyle { it.copy(showTitle = v) } })
        ToggleRow("Description boxes", style.showCallouts, { v -> state.updateStyle { it.copy(showCallouts = v) } },
            hint = "The bullet points of each item")
        ToggleRow("Footnote", style.showLegend, { v -> state.updateStyle { it.copy(showLegend = v) } })

        SectionLabel("Effects")
        ToggleRow("Soft shadows", style.shadows, { v -> state.updateStyle { it.copy(shadows = v) } })
        ToggleRow("Gradients", style.gradients, { v -> state.updateStyle { it.copy(gradients = v) } })
        ToggleRow("Transparent background", style.transparentBackground,
            { v -> state.updateStyle { it.copy(transparentBackground = v) } }, hint = "PNG and SVG without white fill")
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LabeledSlider(
    label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(valueText, fontSize = 13.sp, color = Ui.muted)
            Spacer(Modifier.width(4.dp))
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(thumbColor = Ui.accent, activeTrackColor = Ui.accent),
        )
    }
}
