package com.agrilink.app.ui.orders

import com.agrilink.app.ui.components.unitRes
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrilink.app.R
import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.AppError
import com.agrilink.app.core.Dates
import com.agrilink.app.core.Format
import com.agrilink.app.data.api.dto.OpenDisputeRequest
import com.agrilink.app.data.api.dto.OrderDto
import com.agrilink.app.data.api.dto.PaymentMethodInfoDto
import com.agrilink.app.data.prefs.SessionStore
import com.agrilink.app.data.repo.OrderRepository
import com.agrilink.app.data.repo.ProfileRepository
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.common.errorText
import com.agrilink.app.ui.common.rememberPhotoPicker
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.AgriImage
import com.agrilink.app.ui.components.AgriTopBar
import com.agrilink.app.ui.components.BigCode
import com.agrilink.app.ui.components.BottomActionBar
import com.agrilink.app.ui.components.ButtonKind
import com.agrilink.app.ui.components.ChoiceCard
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.FactRow
import com.agrilink.app.ui.components.LoadingBlock
import com.agrilink.app.ui.components.PillField
import com.agrilink.app.ui.components.QrCodeImage
import com.agrilink.app.ui.components.SurfaceCard
import com.agrilink.app.ui.components.paymentMethodRes
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ------------------------------------------------------------------------------------------------ payment

data class PayUi(
    val order: Load<OrderDto> = Load.Loading,
    val methods: List<PaymentMethodInfoDto> = emptyList(),
    val method: String = "TELEBIRR",
    val account: String = "",
    val paying: Boolean = false,
    val waiting: Boolean = false,
    val paid: Boolean = false,
    val failure: String? = null,
    val error: AppError? = null,
)

/** B3 · Pay: "AgriLink holds your money". Handles instant success, failure (order stays open) and async confirmation. */
class PayViewModel(
    private val orderId: String,
    private val orders: OrderRepository,
    private val session: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(PayUi(account = session.user?.phone?.removePrefix("+251")?.let { "0$it" }.orEmpty()))
    val state: StateFlow<PayUi> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(order = Load.Loading, error = null) }
        viewModelScope.launch {
            val order = orders.get(orderId)
            val methods = (orders.paymentMethods() as? ApiResult.Ok)?.value.orEmpty()
            _state.update {
                it.copy(
                    order = when (order) { is ApiResult.Ok -> Load.Ready(order.value); is ApiResult.Err -> Load.Failed(order.error) },
                    methods = methods.ifEmpty { listOf(PaymentMethodInfoDto("TELEBIRR", "telebirr"), PaymentMethodInfoDto("CBE_BIRR", "CBE Birr")) },
                )
            }
        }
    }

    fun setMethod(m: String) = _state.update { it.copy(method = m, failure = null, error = null) }
    fun setAccount(a: String) = _state.update { it.copy(account = a.filter { c -> c.isDigit() }, failure = null, error = null) }

    fun pay() {
        val s = _state.value
        if (s.paying) return
        _state.update { it.copy(paying = true, failure = null, error = null) }
        viewModelScope.launch {
            when (val r = orders.pay(orderId, s.method, s.account)) {
                is ApiResult.Err -> _state.update { it.copy(paying = false, error = r.error) }
                is ApiResult.Ok -> when (r.value.status) {
                    "HELD", "RELEASED" -> _state.update { it.copy(paying = false, paid = true) }
                    "FAILED" -> { _state.update { it.copy(paying = false, failure = r.value.failureReason ?: "") }; load() }
                    else -> { _state.update { it.copy(paying = false, waiting = true) }; pollUntilSettled() }
                }
            }
        }
    }

    /** Mobile-money confirmation can arrive a little later: watch the order until it is PAID (or falls back to ACCEPTED). */
    private suspend fun pollUntilSettled() {
        repeat(40) {
            delay(3000)
            val order = (orders.get(orderId) as? ApiResult.Ok)?.value ?: return@repeat
            when (order.status) {
                "PAID" -> { _state.update { it.copy(waiting = false, paid = true) }; return }
                "ACCEPTED" -> { _state.update { it.copy(waiting = false, failure = order.payment?.failureReason ?: "") }; load(); return }
            }
        }
        _state.update { it.copy(waiting = false) }
    }
}

