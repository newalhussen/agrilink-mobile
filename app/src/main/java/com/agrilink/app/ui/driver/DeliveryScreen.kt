package com.agrilink.app.ui.driver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrilink.app.R
import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.AppError
import com.agrilink.app.core.Dates
import com.agrilink.app.core.Format
import com.agrilink.app.core.Phone
import com.agrilink.app.data.api.dto.DeliveryDto
import com.agrilink.app.data.api.dto.DeliveryEventDto
import com.agrilink.app.data.repo.DeliveryRepository
import com.agrilink.app.data.repo.ProfileRepository
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.common.errorText
import com.agrilink.app.ui.common.rememberPhotoPicker
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.AgriTopBar
import com.agrilink.app.ui.components.BottomActionBar
import com.agrilink.app.ui.components.ButtonKind
import com.agrilink.app.ui.components.DeliveryStatusTag
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.FactRow
import com.agrilink.app.ui.components.HintText
import com.agrilink.app.ui.components.LoadingBlock
import com.agrilink.app.ui.components.PillField
import com.agrilink.app.ui.components.SectionTitle
import com.agrilink.app.ui.components.SoftDivider
import com.agrilink.app.ui.components.SurfaceCard
import com.agrilink.app.ui.components.Tag
import com.agrilink.app.ui.components.Tone
import com.agrilink.app.ui.orders.UiMessage
import com.agrilink.app.ui.theme.Organic
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HandoverMode { NONE, PICKUP, DELIVER }

data class DeliveryUi(
    val delivery: DeliveryDto,
    val events: List<DeliveryEventDto> = emptyList(),
    val mode: HandoverMode = HandoverMode.NONE,
    val code: String = "",
    val weight: String = "",
    val crates: String = "",
    val note: String = "",
    val photos: List<Uri> = emptyList(),
    val busy: Boolean = false,
    val error: AppError? = null,
    val confirmRelease: Boolean = false,
    val finished: Boolean = false,
)

/** Job detail for a driver: accept an open job, then work through pickup, trip and delivery. */
class DeliveryViewModel(
    private val deliveryId: String,
    private val repo: DeliveryRepository,
    private val profile: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<Load<DeliveryUi>>(Load.Loading)
    val state: StateFlow<Load<DeliveryUi>> = _state.asStateFlow()
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            when (val r = repo.get(deliveryId)) {
                is ApiResult.Err -> if (_state.value !is Load.Ready) _state.value = Load.Failed(r.error)
                is ApiResult.Ok -> {
                    val previous = (_state.value as? Load.Ready)?.value
                    _state.value = Load.Ready((previous ?: DeliveryUi(r.value)).copy(delivery = r.value))
                    (repo.events(deliveryId) as? ApiResult.Ok)?.let { e -> edit { it.copy(events = e.value) } }
                }
            }
        }
    }

    private fun edit(block: (DeliveryUi) -> DeliveryUi) = _state.update { if (it is Load.Ready) Load.Ready(block(it.value)) else it }
    private val current get() = (_state.value as? Load.Ready)?.value

    fun openMode(mode: HandoverMode) = edit { it.copy(mode = mode, code = "", weight = "", crates = "", note = "", photos = emptyList(), error = null) }
    fun closeMode() = edit { it.copy(mode = HandoverMode.NONE, error = null) }
    fun setCode(v: String) = edit { it.copy(code = v.filter { c -> !c.isWhitespace() }.take(64), error = null) }
    fun setWeight(v: String) = edit { it.copy(weight = v.filter { c -> c.isDigit() || c == '.' }.take(8), error = null) }
    fun setCrates(v: String) = edit { it.copy(crates = v.filter(Char::isDigit).take(4), error = null) }
    fun setNote(v: String) = edit { it.copy(note = v.take(300)) }
    fun addPhoto(uri: Uri) = edit { if (it.photos.size < 3) it.copy(photos = it.photos + uri) else it }
    fun removePhoto(uri: Uri) = edit { it.copy(photos = it.photos - uri) }
    fun askRelease(show: Boolean) = edit { it.copy(confirmRelease = show) }

    private fun run(failureMessage: Boolean = false, doneMessage: Int, call: suspend () -> ApiResult<DeliveryDto>) {
        edit { it.copy(busy = true, error = null, confirmRelease = false) }
        viewModelScope.launch {
            when (val r = call()) {
                is ApiResult.Ok -> {
                    _messages.tryEmit(UiMessage.Res(doneMessage))
                    edit { it.copy(delivery = r.value, busy = false, mode = HandoverMode.NONE) }
                    load()
                }
                is ApiResult.Err -> {
                    if (failureMessage) _messages.tryEmit(UiMessage.Raw(r.error.message))
                    edit { it.copy(busy = false, error = r.error) }
                }
            }
        }
    }

    fun accept() = run(true, R.string.job_accepted) { repo.accept(deliveryId) }
    fun release(onDone: () -> Unit) {
        edit { it.copy(busy = true, confirmRelease = false) }
        viewModelScope.launch {
            when (val r = repo.release(deliveryId)) {
                is ApiResult.Ok -> { _messages.tryEmit(UiMessage.Res(R.string.job_released)); onDone() }
                is ApiResult.Err -> { _messages.tryEmit(UiMessage.Raw(r.error.message)); edit { it.copy(busy = false) } }
            }
        }
    }

    fun startTrip() = run(true, R.string.trip_started) { repo.start(deliveryId) }

    fun submitPickup() {
        val ui = current ?: return
        val kg = ui.weight.toDoubleOrNull()
        if (ui.code.isBlank() || kg == null || kg <= 0) { edit { it.copy(error = AppError("VALIDATION_ERROR", "")) }; return }
        run(false, R.string.pickup_confirmed) {
            val ids = uploadAll(ui.photos, "DELIVERY_EVIDENCE") ?: return@run ApiResult.Err(AppError("FILE_UPLOAD_FAILED", "A photo could not be uploaded. Try again or remove it."))
            repo.pickup(deliveryId, ui.code, kg, ui.crates.toIntOrNull(), ui.note, ids)
        }
    }

    fun submitDelivery() {
        val ui = current ?: return
        if (ui.code.isBlank()) { edit { it.copy(error = AppError("VALIDATION_ERROR", "")) }; return }
        edit { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val ids = uploadAll(ui.photos, "DELIVERY_EVIDENCE")
            if (ids == null) { edit { it.copy(busy = false, error = AppError("FILE_UPLOAD_FAILED", "A photo could not be uploaded. Try again or remove it.")) }; return@launch }
            when (val r = repo.deliver(deliveryId, ui.code, ui.note, ids)) {
                is ApiResult.Ok -> { _messages.tryEmit(UiMessage.Res(R.string.delivery_confirmed)); edit { it.copy(delivery = r.value, busy = false, mode = HandoverMode.NONE, finished = true) }; load() }
                is ApiResult.Err -> edit { it.copy(busy = false, error = r.error) }
            }
        }
    }

    private suspend fun uploadAll(photos: List<Uri>, purpose: String): List<String>? {
        val ids = mutableListOf<String>()
        for (uri in photos) {
            val r = profile.uploadImage(uri, purpose)
            if (r is ApiResult.Ok) ids += r.value.id else return null
        }
        return ids
    }

    fun ping(location: Location) {
        viewModelScope.launch { repo.ping(deliveryId, location.latitude, location.longitude) }
    }
}

