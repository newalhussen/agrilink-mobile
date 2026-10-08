package com.agrilink.app.ui.orders

import com.agrilink.app.ui.common.RefreshOnResume
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agrilink.app.R
import com.agrilink.app.core.Dates
import com.agrilink.app.core.Format
import com.agrilink.app.core.Phone
import com.agrilink.app.data.api.dto.AddressDto
import com.agrilink.app.data.api.dto.DeliveryEventDto
import com.agrilink.app.data.api.dto.OrderDto
import com.agrilink.app.data.api.dto.OrderListItemDto
import com.agrilink.app.data.api.dto.PartyViewDto
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.AgriTopBar
import com.agrilink.app.ui.components.Avatar
import com.agrilink.app.ui.components.BottomActionBar
import com.agrilink.app.ui.components.ButtonKind
import com.agrilink.app.ui.components.EmptyState
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.FactRow
import com.agrilink.app.ui.components.LoadingBlock
import com.agrilink.app.ui.components.OrderStatusTag
import com.agrilink.app.ui.components.PillField
import com.agrilink.app.ui.components.QueuedBanner
import com.agrilink.app.ui.components.Segmented
import com.agrilink.app.ui.components.SkeletonList
import com.agrilink.app.ui.components.StarRating
import com.agrilink.app.ui.components.SurfaceCard
import com.agrilink.app.ui.components.Tag
import com.agrilink.app.ui.components.Tone
import com.agrilink.app.ui.components.orderStatusRes
import com.agrilink.app.ui.components.unitRes
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@Composable
private fun rememberNow(everyMillis: Long = 30_000): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(everyMillis); value = System.currentTimeMillis() }
    }
    return now
}

/** "Answer in 1:40", "Pay within 28 min", "Check window 3:12" for the timer running on an order, if any. */
@Composable
private fun deadlineLabel(status: String, farmer: String?, payment: String?, check: String?, now: Long): String? {
    val (iso, label) = when (status) {
        "PENDING" -> farmer to R.string.timer_answer_in
        "ACCEPTED", "PAYMENT_PENDING" -> payment to R.string.timer_pay_within
        "DELIVERED" -> check to R.string.timer_check_window
        else -> null to 0
    }
    val left = Dates.millisUntil(iso, java.time.Instant.ofEpochMilli(now)) ?: return null
    return stringResource(label, Dates.countdown(left))
}

// ------------------------------------------------------------------------------------------------ list

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel,
    tabs: List<Pair<OrdersTab, Int>>,
    role: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.state.collectAsState()
    val now = rememberNow()
    LaunchedEffect(Unit) { while (true) { delay(20_000); viewModel.load() } }
    RefreshOnResume(viewModel::load)
    Column(modifier.fillMaxSize().background(Organic.Bg)) {
        Text(stringResource(R.string.orders_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp))
        Segmented(tabs.map { (tab, label) -> tab to stringResource(label) }, ui.tab, viewModel::select, Modifier.padding(horizontal = 20.dp).fillMaxWidth())
        PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            when (val l = ui.orders) {
                Load.Loading -> SkeletonList(modifier = Modifier.padding(top = 16.dp))
                is Load.Failed -> ErrorState(l.error, viewModel::load)
                is Load.Ready -> if (l.value.isEmpty()) {
                    EmptyState(stringResource(R.string.orders_empty), stringResource(if (role == "BUYER") R.string.orders_empty_buyer else R.string.orders_empty_farmer))
                } else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(l.value, key = { it.id }) { order -> OrderRow(order, role, now) { onOpen(order.id) } }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: OrderListItemDto, role: String, now: Long, onClick: () -> Unit) {
    SurfaceCard(onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(order.orderNumber, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            OrderStatusTag(order.status)
        }
        Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (role == "FARMER") stringResource(R.string.order_from_buyer, order.buyerName) else stringResource(R.string.order_to_farmer, order.farmerName),
                style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(Format.etb(order.totalAmount), style = MaterialTheme.typography.titleSmall)
        }
        val timer = deadlineLabel(order.status, order.deadlines.farmerResponseDeadline, order.deadlines.paymentDeadline, order.deadlines.checkWindowEndsAt, now)
        if (timer != null) Tag(timer, Tone.Accent)
        else if ("ACCEPT" in order.allowedActions || "PAY" in order.allowedActions || "CONFIRM_DELIVERY" in order.allowedActions) {
            Tag(stringResource(R.string.order_needs_you), Tone.SolidDark)
        }
    }
}

// ------------------------------------------------------------------------------------------------ detail