@Composable
fun PayScreen(viewModel: PayViewModel, onBack: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.state.collectAsState()
    Column(modifier.fillMaxSize().background(Organic.Bg).imePadding()) {
        AgriTopBar(stringResource(R.string.pay_title), if (ui.paid) null else onBack)
        when {
            ui.paid -> PaidContent(onDone)
            ui.waiting -> Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = Organic.Accent)
                Text(stringResource(R.string.pay_waiting_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp))
                Text(stringResource(R.string.pay_waiting_body), style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700, modifier = Modifier.padding(top = 8.dp))
            }
            else -> when (val o = ui.order) {
                Load.Loading -> LoadingBlock()
                is Load.Failed -> ErrorState(o.error, viewModel::load)
                is Load.Ready -> PayForm(o.value, ui, viewModel, onBack)
            }
        }
    }
}

@Composable
private fun PayForm(order: OrderDto, ui: PayUi, vm: PayViewModel, onBack: () -> Unit) {
    val now = System.currentTimeMillis()
    val keepFor = Dates.millisUntil(order.deadlines.paymentDeadline)?.let { Dates.countdown(it) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Lock, null, tint = Organic.Sage700)
                Text(stringResource(R.string.pay_held_title), style = MaterialTheme.typography.titleLarge)
            }
            Text(stringResource(R.string.pay_held_note), style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral800)
            SurfaceCard {
                Text(order.items.map { "${it.productName} · ${Format.number(it.quantity)} ${stringResource(unitRes(it.unit))}" }.joinToString(", "), style = MaterialTheme.typography.titleSmall)
                FactRow(stringResource(R.string.checkout_goods), Format.etb(order.amounts.subtotal))
                FactRow(stringResource(R.string.checkout_delivery), Format.etb(order.amounts.deliveryFee))
                FactRow(stringResource(R.string.checkout_fee), Format.etb(order.amounts.platformFee))
                FactRow(stringResource(R.string.checkout_total), Format.etb(order.amounts.total), bold = true)
            }
            if (ui.failure != null) {
                SurfaceCard(color = Organic.Accent100) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.ReportProblem, null, tint = Organic.Accent800)
                        Text(stringResource(R.string.pay_failed_title, stringResource(paymentMethodRes(ui.method))), style = MaterialTheme.typography.titleMedium, color = Organic.Accent900)
                    }
                    Text(stringResource(R.string.pay_failed_body) + (keepFor?.let { " " + stringResource(R.string.pay_failed_kept, it) } ?: ""), style = MaterialTheme.typography.bodyMedium, color = Organic.Accent900)
                    if (ui.failure.isNotBlank()) Text(ui.failure, style = MaterialTheme.typography.bodySmall, color = Organic.Accent800)
                }
            }
            Text(stringResource(R.string.pay_with), style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ui.methods.forEach { m ->
                    ChoiceCard(
                        title = stringResource(paymentMethodRes(m.method)), selected = ui.method == m.method,
                        shape = RoundedCornerShape(24.dp), onClick = { vm.setMethod(m.method) },
                    )
                }
            }
            PillField(
                ui.account, vm::setAccount,
                stringResource(if (ui.method == "TELEBIRR" || ui.method == "CBE_BIRR") R.string.pay_account_phone else R.string.pay_account_number),
                keyboardType = KeyboardType.Phone, hint = stringResource(R.string.pay_test_hint),
            )
            ui.error?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium) }
            Box(Modifier.height(8.dp))
        }
        BottomActionBar {
            AgriButton(stringResource(R.string.pay_button, Format.etb(order.amounts.total)), vm::pay, Modifier.fillMaxWidth(), loading = ui.paying, enabled = ui.account.length >= 5, height = 56)
            AgriButton(stringResource(R.string.action_back), onBack, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost, height = 44)
        }
    }
}

