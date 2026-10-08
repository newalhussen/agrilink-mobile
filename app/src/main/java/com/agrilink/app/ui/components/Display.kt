package com.agrilink.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrilink.app.ui.theme.Organic

enum class Tone { Accent, Sage, Neutral, Outline, Danger, SolidSage, SolidDark }

/** The design's `.tag`: small tinted label. */
@Composable
fun Tag(text: String, tone: Tone = Tone.Neutral, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        Tone.Accent -> Organic.Accent100 to Organic.Accent800
        Tone.Sage -> Organic.Sage100 to Organic.Sage800
        Tone.Neutral -> Organic.Neutral100 to Organic.Neutral800
        Tone.Outline -> Color.Transparent to Organic.Accent
        Tone.Danger -> Organic.Accent200 to Organic.Accent900
        Tone.SolidSage -> Organic.Sage700 to Organic.Neutral100
        Tone.SolidDark -> Organic.Accent800 to Organic.Accent100
    }
    val border = if (tone == Tone.Outline) Modifier.border(BorderStroke(1.dp, Organic.Accent), RoundedCornerShape(12.dp)) else Modifier
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(bg).then(border).padding(horizontal = 10.dp, vertical = 4.dp),
        color = fg,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
}

/** Initials in a coloured circle (the design uses these for farmers, buyers and drivers). */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp, background: Color = Organic.Accent300, content: Color = Organic.Accent900) {
    val initials = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.let { parts ->
        (parts.firstOrNull()?.take(1).orEmpty() + if (parts.size > 1) parts.last().take(1) else "").uppercase()
    }.ifEmpty { "?" }
    Box(modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Text(initials, color = content, style = MaterialTheme.typography.titleSmall.copy(fontSize = (size.value * 0.34f).sp))
    }
}

/** Surface-filled, very rounded container (`.card`). */
/** Fill of text fields: the card colour would make them invisible, so inside a card they use the page colour. */
val LocalFieldFill = androidx.compose.runtime.compositionLocalOf { Organic.Surface }

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    color: Color = Organic.Surface,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val base = modifier.fillMaxWidth().clip(shape).background(color)
    Column(
        (if (onClick != null) base.clickable(onClick = onClick) else base).padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { androidx.compose.runtime.CompositionLocalProvider(LocalFieldFill provides if (color == Organic.Surface) Organic.Bg else Organic.Surface) { content() } }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (action != null && onAction != null) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = Organic.Accent700, modifier = Modifier.clickable(onClick = onAction).padding(8.dp))
        }
    }
}

/** Label on the left, value on the right (price breakdowns, order facts). */
@Composable
fun FactRow(label: String, value: String, modifier: Modifier = Modifier, bold: Boolean = false, valueColor: Color = Organic.Text) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold),
            color = valueColor,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

/** Soft divider line using the design's divider token. */
@Composable
fun SoftDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().size(width = 0.dp, height = 1.dp).background(Organic.Divider))
}
