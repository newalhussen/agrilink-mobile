package com.agrilink.app.ui.buyer

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.agrilink.app.data.api.dto.AddressDto
import com.agrilink.app.data.api.dto.CreateOrderRequest
import com.agrilink.app.data.api.dto.ListingDto
import com.agrilink.app.data.api.dto.OrderItemRequest
import com.agrilink.app.data.api.dto.PublicFarmerDto
import com.agrilink.app.data.api.dto.QuoteResponse
import com.agrilink.app.data.api.dto.RegionDto
import com.agrilink.app.data.prefs.SessionStore
import com.agrilink.app.data.repo.MarketRepository
import com.agrilink.app.data.repo.OrderRepository
import com.agrilink.app.data.repo.ProfileRepository
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.common.display
import com.agrilink.app.ui.common.errorText
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.AgriImage
import com.agrilink.app.ui.components.AgriTopBar
import com.agrilink.app.ui.components.Avatar
import com.agrilink.app.ui.components.BottomActionBar
import com.agrilink.app.ui.components.DropdownField
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.FactRow
import com.agrilink.app.ui.components.LoadingBlock
import com.agrilink.app.ui.components.PillField
import com.agrilink.app.ui.components.QuantityStepper
import com.agrilink.app.ui.components.SurfaceCard
import com.agrilink.app.ui.components.Tag
import com.agrilink.app.ui.components.Tone
import com.agrilink.app.ui.components.unitRes
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListingDetailUi(
    val listing: ListingDto,
    val farmer: PublicFarmerDto? = null,
    val quantity: Double,
    val quote: QuoteResponse? = null,
    val quoting: Boolean = false,
) {
    val step: Double get() = if (listing.unit == "KG" || listing.unit == "LITER") 10.0 else 1.0
    val goods: Double get() = quantity * listing.pricePerUnit
}

/** B2 · Listing: photos, trust facts, a quantity stepper and a live price including delivery and the AgriLink fee. */
class ListingDetailViewModel(
    private val listingId: String,
    private val market: MarketRepository,
    private val orders: OrderRepository,
    private val profile: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<Load<ListingDetailUi>>(Load.Loading)
    val state: StateFlow<Load<ListingDetailUi>> = _state.asStateFlow()
    private var quoteJob: Job? = null
    private var address: AddressDto? = null

    init { load() }

    fun load() {
        _state.value = Load.Loading
        viewModelScope.launch {
            when (val r = market.listing(listingId)) {
                is ApiResult.Err -> _state.value = Load.Failed(r.error)
                is ApiResult.Ok -> {
                    val listing = r.value
                    _state.value = Load.Ready(ListingDetailUi(listing, quantity = listing.minOrderQuantity))
                    launch { (market.publicFarmer(listing.farmer.id) as? ApiResult.Ok)?.let { f -> edit { it.copy(farmer = f.value) } } }
                    address = (profile.buyer() as? ApiResult.Ok)?.value?.address?.takeIf { it.regionId != null || it.latitude != null }
                    requote()
                }
            }
        }
    }

    private fun edit(block: (ListingDetailUi) -> ListingDetailUi) =
        _state.update { if (it is Load.Ready) Load.Ready(block(it.value)) else it }

    fun setQuantity(q: Double) {
        edit { it.copy(quantity = q) }
        requote()
    }

    private fun requote() {
        val ui = (_state.value as? Load.Ready)?.value ?: return
        val addr = address ?: return
        quoteJob?.cancel()
        quoteJob = viewModelScope.launch {
            delay(400)
            edit { it.copy(quoting = true) }
            val request = CreateOrderRequest(listOf(OrderItemRequest(ui.listing.id, ui.quantity)), addr)
            when (val q = orders.quote(request)) {
                is ApiResult.Ok -> edit { it.copy(quote = q.value, quoting = false) }
                is ApiResult.Err -> edit { it.copy(quote = null, quoting = false) }
            }
        }
    }
}

@Composable
fun ListingDetailScreen(viewModel: ListingDetailViewModel, onBack: () -> Unit, onOrder: (listingId: String, quantity: Double) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    Column(modifier.fillMaxSize().background(Organic.Bg)) {
        AgriTopBar("", onBack)
        when (val s = state) {
            Load.Loading -> LoadingBlock()
            is Load.Failed -> ErrorState(s.error, viewModel::load)
            is Load.Ready -> ListingContent(s.value, viewModel::setQuantity, onOrder)
        }
    }
}


