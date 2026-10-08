package com.agrilink.app.ui.driver

import com.agrilink.app.ui.common.RefreshOnResume
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrilink.app.R
import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.AppError
import com.agrilink.app.core.Dates
import com.agrilink.app.core.Format
import com.agrilink.app.data.api.dto.DeliveryDto
import com.agrilink.app.data.api.dto.JobDto
import com.agrilink.app.data.repo.DeliveryRepository
import com.agrilink.app.data.repo.ProfileRepository
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.components.DeliveryStatusTag
import com.agrilink.app.ui.components.EmptyState
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.IconAction
import com.agrilink.app.ui.components.Segmented
import com.agrilink.app.ui.components.SkeletonList
import com.agrilink.app.ui.components.SurfaceCard
import com.agrilink.app.ui.components.Tag
import com.agrilink.app.ui.components.Tone
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JobsUi(
    val jobs: Load<List<JobDto>> = Load.Loading,
    val online: Boolean = false,
    val verified: Boolean = true,
    val togglingAvailability: Boolean = false,
    val capacityKg: Double? = null,
    val refreshing: Boolean = false,
    val error: AppError? = null,
)

/** D1 · the board of open jobs that fit the driver's vehicle, plus the online/offline switch. */
class JobsViewModel(private val delivery: DeliveryRepository, private val profile: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(JobsUi())
    val state: StateFlow<JobsUi> = _state.asStateFlow()

    init {
        loadProfile()
        load()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            (profile.driver() as? ApiResult.Ok)?.value?.let { p ->
                _state.update { it.copy(online = p.availability != "OFFLINE", capacityKg = p.capacityKg, verified = p.verificationStatus == "VERIFIED") }
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        loadProfile()
        load()
    }

    fun load() {
        viewModelScope.launch {
            when (val r = delivery.availableJobs()) {
                is ApiResult.Ok -> _state.update { it.copy(jobs = Load.Ready(r.value.items), refreshing = false) }
                is ApiResult.Err -> _state.update { it.copy(jobs = if (it.jobs is Load.Ready) it.jobs else Load.Failed(r.error), refreshing = false) }
            }
        }
    }

    fun setOnline(online: Boolean) {
        _state.update { it.copy(togglingAvailability = true, error = null) }
        viewModelScope.launch {
            when (val r = delivery.setAvailability(if (online) "AVAILABLE" else "OFFLINE")) {
                is ApiResult.Ok -> _state.update { it.copy(online = r.value.availability != "OFFLINE", togglingAvailability = false) }
                is ApiResult.Err -> _state.update { it.copy(togglingAvailability = false, error = r.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(viewModel: JobsViewModel, unreadNotifications: Int, onOpenJob: (String) -> Unit, onNotifications: () -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { while (true) { delay(20_000); viewModel.load() } }
    RefreshOnResume(viewModel::load)
    Column(modifier.fillMaxSize().background(Organic.Bg)) {
        Row(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.jobs_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            Box {
                IconAction(Icons.Outlined.NotificationsNone, stringResource(R.string.tab_alerts), onNotifications)
                if (unreadNotifications > 0) Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(9.dp).background(Organic.Accent, androidx.compose.foundation.shape.CircleShape))
            }
        }
        SurfaceCard(Modifier.padding(horizontal = 20.dp), color = if (ui.online) Organic.Sage200 else Organic.Surface) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(if (ui.online) R.string.jobs_online else R.string.jobs_offline), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if (ui.online) R.string.jobs_online_hint else R.string.jobs_offline_hint), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
                }
                Switch(ui.online, viewModel::setOnline, enabled = !ui.togglingAvailability, colors = SwitchDefaults.colors(checkedTrackColor = Organic.Accent2, checkedThumbColor = Organic.Bg))
            }
            if (!ui.verified) Text(stringResource(R.string.jobs_unverified), style = MaterialTheme.typography.bodySmall, color = Organic.Accent800)
            ui.error?.let { Text(it.message, style = MaterialTheme.typography.bodySmall, color = Organic.Accent800) }
        }
        PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            when (val jobs = ui.jobs) {
                Load.Loading -> SkeletonList()
                is Load.Failed -> ErrorState(jobs.error, viewModel::load)
                is Load.Ready -> if (jobs.value.isEmpty()) EmptyState(stringResource(R.string.jobs_empty), stringResource(R.string.jobs_empty_body, ui.capacityKg?.let { Format.kg(it) } ?: "—"))
                else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(jobs.value, key = { it.id }) { JobCard(it) { onOpenJob(it.id) } }
                }
            }
        }
    }
}

