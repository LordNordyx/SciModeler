package scimodeler.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Ui {
    val accent = Color(0xFF4F46E5)
    val accentSoft = Color(0xFFEEF0FF)
    val bg = Color(0xFFF3F4F7)
    val canvas = Color(0xFFE9EBF0)
    val surface = Color.White
    val line = Color(0xFFE4E7EC)
    val text = Color(0xFF101828)
    val muted = Color(0xFF667085)

    val colors: ColorScheme = lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accentSoft,
        onPrimaryContainer = accent,
        background = bg,
        surface = surface,
        onSurface = text,
        onSurfaceVariant = muted,
        outline = Color(0xFFD0D5DD),
        outlineVariant = line,
        surfaceVariant = Color(0xFFF2F4F7),
    )
}

fun argbColor(c: Int) = Color(c)

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = Ui.muted, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    placeholder: String? = null,
    hint: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = if (placeholder != null) {
            { Text(placeholder, fontSize = 13.sp, color = Ui.muted) }
        } else null,
        supportingText = if (hint != null) {
            { Text(hint, fontSize = 11.sp) }
        } else null,
        trailingIcon = trailing,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Ui.line,
            focusedBorderColor = Ui.accent,
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = Ui.text)
            if (hint != null) Text(hint, fontSize = 11.sp, color = Ui.muted)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Ui.accent),
            modifier = Modifier.height(28.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tip(text: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1D2939), shadowElevation = 4.dp) {
                Text(text, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
            }
        },
        delayMillis = 450,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 18.dp)),
    ) { content() }
}

@Composable
fun IconBtn(icon: ImageVector, tip: String, enabled: Boolean = true, tint: Color = Ui.text, onClick: () -> Unit) {
    Tip(tip) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
            Icon(icon, contentDescription = tip, tint = if (enabled) tint else tint.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SmallIconBtn(icon: ImageVector, tip: String, enabled: Boolean = true, onClick: () -> Unit) {
    Tip(tip) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(28.dp)) {
            Icon(icon, contentDescription = tip, tint = if (enabled) Ui.muted else Ui.muted.copy(alpha = 0.3f), modifier = Modifier.size(17.dp))
        }
    }
}

/** Pill-shaped segmented control. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF0F1F4)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { o ->
            val sel = o == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (sel) Color.White else Color.Transparent)
                    .then(if (sel) Modifier.border(1.dp, Ui.line, RoundedCornerShape(8.dp)) else Modifier)
                    .clickable { onSelect(o) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (sel) Ui.text else Ui.muted)
            }
        }
    }
}

@Composable
fun Swatch(color: Color, size: Int = 22, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(50))
            .background(color)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) Ui.text else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}