@Composable
private fun ListingContent(ui: ListingDetailUi, onQuantity: (Double) -> Unit, onOrder: (String, Double) -> Unit) {
    val l = ui.listing
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (l.photos.isNotEmpty()) {
                val pager = rememberPagerState { l.photos.size }
                HorizontalPager(pager, Modifier.fillMaxWidth().height(220.dp), pageSpacing = 10.dp) { page ->
                    AgriImage(l.photos[page].url, Modifier.fillMaxSize(), shape = RoundedCornerShape(28.dp))
                }
            }
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(stringResource(R.string.grade_x, l.qualityGrade), Tone.Accent)
                    if (l.organic) Tag(stringResource(R.string.organic), Tone.Sage)
                    l.packaging?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700) }
                }
                Text(l.product.display(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 6.dp))
                if (l.title != l.product.nameEn) Text(l.title, style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700)
            }
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(l.farmer.farmName ?: l.farmer.fullName, size = 48.dp)
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(l.farmer.farmName ?: l.farmer.fullName, style = MaterialTheme.typography.titleMedium)
                            if (l.farmer.verified) Icon(Icons.Filled.VerifiedUser, null, Modifier.size(16.dp), tint = Organic.Sage700)
                        }
                        val trust = if (l.farmer.completedTrades > 0) "${Format.number(l.farmer.ratingAverage, 1)} · ${stringResource(R.string.trades_count, l.farmer.completedTrades)}" else stringResource(R.string.farmer_new)
                        Text(trust, style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
                        ui.farmer?.address?.let { a -> Text(listOfNotNull(a.town, a.regionName).joinToString(", "), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Fact(stringResource(R.string.listing_harvested), Dates.dayLabel(l.harvestDate), Modifier.weight(1f))
                Fact(stringResource(R.string.listing_available), "${Format.number(l.quantityAvailable)} ${stringResource(unitRes(l.unit)).lowercase()}", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Fact(stringResource(R.string.listing_ready_from), Dates.dayLabel(l.availableFrom), Modifier.weight(1f))
                Fact(stringResource(R.string.listing_min_order), "${Format.number(l.minOrderQuantity)} ${stringResource(unitRes(l.unit)).lowercase()}", Modifier.weight(1f))
            }
            l.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            l.qualityNotes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700) }

            SurfaceCard(color = Organic.Neutral100) {
                Text(stringResource(R.string.listing_how_much, stringResource(unitRes(l.unit)).lowercase()), style = MaterialTheme.typography.titleSmall)
                QuantityStepper(ui.quantity, onQuantity, ui.step, l.minOrderQuantity, l.quantityAvailable, unitLabel = stringResource(unitRes(l.unit)).lowercase())
                FactRow("${Format.price(l.pricePerUnit)} × ${Format.number(ui.quantity)}", Format.etb(ui.goods))
                val q = ui.quote
                if (q != null) {
                    FactRow(stringResource(R.string.checkout_delivery), Format.etb(q.deliveryFee))
                    FactRow(stringResource(R.string.checkout_fee), Format.etb(q.platformFee))
                    FactRow(stringResource(R.string.checkout_total), Format.etb(q.total), bold = true)
                } else {
                    Text(stringResource(R.string.listing_delivery_at_checkout), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Lock, null, Modifier.size(16.dp), tint = Organic.Sage700)
                Text(stringResource(R.string.pay_held_note), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral800)
            }
            Box(Modifier.height(8.dp))
        }
        BottomActionBar {
            AgriButton(
                stringResource(R.string.listing_order, ui.quote?.let { Format.etb(it.total) } ?: Format.etb(ui.goods)),
                { onOrder(l.id, ui.quantity) }, Modifier.fillMaxWidth(), height = 56,
                enabled = ui.quantity >= l.minOrderQuantity && ui.quantity <= l.quantityAvailable,
            )
        }
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Organic.Surface, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Organic.Neutral700)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

// ----------------------------------------------------------------------------------------------- checkout

data class CheckoutUi(
    val listing: ListingDto? = null,
    val quantity: Double = 0.0,
    val regions: List<RegionDto> = emptyList(),
    val regionId: String? = null,
    val town: String = "",
    val addressLine: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val notes: String = "",
    val quote: QuoteResponse? = null,
    val quoting: Boolean = false,
    val placing: Boolean = false,
    val error: AppError? = null,
    val loading: Boolean = true,
    val loadError: AppError? = null,
) {
    val addressReady: Boolean get() = regionId != null && town.isNotBlank()
}

