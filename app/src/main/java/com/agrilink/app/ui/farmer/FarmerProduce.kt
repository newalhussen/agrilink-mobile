package com.agrilink.app.ui.farmer

import com.agrilink.app.ui.common.RefreshOnResume
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.agrilink.app.data.api.dto.CategoryDto
import com.agrilink.app.data.api.dto.CreateListingRequest
import com.agrilink.app.data.api.dto.ListingDto
import com.agrilink.app.data.api.dto.ProductDto
import com.agrilink.app.data.api.dto.UpdateListingRequest
import com.agrilink.app.data.repo.MarketRepository
import com.agrilink.app.data.repo.ProfileRepository
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.common.display
import com.agrilink.app.ui.common.errorText
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.AgriImage
import com.agrilink.app.ui.components.AgriTopBar
import com.agrilink.app.ui.components.BottomActionBar
import com.agrilink.app.ui.components.ButtonKind
import com.agrilink.app.ui.components.ChipRow
import com.agrilink.app.ui.components.EmptyState
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.HintText
import com.agrilink.app.ui.components.LoadingBlock
import com.agrilink.app.ui.components.PillField
import com.agrilink.app.ui.components.QuantityStepper
import com.agrilink.app.ui.components.Segmented
import com.agrilink.app.ui.components.SkeletonList
import com.agrilink.app.ui.components.SurfaceCard
import com.agrilink.app.ui.components.Tag
import com.agrilink.app.ui.components.Tone
import com.agrilink.app.ui.components.unitRes
import com.agrilink.app.ui.orders.UiMessage
import com.agrilink.app.ui.common.rememberPhotoPicker
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// ------------------------------------------------------------------------------------------------ list

enum class ProduceFilter(val statuses: Set<String>?) { ALL(null), ACTIVE(setOf("ACTIVE")), PAUSED(setOf("PAUSED", "DRAFT")), SOLD_OUT(setOf("SOLD_OUT", "EXPIRED", "REMOVED")) }

data class ProduceUi(val items: Load<List<ListingDto>> = Load.Loading, val filter: ProduceFilter = ProduceFilter.ALL, val refreshing: Boolean = false)

