package com.agrilink.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrilink.app.ui.theme.Organic
import com.agrilink.app.ui.theme.PillShape

/** Pill-shaped text field on the surface colour, label above (the design's `.field` + `.input`). */
@Composable
fun PillField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: ImageVector? = null,
    prefix: String? = null,
    error: String? = null,
    hint: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Organic.Neutral800)
        val fill = LocalFieldFill.current
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().then(if (singleLine) Modifier.height(56.dp) else Modifier),
            enabled = enabled,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            placeholder = placeholder?.let { { Text(it, color = Organic.Neutral600) } },
            leadingIcon = leadingIcon?.let { { Icon(it, null, tint = Organic.Neutral700) } },
            prefix = prefix?.let { { Text(it, color = Organic.Neutral800, style = MaterialTheme.typography.bodyLarge) } },
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = if (singleLine) PillShape else RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = fill, unfocusedContainerColor = fill, disabledContainerColor = fill.copy(alpha = 0.6f),
                errorContainerColor = fill,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
                cursorColor = Organic.Accent,
            ),
        )
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = Organic.Accent700, fontWeight = FontWeight.SemiBold)
        else if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
    }
}

/** Six boxes for the SMS code; typing anywhere fills them in order. */
@Composable
fun OtpInput(code: String, onCodeChange: (String) -> Unit, modifier: Modifier = Modifier, length: Int = 6, error: Boolean = false) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = code,
        onValueChange = { new -> onCodeChange(new.filter { it.isDigit() }.take(length)) },
        modifier = modifier.focusRequester(focus),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { i ->
                    val char = code.getOrNull(i)
                    val active = i == code.length
                    Box(
                        Modifier.weight(1f).height(62.dp).clip(RoundedCornerShape(20.dp)).background(Organic.Surface)
                            .border(BorderStroke(if (active || error) 2.dp else 1.dp, if (error) Organic.Accent700 else if (active) Organic.Accent else Organic.Divider), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(char?.toString() ?: "", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        },
    )
}

@Composable
fun QuantityStepper(
    value: Double,
    onValueChange: (Double) -> Unit,
    step: Double,
    min: Double,
    max: Double,
    modifier: Modifier = Modifier,
    unitLabel: String = "",
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        StepButton(Icons.Filled.Remove, enabled = value - step >= min - 1e-9) { onValueChange((value - step).coerceAtLeast(min)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(com.agrilink.app.core.Format.number(value), style = MaterialTheme.typography.headlineSmall)
            if (unitLabel.isNotEmpty()) Text(unitLabel, style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
        }
        StepButton(Icons.Filled.Add, enabled = value + step <= max + 1e-9) { onValueChange((value + step).coerceAtMost(max)) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(if (enabled) Organic.Accent200 else Organic.Neutral300.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) Organic.Accent800 else Organic.Neutral500) }
}

/** Horizontally scrolling pill chips (categories, filters). [selected] null means "all". */
@Composable
fun <T> ChipRow(
    items: List<Pair<T?, String>>,
    selected: T?,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = contentPadding) {
        items(items) { (key, label) ->
            val active = key == selected
            Text(
                label,
                modifier = Modifier.clip(PillShape).background(if (active) Organic.Accent else Organic.Surface)
                    .clickable { onSelect(key) }.padding(horizontal = 16.dp, vertical = 9.dp),
                color = if (active) Organic.Bg else Organic.Text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Two or three mutually exclusive pills, the design's segmented control. */
@Composable
fun <T> Segmented(items: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(PillShape).border(BorderStroke(1.dp, Organic.Divider), PillShape)) {
        items.forEachIndexed { index, (key, label) ->
            val active = key == selected
            Box(
                Modifier.weight(1f).background(if (active) Organic.Accent else Color.Transparent).clickable { onSelect(key) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (active) Organic.Bg else Organic.Text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
            if (index < items.lastIndex) Box(Modifier.size(width = 1.dp, height = 40.dp).background(Organic.Divider))
        }
    }
}

/** Large tappable choice (language, role): the selected one is outlined in its accent. */
@Composable
fun ChoiceCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    accent: Color = Organic.Accent,
    selectedFill: Color = Organic.Accent200,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(32.dp),
    onClick: () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().clip(shape).background(if (selected) selectedFill else Organic.Surface)
            .then(if (selected) Modifier.border(BorderStroke(2.dp, accent), shape) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral800)
        }
        if (trailing != null) trailing() else if (selected) Icon(Icons.Filled.CheckCircle, null, tint = accent, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun StarRating(value: Int, onChange: ((Int) -> Unit)?, modifier: Modifier = Modifier, size: Int = 36) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            Icon(
                if (i <= value) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "$i",
                tint = if (i <= value) Organic.Accent else Organic.Neutral500,
                modifier = Modifier.size(size.dp).then(
                    if (onChange != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(i) } else Modifier,
                ),
            )
        }
    }
}

/** Read-only looking field that opens a menu of options. */
@Composable
fun <T> DropdownField(
    label: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    val text = options.firstOrNull { it.first == selected }?.second ?: placeholder
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Organic.Neutral800)
        Box {
            Row(
                Modifier.fillMaxWidth().height(56.dp).clip(PillShape).background(Organic.Surface).clickable { open = true }.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (selected == null) Organic.Neutral600 else Organic.Text, maxLines = 1)
                Icon(Icons.Filled.ArrowDropDown, null, tint = Organic.Neutral700)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Organic.Neutral100) {
                options.forEach { (key, name) ->
                    DropdownMenuItem(text = { Text(name, style = MaterialTheme.typography.bodyLarge) }, onClick = { onSelect(key); open = false })
                }
            }
        }
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = Organic.Accent700, fontWeight = FontWeight.SemiBold)
    }
}

/** Plain decorated text used for "Read aloud"-style hints. */
@Composable
fun HintText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodySmall) {
    Text(text, modifier.padding(horizontal = 4.dp), style = style, color = Organic.Neutral700)
}
