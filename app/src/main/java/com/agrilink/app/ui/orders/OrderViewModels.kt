package com.agrilink.app.ui.orders

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrilink.app.R
import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.AppError
import com.agrilink.app.data.api.dto.DeliveryEventDto
import com.agrilink.app.data.api.dto.DisputeDto
import com.agrilink.app.data.api.dto.OrderDto
import com.agrilink.app.data.api.dto.OrderListItemDto
import com.agrilink.app.data.api.dto.TimelineEntryDto
import com.agrilink.app.data.repo.ActionResult
import com.agrilink.app.data.repo.DeliveryRepository
import com.agrilink.app.data.repo.OrderRepository
import com.agrilink.app.data.repo.Outbox
import com.agrilink.app.ui.common.Load
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-shot message for a snackbar: a string resource, or text the server sent. */
sealed interface UiMessage {
    data class Res(@StringRes val id: Int) : UiMessage
    data class Raw(val text: String) : UiMessage
}

val TERMINAL_STATUSES = listOf("COMPLETED", "REJECTED", "CANCELLED", "EXPIRED")
val ACTIVE_STATUSES = listOf("PENDING", "ACCEPTED", "PAYMENT_PENDING", "PAID", "READY_FOR_PICKUP", "PICKED_UP", "IN_TRANSIT", "DELIVERED", "DISPUTED")

enum class OrdersTab(val statuses: List<String>) {
    NEW(listOf("PENDING")),
    ACTIVE(ACTIVE_STATUSES),
    ACTIVE_NOT_NEW(ACTIVE_STATUSES.filter { it != "PENDING" }),
    DONE(TERMINAL_STATUSES),
}

data class OrdersUi(
    val tab: OrdersTab,
    val orders: Load<List<OrderListItemDto>> = Load.Loading,
    val refreshing: Boolean = false,
)

/** The signed-in user's orders (buyer or farmer), filtered by stage. */
class OrdersViewModel(private val repo: OrderRepository, initial: OrdersTab) : ViewModel() {
    private val _state = MutableStateFlow(OrdersUi(initial))
    val state: StateFlow<OrdersUi> = _state.asStateFlow()

    init { load() }

    fun select(tab: OrdersTab) {
        _state.update { it.copy(tab = tab, orders = Load.Loading) }
        load()
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        load()
    }

    fun load() {
        val tab = _state.value.tab
        viewModelScope.launch {
            when (val r = repo.list(tab.statuses)) {
                is ApiResult.Ok -> _state.update { if (it.tab == tab) it.copy(orders = Load.Ready(r.value.items), refreshing = false) else it }
                is ApiResult.Err -> _state.update { if (it.tab == tab) it.copy(orders = if (it.orders is Load.Ready) it.orders else Load.Failed(r.error), refreshing = false) else it }
            }
        }
    }
}

data class OrderDetailUi(
    val order: OrderDto,
    val timeline: List<TimelineEntryDto> = emptyList(),
    val events: List<DeliveryEventDto> = emptyList(),
    val disputes: List<DisputeDto> = emptyList(),
    val acting: Boolean = false,
)

class OrderDetailViewModel(
    private val orderId: String,
    private val repo: OrderRepository,
    private val delivery: DeliveryRepository,
    outbox: Outbox,
) : ViewModel() {

    private val _state = MutableStateFlow<Load<OrderDetailUi>>(Load.Loading)
    val state: StateFlow<Load<OrderDetailUi>> = _state.asStateFlow()
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()
    val pendingActions = outbox.pendingCount

    init { refresh() }

    /** Safe to call often (polling): keeps the current content on screen while it refreshes. */
    fun refresh() {
        viewModelScope.launch {
            when (val r = repo.get(orderId)) {
                is ApiResult.Err -> if (_state.value !is Load.Ready) _state.value = Load.Failed(r.error)
                is ApiResult.Ok -> {
                    val order = r.value
                    val previous = (_state.value as? Load.Ready)?.value
                    _state.value = Load.Ready((previous ?: OrderDetailUi(order)).copy(order = order, acting = false))
                    launch { (repo.timeline(orderId) as? ApiResult.Ok)?.let { t -> edit { it.copy(timeline = t.value) } } }
                    launch { (repo.disputes(orderId) as? ApiResult.Ok)?.let { d -> edit { it.copy(disputes = d.value) } } }
                    if (order.status in setOf("PICKED_UP", "IN_TRANSIT", "DELIVERED") && order.delivery != null) {
                        launch { (delivery.events(order.delivery.id) as? ApiResult.Ok)?.let { e -> edit { it.copy(events = e.value) } } }
                    }
                }
            }
        }
    }

    private fun edit(block: (OrderDetailUi) -> OrderDetailUi) = _state.update { if (it is Load.Ready) Load.Ready(block(it.value)) else it }

    private fun act(call: suspend () -> ActionResult<OrderDto>, doneMessage: Int) {
        edit { it.copy(acting = true) }
        viewModelScope.launch {
            when (val r = call()) {
                is ActionResult.Done -> { _messages.tryEmit(UiMessage.Res(doneMessage)); refresh() }
                ActionResult.Queued -> { _messages.tryEmit(UiMessage.Res(R.string.queued_saved)); edit { it.copy(acting = false) } }
                is ActionResult.Failed -> { _messages.tryEmit(UiMessage.Raw(r.error.message)); edit { it.copy(acting = false) } }
            }
        }
    }

    private fun actApi(call: suspend () -> ApiResult<OrderDto>, doneMessage: Int) = act({
        when (val r = call()) {
            is ApiResult.Ok -> ActionResult.Done(r.value)
            is ApiResult.Err -> ActionResult.Failed(r.error)
        }
    }, doneMessage)

    private val number: String get() = (_state.value as? Load.Ready)?.value?.order?.orderNumber.orEmpty()

    fun accept() = act({ repo.accept(orderId, number) }, R.string.order_accepted_done)
    fun reject(reason: String) = act({ repo.reject(orderId, number, reason) }, R.string.order_rejected_done)
    fun markReady() = act({ repo.markReady(orderId, number) }, R.string.order_ready_done)
    fun confirmDelivery() = actApi({ repo.confirmDelivery(orderId) }, R.string.order_confirmed_done)
    fun cancel(reason: String) = actApi({ repo.cancel(orderId, reason) }, R.string.order_cancelled_done)

    fun rate(ratings: List<Triple<String, Int, String?>>, onDone: () -> Unit) {
        viewModelScope.launch {
            var failure: AppError? = null
            for ((target, score, comment) in ratings) {
                if (score < 1) continue
                val r = repo.rate(orderId, target, score, comment)
                if (r is ApiResult.Err && r.error.code != "CONFLICT") { failure = r.error; break }
            }
            if (failure != null) _messages.tryEmit(UiMessage.Raw(failure.message)) else { _messages.tryEmit(UiMessage.Res(R.string.rating_thanks)); onDone() }
            refresh()
        }
    }
}