@StringRes
private fun guidance(role: String, status: String): Int? = when (role) {
    "BUYER" -> when (status) {
        "PENDING" -> R.string.guide_buyer_pending
        "ACCEPTED" -> R.string.guide_buyer_accepted
        "PAYMENT_PENDING" -> R.string.guide_buyer_payment_pending
        "PAID", "READY_FOR_PICKUP" -> R.string.guide_buyer_paid
        "PICKED_UP", "IN_TRANSIT" -> R.string.guide_buyer_transit
        "DELIVERED" -> R.string.guide_buyer_delivered
        "COMPLETED" -> R.string.guide_buyer_completed
        else -> null
    }
    "FARMER" -> when (status) {
        "PENDING" -> R.string.guide_farmer_pending
        "ACCEPTED", "PAYMENT_PENDING" -> R.string.guide_farmer_accepted
        "PAID" -> R.string.guide_farmer_paid
        "READY_FOR_PICKUP" -> R.string.guide_farmer_ready
        "PICKED_UP", "IN_TRANSIT", "DELIVERED" -> R.string.guide_farmer_transit
        "COMPLETED" -> R.string.guide_farmer_completed
        else -> null
    }
    else -> null
}.let { it ?: if (status == "DISPUTED") R.string.guide_disputed else null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel,
    role: String,
    onBack: () -> Unit,
    onPay: (String) -> Unit,
    onReport: (String) -> Unit,
    onHandover: (orderId: String, kind: String) -> Unit,
    onOpenDispute: (String) -> Unit,
    onOpenListing: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val pending by viewModel.pendingActions.collectAsState(initial = 0)
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { m ->
            snackbar.showSnackbar(
                when (m) { is UiMessage.Res -> context.getString(m.id); is UiMessage.Raw -> m.text },
            )
        }
    }
    LaunchedEffect(Unit) { while (true) { delay(15_000); viewModel.refresh() } }

    var dialog by remember { mutableStateOf<OrderDialog?>(null) }
    Box(modifier.fillMaxSize().background(Organic.Bg)) {
        Column(Modifier.fillMaxSize()) {
            AgriTopBar((state as? Load.Ready)?.value?.order?.orderNumber ?: "", onBack)
            when (val s = state) {
                Load.Loading -> LoadingBlock()
                is Load.Failed -> ErrorState(s.error, viewModel::refresh)
                is Load.Ready -> {
                    val ui = s.value
                    Column(Modifier.weight(1f)) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            QueuedBanner(pending)
                            OrderBody(ui.order, role, ui.events, ui.timeline, ui.disputes.map { it.id to it.disputeNumber }, onHandover, onOpenDispute)
                            Box(Modifier.height(12.dp))
                        }
                        OrderActions(ui.order, role, ui.acting, onAction = { action ->
                            when (action) {
                                "ACCEPT" -> viewModel.accept()
                                "MARK_READY" -> viewModel.markReady()
                                "PAY" -> onPay(ui.order.id)
                                "REPORT_PROBLEM" -> onReport(ui.order.id)
                                "CONFIRM_DELIVERY" -> dialog = OrderDialog.Confirm
                                "REJECT" -> dialog = OrderDialog.Reject
                                "CANCEL" -> dialog = OrderDialog.Cancel
                                "RATE" -> dialog = OrderDialog.Rate
                                "REORDER" -> ui.order.items.firstOrNull()?.listingId?.let(onOpenListing)
                            }
                        })
                    }
                    when (dialog) {
                        OrderDialog.Reject -> ReasonDialog(stringResource(R.string.dialog_reject_title), stringResource(R.string.dialog_reason), stringResource(R.string.action_reject), { dialog = null }) { viewModel.reject(it); dialog = null }
                        OrderDialog.Cancel -> ReasonDialog(stringResource(R.string.dialog_cancel_title), stringResource(R.string.dialog_reason), stringResource(R.string.action_cancel_order), { dialog = null }) { viewModel.cancel(it); dialog = null }
                        OrderDialog.Confirm -> AlertDialog(
                            onDismissRequest = { dialog = null },
                            title = { Text(stringResource(R.string.dialog_confirm_title)) },
                            text = { Text(stringResource(R.string.dialog_confirm_body)) },
                            confirmButton = { TextButton({ viewModel.confirmDelivery(); dialog = null }) { Text(stringResource(R.string.action_confirm_delivery)) } },
                            dismissButton = { TextButton({ dialog = null }) { Text(stringResource(R.string.action_back)) } },
                        )
                        OrderDialog.Rate -> RatingDialog(ui.order, role, { dialog = null }) { ratings -> viewModel.rate(ratings) { dialog = null } }
                        null -> Unit
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp))
    }
}

