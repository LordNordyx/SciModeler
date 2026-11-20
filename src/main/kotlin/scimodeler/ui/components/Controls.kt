package scimodeler.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import scimodeler.ui.theme.LocalMotion
import scimodeler.ui.theme.Tok

// ------------------------------------------------------------------ Interaction states

data class UiStates(val hovered: Boolean, val pressed: Boolean, val focused: Boolean)

@Composable
fun MutableInteractionSource.states(): UiStates {
    val h by collectIsHoveredAsState()
    val p by collectIsPressedAsState()
    val f by collectIsFocusedAsState()
    return UiStates(h, p, f)
}

/** Visible keyboard-focus ring (drawn only while focused). */
fun Modifier.focusRing(focused: Boolean, shape: Shape, width: Dp = 2.dp): Modifier =
    if (focused) this.border(width, Tok.C.focus, shape) else this

/** Clickable with hand cursor, no ripple (we draw our own hover/pressed states), and keyboard focus. */
fun Modifier.pressable(source: MutableInteractionSource, enabled: Boolean = true, role: Role = Role.Button, onClick: () -> Unit) =
    this.pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClick = onClick)

// ------------------------------------------------------------------ Buttons

enum class ButtonKind { PRIMARY, SECONDARY, GHOST }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.SECONDARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 36.dp,
    tag: String? = null,
) {
    val motion = LocalMotion.current
    val src = remember { MutableInteractionSource() }
    val st = src.states()
    val shape = RoundedCornerShape(Tok.R.md)
    val target = when (kind) {
        ButtonKind.PRIMARY -> when { !enabled -> Tok.C.accent.copy(alpha = 0.4f); st.pressed -> Tok.C.accentPressed; st.hovered -> Tok.C.accentHover; else -> Tok.C.accent }
        ButtonKind.SECONDARY -> when { st.pressed -> Color(0xFFEFF1F5); st.hovered -> Color.White; else -> Tok.C.surface }
        ButtonKind.GHOST -> when { st.pressed -> Tok.C.pressed; st.hovered -> Tok.C.hover; else -> Color.Transparent }
    }
    val bg by animateColorAsState(target, motion.tween(Tok.Motion.fast))
    val scale by animateFloatAsState(if (st.pressed && enabled) 0.97f else 1f, motion.spring())
    val fg = when (kind) {
        ButtonKind.PRIMARY -> Tok.C.onAccent
        else -> if (enabled) Tok.C.text else Tok.C.text3
    }
    Row(
        modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .scale(scale)
            .height(height)
            .then(if (kind == ButtonKind.PRIMARY && enabled) Modifier.shadow(if (st.hovered) 8.dp else 3.dp, shape, ambientColor = Tok.C.accent, spotColor = Tok.C.accent) else Modifier)
            .clip(shape)
            .background(bg)
            .then(if (kind == ButtonKind.SECONDARY) Modifier.border(1.dp, if (st.hovered) Tok.C.lineStrong else Tok.C.line, shape) else Modifier)
            .focusRing(st.focused, shape)
            .pressable(src, enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, style = Tok.T.label.copy(color = fg, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun IconBtn(
    icon: ImageVector,
    tip: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    size: Dp = 34.dp,
    iconSize: Dp = 18.dp,
    tag: String? = null,
    tipBelow: Boolean = false,
) {
    val motion = LocalMotion.current
    val src = remember { MutableInteractionSource() }
    val st = src.states()
    val shape = RoundedCornerShape(Tok.R.md)
    val bg by animateColorAsState(
        when { active -> Tok.C.accentSoft; !enabled -> Color.Transparent; st.pressed -> Tok.C.pressed; st.hovered -> Tok.C.hover; else -> Color.Transparent },
        motion.tween(Tok.Motion.fast),
    )
    val tint by animateColorAsState(
        when { !enabled -> Tok.C.text3.copy(alpha = 0.5f); active -> Tok.C.accent; st.hovered -> Tok.C.text; else -> Tok.C.text2 },
        motion.tween(Tok.Motion.fast),
    )
    val scale by animateFloatAsState(if (st.pressed) 0.92f else 1f, motion.spring())
    Tip(tip, below = tipBelow) {
        Box(
            Modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier)
                .size(size).scale(scale).clip(shape).background(bg)
                .focusRing(st.focused, shape)
                .pressable(src, enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, tip, tint = tint, modifier = Modifier.size(iconSize)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tip(text: String, below: Boolean = false, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            Box(Modifier.shadow(8.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(Tok.C.tooltip)) {
                Text(text, style = Tok.T.small.copy(color = Color.White), modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
            }
        },
        delayMillis = 500,
        // Above the pointer by default: a tooltip squeezed against the window edge would otherwise
        // overlap the pointer and flicker.
        tooltipPlacement = if (below) TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 20.dp))
            else TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, (-10).dp), alignment = Alignment.TopCenter),
    ) { content() }
}

// ------------------------------------------------------------------ Inputs

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
    leading: @Composable (() -> Unit)? = null,
    tag: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = if (label.isNotEmpty()) { { Text(label, style = Tok.T.small) } } else null,
        placeholder = if (placeholder != null) { { Text(placeholder, style = Tok.T.body.copy(color = Tok.C.text3)) } } else null,
        supportingText = if (hint != null) { { Text(hint, style = Tok.T.tiny) } } else null,
        trailingIcon = trailing,
        leadingIcon = leading,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = Tok.T.body,
        shape = RoundedCornerShape(Tok.R.lg),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Tok.C.line,
            focusedBorderColor = Tok.C.accent,
            unfocusedContainerColor = Tok.C.surface,
            focusedContainerColor = Color.White,
            cursorColor = Tok.C.accent,
            focusedLabelColor = Tok.C.accent,
        ),
        modifier = modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier),
    )
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null, tag: String? = null) {
    val src = remember { MutableInteractionSource() }
    val st = src.states()
    val bg by animateColorAsState(if (st.hovered) Tok.C.hover else Color.Transparent, LocalMotion.current.tween(Tok.Motion.fast))
    val shape = RoundedCornerShape(Tok.R.md)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(bg).focusRing(st.focused, shape)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .pressable(src, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 7.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Tok.T.body)
            if (hint != null) Text(hint, style = Tok.T.small.copy(color = Tok.C.text3))
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Tok.C.accent, uncheckedBorderColor = Tok.C.lineStrong),
            modifier = Modifier.scale(0.82f).height(28.dp),
        )
    }
}

