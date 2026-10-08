package com.agrilink.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

private val AgriColors = lightColorScheme(
    primary = Organic.Accent,
    onPrimary = Organic.Bg,
    primaryContainer = Organic.Accent200,
    onPrimaryContainer = Organic.Accent800,
    secondary = Organic.Sage700,
    onSecondary = Organic.Neutral100,
    secondaryContainer = Organic.Sage200,
    onSecondaryContainer = Organic.Sage900,
    tertiary = Organic.Accent800,
    onTertiary = Organic.Accent100,
    tertiaryContainer = Organic.Accent300,
    onTertiaryContainer = Organic.Accent900,
    background = Organic.Bg,
    onBackground = Organic.Text,
    surface = Organic.Bg,
    onSurface = Organic.Text,
    surfaceVariant = Organic.Surface,
    onSurfaceVariant = Organic.Neutral700,
    surfaceContainerLowest = Organic.Neutral100,
    surfaceContainerLow = Organic.Bg,
    surfaceContainer = Organic.Surface,
    surfaceContainerHigh = Organic.Bg,
    surfaceContainerHighest = Organic.Neutral300,
    outline = Organic.Neutral500,
    outlineVariant = Organic.Divider,
    error = Organic.Accent700,
    onError = Organic.Neutral100,
    errorContainer = Organic.Accent200,
    onErrorContainer = Organic.Accent900,
)

private val AgriShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** The pill used for buttons, tags and inputs. */
val PillShape = RoundedCornerShape(percent = 50)

@Composable
fun AgriLinkTheme(language: String = "en", content: @Composable () -> Unit) {
    val typography = remember(language) { agriTypography(language) }
    MaterialTheme(colorScheme = AgriColors, typography = typography, shapes = AgriShapes, content = content)
}