@Composable
private fun JobCard(job: JobDto, onClick: () -> Unit) {
    SurfaceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Format.etb(job.driverFee), style = MaterialTheme.typography.headlineSmall, color = Organic.Accent700, modifier = Modifier.weight(1f))
            Tag(job.orderNumber, Tone.Outline)
        }
        Text(job.load.joinToString(" · ") { "${Format.number(it.quantity)} ${it.name}" }.ifBlank { Format.kg(job.totalWeightKg) }, style = MaterialTheme.typography.titleMedium)
        Text(routeLine(job.pickup?.town, job.dropoff?.town), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            job.distanceKm?.let { Tag("${Format.number(it, 0)} km", Tone.Neutral) }
            Tag(Format.kg(job.totalWeightKg), Tone.Neutral)
            job.scheduledPickupDate?.let { Tag(Dates.dayLabel(it), Tone.Sage) }
        }
    }
}

fun routeLine(from: String?, to: String?) = "${from ?: "—"} → ${to ?: "—"}"

// ------------------------------------------------------------------------------------------------ trips

enum class TripsTab(val statuses: List<String>) { ACTIVE(listOf("ASSIGNED", "PICKED_UP", "IN_TRANSIT")), HISTORY(listOf("DELIVERED", "CANCELLED")) }

data class TripsUi(val tab: TripsTab = TripsTab.ACTIVE, val trips: Load<List<DeliveryDto>> = Load.Loading, val refreshing: Boolean = false)

class TripsViewModel(private val delivery: DeliveryRepository, initial: TripsTab = TripsTab.ACTIVE) : ViewModel() {
    private val _state = MutableStateFlow(TripsUi(initial))
    val state: StateFlow<TripsUi> = _state.asStateFlow()

    init { load() }

    fun select(tab: TripsTab) {
        _state.update { it.copy(tab = tab, trips = Load.Loading) }
        load()
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        load()
    }

    fun load() {
        val tab = _state.value.tab
        viewModelScope.launch {
            when (val r = delivery.mine(tab.statuses)) {
                is ApiResult.Ok -> _state.update { if (it.tab == tab) it.copy(trips = Load.Ready(r.value.items), refreshing = false) else it }
                is ApiResult.Err -> _state.update { if (it.tab == tab) it.copy(trips = if (it.trips is Load.Ready) it.trips else Load.Failed(r.error), refreshing = false) else it }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(viewModel: TripsViewModel, onOpenTrip: (String) -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { while (true) { delay(30_000); viewModel.load() } }
    RefreshOnResume(viewModel::load)
    Column(modifier.fillMaxSize().background(Organic.Bg)) {
        Text(stringResource(R.string.trips_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp))
        Segmented(
            listOf(TripsTab.ACTIVE to stringResource(R.string.trips_active), TripsTab.HISTORY to stringResource(R.string.trips_history)),
            ui.tab, viewModel::select, Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        )
        PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f).padding(top = 12.dp)) {
            when (val trips = ui.trips) {
                Load.Loading -> SkeletonList()
                is Load.Failed -> ErrorState(trips.error, viewModel::load)
                is Load.Ready -> if (trips.value.isEmpty()) EmptyState(stringResource(if (ui.tab == TripsTab.ACTIVE) R.string.trips_empty_active else R.string.trips_empty_history))
                else LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(trips.value, key = { it.id }) { TripCard(it) { onOpenTrip(it.id) } }
                }
            }
        }
    }
}

@Composable
private fun TripCard(d: DeliveryDto, onClick: () -> Unit) {
    SurfaceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.orderNumber, style = MaterialTheme.typography.labelLarge, color = Organic.Neutral700, modifier = Modifier.weight(1f))
            DeliveryStatusTag(d.status)
        }
        Text(d.load.joinToString(" · ") { "${Format.number(it.quantity)} ${it.name}" }.ifBlank { Format.kg(d.totalWeightKg) }, style = MaterialTheme.typography.titleMedium)
        Text(routeLine(d.pickup?.address?.town, d.dropoff?.address?.town), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(Format.etb(d.driverFee), style = MaterialTheme.typography.titleMedium, color = Organic.Accent700, modifier = Modifier.weight(1f))
            d.deliveredAt?.let { Text(Dates.dateTime(it), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700) }
        }
    }
}
