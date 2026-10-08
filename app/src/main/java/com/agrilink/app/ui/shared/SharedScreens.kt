package com.agrilink.app.ui.shared

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.AlertDialog
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
import com.agrilink.app.core.Phone
import com.agrilink.app.data.api.dto.DisputeDto
import com.agrilink.app.data.api.dto.NotificationDto
import com.agrilink.app.data.api.dto.WalletDto
import com.agrilink.app.data.api.dto.WalletTransactionDto
import com.agrilink.app.data.prefs.SessionStore
import com.agrilink.app.data.repo.AuthRepository
import com.agrilink.app.data.repo.NotificationRepository
import com.agrilink.app.data.repo.OrderRepository
import com.agrilink.app.data.repo.WalletRepository
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.common.LocaleHelper
import com.agrilink.app.ui.common.errorText
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.AgriImage
import com.agrilink.app.ui.components.AgriTopBar
import com.agrilink.app.ui.components.Avatar
import com.agrilink.app.ui.components.ButtonKind
import com.agrilink.app.ui.components.ChipRow
import com.agrilink.app.ui.components.EmptyState
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.FactRow
import com.agrilink.app.ui.components.HintText
import com.agrilink.app.ui.components.LoadingBlock
import com.agrilink.app.ui.components.PillField
import com.agrilink.app.ui.components.SectionTitle
import com.agrilink.app.ui.components.Segmented
import com.agrilink.app.ui.components.SkeletonList
import com.agrilink.app.ui.components.SoftDivider
import com.agrilink.app.ui.components.SurfaceCard
import com.agrilink.app.ui.components.Tag
import com.agrilink.app.ui.components.Tone
import com.agrilink.app.ui.components.VerificationTag
import com.agrilink.app.ui.components.paymentMethodRes
import com.agrilink.app.ui.orders.UiMessage
import com.agrilink.app.ui.orders.disputeTypes
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ------------------------------------------------------------------------------------------------ wallet

data class WalletUi(
    val wallet: WalletDto = WalletDto(),
    val transactions: List<WalletTransactionDto> = emptyList(),
    val hasMore: Boolean = false,
    val page: Int = 0,
    val refreshing: Boolean = false,
    val withdrawing: Boolean = false,
    val showWithdraw: Boolean = false,
    val amount: String = "",
    val method: String = "TELEBIRR",
    val account: String = "",
    val accountName: String = "",
    val withdrawError: AppError? = null,
)

class WalletViewModel(private val repo: WalletRepository, private val session: SessionStore) : ViewModel() {
    private val _state = MutableStateFlow<Load<WalletUi>>(Load.Loading)
    val state: StateFlow<Load<WalletUi>> = _state.asStateFlow()
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init { load() }

    private fun edit(block: (WalletUi) -> WalletUi) = _state.update { if (it is Load.Ready) Load.Ready(block(it.value)) else it }
    private val current get() = (_state.value as? Load.Ready)?.value

    fun refresh() { edit { it.copy(refreshing = true) }; load() }

    fun load() {
        viewModelScope.launch {
            val wallet = repo.wallet()
            val tx = repo.transactions(0)
            if (wallet is ApiResult.Err) { if (current == null) _state.value = Load.Failed(wallet.error) else edit { it.copy(refreshing = false) }; return@launch }
            val w = (wallet as ApiResult.Ok).value
            val keep = current
            _state.value = Load.Ready(
                (keep ?: WalletUi()).copy(
                    wallet = w, refreshing = false,
                    transactions = (tx as? ApiResult.Ok)?.value?.items ?: keep?.transactions.orEmpty(),
                    hasMore = (tx as? ApiResult.Ok)?.value?.hasNext ?: false, page = 0,
                ),
            )
        }
    }

    fun loadMore() {
        val ui = current ?: return
        if (!ui.hasMore) return
        viewModelScope.launch {
            (repo.transactions(ui.page + 1) as? ApiResult.Ok)?.let { r -> edit { it.copy(transactions = it.transactions + r.value.items, hasMore = r.value.hasNext, page = it.page + 1) } }
        }
    }