@Composable
fun DeliveryScreen(viewModel: DeliveryViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.messages.collect { m -> snackbar.showSnackbar(when (m) { is UiMessage.Res -> context.getString(m.id); is UiMessage.Raw -> m.text }) } }
    LaunchedEffect(Unit) { while (true) { delay(15_000); viewModel.load() } }
    Box(modifier.fillMaxSize().background(Organic.Bg)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            AgriTopBar(stringResource(R.string.job_title), onBack)
            when (val s = state) {
                Load.Loading -> LoadingBlock()
                is Load.Failed -> ErrorState(s.error, viewModel::load)
                is Load.Ready -> DeliveryContent(s.value, viewModel, onBack)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp))
    }
    ((state as? Load.Ready)?.value)?.let { ui ->
        if (ui.confirmRelease) AlertDialog(
            onDismissRequest = { viewModel.askRelease(false) },
            title = { Text(stringResource(R.string.job_release_title)) },
            text = { Text(stringResource(R.string.job_release_body)) },
            confirmButton = { TextButton({ viewModel.release(onBack) }) { Text(stringResource(R.string.job_release)) } },
            dismissButton = { TextButton({ viewModel.askRelease(false) }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.DeliveryContent(ui: DeliveryUi, vm: DeliveryViewModel, onBack: () -> Unit) {
    val d = ui.delivery
    val actions = d.allowedActions
    if (d.status in setOf("PICKED_UP", "IN_TRANSIT")) TripTracking(vm)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SurfaceCard(color = Organic.Accent100) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Format.etb(d.driverFee), style = MaterialTheme.typography.displaySmall, color = Organic.Accent700, modifier = Modifier.weight(1f))
                DeliveryStatusTag(d.status)
            }
            Text(stringResource(R.string.job_your_pay, d.orderNumber), style = MaterialTheme.typography.bodySmall, color = Organic.Accent800)
        }
        SurfaceCard {
            Text(stringResource(R.string.job_load), style = MaterialTheme.typography.titleMedium)
            d.load.forEach { Text("${Format.number(it.quantity)} ${it.name} · ${Format.kg(it.weightKg)}", style = MaterialTheme.typography.bodyMedium) }
            FactRow(stringResource(R.string.job_total_weight), Format.kg(d.totalWeightKg), bold = true)
            d.distanceKm?.let { FactRow(stringResource(R.string.job_distance), "${Format.number(it, 0)} km") }
            d.scheduledPickupDate?.let { FactRow(stringResource(R.string.job_pickup_day), Dates.dayLabel(it)) }
        }
        StopCard(stringResource(R.string.job_pickup_from), d.pickup?.address?.let { listOfNotNull(it.town, it.woreda, it.addressLine, it.regionName).joinToString(", ") }, d.pickup?.contactName, d.pickup?.contactPhone)
        StopCard(stringResource(R.string.job_deliver_to), d.dropoff?.address?.let { listOfNotNull(it.town, it.woreda, it.addressLine, it.regionName).joinToString(", ") }, d.dropoff?.contactName, d.dropoff?.contactPhone)
        d.pickedUpWeightKg?.let {
            SurfaceCard(color = Organic.Sage200) {
                Text(stringResource(if (d.pickedUpCrateCount != null) R.string.job_picked_up_summary else R.string.job_picked_up_summary_no_crates, Format.kg(it), d.pickedUpCrateCount?.toString().orEmpty()), style = MaterialTheme.typography.bodyMedium, color = Organic.Sage900)
            }
        }
        when (ui.mode) {
            HandoverMode.PICKUP -> PickupForm(ui, vm)
            HandoverMode.DELIVER -> DeliverForm(ui, vm)
            HandoverMode.NONE -> {}
        }
        if (ui.events.isNotEmpty() && ui.mode == HandoverMode.NONE) {
            SectionTitle(stringResource(R.string.job_activity))
            SurfaceCard(padding = 16.dp) {
                ui.events.take(8).forEachIndexed { i, e ->
                    if (i > 0) SoftDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.note ?: eventLabel(e.type), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(Dates.dateTime(e.at), style = MaterialTheme.typography.labelSmall, color = Organic.Neutral700)
                    }
                }
            }
        }
    }
    if (ui.mode == HandoverMode.NONE) BottomActionBar {
        when {
            "ACCEPT" in actions -> AgriButton(stringResource(R.string.job_accept), vm::accept, Modifier.fillMaxWidth(), loading = ui.busy)
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.error?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium) }
                if ("CONFIRM_PICKUP" in actions) AgriButton(stringResource(R.string.job_confirm_pickup), { vm.openMode(HandoverMode.PICKUP) }, Modifier.fillMaxWidth(), loading = ui.busy)
                else if (d.status == "ASSIGNED") HintText(stringResource(R.string.job_wait_ready))
                if ("START_TRIP" in actions) AgriButton(stringResource(R.string.job_start_trip), vm::startTrip, Modifier.fillMaxWidth(), loading = ui.busy)
                if ("CONFIRM_DELIVERY" in actions) AgriButton(stringResource(R.string.job_confirm_delivery), { vm.openMode(HandoverMode.DELIVER) }, Modifier.fillMaxWidth(), kind = if ("START_TRIP" in actions) ButtonKind.Secondary else ButtonKind.Primary, enabled = !ui.busy)
                if ("RELEASE" in actions) AgriButton(stringResource(R.string.job_release), { vm.askRelease(true) }, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost, enabled = !ui.busy)
                if (d.status == "DELIVERED") AgriButton(stringResource(R.string.action_back), onBack, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
            }
        }
    }
}

