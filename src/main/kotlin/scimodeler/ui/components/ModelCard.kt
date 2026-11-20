package scimodeler.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import scimodeler.ui.theme.LocalMotion
import scimodeler.ui.theme.Tok

/**
 * A selectable preview card used for models and templates. Thumbnails sit in a fixed 4:3 frame
 * and are scaled to fit, so every card has the same size while each image keeps its proportions.
 */
@Composable
fun PreviewCard(
    title: String,
    description: String,
    thumbnail: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    aspect: Float = 4f / 3f,
) {
    val motion = LocalMotion.current
    val src = remember { MutableInteractionSource() }
    val st = src.states()
    val shape = RoundedCornerShape(Tok.R.lg)
    val lift by animateDpAsState(if (st.hovered && !st.pressed) 10.dp else if (selected) 4.dp else 1.dp, motion.tween(Tok.Motion.base))
    val raise by animateDpAsState(if (st.hovered && !st.pressed) (-2).dp else 0.dp, motion.tween(Tok.Motion.base))
    val scale by animateFloatAsState(if (st.pressed) 0.96f else 1f, motion.spring())
    val border by animateColorAsState(
        when { selected -> Tok.C.accent; st.hovered -> Tok.C.lineStrong; else -> Tok.C.line }, motion.tween(Tok.Motion.fast),
    )
    val frameBg by animateColorAsState(if (selected) Tok.C.accentSoft else Color.White, motion.tween(Tok.Motion.base))
    val titleColor by animateColorAsState(if (selected) Tok.C.accent else Tok.C.text, motion.tween(Tok.Motion.fast))

    Tip(description) {
        Column(
            modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier)
                .offset(y = raise).scale(scale)
                .pressable(src, role = Role.RadioButton, onClick = onClick),
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(aspect)
                    .shadow(lift, shape, ambientColor = Tok.C.glassShadow, spotColor = Tok.C.glassShadow)
                    .clip(shape).background(frameBg)
                    .border(if (selected) 2.dp else 1.dp, border, shape)
                    .focusRing(st.focused, shape, 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnail != null) {
                    Image(thumbnail, title, Modifier.fillMaxSize().padding(10.dp), contentScale = ContentScale.Fit)
                } else {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Tok.C.text3)
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = selected, enter = motion.popIn(), exit = motion.popOut(),
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                ) {
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(Tok.C.accent), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Text(
                title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = Tok.T.label.copy(color = titleColor, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                modifier = Modifier.padding(top = 6.dp, start = 2.dp),
            )
        }
    }
}
