package com.agrilink.app.ui.buyer

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrilink.app.R
import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.Dates
import com.agrilink.app.core.Format
import com.agrilink.app.data.api.dto.CategoryDto
import com.agrilink.app.data.api.dto.ListingDto
import com.agrilink.app.data.api.dto.OrderListItemDto
import com.agrilink.app.data.prefs.SessionStore
import com.agrilink.app.data.repo.ListingQuery
import com.agrilink.app.data.repo.MarketRepository
import com.agrilink.app.data.repo.OrderRepository
import com.agrilink.app.data.repo.ProfileRepository
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.common.display
import com.agrilink.app.ui.components.AgriImage
import com.agrilink.app.ui.components.Avatar
import com.agrilink.app.ui.components.ChipRow
import com.agrilink.app.ui.components.EmptyState
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.OfflineBanner
import com.agrilink.app.ui.components.SkeletonList
import com.agrilink.app.ui.components.SurfaceCard
import com.agrilink.app.ui.components.unitRes
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketUi(
    val categories: List<CategoryDto> = emptyList(),
    val listings: Load<List<ListingDto>> = Load.Loading,
    val categoryId: String? = null,
    val search: String = "",
    val offlineSavedAt: Long? = null,
    val refreshing: Boolean = false,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val deliverTo: String? = null,
    val reorder: OrderListItemDto? = null,
    val unread: Int = 0,
)

/**
 * The buyer's market. The first page of each distinct query is cached in Room, so the list appears at once on a cold
 * start and still works offline ("prices from 08:12"), then refreshes from the server.
 */
class MarketViewModel(
    private val market: MarketRepository,
    private val orders: OrderRepository,
    private val profile: ProfileRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(MarketUi())
    val state: StateFlow<MarketUi> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            (market.categories() as? ApiResult.Ok)?.let { r -> _state.update { it.copy(categories = r.value) } }
        }
        viewModelScope.launch {
            (profile.buyer() as? ApiResult.Ok)?.value?.let { p ->
                val where = listOfNotNull(p.address?.town, p.businessName).joinToString(", ")
                _state.update { it.copy(deliverTo = where.ifBlank { null }) }
            }
        }
        viewModelScope.launch {
            (orders.list(listOf("COMPLETED")) as? ApiResult.Ok)?.value?.items?.firstOrNull()?.let { last ->
                _state.update { it.copy(reorder = last) }
            }
        }
        load()
    }

    val userName: String get() = session.user?.fullName.orEmpty()

    private fun query(s: MarketUi) = ListingQuery(q = s.search.takeIf { it.isNotBlank() }, categoryId = s.categoryId)

    fun selectCategory(id: String?) {
        _state.update { it.copy(categoryId = id) }
        load()
    }

    fun setSearch(text: String) {
        _state.update { it.copy(search = text) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(350); load() }
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        load(showCacheFirst = false)
    }

    fun load(showCacheFirst: Boolean = true) {
        val q = query(_state.value)
        viewModelScope.launch {
            if (showCacheFirst) {
                market.cached(q)?.let { c ->
                    _state.update { it.copy(listings = Load.Ready(c.page.items), page = 0, hasMore = c.page.hasNext, offlineSavedAt = null) }
                }
            }
            when (val r = market.search(q, 0)) {
                is ApiResult.Ok -> _state.update { it.copy(listings = Load.Ready(r.value.items), page = 0, hasMore = r.value.hasNext, offlineSavedAt = null, refreshing = false) }
                is ApiResult.Err -> _state.update { cur ->
                    val cached = market.cached(q)
                    when {
                        cached != null -> cur.copy(listings = Load.Ready(cached.page.items), hasMore = false, offlineSavedAt = cached.savedAt, refreshing = false)
                        cur.listings is Load.Ready -> cur.copy(refreshing = false, offlineSavedAt = System.currentTimeMillis())
                        else -> cur.copy(listings = Load.Failed(r.error), refreshing = false)
                    }
                }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (!s.hasMore || s.loadingMore) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            when (val r = market.search(query(s), s.page + 1)) {
                is ApiResult.Ok -> _state.update {
                    val current = (it.listings as? Load.Ready)?.value.orEmpty()
                    it.copy(listings = Load.Ready(current + r.value.items), page = s.page + 1, hasMore = r.value.hasNext, loadingMore = false)
                }
                is ApiResult.Err -> _state.update { it.copy(loadingMore = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    viewModel: MarketViewModel,
    unreadNotifications: Int,
    onOpenListing: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    onNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.state.collectAsState()
    Column(modifier.fillMaxSize().background(Organic.Bg)) {
        Row(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                ui.deliverTo?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, null, Modifier.size(16.dp), tint = Organic.Accent700)
                        Text(stringResource(R.string.market_deliver_to, it), style = MaterialTheme.typography.labelMedium, color = Organic.Neutral700, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Text(stringResource(R.string.market_greeting, viewModel.userName.substringBefore(' ')), style = MaterialTheme.typography.headlineSmall)
            }
            Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Organic.Surface).clickable(onClick = onNotifications), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.NotificationsNone, stringResource(R.string.tab_alerts))
                if (unreadNotifications > 0) Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Organic.Accent))
            }
        }
        SearchBar(ui.search, viewModel::setSearch, Modifier.padding(horizontal = 20.dp))
        ChipRow(
            items = listOf<Pair<String?, String>>(null to stringResource(R.string.market_all)) + ui.categories.map { it.id to it.display() },
            selected = ui.categoryId, onSelect = viewModel::selectCategory, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp), contentPadding = PaddingValues(horizontal = 20.dp),
        )
        PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            when (val l = ui.listings) {
                Load.Loading -> SkeletonList(rows = 6, modifier = Modifier.padding(top = 12.dp))
                is Load.Failed -> ErrorState(l.error, { viewModel.load(false) })
                is Load.Ready -> LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    ui.offlineSavedAt?.let { saved ->
                        item { OfflineBanner(stringResource(R.string.market_offline, Dates.time(java.time.Instant.ofEpochMilli(saved).toString())), Modifier.padding(bottom = 10.dp)) }
                    }
                    ui.reorder?.takeIf { ui.search.isBlank() && ui.categoryId == null }?.let { last ->
                        item { ReorderCard(last, onOpenOrder) }
                    }
                    if (l.value.isEmpty()) item { EmptyState(stringResource(R.string.market_empty), stringResource(R.string.market_empty_hint)) }
                    items(l.value, key = { it.id }) { listing -> ListingRow(listing) { onOpenListing(listing.id) } }
                    if (ui.hasMore) item {
                        LaunchedEffect(ui.page) { viewModel.loadMore() }
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp), color = Organic.Accent, strokeWidth = 2.5.dp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)).background(Organic.Surface).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = Organic.Neutral700, modifier = Modifier.size(20.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Organic.Text),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Organic.Accent),
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 15.dp),
            decorationBox = { inner ->
                Box { if (value.isEmpty()) Text(stringResource(R.string.market_search), color = Organic.Neutral600, style = MaterialTheme.typography.bodyLarge); inner() }
            },
        )
    }
}