private enum class OrderDialog { Reject, Cancel, Confirm, Rate }

@Composable
private fun OrderBody(
    order: OrderDto,
    role: String,
    events: List<DeliveryEventDto>,
    timeline: List<com.agrilink.app.data.api.dto.TimelineEntryDto>,
    disputes: List<Pair<String, String>>,
    onHandover: (String, String) -> Unit,
    onOpenDispute: (String) -> Unit,
) {
    val now = rememberNow(15_000)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OrderStatusTag(order.status)
        deadlineLabel(order.status, order.deadlines.farmerResponseDeadline, order.deadlines.paymentDeadline, order.deadlines.checkWindowEndsAt, now)?.let { Tag(it, Tone.Accent) }
    }
    guidance(role, order.status)?.let { g ->
        SurfaceCard(color = if (order.status == "DISPUTED") Organic.Accent100 else Organic.Sage100, padding = 16.dp) {
            Text(stringResource(g), style = MaterialTheme.typography.bodyMedium, color = if (order.status == "DISPUTED") Organic.Accent900 else Organic.Sage900)
        }
    }
    order.statusReason?.takeIf { order.status in setOf("REJECTED", "CANCELLED", "EXPIRED") }?.let {
        Text(stringResource(R.string.order_reason, it), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
    }

    if (order.status in setOf("PICKED_UP", "IN_TRANSIT") ) TrackingCard(order, events, now)

    // People
    if (role != "BUYER") PartyCard(stringResource(R.string.party_buyer), order.buyer)
    if (role != "FARMER") PartyCard(stringResource(R.string.party_farmer), order.farmer)
    order.driver?.let { if (role != "DRIVER") PartyCard(stringResource(R.string.party_driver), it) }

    // Handover codes
    val delivery = order.delivery
    if (delivery != null && role == "BUYER" && order.status in setOf("PAID", "READY_FOR_PICKUP", "PICKED_UP", "IN_TRANSIT", "DELIVERED") && delivery.deliveryCode != null) {
        SurfaceCard(color = Organic.Accent100, onClick = { onHandover(order.id, "delivery") }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.QrCode2, null, tint = Organic.Accent800, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.handover_show_delivery), style = MaterialTheme.typography.titleMedium, color = Organic.Accent900)
                    Text(stringResource(R.string.handover_delivery_hint), style = MaterialTheme.typography.bodySmall, color = Organic.Accent900)
                }
            }
        }
    }
    if (delivery != null && role == "FARMER" && order.status in setOf("PAID", "READY_FOR_PICKUP") && delivery.pickupCode != null) {
        SurfaceCard(color = Organic.Sage200, onClick = { onHandover(order.id, "pickup") }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.QrCode2, null, tint = Organic.Sage800, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.handover_show_pickup), style = MaterialTheme.typography.titleMedium, color = Organic.Sage900)
                    Text(
                        if (delivery.assignedAt != null) stringResource(R.string.handover_pickup_hint_driver, order.driver?.fullName.orEmpty()) else stringResource(R.string.handover_pickup_hint_wait),
                        style = MaterialTheme.typography.bodySmall, color = Organic.Sage900,
                    )
                }
            }
        }
    }

    // Items and price
    SurfaceCard {
        Text(stringResource(R.string.order_items), style = MaterialTheme.typography.titleMedium)
        order.items.forEach { item ->
            FactRow(
                "${item.productName} · ${Format.number(item.quantity)} ${stringResource(unitRes(item.unit)).lowercase()}",
                Format.etb(item.lineTotal),
            )
        }
        androidx.compose.material3.HorizontalDivider(color = Organic.Divider)
        FactRow(stringResource(R.string.checkout_goods), Format.etb(order.amounts.subtotal))
        FactRow(stringResource(R.string.checkout_delivery), Format.etb(order.amounts.deliveryFee))
        FactRow(stringResource(R.string.checkout_fee), Format.etb(order.amounts.platformFee))
        FactRow(stringResource(R.string.checkout_total), Format.etb(order.amounts.total), bold = true)
        if (role == "FARMER") FactRow(stringResource(R.string.order_you_receive), Format.etb(order.amounts.subtotal), bold = true, valueColor = Organic.Sage700)
        FactRow(stringResource(R.string.order_weight), Format.kg(order.totalWeightKg))
        order.payment?.let { p ->
            FactRow(stringResource(R.string.order_payment), stringResource(com.agrilink.app.ui.components.paymentMethodRes(p.method)) + " · " + p.status.lowercase().replace('_', ' '))
            p.failureReason?.takeIf { p.status == "FAILED" }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Organic.Accent700) }
        }
    }

    // Where
    SurfaceCard {
        Text(stringResource(R.string.order_delivery_to), style = MaterialTheme.typography.titleMedium)
        Text(addressText(order.deliveryAddress), style = MaterialTheme.typography.bodyMedium)
        order.deliveryContactName?.let { Text("$it · ${Phone.display(order.deliveryContactPhone)}", style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700) }
        order.buyerNotes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700) }
        if (role != "BUYER" && order.pickupAddress != null) {
            Text(stringResource(R.string.order_pickup_from), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
            Text(addressText(order.pickupAddress), style = MaterialTheme.typography.bodyMedium)
        }
        order.requestedDeliveryDate?.let { FactRow(stringResource(R.string.order_requested_date), Dates.dayLabel(it)) }
    }

    if (disputes.isNotEmpty()) {
        SurfaceCard(color = Organic.Accent100) {
            Text(stringResource(R.string.order_disputes), style = MaterialTheme.typography.titleMedium, color = Organic.Accent900)
            disputes.forEach { (id, number) ->
                Row(Modifier.fillMaxWidth().clickable { onOpenDispute(id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Flag, null, tint = Organic.Accent800, modifier = Modifier.size(18.dp))
                    Text(number, style = MaterialTheme.typography.titleSmall, color = Organic.Accent900, modifier = Modifier.padding(start = 8.dp).weight(1f))
                    Text(stringResource(R.string.action_open), style = MaterialTheme.typography.labelLarge, color = Organic.Accent700)
                }
            }
        }
    }

    if (timeline.isNotEmpty()) {
        SurfaceCard(color = Organic.Neutral100) {
            Text(stringResource(R.string.order_timeline), style = MaterialTheme.typography.titleMedium)
            timeline.forEach { t ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(Dates.dateTime(t.at), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700, modifier = Modifier.width(84.dp))
                    Column {
                        Text(stringResource(orderStatusRes(t.to)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
                        t.note?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700) }
                    }
                }
            }
        }
    }
}