@Composable
private fun PaidContent(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(Organic.Sage200), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.CheckCircle, null, tint = Organic.Sage700, modifier = Modifier.size(56.dp))
        }
        Text(stringResource(R.string.pay_done_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 20.dp))
        Text(stringResource(R.string.pay_done_body), style = MaterialTheme.typography.bodyLarge, color = Organic.Neutral800, modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))
        AgriButton(stringResource(R.string.pay_done_action), onDone, Modifier.fillMaxWidth(), height = 56)
    }
}

// ------------------------------------------------------------------------------------------------ report a problem

val disputeTypes = listOf(
    "LESS_THAN_ORDERED" to R.string.dispute_less,
    "POOR_QUALITY" to R.string.dispute_quality,
    "DAMAGED" to R.string.dispute_damaged,
    "WRONG_PRODUCT" to R.string.dispute_wrong,
    "NOT_DELIVERED" to R.string.dispute_not_delivered,
    "OTHER" to R.string.dispute_other,
)

data class ReportUi(
    val order: Load<OrderDto> = Load.Loading,
    val type: String = "LESS_THAN_ORDERED",
    val receivedKg: String = "",
    val description: String = "",
    val photos: List<Uri> = emptyList(),
    val busy: Boolean = false,
    val error: AppError? = null,
    val done: Boolean = false,
)

/** B6 · Report a problem: what happened, how much arrived, photos. The money stays held until an agent decides. */
class ReportViewModel(
    private val orderId: String,
    private val orders: OrderRepository,
    private val profile: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ReportUi())
    val state: StateFlow<ReportUi> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(order = Load.Loading) }
            _state.update { s ->
                s.copy(order = when (val r = orders.get(orderId)) { is ApiResult.Ok -> Load.Ready(r.value); is ApiResult.Err -> Load.Failed(r.error) })
            }
        }
    }

    fun edit(block: (ReportUi) -> ReportUi) = _state.update { block(it).copy(error = null) }
    fun addPhoto(uri: Uri) = edit { if (it.photos.size < 4) it.copy(photos = it.photos + uri) else it }

    fun submit() {
        val s = _state.value
        if (s.busy || s.description.trim().length < 3) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val ids = mutableListOf<String>()
            for (uri in s.photos) {
                when (val up = profile.uploadImage(uri, "DISPUTE_EVIDENCE")) {
                    is ApiResult.Ok -> ids += up.value.id
                    is ApiResult.Err -> { _state.update { it.copy(busy = false, error = up.error) }; return@launch }
                }
            }
            val request = OpenDisputeRequest(
                type = s.type, description = s.description.trim(),
                receivedQuantityKg = s.receivedKg.toDoubleOrNull().takeIf { s.type == "LESS_THAN_ORDERED" },
                evidenceFileIds = ids.takeIf { it.isNotEmpty() },
            )
            when (val r = orders.openDispute(orderId, request)) {
                is ApiResult.Ok -> _state.update { it.copy(busy = false, done = true) }
                is ApiResult.Err -> _state.update { it.copy(busy = false, error = r.error) }
            }
        }
    }
}