    fun openWithdraw(show: Boolean) = edit {
        it.copy(showWithdraw = show, withdrawError = null, amount = if (show) Format.number(it.wallet.balance) else "", account = if (show && it.account.isBlank()) session.user?.phone?.let(Phone::display).orEmpty().replace(" ", "") else it.account, accountName = if (show && it.accountName.isBlank()) session.user?.fullName.orEmpty() else it.accountName)
    }

    fun setAmount(v: String) = edit { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }.take(10), withdrawError = null) }
    fun setMethod(m: String) = edit { it.copy(method = m, withdrawError = null) }
    fun setAccount(v: String) = edit { it.copy(account = v.take(40), withdrawError = null) }
    fun setAccountName(v: String) = edit { it.copy(accountName = v.take(100), withdrawError = null) }

    fun withdraw() {
        val ui = current ?: return
        val amount = ui.amount.toDoubleOrNull() ?: return
        edit { it.copy(withdrawing = true, withdrawError = null) }
        viewModelScope.launch {
            when (val r = repo.withdraw(amount, ui.method, ui.account.trim(), ui.accountName.trim())) {
                is ApiResult.Ok -> {
                    _messages.tryEmit(UiMessage.Res(if (r.value.status == "FAILED") R.string.wallet_withdraw_failed else R.string.wallet_withdraw_done))
                    edit { it.copy(withdrawing = false, showWithdraw = false) }
                    load()
                }
                is ApiResult.Err -> edit { it.copy(withdrawing = false, withdrawError = r.error) }
            }
        }
    }
}

private val payoutMethods = listOf("TELEBIRR" to R.string.method_telebirr, "CBE_BIRR" to R.string.method_cbe_birr, "BANK_ACCOUNT" to R.string.method_bank)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: WalletViewModel, titleRes: Int, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    RefreshOnResume(viewModel::load)
    LaunchedEffect(Unit) { viewModel.messages.collect { m -> snackbar.showSnackbar(when (m) { is UiMessage.Res -> context.getString(m.id); is UiMessage.Raw -> m.text }) } }
    Box(modifier.fillMaxSize().background(Organic.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp))
            when (val s = state) {
                Load.Loading -> SkeletonList(3)
                is Load.Failed -> ErrorState(s.error, viewModel::load)
                is Load.Ready -> PullToRefreshBox(isRefreshing = s.value.refreshing, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
                    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            SurfaceCard(color = Organic.Sage200) {
                                Text(stringResource(R.string.wallet_available), style = MaterialTheme.typography.labelMedium, color = Organic.Sage900)
                                Text(Format.etb(s.value.wallet.balance), style = MaterialTheme.typography.displaySmall, color = Organic.Sage900)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.wallet_held), style = MaterialTheme.typography.bodyMedium, color = Organic.Sage900, modifier = Modifier.weight(1f))
                                    Text(Format.etb(s.value.wallet.heldForRelease), style = MaterialTheme.typography.titleMedium, color = Organic.Sage900)
                                }
                                Text(stringResource(R.string.wallet_held_hint), style = MaterialTheme.typography.bodySmall, color = Organic.Sage800)
                                AgriButton(stringResource(R.string.wallet_withdraw), { viewModel.openWithdraw(true) }, Modifier.fillMaxWidth(), enabled = s.value.wallet.balance >= 10.0)
                            }
                        }
                        item { SectionTitle(stringResource(R.string.wallet_history)) }
                        if (s.value.transactions.isEmpty()) item { HintText(stringResource(R.string.wallet_no_history)) }
                        items(s.value.transactions, key = { it.id }) { TransactionRow(it) }
                        if (s.value.hasMore) item { AgriButton(stringResource(R.string.action_load_more), viewModel::loadMore, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, height = 46) }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
    (state as? Load.Ready)?.value?.takeIf { it.showWithdraw }?.let { ui -> WithdrawDialog(ui, viewModel) }
}

