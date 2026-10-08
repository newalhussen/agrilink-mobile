package com.agrilink.app.data.repo

import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.apiCall
import com.agrilink.app.data.api.ApiService
import com.agrilink.app.data.api.dto.NotificationDto
import com.agrilink.app.data.api.dto.PageResponse
import com.agrilink.app.data.api.dto.PayoutDto
import com.agrilink.app.data.api.dto.RegisterDeviceRequest
import com.agrilink.app.data.api.dto.WalletDto
import com.agrilink.app.data.api.dto.WalletTransactionDto
import com.agrilink.app.data.api.dto.WithdrawRequest

/** Earnings of farmers and drivers: "held for you", "ready to withdraw", the ledger and withdrawals. */
class WalletRepository(private val api: ApiService) {

    suspend fun wallet(): ApiResult<WalletDto> = apiCall { api.wallet() }

    suspend fun transactions(page: Int = 0): ApiResult<PageResponse<WalletTransactionDto>> =
        apiCall { api.walletTransactions(page) }

    suspend fun withdraw(amount: Double, method: String?, account: String?, name: String?): ApiResult<PayoutDto> =
        apiCall {
            api.withdraw(
                WithdrawRequest(amount, method?.takeIf { it.isNotBlank() }, account?.takeIf { it.isNotBlank() }, name?.takeIf { it.isNotBlank() }),
            )
        }
}

class NotificationRepository(private val api: ApiService) {

    suspend fun list(unreadOnly: Boolean = false, page: Int = 0): ApiResult<PageResponse<NotificationDto>> =
        apiCall { api.notifications(unreadOnly, page) }

    suspend fun unreadCount(): ApiResult<Int> = apiCall { api.unreadCount().unread }

    suspend fun markRead(id: String): ApiResult<Unit> = apiCall { api.markNotificationRead(id) }

    suspend fun markAllRead(): ApiResult<Unit> = apiCall { api.markAllNotificationsRead() }

    /** Registers a push token (FCM) for this device once Firebase is configured for the app. */
    suspend fun registerDevice(token: String): ApiResult<Unit> = apiCall { api.registerDevice(RegisterDeviceRequest(token)) }
}