/** Segmented control with a sliding selection indicator. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String? = null,
) {
    val motion = LocalMotion.current
    val idx = options.indexOf(selected)
    BoxWithConstraints(
        modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(Tok.R.md)).background(Tok.C.surfaceSunken).padding(3.dp),
    ) {
        val segW = maxWidth / options.size.coerceAtLeast(1)
        val x by animateDpAsState(segW * idx.coerceAtLeast(0), motion.tween(Tok.Motion.base))
        if (idx >= 0) {
            Box(
                Modifier.offset(x = x).width(segW).fillMaxHeight()
                    .shadow(3.dp, RoundedCornerShape(Tok.R.sm)).clip(RoundedCornerShape(Tok.R.sm)).background(Color.White),
            )
        }
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { k, o ->
                val src = remember(k) { MutableInteractionSource() }
                val st = src.states()
                val sel = k == idx
                val fg by animateColorAsState(
                    when { sel -> Tok.C.text; st.hovered -> Tok.C.text; else -> Tok.C.text2 }, motion.tween(Tok.Motion.fast),
                )
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(Tok.R.sm))
                        .focusRing(st.focused, RoundedCornerShape(Tok.R.sm))
                        .then(if (tagPrefix != null) Modifier.testTag("$tagPrefix-${label(o).lowercase()}") else Modifier)
                        .pressable(src, role = Role.Tab) { onSelect(o) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label(o), style = Tok.T.label.copy(color = fg, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium), maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun LabeledSlider(label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = Tok.T.body, modifier = Modifier.weight(1f))
            Text(valueText, style = Tok.T.small)
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Tok.C.accent, activeTrackColor = Tok.C.accent, inactiveTrackColor = Tok.C.line),
        )
    }
}

// ------------------------------------------------------------------ Small parts

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = Tok.T.caption, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun Swatch(color: Color, size: Dp = 22.dp, selected: Boolean = false, tip: String? = null, onClick: (() -> Unit)? = null) {
    val src = remember { MutableInteractionSource() }
    val st = src.states()
    val scale by animateFloatAsState(if (st.hovered && onClick != null) 1.12f else 1f, LocalMotion.current.spring())
    val shape = RoundedCornerShape(50)
    val body = @Composable {
        Box(
            Modifier.size(size).scale(scale).clip(shape).background(color)
                .border(if (selected) 2.5.dp else 1.dp, if (selected) Tok.C.text else Color.Black.copy(alpha = 0.08f), shape)
                .focusRing(st.focused, shape)
                .then(if (onClick != null) Modifier.pressable(src, onClick = onClick) else Modifier),
        )
    }
    if (tip != null) Tip(tip) { body() } else body()
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, color: Color = Tok.C.text2, bg: Color = Tok.C.surfaceSunken) {
    Text(
        text, style = Tok.T.tiny.copy(color = color),
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}