@Composable
private fun StopCard(title: String, address: String?, contact: String?, phone: String?) {
    val context = LocalContext.current
    SurfaceCard {
        Text(title, style = MaterialTheme.typography.labelMedium, color = Organic.Neutral700)
        Text(address?.ifBlank { null } ?: "—", style = MaterialTheme.typography.titleMedium)
        if (contact != null || phone != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(listOfNotNull(contact, phone?.let(Phone::display)).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (phone != null) Box(
                Modifier.size(44.dp).clip(CircleShape).background(Organic.Sage300).clickable {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Call, stringResource(R.string.action_call), tint = Organic.Sage900) }
        }
    }
}

@Composable
private fun PhotoStrip(photos: List<Uri>, onAdd: (Uri) -> Unit, onRemove: (Uri) -> Unit) {
    val pick = rememberPhotoPicker(onAdd)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(photos, key = { it.toString() }) { uri ->
            Box {
                coil3.compose.AsyncImage(uri, null, Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(Organic.Bg).clickable { onRemove(uri) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, stringResource(R.string.action_remove), Modifier.size(14.dp))
                }
            }
        }
        if (photos.size < 3) item {
            Column(Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)).background(Organic.Neutral200).clickable(onClick = pick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.AddAPhoto, null, tint = Organic.Accent700)
                Text(stringResource(R.string.add_photo), style = MaterialTheme.typography.labelSmall, color = Organic.Accent700)
            }
        }
    }
}

