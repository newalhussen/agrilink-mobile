package com.agrilink.app.ui.common

import com.agrilink.app.data.api.dto.CategoryDto
import com.agrilink.app.data.api.dto.ProductDto
import com.agrilink.app.data.api.dto.RegionDto

/** Picks the Amharic or Afaan Oromoo name when the UI is in that language and the catalogue has one. */
fun localized(en: String, am: String?, om: String?, language: String = LocaleHelper.current()): String = when (language) {
    "am" -> am?.takeIf { it.isNotBlank() } ?: en
    "om" -> om?.takeIf { it.isNotBlank() } ?: en
    else -> en
}

fun ProductDto.display(language: String = LocaleHelper.current()) = localized(nameEn, nameAm, nameOm, language)
fun CategoryDto.display(language: String = LocaleHelper.current()) = localized(nameEn, nameAm, nameOm, language)
fun RegionDto.display(language: String = LocaleHelper.current()) = localized(nameEn, nameAm, nameOm, language)