/** C2 · the farmer's own produce listings. */
class ProduceViewModel(private val market: MarketRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProduceUi())
    val state: StateFlow<ProduceUi> = _state.asStateFlow()

    init { load() }

    fun select(filter: ProduceFilter) = _state.update { it.copy(filter = filter) }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            when (val r = market.myListings()) {
                is ApiResult.Ok -> _state.update { it.copy(items = Load.Ready(r.value.items), refreshing = false) }
                is ApiResult.Err -> _state.update { it.copy(items = if (it.items is Load.Ready) it.items else Load.Failed(r.error), refreshing = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduceScreen(viewModel: ProduceViewModel, onAdd: () -> Unit, onEdit: (String) -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.state.collectAsState()
    RefreshOnResume(viewModel::load)
    Box(modifier.fillMaxSize().background(Organic.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Text(stringResource(R.string.produce_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp))
            ChipRow(
                ProduceFilter.entries.map { it to stringResource(when (it) { ProduceFilter.ALL -> R.string.filter_all; ProduceFilter.ACTIVE -> R.string.listing_active; ProduceFilter.PAUSED -> R.string.listing_paused; ProduceFilter.SOLD_OUT -> R.string.listing_sold_out }) },
                ui.filter, { it?.let(viewModel::select) }, contentPadding = PaddingValues(horizontal = 20.dp),
            )
            Spacer(Modifier.height(10.dp))
            PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
                when (val items = ui.items) {
                    Load.Loading -> SkeletonList()
                    is Load.Failed -> ErrorState(items.error, viewModel::load)
                    is Load.Ready -> {
                        val shown = items.value.filter { ui.filter.statuses == null || it.status in ui.filter.statuses!! }
                        if (shown.isEmpty()) EmptyState(stringResource(R.string.produce_empty), stringResource(R.string.produce_empty_body)) { AgriButton(stringResource(R.string.produce_add), onAdd, icon = Icons.Filled.Add) }
                        else LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(shown, key = { it.id }) { ProduceRow(it) { onEdit(it.id) } }
                        }
                    }
                }
            }
        }
        AgriButton(stringResource(R.string.produce_add), onAdd, Modifier.align(Alignment.BottomEnd).padding(20.dp), icon = Icons.Filled.Add)
    }
}

@Composable
private fun ProduceRow(l: ListingDto, onClick: () -> Unit) {
    SurfaceCard(onClick = onClick, padding = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AgriImage(l.photos.firstOrNull()?.url, Modifier.size(76.dp), shape = RoundedCornerShape(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(l.product.display(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    ListingStatusTag(l.status)
                }
                Text("${Format.etb(l.pricePerUnit)} / ${stringResource(unitRes(l.unit))}", style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.produce_left, Format.number(l.quantityAvailable), stringResource(unitRes(l.unit))), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
            }
        }
    }
}

@Composable
fun ListingStatusTag(status: String) {
    val (text, tone) = when (status) {
        "ACTIVE" -> R.string.listing_active to Tone.Sage
        "PAUSED" -> R.string.listing_paused to Tone.Neutral
        "DRAFT" -> R.string.listing_draft to Tone.Outline
        "SOLD_OUT" -> R.string.listing_sold_out to Tone.Accent
        "EXPIRED" -> R.string.listing_expired to Tone.Neutral
        else -> R.string.listing_removed to Tone.Danger
    }
    Tag(stringResource(text), tone)
}

// ------------------------------------------------------------------------------------------------ add

val GRADES = listOf("A", "B", "C")
val UNITS = listOf("KG", "QUINTAL", "SACK", "CRATE", "BUNCH", "PIECE")
val UNIT_KG = mapOf("KG" to 1.0, "QUINTAL" to 100.0, "SACK" to 50.0, "CRATE" to 20.0, "BUNCH" to 1.0, "PIECE" to 0.25)

data class AddProduceForm(
    val step: Int = 0,
    val categoryId: String? = null,
    val product: ProductDto? = null,
    val unit: String = "KG",
    val quantity: String = "",
    val minOrder: String = "",
    val grade: String = "A",
    val organic: Boolean = false,
    val packaging: String = "",
    val harvestDate: LocalDate? = null,
    val description: String = "",
    val price: String = "",
    val photos: List<Uri> = emptyList(),
    val query: String = "",
)

data class AddProduceUi(
    val catalogue: Load<Pair<List<CategoryDto>, List<ProductDto>>> = Load.Loading,
    val form: AddProduceForm = AddProduceForm(),
    val submitting: Boolean = false,
    val error: AppError? = null,
    val photoWarning: Boolean = false,
    val createdId: String? = null,
) {
    val quantityValue get() = form.quantity.toDoubleOrNull()
    val priceValue get() = form.price.toDoubleOrNull()
    val stepValid: Boolean
        get() = when (form.step) {
            0 -> form.product != null
            1 -> (quantityValue ?: 0.0) > 0.0 && (form.minOrder.isBlank() || ((form.minOrder.toDoubleOrNull() ?: 0.0) in 0.0001..(quantityValue ?: 0.0)))
            2 -> true
            else -> (priceValue ?: 0.0) > 0.0
        }
}

class AddProduceViewModel(
    private val market: MarketRepository,
    private val profile: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AddProduceUi())
    val state: StateFlow<AddProduceUi> = _state.asStateFlow()

    init { loadCatalogue() }

    fun loadCatalogue() {
        _state.update { it.copy(catalogue = Load.Loading) }
        viewModelScope.launch {
            val categories = market.categories()
            val products = market.products()
            if (categories is ApiResult.Ok && products is ApiResult.Ok) _state.update { it.copy(catalogue = Load.Ready(categories.value to products.value)) }
            else _state.update { it.copy(catalogue = Load.Failed((categories as? ApiResult.Err)?.error ?: (products as ApiResult.Err).error)) }
        }
    }

    private fun edit(block: (AddProduceForm) -> AddProduceForm) = _state.update { it.copy(form = block(it.form), error = null) }

    fun setCategory(id: String?) = edit { it.copy(categoryId = id) }
    fun setQuery(q: String) = edit { it.copy(query = q) }
    fun pickProduct(p: ProductDto) = edit { it.copy(product = p, unit = p.defaultUnit.takeIf { u -> u in UNITS } ?: "KG") }
    fun setUnit(u: String) = edit { it.copy(unit = u) }
    fun setQuantity(v: String) = edit { it.copy(quantity = v.filter { c -> c.isDigit() || c == '.' }.take(9)) }
    fun setMinOrder(v: String) = edit { it.copy(minOrder = v.filter { c -> c.isDigit() || c == '.' }.take(9)) }
    fun setGrade(g: String) = edit { it.copy(grade = g) }
    fun setOrganic(v: Boolean) = edit { it.copy(organic = v) }
    fun setPackaging(v: String) = edit { it.copy(packaging = v.take(120)) }
    fun setHarvest(d: LocalDate?) = edit { it.copy(harvestDate = d) }
    fun setDescription(v: String) = edit { it.copy(description = v.take(500)) }
    fun setPrice(v: String) = edit { it.copy(price = v.filter { c -> c.isDigit() || c == '.' }.take(9)) }
    fun addPhoto(uri: Uri) = edit { if (it.photos.size < 4) it.copy(photos = it.photos + uri) else it }
    fun removePhoto(uri: Uri) = edit { it.copy(photos = it.photos - uri) }

    fun next() = _state.update { if (it.stepValid && it.form.step < 3) it.copy(form = it.form.copy(step = it.form.step + 1)) else it }

    /** Returns false when the back press was consumed by going to the previous step. */
    fun back(): Boolean {
        if (_state.value.form.step == 0) return false
        _state.update { it.copy(form = it.form.copy(step = it.form.step - 1), error = null) }
        return true
    }

    fun submit() {
        val ui = _state.value
        val f = ui.form
        val product = f.product ?: return
        val qty = ui.quantityValue ?: return
        val price = ui.priceValue ?: return
        if (ui.submitting) return
        _state.update { it.copy(submitting = true, error = null, photoWarning = false) }
        viewModelScope.launch {
            val address: AddressDto? = (profile.farmer() as? ApiResult.Ok)?.value?.address
            val request = CreateListingRequest(
                productId = product.id,
                description = f.description.takeIf { it.isNotBlank() },
                qualityGrade = f.grade,
                packaging = f.packaging.takeIf { it.isNotBlank() },
                harvestDate = f.harvestDate?.toString(),
                unit = f.unit,
                unitWeightKg = UNIT_KG[f.unit],
                quantity = qty,
                minOrderQuantity = f.minOrder.toDoubleOrNull(),
                pricePerUnit = price,
                organic = f.organic,
                address = address,
                publish = true,
            )
            when (val created = market.createListing(request)) {
                is ApiResult.Err -> _state.update { it.copy(submitting = false, error = created.error) }
                is ApiResult.Ok -> {
                    var failedPhotos = false
                    f.photos.forEachIndexed { index, uri ->
                        val file = profile.uploadImage(uri, "LISTING_PHOTO")
                        if (file is ApiResult.Ok) {
                            if (market.addPhoto(created.value.id, file.value.id, primary = index == 0) is ApiResult.Err) failedPhotos = true
                        } else failedPhotos = true
                    }
                    _state.update { it.copy(submitting = false, createdId = created.value.id, photoWarning = failedPhotos) }
                }
            }
        }
    }
}

@Composable
fun AddProduceScreen(viewModel: AddProduceViewModel, onClose: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.state.collectAsState()
    val f = ui.form
    val goBack = { if (!viewModel.back()) onClose() }
    androidx.activity.compose.BackHandler(enabled = f.step > 0 && ui.createdId == null) { viewModel.back() }

    Column(modifier.fillMaxSize().background(Organic.Bg).imePadding()) {
        AgriTopBar(stringResource(R.string.add_title), if (ui.createdId == null) goBack else null)
        if (ui.createdId != null) { ProduceCreated(ui.photoWarning, onDone); return@Column }
        StepDots(f.step)
        when (val c = ui.catalogue) {
            Load.Loading -> LoadingBlock()
            is Load.Failed -> ErrorState(c.error, viewModel::loadCatalogue)
            is Load.Ready -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (f.step) {
                        0 -> StepProduct(c.value.first, c.value.second, f, viewModel)
                        1 -> StepQuantity(f, ui, viewModel)
                        2 -> StepQuality(f, viewModel)
                        else -> StepPrice(f, ui, viewModel)
                    }
                }
                BottomActionBar {
                    ui.error?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp)) }
                    if (f.step < 3) AgriButton(stringResource(R.string.action_next), viewModel::next, Modifier.fillMaxWidth(), enabled = ui.stepValid)
                    else AgriButton(stringResource(R.string.add_publish), viewModel::submit, Modifier.fillMaxWidth(), loading = ui.submitting, enabled = ui.stepValid)
                }
            }
        }
    }
}

