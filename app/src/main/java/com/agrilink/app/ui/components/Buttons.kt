package com.agrilink.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrilink.app.ui.theme.Organic
import com.agrilink.app.ui.theme.PillShape

enum class ButtonKind { Primary, Secondary, Ghost, Sage, Danger }

/** Pill button in the Organic style: Caprasimo label, accent fill, themed hover/pressed states. */
@Composable
fun AgriButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    height: Int = 52,
) {
    val label: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp, color = LocalContentColorOr(kind))
        } else {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp))
        }
    }
    val padding = PaddingValues(horizontal = 24.dp)
    val sizing = modifier.defaultMinSize(minHeight = height.dp).height(height.dp)
    val canClick = enabled && !loading
    when (kind) {
        ButtonKind.Primary -> Button(
            onClick, sizing, canClick, shape = PillShape, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = Organic.Accent, contentColor = Organic.Bg, disabledContainerColor = Organic.Accent.copy(alpha = 0.45f), disabledContentColor = Organic.Bg),
        ) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = label) }
        ButtonKind.Sage -> Button(
            onClick, sizing, canClick, shape = PillShape, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = Organic.Sage700, contentColor = Organic.Neutral100, disabledContainerColor = Organic.Sage700.copy(alpha = 0.45f)),
        ) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = label) }
        ButtonKind.Danger -> Button(
            onClick, sizing, canClick, shape = PillShape, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = Organic.Accent800, contentColor = Organic.Accent100, disabledContainerColor = Organic.Accent800.copy(alpha = 0.45f)),
        ) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = label) }
        ButtonKind.Secondary -> OutlinedButton(
            onClick, sizing, canClick, shape = PillShape, contentPadding = padding,
            border = BorderStroke(1.dp, Organic.Divider),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Organic.Text),
        ) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = label) }
        ButtonKind.Ghost -> TextButton(
            onClick, sizing, canClick, shape = PillShape, contentPadding = padding,
            colors = ButtonDefaults.textButtonColors(contentColor = Organic.Accent700),
        ) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = label) }
    }
}

private fun LocalContentColorOr(kind: ButtonKind): Color = when (kind) {
    ButtonKind.Primary -> Organic.Bg
    ButtonKind.Sage -> Organic.Neutral100
    ButtonKind.Danger -> Organic.Accent100
    else -> Organic.Accent700
}

/** Round 44 dp icon action used in top bars and cards. */
@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Organic.Text) {
    IconButton(onClick = onClick, modifier = modifier.size(44.dp)) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}