private fun addressText(a: AddressDto?): String =
    listOfNotNull(a?.addressLine, a?.town, a?.zone, a?.regionName).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "—" }

private fun androidx.compose.ui.Modifier.width(dp: androidx.compose.ui.unit.Dp) = this.then(Modifier.size(width = dp, height = androidx.compose.ui.unit.Dp.Unspecified))

@Composable
private fun PartyCard(title: String, party: PartyViewDto) {
    val context = LocalContext.current
    SurfaceCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(party.displayName ?: party.fullName, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Organic.Neutral700)
                Text(party.displayName ?: party.fullName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = buildList {
                    if (party.displayName != null) add(party.fullName)
                    if (party.ratingCount > 0) add("${Format.number(party.ratingAverage, 1)} ★")
                    party.phone?.let { add(Phone.display(it)) }
                }.joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
            }
            party.phone?.let { phone ->
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Organic.Sage700).clickable { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Call, stringResource(R.string.action_call), tint = Organic.Neutral100) }
            }
        }
    }
}

/** B4 · "On the road": route progress from the driver's last GPS ping, plus an Open in Maps shortcut. */
@Composable
private fun TrackingCard(order: OrderDto, events: List<DeliveryEventDto>, now: Long) {
    val context = LocalContext.current
    val last = events.lastOrNull { it.type == "LOCATION" && it.latitude != null }
    val from = order.pickupAddress
    val to = order.deliveryAddress
    val progress: Float? = if (last != null && from?.latitude != null && to?.latitude != null) {
        val total = haversine(from.latitude, from.longitude!!, to.latitude, to.longitude!!)
        val done = haversine(from.latitude, from.longitude, last.latitude!!, last.longitude!!)
        if (total > 0.1) min(1f, max(0f, (done / total).toFloat())) else null
    } else null
    SurfaceCard(color = Organic.Neutral100) {
        Text(stringResource(R.string.tracking_title), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(from?.town ?: stringResource(R.string.tracking_pickup), style = MaterialTheme.typography.labelLarge)
            Text(to?.town ?: stringResource(R.string.tracking_dropoff), style = MaterialTheme.typography.labelLarge)
        }
        if (progress != null) LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(10.dp).clip(CircleShape), color = Organic.Accent, trackColor = Organic.Neutral300)
        else LinearProgressIndicator(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape), color = Organic.Accent, trackColor = Organic.Neutral300)
        if (last != null) {
            Text(stringResource(R.string.tracking_updated, Dates.ago(last.at, java.time.Instant.ofEpochMilli(now))), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
            AgriButton(stringResource(R.string.tracking_open_maps), {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${last.latitude},${last.longitude}?q=${last.latitude},${last.longitude}")))
            }, kind = ButtonKind.Secondary, icon = Icons.Filled.Map, height = 44)
        } else {
            Text(stringResource(R.string.tracking_waiting), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
        }
        order.delivery?.pickedUpWeightKg?.let { Text(stringResource(R.string.tracking_weighed, Format.kg(it)) + (order.delivery.pickedUpCrateCount?.let { c -> " · " + stringResource(R.string.tracking_crates, c) } ?: ""), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700) }
    }
}