@Composable
private fun TransactionRow(t: WalletTransactionDto) {
    val credit = t.direction == "CREDIT"
    SurfaceCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(when (t.type) { "ESCROW_RELEASE" -> R.string.tx_earning; "WITHDRAWAL" -> R.string.tx_withdrawal; "WITHDRAWAL_REVERSAL" -> R.string.tx_reversal; else -> R.string.tx_adjustment }), style = MaterialTheme.typography.titleMedium)
                t.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700) }
                Text(Dates.dateTime(t.createdAt), style = MaterialTheme.typography.labelSmall, color = Organic.Neutral700)
            }
            Text((if (credit) "+" else "−") + Format.etb(t.amount), style = MaterialTheme.typography.titleMedium, color = if (credit) Organic.Sage700 else Organic.Accent700)
        }
    }
}

@Composable
private fun WithdrawDialog(ui: WalletUi, vm: WalletViewModel) {
    val amount = ui.amount.toDoubleOrNull() ?: 0.0
    val valid = amount >= 10.0 && amount <= ui.wallet.balance && ui.account.isNotBlank() && ui.accountName.isNotBlank()
    AlertDialog(
        onDismissRequest = { if (!ui.withdrawing) vm.openWithdraw(false) },
        containerColor = Organic.Bg,
        title = { Text(stringResource(R.string.wallet_withdraw), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PillField(ui.amount, vm::setAmount, stringResource(R.string.wallet_amount), keyboardType = KeyboardType.Decimal, prefix = "ETB ", hint = stringResource(R.string.wallet_available_x, Format.etb(ui.wallet.balance)), error = if (ui.amount.isNotBlank() && (amount < 10.0 || amount > ui.wallet.balance)) stringResource(R.string.wallet_amount_error) else null)
                ChipRow(payoutMethods.map { it.first to stringResource(it.second) }, ui.method, { it?.let(vm::setMethod) })
                PillField(ui.account, vm::setAccount, stringResource(R.string.wallet_account), keyboardType = KeyboardType.Phone)
                PillField(ui.accountName, vm::setAccountName, stringResource(R.string.wallet_account_name))
                ui.withdrawError?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { AgriButton(stringResource(R.string.wallet_withdraw), vm::withdraw, loading = ui.withdrawing, enabled = valid, height = 46) },
        dismissButton = { TextButton({ vm.openWithdraw(false) }, enabled = !ui.withdrawing) { Text(stringResource(R.string.action_cancel)) } },
    )
}

// ------------------------------------------------------------------------------------------------ notifications

data class NotificationsUi(val items: Load<List<NotificationDto>> = Load.Loading, val refreshing: Boolean = false)

class NotificationsViewModel(private val repo: NotificationRepository) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUi())
    val state: StateFlow<NotificationsUi> = _state.asStateFlow()

    init { load() }

    fun refresh() { _state.update { it.copy(refreshing = true) }; load() }

    fun load() {
        viewModelScope.launch {
            when (val r = repo.list()) {
                is ApiResult.Ok -> _state.update { it.copy(items = Load.Ready(r.value.items), refreshing = false) }
                is ApiResult.Err -> _state.update { it.copy(items = if (it.items is Load.Ready) it.items else Load.Failed(r.error), refreshing = false) }
            }
        }
    }

    fun open(n: NotificationDto) {
        if (n.read) return
        _state.update { s -> s.copy(items = (s.items as? Load.Ready)?.let { r -> Load.Ready(r.value.map { if (it.id == n.id) it.copy(read = true) else it }) } ?: s.items) }
        viewModelScope.launch { repo.markRead(n.id) }
    }

    fun markAll() {
        _state.update { s -> s.copy(items = (s.items as? Load.Ready)?.let { r -> Load.Ready(r.value.map { it.copy(read = true) }) } ?: s.items) }
        viewModelScope.launch { repo.markAllRead() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel, onBack: (() -> Unit)?, onOpen: (NotificationDto) -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.state.collectAsState()
    Column(modifier.fillMaxSize().background(Organic.Bg)) {
        AgriTopBar(stringResource(R.string.notifications_title), onBack) {
            val hasUnread = (ui.items as? Load.Ready)?.value?.any { !it.read } == true
            if (hasUnread) TextButton(viewModel::markAll) { Text(stringResource(R.string.notifications_mark_all)) }
        }
        PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            when (val items = ui.items) {
                Load.Loading -> SkeletonList()
                is Load.Failed -> ErrorState(items.error, viewModel::load)
                is Load.Ready -> if (items.value.isEmpty()) EmptyState(stringResource(R.string.notifications_empty), stringResource(R.string.notifications_empty_body), icon = Icons.Outlined.NotificationsNone)
                else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items.value, key = { it.id }) { n ->
                        SurfaceCard(color = if (n.read) Organic.Surface else Organic.Accent100, onClick = { viewModel.open(n); onOpen(n) }, padding = 16.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!n.read) Box(Modifier.size(9.dp).clip(CircleShape).background(Organic.Accent))
                                Text(n.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(Dates.ago(n.createdAt), style = MaterialTheme.typography.labelSmall, color = Organic.Neutral700)
                            }
                            Text(n.body, style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral800)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ account

val LANGUAGES = listOf("en" to "English", "am" to "አማርኛ", "om" to "Afaan Oromoo")

class AccountViewModel(private val auth: AuthRepository, private val session: SessionStore) : ViewModel() {
    val user get() = session.user
    private val _language = MutableStateFlow(LocaleHelper.current())
    val language: StateFlow<String> = _language.asStateFlow()
    private val _signingOut = MutableStateFlow(false)
    val signingOut: StateFlow<Boolean> = _signingOut.asStateFlow()

    init { viewModelScope.launch { auth.refreshMe() } }

    fun setLanguage(code: String) {
        _language.value = code
        viewModelScope.launch { auth.setLanguage(code) }
        LocaleHelper.apply(code)
    }

    fun signOut() {
        _signingOut.value = true
        viewModelScope.launch { auth.logout() }
    }
}

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    sessionUser: com.agrilink.app.data.api.dto.UserDto?,
    onEditProfile: () -> Unit,
    onVerification: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language by viewModel.language.collectAsState()
    val signingOut by viewModel.signingOut.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    val user = sessionUser ?: return
    Column(modifier.fillMaxSize().background(Organic.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.account_title), style = MaterialTheme.typography.headlineMedium)
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (user.profilePhotoUrl != null) AgriImage(user.profilePhotoUrl, Modifier.size(64.dp), shape = CircleShape) else Avatar(user.fullName, size = 64.dp)
                Column(Modifier.weight(1f)) {
                    Text(user.fullName, style = MaterialTheme.typography.titleLarge)
                    Text(Phone.display(user.phone), style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        Tag(stringResource(when (user.role) { "FARMER" -> R.string.role_farmer; "DRIVER" -> R.string.role_driver; else -> R.string.role_buyer }), Tone.Accent)
                        if (user.ratingCount > 0) Tag("★ ${Format.number(user.ratingAverage, 1)} (${user.ratingCount})", Tone.Neutral)
                    }
                }
            }
        }
        AccountRow(stringResource(R.string.account_edit_profile), null, onEditProfile)
        AccountRow(stringResource(R.string.account_verification), { VerificationTag(user.verificationStatus) }, onVerification)
        SurfaceCard {
            Text(stringResource(R.string.account_language), style = MaterialTheme.typography.titleMedium)
            Segmented(LANGUAGES.map { it.first to it.second }, language, viewModel::setLanguage, Modifier.fillMaxWidth())
        }
        AgriButton(stringResource(R.string.account_sign_out), { confirm = true }, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, loading = signingOut)
        Text(stringResource(R.string.account_version, com.agrilink.app.BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall, color = Organic.Neutral600, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.account_sign_out_title)) },
        text = { Text(stringResource(R.string.account_sign_out_body)) },
        confirmButton = { TextButton({ confirm = false; viewModel.signOut() }) { Text(stringResource(R.string.account_sign_out)) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun AccountRow(title: String, trailing: (@Composable () -> Unit)?, onClick: () -> Unit) {
    SurfaceCard(onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            trailing?.invoke()
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, Modifier.size(14.dp), tint = Organic.Neutral600)
        }
    }
}

// ------------------------------------------------------------------------------------------------ dispute

class DisputeViewModel(private val disputeId: String, private val orders: OrderRepository) : ViewModel() {
    private val _state = MutableStateFlow<Load<DisputeDto>>(Load.Loading)
    val state: StateFlow<Load<DisputeDto>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            when (val r = orders.dispute(disputeId)) {
                is ApiResult.Ok -> _state.value = Load.Ready(r.value)
                is ApiResult.Err -> if (_state.value !is Load.Ready) _state.value = Load.Failed(r.error)
            }
        }
    }
}

