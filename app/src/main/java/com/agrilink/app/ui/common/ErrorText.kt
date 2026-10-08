package com.agrilink.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.agrilink.app.R
import com.agrilink.app.core.AppError

/** Known API error codes that have a translated message; everything else shows the server's own text. */
@StringRes
fun AppError.messageRes(): Int? = when (code) {
    AppError.NETWORK -> R.string.error_network
    "INVALID_CREDENTIALS" -> R.string.error_invalid_credentials
    "OTP_INVALID" -> R.string.error_otp_invalid
    "OTP_EXPIRED" -> R.string.error_otp_expired
    "OTP_RATE_LIMITED" -> R.string.error_otp_rate_limited
    "ACCOUNT_LOCKED" -> R.string.error_account_locked
    "ACCOUNT_SUSPENDED" -> R.string.error_account_suspended
    "PHONE_ALREADY_REGISTERED" -> R.string.error_phone_registered
    "PHONE_NOT_VERIFIED" -> R.string.error_phone_not_verified
    "VERIFICATION_REQUIRED" -> R.string.error_verification_required
    "INVALID_HANDOVER_CODE" -> R.string.error_handover_code
    "HANDOVER_LOCKED" -> R.string.error_handover_locked
    "TOKEN_INVALID", "UNAUTHENTICATED" -> R.string.error_session_expired
    else -> null
}

@Composable
fun AppError.errorText(): String {
    val res = messageRes()
    if (res != null) return stringResource(res)
    val field = fieldErrors.firstOrNull()
    if (field != null && field.message.isNotBlank()) return field.message
    return message.ifBlank { stringResource(R.string.error_generic) }
}
