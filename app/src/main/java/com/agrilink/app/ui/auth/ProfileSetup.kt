package com.agrilink.app.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrilink.app.R
import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.AppError
import com.agrilink.app.data.api.dto.AddressDto
import com.agrilink.app.data.api.dto.RegionDto
import com.agrilink.app.data.api.dto.UpdateBuyerProfileRequest
import com.agrilink.app.data.api.dto.UpdateDriverProfileRequest
import com.agrilink.app.data.api.dto.UpdateFarmerProfileRequest
import com.agrilink.app.data.repo.MarketRepository
import com.agrilink.app.data.repo.ProfileRepository
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.common.errorText
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.AgriTopBar
import com.agrilink.app.ui.components.DropdownField
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.LoadingBlock
import com.agrilink.app.ui.components.PillField
import com.agrilink.app.ui.components.Segmented
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val buyerTypes = listOf(
    "RESTAURANT" to R.string.buyer_type_restaurant, "HOTEL" to R.string.buyer_type_hotel, "SUPERMARKET" to R.string.buyer_type_supermarket,
    "GROCER" to R.string.buyer_type_grocer, "WHOLESALER" to R.string.buyer_type_wholesaler, "PROCESSOR" to R.string.buyer_type_processor,
    "INSTITUTION" to R.string.buyer_type_institution, "EXPORTER" to R.string.buyer_type_exporter, "INDIVIDUAL" to R.string.buyer_type_individual,
    "OTHER" to R.string.buyer_type_other,
)

val vehicleTypes = listOf(
    "MOTORCYCLE" to R.string.vehicle_motorcycle, "BAJAJ" to R.string.vehicle_bajaj, "PICKUP" to R.string.vehicle_pickup,
    "MINI_TRUCK" to R.string.vehicle_mini_truck, "ISUZU_TRUCK" to R.string.vehicle_isuzu, "HEAVY_TRUCK" to R.string.vehicle_heavy,
)

data class SetupForm(
    val name: String = "",
    val type: String = "",
    val regionId: String? = null,
    val town: String = "",
    val zone: String = "",
    val addressLine: String = "",
    val landHectares: String = "",
    val payoutNumber: String = "",
    val vehiclePlate: String = "",
    val capacityKg: String = "",
    val licenseNumber: String = "",
    val regions: List<RegionDto> = emptyList(),
    val busy: Boolean = false,
    val error: AppError? = null,
    val submitted: Boolean = false,
)