@Composable
private fun StepDots(step: Int) {
    Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(if (it <= step) Organic.Accent else Organic.Neutral300)) }
        Text(stringResource(R.string.add_step_of, step + 1, 4), style = MaterialTheme.typography.labelMedium, color = Organic.Neutral700, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun StepProduct(categories: List<CategoryDto>, products: List<ProductDto>, f: AddProduceForm, vm: AddProduceViewModel) {
    Text(stringResource(R.string.add_what), style = MaterialTheme.typography.headlineSmall)
    PillField(f.query, vm::setQuery, stringResource(R.string.search_hint), leadingIcon = Icons.Filled.Search)
    ChipRow(listOf<Pair<String?, String>>(null to stringResource(R.string.filter_all)) + categories.map { it.id to it.display() }, f.categoryId, vm::setCategory)
    val shown = products.filter { p ->
        (f.categoryId == null || p.categoryId == f.categoryId) &&
            (f.query.isBlank() || p.nameEn.contains(f.query, true) || p.display().contains(f.query, true))
    }
    shown.forEach { p ->
        com.agrilink.app.ui.components.ChoiceCard(p.display(), subtitle = p.categoryName, selected = f.product?.id == p.id, onClick = { vm.pickProduct(p) })
    }
    if (shown.isEmpty()) HintText(stringResource(R.string.add_no_product))
}

@Composable
private fun StepQuantity(f: AddProduceForm, ui: AddProduceUi, vm: AddProduceViewModel) {
    Text(stringResource(R.string.add_how_much, f.product?.display().orEmpty()), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(R.string.add_unit), style = MaterialTheme.typography.labelMedium, color = Organic.Neutral800)
    ChipRow(UNITS.map { it to stringResource(unitRes(it)) }, f.unit, { it?.let(vm::setUnit) })
    PillField(f.quantity, vm::setQuantity, stringResource(R.string.add_quantity), keyboardType = KeyboardType.Decimal, hint = stringResource(R.string.add_quantity_hint, stringResource(unitRes(f.unit))))
    PillField(
        f.minOrder, vm::setMinOrder, stringResource(R.string.add_min_order), keyboardType = KeyboardType.Decimal,
        hint = stringResource(R.string.add_min_order_hint),
        error = if (f.minOrder.isNotBlank() && !ui.stepValid) stringResource(R.string.add_min_order_error) else null,
    )
}

@Composable
private fun StepQuality(f: AddProduceForm, vm: AddProduceViewModel) {
    val pick = rememberPhotoPicker(vm::addPhoto)
    Text(stringResource(R.string.add_quality_title), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(R.string.add_grade), style = MaterialTheme.typography.labelMedium, color = Organic.Neutral800)
    Segmented(GRADES.map { it to stringResource(R.string.grade_x, it) }, f.grade, vm::setGrade, Modifier.fillMaxWidth())
    HintText(stringResource(R.string.add_grade_hint))
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Organic.Surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.organic), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.add_organic_hint), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
        }
        Switch(f.organic, vm::setOrganic, colors = SwitchDefaults.colors(checkedTrackColor = Organic.Accent2, checkedThumbColor = Organic.Bg))
    }
    PillField(f.packaging, vm::setPackaging, stringResource(R.string.add_packaging), placeholder = stringResource(R.string.add_packaging_hint))
    HarvestDateField(f.harvestDate, vm::setHarvest)
    PillField(f.description, vm::setDescription, stringResource(R.string.add_description), singleLine = false, minLines = 3)
    Text(stringResource(R.string.add_photos), style = MaterialTheme.typography.labelMedium, color = Organic.Neutral800)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(f.photos, key = { it.toString() }) { uri ->
            Box {
                coil3.compose.AsyncImage(uri, null, Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp).clip(CircleShape).background(Organic.Bg).clickable { vm.removePhoto(uri) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, stringResource(R.string.action_remove), Modifier.size(16.dp))
                }
            }
        }
        if (f.photos.size < 4) item {
            Column(Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)).background(Organic.Surface).clickable(onClick = pick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.AddAPhoto, null, tint = Organic.Accent700)
                Text(stringResource(R.string.add_photo), style = MaterialTheme.typography.labelSmall, color = Organic.Accent700)
            }
        }
    }
}