@Composable
fun DisputeScreen(viewModel: DisputeViewModel, onBack: () -> Unit, onOpenOrder: (String) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    Column(modifier.fillMaxSize().background(Organic.Bg)) {
        AgriTopBar(stringResource(R.string.dispute_title), onBack)
        when (val s = state) {
            Load.Loading -> LoadingBlock()
            is Load.Failed -> ErrorState(s.error, viewModel::load)
            is Load.Ready -> {
                val d = s.value
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SurfaceCard(color = if (d.status == "RESOLVED") Organic.Sage200 else Organic.Accent100) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(d.disputeNumber, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Tag(stringResource(when (d.status) { "OPEN" -> R.string.dispute_status_open; "UNDER_REVIEW" -> R.string.dispute_status_review; else -> R.string.dispute_status_resolved }), if (d.status == "RESOLVED") Tone.SolidSage else Tone.Accent)
                        }
                        Text(stringResource(disputeTypes.firstOrNull { it.first == d.type }?.second ?: R.string.dispute_other), style = MaterialTheme.typography.titleMedium)
                        d.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        if (d.status != "RESOLVED") Text(stringResource(R.string.dispute_wait_hint), style = MaterialTheme.typography.bodySmall, color = Organic.Neutral800)
                    }
                    SurfaceCard {
                        FactRow(stringResource(R.string.dispute_order), d.orderNumber)
                        FactRow(stringResource(R.string.dispute_held), Format.etb(d.totalHeldAmount))
                        d.claimedReceivedQuantityKg?.let { FactRow(stringResource(R.string.dispute_received), Format.kg(it)) }
                        FactRow(stringResource(R.string.dispute_ordered), Format.kg(d.orderedWeightKg))
                        d.raisedBy?.let { FactRow(stringResource(R.string.dispute_raised_by), it.fullName) }
                        d.dueAt?.let { FactRow(stringResource(R.string.dispute_due), Dates.dateTime(it)) }
                    }
                    d.resolution?.let { r ->
                        SurfaceCard(color = Organic.Sage200) {
                            Text(stringResource(R.string.dispute_resolution), style = MaterialTheme.typography.titleLarge, color = Organic.Sage900)
                            FactRow(stringResource(R.string.dispute_to_farmer), Format.etb(r.farmerAmount), valueColor = Organic.Sage900)
                            FactRow(stringResource(R.string.dispute_to_driver), Format.etb(r.driverAmount), valueColor = Organic.Sage900)
                            FactRow(stringResource(R.string.dispute_to_buyer), Format.etb(r.buyerRefundAmount), valueColor = Organic.Sage900)
                            r.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Organic.Sage900) }
                        }
                    }
                    if (d.evidence.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.dispute_evidence))
                        d.evidence.forEach { e ->
                            SurfaceCard(padding = 14.dp) {
                                e.fileUrl?.let { AgriImage(it, Modifier.fillMaxWidth().size(width = 280.dp, height = 180.dp)) }
                                e.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                Text("${e.submittedBy?.fullName.orEmpty()} · ${Dates.dateTime(e.createdAt)}", style = MaterialTheme.typography.labelSmall, color = Organic.Neutral700)
                            }
                        }
                    }
                    AgriButton(stringResource(R.string.dispute_open_order), { onOpenOrder(d.orderId) }, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
                }
            }
        }
    }
}

