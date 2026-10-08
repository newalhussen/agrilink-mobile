package com.agrilink.app.ui.farmer

import com.agrilink.app.ui.common.RefreshOnResume
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrilink.app.R
import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.Dates
import com.agrilink.app.core.Format
import com.agrilink.app.data.api.dto.OrderListItemDto
import com.agrilink.app.data.api.dto.WalletDto
import com.agrilink.app.data.prefs.SessionStore
import com.agrilink.app.data.repo.ActionResult
import com.agrilink.app.data.repo.OrderRepository
import com.agrilink.app.data.repo.Outbox
import com.agrilink.app.data.repo.WalletRepository
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.orders.UiMessage
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.ButtonKind
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.QueuedBanner
import com.agrilink.app.ui.components.SkeletonList
import com.agrilink.app.ui.components.SurfaceCard
import com.agrilink.app.ui.components.Tag
import com.agrilink.app.ui.components.Tone
import com.agrilink.app.ui.components.VerificationTag
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayUi(
    val wallet: WalletDto? = null,
    val newOrders: List<OrderListItemDto> = emptyList(),
    val comingUp: List<OrderListItemDto> = emptyList(),
    val busyOrder: String? = null,
    val refreshing: Boolean = false,
)

/** C1 · Today: money held and ready to withdraw, orders waiting for an answer, and what is being picked up. */
class FarmerTodayViewModel(
    private val orders: OrderRepository,
    private val wallet: WalletRepository,
    private val session: SessionStore,
    outbox: Outbox,
) : ViewModel() {
    private val _state = MutableStateFlow<Load<TodayUi>>(Load.Loading)
    val state: StateFlow<Load<TodayUi>> = _state.asStateFlow()
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()
    val pending = outbox.pendingCount
    val userName: String get() = session.user?.fullName.orEmpty()
    val verification: String get() = session.user?.verificationStatus ?: "UNVERIFIED"

    init { load() }

    fun refresh() {
        _state.update { if (it is Load.Ready) Load.Ready(it.value.copy(refreshing = true)) else it }
        load()
    }

    fun load() {
        viewModelScope.launch {
            val newOrders = orders.list(listOf("PENDING"))
            val upcoming = orders.list(listOf("PAID", "READY_FOR_PICKUP", "PICKED_UP", "IN_TRANSIT"))
            val w = (wallet.wallet() as? ApiResult.Ok)?.value
            if (newOrders is ApiResult.Err && _state.value !is Load.Ready) { _state.value = Load.Failed(newOrders.error); return@launch }
            _state.value = Load.Ready(
                TodayUi(
                    wallet = w ?: (_state.value as? Load.Ready)?.value?.wallet,
                    newOrders = (newOrders as? ApiResult.Ok)?.value?.items ?: (_state.value as? Load.Ready)?.value?.newOrders.orEmpty(),
                    comingUp = (upcoming as? ApiResult.Ok)?.value?.items ?: (_state.value as? Load.Ready)?.value?.comingUp.orEmpty(),
                ),
            )
        }
    }

    private fun answer(order: OrderListItemDto, call: suspend () -> ActionResult<*>) {
        _state.update { if (it is Load.Ready) Load.Ready(it.value.copy(busyOrder = order.id)) else it }
        viewModelScope.launch {
            when (val r = call()) {
                is ActionResult.Done -> _messages.tryEmit(UiMessage.Res(R.string.order_accepted_done))
                ActionResult.Queued -> {
                    _messages.tryEmit(UiMessage.Res(R.string.queued_saved))
                    // Hide it from "new" right away; the outbox will send the answer when there is signal.
                    _state.update { if (it is Load.Ready) Load.Ready(it.value.copy(newOrders = it.value.newOrders.filter { o -> o.id != order.id }, busyOrder = null)) else it }
                    return@launch
                }
                is ActionResult.Failed -> _messages.tryEmit(UiMessage.Raw(r.error.message))
            }
            _state.update { if (it is Load.Ready) Load.Ready(it.value.copy(busyOrder = null)) else it }
            load()
        }
    }

    fun accept(order: OrderListItemDto) = answer(order) { orders.accept(order.id, order.orderNumber) }
    fun reject(order: OrderListItemDto) = answer(order) { orders.reject(order.id, order.orderNumber, "Cannot supply this order") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerTodayScreen(
    viewModel: FarmerTodayViewModel,
    unreadNotifications: Int,
    onOpenOrder: (String) -> Unit,
    onNotifications: () -> Unit,
    onVerification: () -> Unit,
    onWallet: () -> Unit,
    onAddProduce: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val pending by viewModel.pending.collectAsState(initial = 0)
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) { while (true) { delay(30_000); value = System.currentTimeMillis() } }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { m -> snackbar.showSnackbar(when (m) { is UiMessage.Res -> context.getString(m.id); is UiMessage.Raw -> m.text }) }
    }
    LaunchedEffect(Unit) { while (true) { delay(20_000); viewModel.load() } }
    RefreshOnResume(viewModel::load)

    Box(modifier.fillMaxSize().background(Organic.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.farmer_greeting, viewModel.userName.substringBefore(' ')), style = MaterialTheme.typography.headlineMedium)
                }
                Box(Modifier.size(44.dp).clip(CircleShape).background(Organic.Surface).clickable(onClick = onNotifications), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.NotificationsNone, stringResource(R.string.tab_alerts))
                    if (unreadNotifications > 0) Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(9.dp).clip(CircleShape).background(Organic.Accent))
                }
            }
            PullToRefreshBox(isRefreshing = (state as? Load.Ready)?.value?.refreshing == true, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
                when (val s = state) {
                    Load.Loading -> SkeletonList()
                    is Load.Failed -> ErrorState(s.error, viewModel::load)
                    is Load.Ready -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        item { QueuedBanner(pending) }
                        if (viewModel.verification != "VERIFIED") item {
                            SurfaceCard(color = Organic.Accent100, onClick = onVerification) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(R.string.verification_almost), style = MaterialTheme.typography.titleMedium, color = Organic.Accent900)
                                        Text(stringResource(R.string.verification_sub_farmer), style = MaterialTheme.typography.bodySmall, color = Organic.Accent900)
                                    }
                                    VerificationTag(viewModel.verification)
                                }
                            }
                        }
                        item { MoneyCard(s.value.wallet, onWallet) }
                        if (s.value.newOrders.isNotEmpty()) {
                            item { Text(stringResource(R.string.today_new_orders), style = MaterialTheme.typography.titleLarge) }
                            items(s.value.newOrders.size) { i ->
                                val order = s.value.newOrders[i]
                                NewOrderCard(order, now, busy = s.value.busyOrder == order.id, onOpen = { onOpenOrder(order.id) }, onAccept = { viewModel.accept(order) }, onReject = { viewModel.reject(order) })
                            }
                        }
                        item { Text(stringResource(R.string.today_coming_up), style = MaterialTheme.typography.titleLarge) }
                        if (s.value.comingUp.isEmpty()) {
                            item {
                                SurfaceCard {
                                    Text(stringResource(R.string.today_nothing), style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700)
                                    AgriButton(stringResource(R.string.produce_add), onAddProduce, kind = ButtonKind.Secondary, height = 44)
                                }
                            }
                        } else items(s.value.comingUp.size) { i -> UpcomingCard(s.value.comingUp[i]) { onOpenOrder(s.value.comingUp[i].id) } }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}

