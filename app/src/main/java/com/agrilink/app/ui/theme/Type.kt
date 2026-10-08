package com.agrilink.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.agrilink.app.R

/** Caprasimo for headings, Figtree for body; Amharic uses Noto Sans Ethiopic for both (Caprasimo has no Ethiopic glyphs). */
val Caprasimo = FontFamily(Font(R.font.caprasimo_regular, FontWeight.Normal))

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val Figtree = FontFamily(
    variable(R.font.figtree_variable, 400), variable(R.font.figtree_variable, 500),
    variable(R.font.figtree_variable, 600), variable(R.font.figtree_variable, 700),
)

val NotoEthiopic = FontFamily(
    variable(R.font.noto_sans_ethiopic_variable, 400), variable(R.font.noto_sans_ethiopic_variable, 500),
    variable(R.font.noto_sans_ethiopic_variable, 600), variable(R.font.noto_sans_ethiopic_variable, 700),
)

/** Headline family for the given UI language ("am" falls back to Ethiopic bold). */
fun headingFamily(language: String): FontFamily = if (language == "am") NotoEthiopic else Caprasimo

fun bodyFamily(language: String): FontFamily = if (language == "am") NotoEthiopic else Figtree

fun agriTypography(language: String): Typography {
    val heading = headingFamily(language)
    val body = bodyFamily(language)
    val headingWeight = if (language == "am") FontWeight.Bold else FontWeight.Normal
    fun h(size: Int, line: Int) = TextStyle(fontFamily = heading, fontWeight = headingWeight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = (-0.2).sp)
    fun b(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
        TextStyle(fontFamily = body, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)
    return Typography(
        displayLarge = h(44, 48), displayMedium = h(36, 40), displaySmall = h(30, 34),
        headlineLarge = h(30, 34), headlineMedium = h(26, 30), headlineSmall = h(22, 26),
        titleLarge = h(20, 24), titleMedium = b(16, 22, FontWeight.Bold), titleSmall = b(14, 20, FontWeight.Bold),
        bodyLarge = b(16, 24), bodyMedium = b(14, 21), bodySmall = b(12, 17),
        labelLarge = b(14, 18, FontWeight.Bold), labelMedium = b(12, 16, FontWeight.SemiBold), labelSmall = b(11, 14, FontWeight.SemiBold),
    )
}