/** Collects the few details each role needs before verification (and edits them later from Account). */
class SetupViewModel(
    private val role: String,
    private val profile: ProfileRepository,
    private val market: MarketRepository,
    private val userName: String,
) : ViewModel() {

    private val _load = MutableStateFlow<Load<Unit>>(Load.Loading)
    val load: StateFlow<Load<Unit>> = _load.asStateFlow()
    private val _form = MutableStateFlow(SetupForm(type = defaultType()))
    val form: StateFlow<SetupForm> = _form.asStateFlow()

    init { load() }

    private fun defaultType() = when (role) { "FARMER" -> "INDIVIDUAL"; "BUYER" -> "RESTAURANT"; else -> "PICKUP" }

    fun load() {
        _load.value = Load.Loading
        viewModelScope.launch {
            val regions = (market.regions() as? ApiResult.Ok)?.value
            if (regions == null) { _load.value = Load.Failed(AppError(AppError.NETWORK, "")); return@launch }
            _form.update { it.copy(regions = regions) }
            when (role) {
                "FARMER" -> (profile.farmer() as? ApiResult.Ok)?.value?.let { p ->
                    _form.update {
                        it.copy(
                            name = p.farmName.orEmpty(), type = p.farmerType, regionId = p.address?.regionId, town = p.address?.town.orEmpty(),
                            zone = p.address?.zone.orEmpty(), landHectares = p.landSizeHectares?.let { v -> com.agrilink.app.core.Format.number(v) }.orEmpty(),
                            payoutNumber = p.payoutAccountNumber.orEmpty(),
                        )
                    }
                }
                "BUYER" -> (profile.buyer() as? ApiResult.Ok)?.value?.let { p ->
                    _form.update {
                        it.copy(name = p.businessName.orEmpty(), type = p.buyerType, regionId = p.address?.regionId, town = p.address?.town.orEmpty(), addressLine = p.address?.addressLine.orEmpty())
                    }
                }
                "DRIVER" -> (profile.driver() as? ApiResult.Ok)?.value?.let { p ->
                    _form.update {
                        it.copy(
                            type = p.vehicleType ?: "PICKUP", vehiclePlate = p.vehiclePlate.orEmpty(), regionId = p.regionId,
                            capacityKg = p.capacityKg?.let { v -> com.agrilink.app.core.Format.number(v) }.orEmpty(), licenseNumber = p.licenseNumber.orEmpty(),
                        )
                    }
                }
            }
            _load.value = Load.Ready(Unit)
        }
    }

    fun update(block: (SetupForm) -> SetupForm) = _form.update { block(it).copy(error = null) }

    fun canSave(f: SetupForm): Boolean = when (role) {
        "FARMER" -> f.name.isNotBlank() && f.regionId != null && f.town.isNotBlank()
        "BUYER" -> f.name.isNotBlank() && f.regionId != null && f.town.isNotBlank()
        else -> f.vehiclePlate.isNotBlank() && (f.capacityKg.toDoubleOrNull() ?: 0.0) > 0 && f.licenseNumber.isNotBlank()
    }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (!canSave(f) || f.busy) return
        _form.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val address = AddressDto(regionId = f.regionId, town = f.town.trim(), zone = f.zone.trim().ifBlank { null }, addressLine = f.addressLine.trim().ifBlank { null })
            val result: ApiResult<*> = when (role) {
                "FARMER" -> profile.updateFarmer(
                    UpdateFarmerProfileRequest(
                        farmerType = f.type, farmName = f.name.trim(), address = address, landSizeHectares = f.landHectares.toDoubleOrNull(),
                        payoutMethod = f.payoutNumber.takeIf { it.isNotBlank() }?.let { "TELEBIRR" }, payoutAccountName = userName,
                        payoutAccountNumber = f.payoutNumber.trim().ifBlank { null },
                    ),
                )
                "BUYER" -> profile.updateBuyer(UpdateBuyerProfileRequest(buyerType = f.type, businessName = f.name.trim(), address = address))
                else -> profile.updateDriver(
                    UpdateDriverProfileRequest(
                        vehicleType = f.type, vehiclePlate = f.vehiclePlate.trim(), capacityKg = f.capacityKg.toDoubleOrNull(),
                        licenseNumber = f.licenseNumber.trim(), regionId = f.regionId,
                    ),
                )
            }
            when (result) {
                is ApiResult.Ok -> { _form.update { it.copy(busy = false, submitted = true) }; onDone() }
                is ApiResult.Err -> _form.update { it.copy(busy = false, error = result.error) }
            }
        }
    }
}

@Composable
fun SetupScreen(viewModel: SetupViewModel, role: String, edit: Boolean, onBack: (() -> Unit)?, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val load by viewModel.load.collectAsState()
    val form by viewModel.form.collectAsState()
    Column(modifier.fillMaxSize().background(Organic.Bg)) {
        AgriTopBar(stringResource(if (edit) R.string.profile_edit_title else R.string.setup_title), onBack)
        when (val l = load) {
            Load.Loading -> LoadingBlock()
            is Load.Failed -> ErrorState(l.error, viewModel::load)
            is Load.Ready -> Column(
                Modifier.weight(1f).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!edit) Text(stringResource(R.string.setup_sub), style = MaterialTheme.typography.bodyLarge, color = Organic.Neutral700)
                when (role) {
                    "FARMER" -> FarmerFields(form, viewModel)
                    "BUYER" -> BuyerFields(form, viewModel)
                    else -> DriverFields(form, viewModel)
                }
                form.error?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium) }
                AgriButton(
                    stringResource(if (edit) R.string.action_save else R.string.action_continue), { viewModel.save(onDone) },
                    Modifier.fillMaxWidth(), loading = form.busy, enabled = viewModel.canSave(form), height = 56,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 24.dp))
            }
        }
    }
}