class CheckoutViewModel(
    private val listingId: String,
    quantity: Double,
    private val market: MarketRepository,
    private val orders: OrderRepository,
    private val profile: ProfileRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutUi(quantity = quantity))
    val state: StateFlow<CheckoutUi> = _state.asStateFlow()
    private var quoteJob: Job? = null

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            val listing = market.listing(listingId)
            val regions = market.regions()
            if (listing is ApiResult.Err) { _state.update { it.copy(loading = false, loadError = listing.error) }; return@launch }
            val buyer = (profile.buyer() as? ApiResult.Ok)?.value
            _state.update {
                it.copy(
                    loading = false, listing = (listing as ApiResult.Ok).value, regions = (regions as? ApiResult.Ok)?.value.orEmpty(),
                    regionId = buyer?.address?.regionId, town = buyer?.address?.town.orEmpty(), addressLine = buyer?.address?.addressLine.orEmpty(),
                    contactName = buyer?.contactPerson ?: session.user?.fullName.orEmpty(), contactPhone = session.user?.phone.orEmpty(),
                )
            }
            requote()
        }
    }

    fun update(block: (CheckoutUi) -> CheckoutUi) {
        _state.update { block(it).copy(error = null) }
        requote()
    }

    private fun request(s: CheckoutUi) = CreateOrderRequest(
        items = listOf(OrderItemRequest(listingId, s.quantity)),
        deliveryAddress = AddressDto(regionId = s.regionId, town = s.town.trim(), addressLine = s.addressLine.trim().ifBlank { null }),
        deliveryContactName = s.contactName.trim().ifBlank { null }, deliveryContactPhone = s.contactPhone.trim().ifBlank { null },
        notes = s.notes.trim().ifBlank { null },
    )

    private fun requote() {
        val s = _state.value
        if (!s.addressReady || s.listing == null) return
        quoteJob?.cancel()
        quoteJob = viewModelScope.launch {
            delay(350)
            _state.update { it.copy(quoting = true) }
            when (val r = orders.quote(request(_state.value))) {
                is ApiResult.Ok -> _state.update { it.copy(quote = r.value, quoting = false) }
                is ApiResult.Err -> _state.update { it.copy(quote = null, quoting = false, error = r.error) }
            }
        }
    }

    fun place(onPlaced: (orderId: String) -> Unit) {
        val s = _state.value
        if (!s.addressReady || s.placing) return
        _state.update { it.copy(placing = true, error = null) }
        viewModelScope.launch {
            when (val r = orders.create(request(s))) {
                is ApiResult.Ok -> { _state.update { it.copy(placing = false) }; onPlaced(r.value.id) }
                is ApiResult.Err -> _state.update { it.copy(placing = false, error = r.error) }
            }
        }
    }
}

/** B3 (first half) · Where to deliver, the full price, and place the order. Payment comes after the farmer accepts. */
@Composable
fun CheckoutScreen(viewModel: CheckoutViewModel, onBack: () -> Unit, onPlaced: (String) -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.state.collectAsState()
    Column(modifier.fillMaxSize().background(Organic.Bg).imePadding()) {
        AgriTopBar(stringResource(R.string.checkout_title), onBack)
        when {
            ui.loading -> LoadingBlock()
            ui.loadError != null -> ErrorState(ui.loadError!!, viewModel::load)
            else -> {
                val l = ui.listing!!
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SurfaceCard {
                        Text("${l.product.display()} · ${stringResource(R.string.grade_x, l.qualityGrade)}", style = MaterialTheme.typography.titleMedium)
                        Text(l.farmer.farmName ?: l.farmer.fullName, style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700)
                        FactRow(stringResource(R.string.checkout_quantity), "${Format.number(ui.quantity)} ${stringResource(unitRes(l.unit)).lowercase()}")
                    }
                    Text(stringResource(R.string.checkout_deliver_to), style = MaterialTheme.typography.titleLarge)
                    DropdownField(
                        stringResource(R.string.setup_region), ui.regions.map { it.id to it.display() }, ui.regionId,
                        { id -> viewModel.update { it.copy(regionId = id) } }, placeholder = stringResource(R.string.setup_region_pick),
                    )
                    PillField(ui.town, { v -> viewModel.update { it.copy(town = v) } }, stringResource(R.string.setup_town))
                    PillField(ui.addressLine, { v -> viewModel.update { it.copy(addressLine = v) } }, stringResource(R.string.setup_address_line), singleLine = false, minLines = 2)
                    PillField(ui.contactName, { v -> viewModel.update { it.copy(contactName = v) } }, stringResource(R.string.checkout_contact_name))
                    PillField(ui.contactPhone, { v -> viewModel.update { it.copy(contactPhone = v) } }, stringResource(R.string.checkout_contact_phone), keyboardType = KeyboardType.Phone)
                    PillField(ui.notes, { v -> viewModel.update { it.copy(notes = v) } }, stringResource(R.string.checkout_notes), singleLine = false, minLines = 2, hint = stringResource(R.string.setup_optional))

                    SurfaceCard(color = Organic.Neutral100) {
                        val q = ui.quote
                        if (q == null) {
                            Text(stringResource(if (ui.addressReady) R.string.checkout_pricing else R.string.checkout_need_address), style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700)
                        } else {
                            FactRow(stringResource(R.string.checkout_goods), Format.etb(q.subtotal))
                            FactRow(stringResource(R.string.checkout_delivery) + q.distanceKm?.let { " · ${Format.number(it, 0)} km" }.orEmpty(), Format.etb(q.deliveryFee))
                            FactRow(stringResource(R.string.checkout_fee), Format.etb(q.platformFee))
                            FactRow(stringResource(R.string.checkout_total), Format.etb(q.total), bold = true)
                        }
                    }
                    ui.error?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium) }
                    Text(stringResource(R.string.checkout_next_steps), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
                    Box(Modifier.height(8.dp))
                }
                BottomActionBar {
                    AgriButton(
                        stringResource(R.string.checkout_place), { viewModel.place(onPlaced) }, Modifier.fillMaxWidth(),
                        loading = ui.placing, enabled = ui.addressReady && ui.quote != null, height = 56,
                    )
                }
            }
        }
    }
}
