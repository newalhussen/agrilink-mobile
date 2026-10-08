package com.agrilink.app.data.prefs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Device-level choices made before (or independently of) signing in. Stored locally before any network call. */
class SettingsStore(private val store: KeyValueStore) {

    private val languageState = MutableStateFlow(store.getString(LANGUAGE))

    /** "en", "am" or "om"; null until the user picks one on the first screen. */
    val language: StateFlow<String?> = languageState.asStateFlow()

    private val roleState = MutableStateFlow(store.getString(ROLE))

    /** Role chosen during onboarding (FARMER, BUYER or DRIVER), used by the sign-up form. */
    val onboardingRole: StateFlow<String?> = roleState.asStateFlow()

    fun setLanguage(code: String) {
        store.putString(LANGUAGE, code)
        languageState.value = code
    }

    fun setOnboardingRole(role: String?) {
        store.putString(ROLE, role)
        roleState.value = role
    }

    /** Notification ids already shown as system notifications, so each is posted once. */
    fun notifiedIds(): Set<String> =
        store.getString(NOTIFIED)?.split(',')?.filter { it.isNotBlank() }?.toSet().orEmpty()

    fun rememberNotified(ids: Collection<String>) {
        val merged = (notifiedIds() + ids).toList().takeLast(200)
        store.putString(NOTIFIED, merged.joinToString(","))
    }

    private companion object {
        const val LANGUAGE = "language"
        const val ROLE = "role"
        const val NOTIFIED = "notified"
    }
}
