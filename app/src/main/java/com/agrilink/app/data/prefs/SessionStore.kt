package com.agrilink.app.data.prefs

import com.agrilink.app.data.api.dto.AuthResponse
import com.agrilink.app.data.api.dto.UserDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class Session(val accessToken: String, val refreshToken: String, val user: UserDto)

/**
 * Signed-in state. Tokens and the cached user profile are stored in encrypted preferences; the in-memory
 * [session] flow drives navigation (signed out when it becomes null).
 */
class SessionStore(
    private val store: KeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    private val state = MutableStateFlow(load())
    val session: StateFlow<Session?> = state.asStateFlow()

    private val expiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the server rejected our refresh token and the user has been signed out. */
    val expired: SharedFlow<Unit> = expiredEvents.asSharedFlow()

    val accessToken: String? get() = state.value?.accessToken
    val refreshToken: String? get() = state.value?.refreshToken
    val user: UserDto? get() = state.value?.user

    fun save(auth: AuthResponse) {
        store.putString(ACCESS, auth.accessToken)
        store.putString(REFRESH, auth.refreshToken)
        store.putString(USER, json.encodeToString(auth.user))
        state.value = Session(auth.accessToken, auth.refreshToken, auth.user)
    }

    /** Keeps the user in sync after profile edits without touching the tokens. */
    fun updateUser(user: UserDto) {
        val current = state.value ?: return
        store.putString(USER, json.encodeToString(user))
        state.value = current.copy(user = user)
    }

    fun clear(expired: Boolean = false) {
        store.remove(ACCESS, REFRESH, USER)
        val wasSignedIn = state.value != null
        state.value = null
        if (expired && wasSignedIn) expiredEvents.tryEmit(Unit)
    }

    private fun load(): Session? {
        val access = store.getString(ACCESS) ?: return null
        val refresh = store.getString(REFRESH) ?: return null
        val user = store.getString(USER)?.let { runCatching { json.decodeFromString<UserDto>(it) }.getOrNull() }
            ?: return null
        return Session(access, refresh, user)
    }

    private companion object {
        const val ACCESS = "access"
        const val REFRESH = "refresh"
        const val USER = "user"
    }
}
