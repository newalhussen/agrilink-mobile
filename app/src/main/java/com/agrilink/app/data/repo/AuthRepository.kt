package com.agrilink.app.data.repo

import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.apiCall
import com.agrilink.app.core.map
import com.agrilink.app.core.onOk
import com.agrilink.app.data.api.ApiService
import com.agrilink.app.data.api.dto.AuthResponse
import com.agrilink.app.data.api.dto.LoginRequest
import com.agrilink.app.data.api.dto.LogoutRequest
import com.agrilink.app.data.api.dto.OtpRequest
import com.agrilink.app.data.api.dto.OtpRequestResponse
import com.agrilink.app.data.api.dto.OtpVerifyRequest
import com.agrilink.app.data.api.dto.RegisterRequest
import com.agrilink.app.data.api.dto.RegisterResponse
import com.agrilink.app.data.api.dto.UpdateMeRequest
import com.agrilink.app.data.api.dto.UserDto
import com.agrilink.app.data.local.PendingActionDao
import com.agrilink.app.data.prefs.SessionStore
import com.agrilink.app.data.prefs.SettingsStore

/** Registration, OTP sign-in, password sign-in and the signed-in account. */
class AuthRepository(
    private val api: ApiService,
    private val session: SessionStore,
    private val settings: SettingsStore,
    private val pending: PendingActionDao,
) {
    suspend fun register(phone: String, fullName: String, role: String, password: String?): ApiResult<RegisterResponse> =
        apiCall {
            api.register(
                RegisterRequest(
                    phone = phone,
                    fullName = fullName.trim(),
                    role = role,
                    password = password?.takeIf { it.isNotBlank() },
                    preferredLanguage = settings.language.value ?: "en",
                ),
            )
        }

    suspend fun requestOtp(phone: String, purpose: String): ApiResult<OtpRequestResponse> =
        apiCall { api.requestOtp(OtpRequest(phone, purpose)) }

    suspend fun verifyOtp(phone: String, code: String, purpose: String): ApiResult<AuthResponse> =
        apiCall { api.verifyOtp(OtpVerifyRequest(phone, code, purpose)) }.onOk { session.save(it) }

    suspend fun login(phone: String, password: String): ApiResult<AuthResponse> =
        apiCall { api.login(LoginRequest(phone, password)) }.onOk { session.save(it) }

    /** Refreshes the cached user (verification status changes while the app is closed). */
    suspend fun refreshMe(): ApiResult<UserDto> = apiCall { api.me() }.onOk { session.updateUser(it) }

    suspend fun updateMe(request: UpdateMeRequest): ApiResult<UserDto> =
        apiCall { api.updateMe(request) }.onOk { session.updateUser(it) }

    suspend fun setLanguage(code: String): ApiResult<Unit> {
        settings.setLanguage(code)
        if (session.user == null) return ApiResult.Ok(Unit)
        return updateMe(UpdateMeRequest(preferredLanguage = code)).map { }
    }

    suspend fun logout() {
        val refresh = session.refreshToken
        if (refresh != null) apiCall { api.logout(LogoutRequest(refresh)) }
        pending.clear()
        session.clear()
    }
}