@Composable
private fun ReorderCard(order: OrderListItemDto, onOpen: (String) -> Unit) {
    SurfaceCard(Modifier.padding(bottom = 12.dp), color = Organic.Accent100, onClick = { onOpen(order.id) }, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Replay, null, tint = Organic.Accent800)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.market_repeat_last), style = MaterialTheme.typography.titleSmall, color = Organic.Accent900)
                Text(order.itemsSummary, style = MaterialTheme.typography.bodySmall, color = Organic.Accent900, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(stringResource(R.string.market_reorder), style = MaterialTheme.typography.labelLarge, color = Organic.Accent700)
        }
    }
}

/** One farmer's offer: avatar, farmer and verification badge, "4.8 · 126 trades · 2.4 t ready · Thu" and the price. */
@Composable
fun ListingRow(listing: ListingDto, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val photo = listing.photos.firstOrNull { it.primary } ?: listing.photos.firstOrNull()
        if (photo != null) AgriImage(photo.url, Modifier.size(56.dp), shape = RoundedCornerShape(18.dp))
        else Avatar(listing.product.nameEn, size = 56.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(listing.farmer.farmName ?: listing.farmer.fullName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (listing.farmer.verified) Icon(Icons.Filled.VerifiedUser, null, Modifier.size(16.dp), tint = Organic.Sage700)
            }
            Text(
                "${listing.product.display()} · ${stringResource(R.string.grade_x, listing.qualityGrade)}",
                style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val ready = Format.kg(listing.quantityAvailable * listing.unitWeightKg)
            val trust = if (listing.farmer.completedTrades > 0) "${Format.number(listing.farmer.ratingAverage, 1)} · ${stringResource(R.string.trades_count, listing.farmer.completedTrades)}" else stringResource(R.string.farmer_new)
            Text("$trust · $ready ${stringResource(R.string.ready)}", style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Format.price(listing.pricePerUnit), style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp))
            Text("ETB/${stringResource(unitRes(listing.unit))}", style = MaterialTheme.typography.labelSmall, color = Organic.Neutral700)
            listing.distanceKm?.let { Text("${Format.number(it, 0)} km", style = MaterialTheme.typography.labelSmall, color = Organic.Neutral700) }
        }
    }
    androidx.compose.material3.HorizontalDivider(color = Organic.Divider)
}