@Composable
private fun MoneyCard(wallet: WalletDto?, onWallet: () -> Unit) {
    SurfaceCard(color = Organic.Sage200, onClick = onWallet) {
        Text(stringResource(R.string.today_held), style = MaterialTheme.typography.labelMedium, color = Organic.Sage900)
        Text(Format.etb(wallet?.heldForRelease ?: 0.0), style = MaterialTheme.typography.displaySmall, color = Organic.Sage900)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.today_ready_withdraw), style = MaterialTheme.typography.labelMedium, color = Organic.Sage900)
                Text(Format.etb(wallet?.balance ?: 0.0), style = MaterialTheme.typography.titleLarge, color = Organic.Sage900)
            }
            Text(stringResource(R.string.wallet_withdraw), style = MaterialTheme.typography.labelLarge, color = Organic.Sage800)
        }
    }
}

@Composable
private fun NewOrderCard(order: OrderListItemDto, now: Long, busy: Boolean, onOpen: () -> Unit, onAccept: () -> Unit, onReject: () -> Unit) {
    val left = Dates.millisUntil(order.deadlines.farmerResponseDeadline, java.time.Instant.ofEpochMilli(now))
    SurfaceCard(color = Organic.Accent100, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.today_answer_in, left?.let { Dates.countdown(it) } ?: "—"), style = MaterialTheme.typography.labelLarge, color = Organic.Accent800, modifier = Modifier.weight(1f))
            Text(order.orderNumber, style = MaterialTheme.typography.labelMedium, color = Organic.Accent800)
        }
        Text(order.itemsSummary, style = MaterialTheme.typography.titleMedium, color = Organic.Accent900, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("${Format.etb(order.totalAmount)} · ${order.buyerName}${order.deliveryTown?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.bodyMedium, color = Organic.Accent900)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AgriButton(stringResource(R.string.action_accept), onAccept, Modifier.weight(1f), loading = busy, height = 48)
            AgriButton(stringResource(R.string.action_reject), onReject, Modifier.weight(1f), kind = ButtonKind.Secondary, height = 48, enabled = !busy)
        }
    }
}

@Composable
private fun UpcomingCard(order: OrderListItemDto, onClick: () -> Unit) {
    SurfaceCard(onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(order.itemsSummary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            com.agrilink.app.ui.components.OrderStatusTag(order.status)
        }
        Text("${order.orderNumber} · ${order.buyerName}${order.driverName?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
        if (order.status in setOf("PAID", "READY_FOR_PICKUP")) Tag(stringResource(R.string.today_prepare), Tone.Sage)
    }
}

