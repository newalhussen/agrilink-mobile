package com.agrilink.app.ui.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Per-app language: English, Amharic (am) or Afaan Oromoo (om). Android recreates the activity when it changes. */
object LocaleHelper {
    val supported = listOf("om", "am", "en")

    fun apply(code: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
    }

    /** Language the UI is currently showing (never null; falls back to English). */
    fun current(): String {
        val tag = AppCompatDelegate.getApplicationLocales().get(0)?.language
        return if (tag in supported) tag!! else "en"
    }
}
