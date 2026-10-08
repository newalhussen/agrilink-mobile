package com.agrilink.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.AppError
import com.agrilink.app.core.Phone
import com.agrilink.app.data.prefs.SettingsStore
import com.agrilink.app.data.repo.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val login: Boolean = false,
    val fullName: String = "",
    val phone: String = "",
    val usePassword: Boolean = false,
    val password: String = "",
    val code: String = "",
    val purpose: String = "REGISTER",
    val busy: Boolean = false,
    val error: AppError? = null,
    val devOtp: String? = null,
    val resendSeconds: Int = 0,
) {
    val normalizedPhone: String get() = Phone.normalize(phone)
    val canSubmitPhone: Boolean get() = Phone.looksValid(phone) && (login || fullName.trim().length >= 2) && (!usePassword || password.length >= 4)
}

/**
 * Shared by the whole sign-in flow (phone, code). Sign-up registers and sends an SMS code; sign-in sends a code or
 * accepts a password. On success the session is stored by the repository and the caller navigates onward.
 */
class AuthViewModel(
    private val auth: AuthRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private var ticker: Job? = null

    val role: String? get() = settings.onboardingRole.value

    fun startFlow(login: Boolean) = _state.update { AuthUiState(login = login, phone = it.phone, fullName = it.fullName) }

    fun setName(v: String) = _state.update { it.copy(fullName = v, error = null) }
    fun setPhone(v: String) = _state.update { it.copy(phone = v.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(16), error = null) }
    fun setPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun setCode(v: String) = _state.update { it.copy(code = v, error = null) }
    fun togglePassword() = _state.update { it.copy(usePassword = !it.usePassword, error = null, password = "") }
    fun clearError() = _state.update { it.copy(error = null) }

    /** Phone screen "Continue". [onCodeSent] opens the code screen; [onSignedIn] is used by password sign-in. */
    fun submitPhone(onCodeSent: () -> Unit, onSignedIn: () -> Unit) {
        val s = _state.value
        if (!s.canSubmitPhone || s.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when {
                !s.login -> {
                    val role = settings.onboardingRole.value ?: "BUYER"
                    when (val r = auth.register(s.normalizedPhone, s.fullName, role, null)) {
                        is ApiResult.Ok -> {
                            _state.update { it.copy(busy = false, purpose = "REGISTER", code = "", devOtp = r.value.devOtp) }
                            startTicker(r.value.resendAfterSeconds)
                            onCodeSent()
                        }
                        is ApiResult.Err -> _state.update { it.copy(busy = false, error = r.error) }
                    }
                }
                s.usePassword -> when (val r = auth.login(s.normalizedPhone, s.password)) {
                    is ApiResult.Ok -> { _state.update { it.copy(busy = false) }; onSignedIn() }
                    is ApiResult.Err -> _state.update { it.copy(busy = false, error = r.error) }
                }
                else -> when (val r = auth.requestOtp(s.normalizedPhone, "LOGIN")) {
                    is ApiResult.Ok -> {
                        _state.update { it.copy(busy = false, purpose = "LOGIN", code = "", devOtp = r.value.devOtp) }
                        startTicker(r.value.resendAfterSeconds)
                        onCodeSent()
                    }
                    is ApiResult.Err -> _state.update { it.copy(busy = false, error = r.error) }
                }
            }
        }
    }

    /** [onDone] receives true when this was a brand-new account (continue to profile setup). */
    fun verify(onDone: (newAccount: Boolean) -> Unit) {
        val s = _state.value
        if (s.code.length < 6 || s.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.verifyOtp(s.normalizedPhone, s.code, s.purpose)) {
                is ApiResult.Ok -> {
                    ticker?.cancel()
                    _state.update { it.copy(busy = false) }
                    onDone(s.purpose == "REGISTER")
                }
                is ApiResult.Err -> _state.update { it.copy(busy = false, error = r.error, code = "") }
            }
        }
    }

    fun resend() {
        val s = _state.value
        if (s.resendSeconds > 0 || s.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.requestOtp(s.normalizedPhone, s.purpose)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(busy = false, devOtp = r.value.devOtp, code = "") }
                    startTicker(r.value.resendAfterSeconds)
                }
                is ApiResult.Err -> _state.update { it.copy(busy = false, error = r.error) }
            }
        }
    }

    private fun startTicker(seconds: Int) {
        ticker?.cancel()
        _state.update { it.copy(resendSeconds = seconds) }
        ticker = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1000)
                left--
                _state.update { it.copy(resendSeconds = left) }
            }
        }
    }
}