@Composable
fun ReportScreen(viewModel: ReportViewModel, onBack: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.state.collectAsState()
    val pick = rememberPhotoPicker(viewModel::addPhoto)
    Column(modifier.fillMaxSize().background(Organic.Bg).imePadding()) {
        AgriTopBar(stringResource(R.string.report_title), onBack)
        if (ui.done) {
            Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.CheckCircle, null, tint = Organic.Sage700, modifier = Modifier.size(64.dp))
                Text(stringResource(R.string.report_sent_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
                Text(stringResource(R.string.report_sent_body), style = MaterialTheme.typography.bodyLarge, color = Organic.Neutral800, modifier = Modifier.padding(vertical = 10.dp))
                AgriButton(stringResource(R.string.action_done), onDone, Modifier.fillMaxWidth(), height = 56)
            }
            return@Column
        }
        when (val o = ui.order) {
            Load.Loading -> LoadingBlock()
            is Load.Failed -> ErrorState(o.error, viewModel::load)
            is Load.Ready -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.report_what), style = MaterialTheme.typography.titleLarge)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    disputeTypes.forEach { (type, label) ->
                        ChoiceCard(stringResource(label), selected = ui.type == type, shape = RoundedCornerShape(percent = 50), onClick = { viewModel.edit { it.copy(type = type) } })
                    }
                }
                if (ui.type == "LESS_THAN_ORDERED") {
                    PillField(
                        ui.receivedKg, { v -> viewModel.edit { it.copy(receivedKg = v) } }, stringResource(R.string.report_received, Format.kg(o.value.totalWeightKg)),
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                PillField(ui.description, { v -> viewModel.edit { it.copy(description = v) } }, stringResource(R.string.report_describe), singleLine = false, minLines = 3)
                Text(stringResource(R.string.report_photos), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ui.photos.forEach { uri ->
                        androidx.compose.foundation.Image(
                            painter = coil3.compose.rememberAsyncImagePainter(uri), contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)),
                        )
                    }
                    if (ui.photos.size < 4) Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(Organic.Surface).then(Modifier), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.IconButton(pick) { Icon(Icons.Filled.AddAPhoto, stringResource(R.string.action_add), tint = Organic.Neutral700) }
                    }
                }
                SurfaceCard(color = Organic.Sage100, padding = 14.dp) {
                    Text(stringResource(R.string.report_money_held, Format.etb(o.value.amounts.total)), style = MaterialTheme.typography.bodyMedium, color = Organic.Sage900)
                }
                ui.error?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium) }
                Box(Modifier.height(8.dp))
            }
        }
        if (ui.order is Load.Ready) BottomActionBar {
            AgriButton(stringResource(R.string.report_send), viewModel::submit, Modifier.fillMaxWidth(), loading = ui.busy, enabled = ui.description.trim().length >= 3, height = 56)
        }
    }
}

// ------------------------------------------------------------------------------------------------ handover codes

/**
 * B5 / C3 · The code a person shows at a handover. Buyer: "Check first, then show this code". Farmer: "Show this code
 * to the driver". Also available as a QR token the driver can scan.
 */
@Composable
fun HandoverScreen(orderState: Load<OrderDto>, kind: String, onBack: () -> Unit, onRetry: () -> Unit, onReport: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(Organic.Bg)) {
        AgriTopBar(stringResource(if (kind == "delivery") R.string.handover_delivery_title else R.string.handover_pickup_title), onBack)
        when (orderState) {
            Load.Loading -> LoadingBlock()
            is Load.Failed -> ErrorState(orderState.error, onRetry)
            is Load.Ready -> {
                val order = orderState.value
                val d = order.delivery
                val code = if (kind == "delivery") d?.deliveryCode else d?.pickupCode
                val qr = if (kind == "delivery") d?.deliveryQrToken else d?.pickupQrToken
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(order.orderNumber, style = MaterialTheme.typography.titleMedium, color = Organic.Neutral700)
                    Text(stringResource(if (kind == "delivery") R.string.handover_delivery_head else R.string.handover_pickup_head), style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(
                        stringResource(if (kind == "delivery") R.string.handover_delivery_body else R.string.handover_pickup_body),
                        style = MaterialTheme.typography.bodyLarge, color = Organic.Neutral800, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    if (code == null) {
                        Text(stringResource(R.string.handover_no_code), style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700)
                    } else {
                        BigCode(code)
                        if (qr != null) QrCodeImage(qr, size = 220.dp)
                        Text(stringResource(R.string.handover_read_aloud), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
                    }
                    if (kind == "pickup") {
                        order.driver?.let {
                            SurfaceCard { FactRow(stringResource(R.string.party_driver), it.fullName); it.phone?.let { p -> FactRow(stringResource(R.string.order_phone), com.agrilink.app.core.Phone.display(p)) } }
                        }
                        SurfaceCard(color = Organic.Neutral100) { Text(stringResource(R.string.handover_pickup_tips), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                if (kind == "delivery" && onReport != null) {
                    BottomActionBar { AgriButton(stringResource(R.string.handover_something_wrong), onReport, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, height = 52) }
                }
            }
        }
    }
}
