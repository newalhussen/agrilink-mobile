package com.agrilink.app.data.repo

import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.apiCall
import com.agrilink.app.data.api.ApiService
import com.agrilink.app.data.api.AppJson
import com.agrilink.app.data.api.dto.AddEvidenceRequest
import com.agrilink.app.data.api.dto.CreateOrderRequest
import com.agrilink.app.data.api.dto.CreateRatingRequest
import com.agrilink.app.data.api.dto.DisputeDto
import com.agrilink.app.data.api.dto.InitiatePaymentRequest
import com.agrilink.app.data.api.dto.OpenDisputeRequest
import com.agrilink.app.data.api.dto.OrderDto
import com.agrilink.app.data.api.dto.OrderListItemDto
import com.agrilink.app.data.api.dto.PageResponse
import com.agrilink.app.data.api.dto.PaymentDto
import com.agrilink.app.data.api.dto.PaymentMethodInfoDto
import com.agrilink.app.data.api.dto.QuoteResponse
import com.agrilink.app.data.api.dto.RatingDto
import com.agrilink.app.data.api.dto.ReasonRequest
import com.agrilink.app.data.api.dto.TimelineEntryDto
import kotlinx.serialization.encodeToString

/**
 * Orders, payments, ratings and disputes. Accept / reject / ready are queued when there is no signal
 * (farmers in the field), everything involving money or a code check needs a live connection.
 */
class OrderRepository(
    private val api: ApiService,
    private val outbox: Outbox,
) {
    suspend fun quote(request: CreateOrderRequest): ApiResult<QuoteResponse> = apiCall { api.quote(request) }

    suspend fun create(request: CreateOrderRequest): ApiResult<OrderDto> = apiCall { api.createOrder(request) }

    suspend fun list(statuses: List<String>? = null, page: Int = 0): ApiResult<PageResponse<OrderListItemDto>> =
        apiCall { api.orders(statuses?.takeIf { it.isNotEmpty() }, page) }

    suspend fun get(id: String): ApiResult<OrderDto> = apiCall { api.order(id) }

    suspend fun timeline(id: String): ApiResult<List<TimelineEntryDto>> = apiCall { api.orderTimeline(id) }

    suspend fun accept(id: String, orderNumber: String): ActionResult<OrderDto> =
        queueable(
            call = { api.acceptOrder(id) },
            queue = { outbox.enqueue("ORDER_ACCEPT", "POST", "orders/$id/accept", null, orderNumber) },
        )

    suspend fun reject(id: String, orderNumber: String, reason: String): ActionResult<OrderDto> =
        queueable(
            call = { api.rejectOrder(id, ReasonRequest(reason)) },
            queue = {
                outbox.enqueue("ORDER_REJECT", "POST", "orders/$id/reject", AppJson.encodeToString(ReasonRequest(reason)), orderNumber)
            },
        )

    suspend fun markReady(id: String, orderNumber: String): ActionResult<OrderDto> =
        queueable(
            call = { api.markReady(id) },
            queue = { outbox.enqueue("ORDER_READY", "POST", "orders/$id/ready", null, orderNumber) },
        )

    suspend fun confirmDelivery(id: String): ApiResult<OrderDto> = apiCall { api.confirmDelivery(id) }

    suspend fun cancel(id: String, reason: String): ApiResult<OrderDto> = apiCall { api.cancelOrder(id, ReasonRequest(reason)) }

    suspend fun paymentMethods(): ApiResult<List<PaymentMethodInfoDto>> = apiCall { api.paymentMethods() }

    suspend fun pay(orderId: String, method: String, account: String?): ApiResult<PaymentDto> =
        apiCall { api.pay(orderId, InitiatePaymentRequest(method, account?.takeIf { it.isNotBlank() })) }

    suspend fun rate(orderId: String, target: String, score: Int, comment: String?): ApiResult<RatingDto> =
        apiCall { api.rate(orderId, CreateRatingRequest(target, score, comment?.takeIf { it.isNotBlank() })) }

    suspend fun ratings(orderId: String): ApiResult<List<RatingDto>> = apiCall { api.ratingsForOrder(orderId) }

    suspend fun openDispute(orderId: String, request: OpenDisputeRequest): ApiResult<DisputeDto> =
        apiCall { api.openDispute(orderId, request) }

    suspend fun dispute(id: String): ApiResult<DisputeDto> = apiCall { api.dispute(id) }

    suspend fun disputes(orderId: String): ApiResult<List<DisputeDto>> = apiCall { api.disputesForOrder(orderId) }

    suspend fun addEvidence(disputeId: String, kind: String, fileId: String?, note: String?): ApiResult<DisputeDto> =
        apiCall { api.addEvidence(disputeId, AddEvidenceRequest(kind, fileId, note)) }

    private suspend fun <T> queueable(call: suspend () -> T, queue: suspend () -> Unit): ActionResult<T> =
        when (val result = apiCall(call)) {
            is ApiResult.Ok -> ActionResult.Done(result.value)
            is ApiResult.Err ->
                if (result.error.isNetwork) {
                    queue()
                    ActionResult.Queued
                } else {
                    ActionResult.Failed(result.error)
                }
        }
}