private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2).let { it * it } + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).let { it * it }
    return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

@Composable
private fun OrderActions(order: OrderDto, role: String, acting: Boolean, onAction: (String) -> Unit) {
    val actions = order.allowedActions
    val primary = listOf("ACCEPT", "PAY", "MARK_READY", "CONFIRM_DELIVERY", "RATE").firstOrNull { it in actions }
    val reorder = if (role == "BUYER" && order.status == "COMPLETED" && order.items.isNotEmpty()) "REORDER" else null
    val secondary = listOf("REJECT", "REPORT_PROBLEM", "CANCEL").filter { it in actions }
    if (primary == null && secondary.isEmpty() && reorder == null) return
    BottomActionBar {
        val main = primary ?: reorder
        if (main != null) {
            AgriButton(
                stringResource(
                    when (main) {
                        "ACCEPT" -> R.string.action_accept_order
                        "PAY" -> R.string.action_pay_now
                        "MARK_READY" -> R.string.action_mark_ready
                        "CONFIRM_DELIVERY" -> R.string.action_confirm_delivery
                        "RATE" -> R.string.action_rate
                        else -> R.string.market_reorder
                    },
                ),
                { onAction(main) }, Modifier.fillMaxWidth(), height = 56, loading = acting, kind = if (main == "CONFIRM_DELIVERY") ButtonKind.Sage else ButtonKind.Primary,
            )
        }
        if (secondary.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                secondary.forEach { a ->
                    AgriButton(
                        stringResource(when (a) { "REJECT" -> R.string.action_reject; "REPORT_PROBLEM" -> R.string.action_report_problem; else -> R.string.action_cancel_order }),
                        { onAction(a) }, Modifier.weight(1f), kind = if (a == "REPORT_PROBLEM") ButtonKind.Secondary else ButtonKind.Ghost, height = 46, enabled = !acting,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasonDialog(title: String, label: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { PillField(reason, { reason = it }, label, singleLine = false, minLines = 3) },
        confirmButton = { TextButton({ onConfirm(reason.trim()) }, enabled = reason.trim().length >= 3) { Text(confirm) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.action_back)) } },
    )
}

@Composable
private fun RatingDialog(order: OrderDto, role: String, onDismiss: () -> Unit, onSubmit: (List<Triple<String, Int, String?>>) -> Unit) {
    val targets: List<Pair<String, Int>> = when (role) {
        "BUYER" -> listOf("FARMER" to R.string.rate_farmer, "DELIVERY" to R.string.rate_delivery, "ORDER" to R.string.rate_order)
        "FARMER" -> listOf("BUYER" to R.string.rate_buyer, "DRIVER" to R.string.rate_driver)
        else -> listOf("BUYER" to R.string.rate_buyer, "FARMER" to R.string.rate_farmer)
    }.filter { (t, _) -> !(t == "DELIVERY" || t == "DRIVER") || order.driver != null }
    val scores = remember { mutableStateOf(targets.associate { it.first to 0 }) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rate_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                targets.forEach { (target, label) ->
                    Column {
                        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
                        StarRating(scores.value[target] ?: 0, { v -> scores.value = scores.value + (target to v) }, size = 34)
                    }
                }
                PillField(comment, { comment = it }, stringResource(R.string.rate_comment), singleLine = false, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(
                { onSubmit(targets.map { (t, _) -> Triple(t, scores.value[t] ?: 0, comment.takeIf { it.isNotBlank() }) }) },
                enabled = scores.value.values.any { it > 0 },
            ) { Text(stringResource(R.string.rate_submit)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.action_back)) } },
    )
}