/** Harvest date: quick choices (today / yesterday / 3 days / a week ago) rather than a calendar dialog; farmers rarely need the exact day. */
@Composable
private fun HarvestDateField(value: LocalDate?, onChange: (LocalDate?) -> Unit) {
    val today = remember { Dates.today() }
    Text(stringResource(R.string.add_harvest), style = MaterialTheme.typography.labelMedium, color = Organic.Neutral800)
    ChipRow(
        listOf<Pair<LocalDate?, String>>(
            null to stringResource(R.string.add_harvest_unknown),
            today to stringResource(R.string.add_harvest_today),
            today.minusDays(1) to stringResource(R.string.add_harvest_yesterday),
            today.minusDays(3) to stringResource(R.string.add_harvest_3d),
            today.minusDays(7) to stringResource(R.string.add_harvest_week),
        ),
        value, onChange,
    )
}

@Composable
private fun StepPrice(f: AddProduceForm, ui: AddProduceUi, vm: AddProduceViewModel) {
    Text(stringResource(R.string.add_price_title), style = MaterialTheme.typography.headlineSmall)
    PillField(f.price, vm::setPrice, stringResource(R.string.add_price_per, stringResource(unitRes(f.unit))), keyboardType = KeyboardType.Decimal, prefix = "ETB ")
    val qty = ui.quantityValue ?: 0.0
    val price = ui.priceValue ?: 0.0
    SurfaceCard(color = Organic.Sage200) {
        Text(stringResource(R.string.add_review), style = MaterialTheme.typography.labelMedium, color = Organic.Sage900)
        Text(f.product?.display().orEmpty(), style = MaterialTheme.typography.titleLarge, color = Organic.Sage900)
        Text("${Format.number(qty)} ${stringResource(unitRes(f.unit))} · ${stringResource(R.string.grade_x, f.grade)}${if (f.organic) " · " + stringResource(R.string.organic) else ""}", style = MaterialTheme.typography.bodyMedium, color = Organic.Sage900)
        Text(stringResource(R.string.add_potential, Format.etb(qty * price)), style = MaterialTheme.typography.titleMedium, color = Organic.Sage900)
        if (f.photos.isEmpty()) Text(stringResource(R.string.add_no_photo_warning), style = MaterialTheme.typography.bodySmall, color = Organic.Sage800)
    }
    HintText(stringResource(R.string.add_fee_hint))
}