@Composable
private fun RegionFields(form: SetupForm, vm: SetupViewModel, withAddressLine: Boolean) {
    DropdownField(
        label = stringResource(R.string.setup_region), options = form.regions.map { it.id to it.nameEn }, selected = form.regionId,
        onSelect = { id -> vm.update { it.copy(regionId = id) } }, placeholder = stringResource(R.string.setup_region_pick),
    )
    PillField(form.zone, { v -> vm.update { it.copy(zone = v) } }, stringResource(R.string.setup_zone))
    PillField(form.town, { v -> vm.update { it.copy(town = v) } }, stringResource(R.string.setup_town))
    if (withAddressLine) PillField(form.addressLine, { v -> vm.update { it.copy(addressLine = v) } }, stringResource(R.string.setup_address_line), singleLine = false, minLines = 2)
}

@Composable
private fun FarmerFields(form: SetupForm, vm: SetupViewModel) {
    Segmented(
        listOf("INDIVIDUAL" to stringResource(R.string.farmer_individual), "COOPERATIVE" to stringResource(R.string.farmer_cooperative)),
        form.type, { t -> vm.update { it.copy(type = t) } }, Modifier.fillMaxWidth(),
    )
    PillField(form.name, { v -> vm.update { it.copy(name = v) } }, stringResource(if (form.type == "COOPERATIVE") R.string.setup_coop_name else R.string.setup_farm_name))
    RegionFields(form, vm, withAddressLine = false)
    PillField(form.landHectares, { v -> vm.update { it.copy(landHectares = v) } }, stringResource(R.string.setup_land), keyboardType = KeyboardType.Decimal, hint = stringResource(R.string.setup_optional))
    PillField(form.payoutNumber, { v -> vm.update { it.copy(payoutNumber = v.filter { c -> c.isDigit() }) } }, stringResource(R.string.setup_payout_telebirr), keyboardType = KeyboardType.Phone, hint = stringResource(R.string.setup_payout_hint))
}

@Composable
private fun BuyerFields(form: SetupForm, vm: SetupViewModel) {
    DropdownField(
        label = stringResource(R.string.setup_business_type), options = buyerTypes.map { (k, res) -> k to stringResource(res) }, selected = form.type,
        onSelect = { t -> vm.update { it.copy(type = t) } },
    )
    PillField(form.name, { v -> vm.update { it.copy(name = v) } }, stringResource(R.string.setup_business_name))
    RegionFields(form, vm, withAddressLine = true)
}

@Composable
private fun DriverFields(form: SetupForm, vm: SetupViewModel) {
    DropdownField(
        label = stringResource(R.string.setup_vehicle_type), options = vehicleTypes.map { (k, res) -> k to stringResource(res) }, selected = form.type,
        onSelect = { t -> vm.update { it.copy(type = t) } },
    )
    PillField(form.vehiclePlate, { v -> vm.update { it.copy(vehiclePlate = v.uppercase()) } }, stringResource(R.string.setup_plate), placeholder = "3-A 41218 AA")
    PillField(form.capacityKg, { v -> vm.update { it.copy(capacityKg = v) } }, stringResource(R.string.setup_capacity), keyboardType = KeyboardType.Decimal, hint = stringResource(R.string.setup_capacity_hint))
    PillField(form.licenseNumber, { v -> vm.update { it.copy(licenseNumber = v) } }, stringResource(R.string.setup_license))
    DropdownField(
        label = stringResource(R.string.setup_home_region), options = form.regions.map { it.id to it.nameEn }, selected = form.regionId,
        onSelect = { id -> vm.update { it.copy(regionId = id) } }, placeholder = stringResource(R.string.setup_region_pick),
    )
}

@StringRes
fun buyerTypeRes(type: String): Int = buyerTypes.firstOrNull { it.first == type }?.second ?: R.string.buyer_type_other

@StringRes
fun vehicleTypeRes(type: String): Int = vehicleTypes.firstOrNull { it.first == type }?.second ?: R.string.vehicle_pickup