@Composable
private fun CodeEntry(code: String, onCode: (String) -> Unit, label: String, error: String?) {
    val context = LocalContext.current
    PillField(code, onCode, label, keyboardType = KeyboardType.Text, error = error)
    AgriButton(
        stringResource(R.string.scan_qr),
        {
            val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            GmsBarcodeScanning.getClient(context, options).startScan()
                .addOnSuccessListener { barcode -> barcode.rawValue?.let(onCode) }
        },
        Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, icon = Icons.Filled.QrCodeScanner, height = 46,
    )
}

@Composable
private fun PickupForm(ui: DeliveryUi, vm: DeliveryViewModel) {
    SurfaceCard {
        Text(stringResource(R.string.pickup_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.pickup_hint), style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700)
        CodeEntry(ui.code, vm::setCode, stringResource(R.string.pickup_code), ui.error?.takeIf { it.code.contains("HANDOVER") || it.code == "VALIDATION_ERROR" && ui.code.isBlank() }?.errorText())
        PillField(ui.weight, vm::setWeight, stringResource(R.string.pickup_weight), keyboardType = KeyboardType.Decimal, hint = stringResource(R.string.pickup_weight_hint, Format.kg(ui.delivery.totalWeightKg)))
        PillField(ui.crates, vm::setCrates, stringResource(R.string.pickup_crates), keyboardType = KeyboardType.Number)
        PillField(ui.note, vm::setNote, stringResource(R.string.pickup_note), singleLine = false, minLines = 2)
        Text(stringResource(R.string.pickup_photos), style = MaterialTheme.typography.labelMedium, color = Organic.Neutral800)
        PhotoStrip(ui.photos, vm::addPhoto, vm::removePhoto)
        ui.error?.takeIf { !it.code.contains("HANDOVER") && it.code != "VALIDATION_ERROR" }?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AgriButton(stringResource(R.string.action_cancel), vm::closeMode, Modifier.weight(1f), kind = ButtonKind.Secondary, enabled = !ui.busy)
            AgriButton(stringResource(R.string.pickup_confirm), vm::submitPickup, Modifier.weight(1f), loading = ui.busy, enabled = ui.code.isNotBlank() && (ui.weight.toDoubleOrNull() ?: 0.0) > 0)
        }
    }
}

@Composable
private fun DeliverForm(ui: DeliveryUi, vm: DeliveryViewModel) {
    SurfaceCard {
        Text(stringResource(R.string.deliver_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.deliver_hint), style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700)
        CodeEntry(ui.code, vm::setCode, stringResource(R.string.deliver_code), ui.error?.takeIf { it.code.contains("HANDOVER") }?.errorText())
        PillField(ui.note, vm::setNote, stringResource(R.string.pickup_note), singleLine = false, minLines = 2)
        Text(stringResource(R.string.deliver_photos), style = MaterialTheme.typography.labelMedium, color = Organic.Neutral800)
        PhotoStrip(ui.photos, vm::addPhoto, vm::removePhoto)
        ui.error?.takeIf { !it.code.contains("HANDOVER") }?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AgriButton(stringResource(R.string.action_cancel), vm::closeMode, Modifier.weight(1f), kind = ButtonKind.Secondary, enabled = !ui.busy)
            AgriButton(stringResource(R.string.deliver_confirm), vm::submitDelivery, Modifier.weight(1f), loading = ui.busy, enabled = ui.code.isNotBlank())
        }
    }
}

/**
 * While the trip screen is open, shares the phone's position with the buyer every ~30 s.
 * This only runs in the foreground (no background location permission is requested).
 */
@SuppressLint("MissingPermission")
@Composable
private fun TripTracking(vm: DeliveryViewModel) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val onLocation by rememberUpdatedState(vm::ping)

    LaunchedEffect(granted) { if (!granted && !asked) { asked = true; launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) } }

    DisposableEffect(granted) {
        if (!granted) return@DisposableEffect onDispose { }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { location -> onLocation(location) }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        providers.forEach { runCatching { manager.requestLocationUpdates(it, 30_000L, 50f, listener, android.os.Looper.getMainLooper()) } }
        providers.firstNotNullOfOrNull { manager.getLastKnownLocation(it) }?.let(onLocation)
        onDispose { manager.removeUpdates(listener) }
    }

    Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (granted) Tag(stringResource(R.string.trip_sharing_location), Tone.Sage)
        else Tag(stringResource(R.string.trip_location_off), Tone.Danger, Modifier.clickable { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) })
    }
}

@Composable
private fun eventLabel(type: String): String = stringResource(
    when (type) {
        "LOCATION" -> R.string.event_location
        "POSTED" -> R.string.event_posted
        "WEIGHT_VARIANCE" -> R.string.event_weight_variance
        else -> R.string.event_other
    },
)