@Composable
private fun ProduceCreated(photoWarning: Boolean, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.add_done_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(if (photoWarning) R.string.add_done_photo_warning else R.string.add_done_body), style = MaterialTheme.typography.bodyLarge, color = Organic.Neutral800, modifier = Modifier.padding(vertical = 12.dp))
        AgriButton(stringResource(R.string.action_done), onDone, Modifier.fillMaxWidth(), height = 56)
    }
}

// ------------------------------------------------------------------------------------------------ edit

data class EditUi(
    val listing: Load<ListingDto> = Load.Loading,
    val price: String = "",
    val quantity: String = "",
    val saving: Boolean = false,
    val savedOnce: Boolean = false,
    val confirmRemove: Boolean = false,
)

class EditListingViewModel(private val listingId: String, private val market: MarketRepository) : ViewModel() {
    private val _state = MutableStateFlow(EditUi())
    val state: StateFlow<EditUi> = _state.asStateFlow()
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(listing = Load.Loading) }
        viewModelScope.launch {
            when (val r = market.listing(listingId)) {
                is ApiResult.Ok -> _state.update { it.copy(listing = Load.Ready(r.value), price = Format.number(r.value.pricePerUnit), quantity = Format.number(r.value.quantityAvailable)) }
                is ApiResult.Err -> _state.update { it.copy(listing = Load.Failed(r.error)) }
            }
        }
    }

    fun setPrice(v: String) = _state.update { it.copy(price = v.filter { c -> c.isDigit() || c == '.' }.take(9)) }
    fun setQuantity(v: String) = _state.update { it.copy(quantity = v.filter { c -> c.isDigit() || c == '.' }.take(9)) }
    fun askRemove(show: Boolean) = _state.update { it.copy(confirmRemove = show) }

    fun save() {
        val s = _state.value
        val price = s.price.toDoubleOrNull()?.takeIf { it > 0 } ?: return
        val qty = s.quantity.toDoubleOrNull()?.takeIf { it >= 0 } ?: return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = market.updateListing(listingId, UpdateListingRequest(pricePerUnit = price, quantityAvailable = qty))) {
                is ApiResult.Ok -> { _messages.tryEmit(UiMessage.Res(R.string.edit_saved)); _state.update { it.copy(listing = Load.Ready(r.value), saving = false, savedOnce = true) } }
                is ApiResult.Err -> { _messages.tryEmit(UiMessage.Raw(r.error.message)); _state.update { it.copy(saving = false) } }
            }
        }
    }

    fun setStatus(status: String, onDone: () -> Unit = {}) {
        _state.update { it.copy(saving = true, confirmRemove = false) }
        viewModelScope.launch {
            when (val r = market.changeListingStatus(listingId, status)) {
                is ApiResult.Ok -> { _state.update { it.copy(listing = Load.Ready(r.value), saving = false) }; if (status == "REMOVED") onDone() }
                is ApiResult.Err -> { _messages.tryEmit(UiMessage.Raw(r.error.message)); _state.update { it.copy(saving = false) } }
            }
        }
    }
}

@Composable
fun EditListingScreen(viewModel: EditListingViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.messages.collect { m -> snackbar.showSnackbar(when (m) { is UiMessage.Res -> context.getString(m.id); is UiMessage.Raw -> m.text }) } }
    Box(modifier.fillMaxSize().background(Organic.Bg)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            AgriTopBar(stringResource(R.string.edit_title), onBack)
            when (val l = ui.listing) {
                Load.Loading -> LoadingBlock()
                is Load.Failed -> ErrorState(l.error, viewModel::load)
                is Load.Ready -> {
                    val listing = l.value
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            AgriImage(listing.photos.firstOrNull()?.url, Modifier.size(84.dp), shape = RoundedCornerShape(22.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(listing.product.display(), style = MaterialTheme.typography.headlineSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ListingStatusTag(listing.status); Tag(stringResource(R.string.grade_x, listing.qualityGrade), Tone.Outline) }
                            }
                        }
                        PillField(ui.price, viewModel::setPrice, stringResource(R.string.add_price_per, stringResource(unitRes(listing.unit))), keyboardType = KeyboardType.Decimal, prefix = "ETB ")
                        PillField(ui.quantity, viewModel::setQuantity, stringResource(R.string.edit_available, stringResource(unitRes(listing.unit))), keyboardType = KeyboardType.Decimal, hint = stringResource(R.string.edit_total_hint, Format.number(listing.quantityTotal)))
                        AgriButton(stringResource(R.string.action_save), viewModel::save, Modifier.fillMaxWidth(), loading = ui.saving)
                        when (listing.status) {
                            "ACTIVE" -> AgriButton(stringResource(R.string.edit_pause), { viewModel.setStatus("PAUSED") }, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, enabled = !ui.saving)
                            "PAUSED", "DRAFT", "SOLD_OUT", "EXPIRED" -> AgriButton(stringResource(R.string.edit_activate), { viewModel.setStatus("ACTIVE") }, Modifier.fillMaxWidth(), kind = ButtonKind.Sage, enabled = !ui.saving)
                        }
                        if (listing.status != "REMOVED") AgriButton(stringResource(R.string.edit_remove), { viewModel.askRemove(true) }, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost, enabled = !ui.saving)
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
    if (ui.confirmRemove) AlertDialog(
        onDismissRequest = { viewModel.askRemove(false) },
        title = { Text(stringResource(R.string.edit_remove_title)) },
        text = { Text(stringResource(R.string.edit_remove_body)) },
        confirmButton = { TextButton({ viewModel.setStatus("REMOVED", onBack) }) { Text(stringResource(R.string.edit_remove)) } },
        dismissButton = { TextButton({ viewModel.askRemove(false) }) { Text(stringResource(R.string.action_cancel)) } },
    )
}

