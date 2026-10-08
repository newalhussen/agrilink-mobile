package com.agrilink.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agrilink.app.R
import com.agrilink.app.core.ApiResult
import com.agrilink.app.data.api.dto.NotificationDto
import com.agrilink.app.ui.auth.AuthViewModel
import com.agrilink.app.ui.auth.LanguageScreen
import com.agrilink.app.ui.auth.OtpScreen
import com.agrilink.app.ui.auth.PhoneScreen
import com.agrilink.app.ui.auth.RoleScreen
import com.agrilink.app.ui.auth.SetupScreen
import com.agrilink.app.ui.auth.SetupViewModel
import com.agrilink.app.ui.auth.VerificationScreen
import com.agrilink.app.ui.auth.VerificationViewModel
import com.agrilink.app.ui.buyer.CheckoutScreen
import com.agrilink.app.ui.buyer.CheckoutViewModel
import com.agrilink.app.ui.buyer.ListingDetailScreen
import com.agrilink.app.ui.buyer.ListingDetailViewModel
import com.agrilink.app.ui.buyer.MarketScreen
import com.agrilink.app.ui.buyer.MarketViewModel
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.common.LocaleHelper
import com.agrilink.app.ui.common.SystemNotifications
import com.agrilink.app.ui.common.appViewModel
import com.agrilink.app.ui.common.rememberContainer
import com.agrilink.app.ui.components.AgriBottomBar
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.ButtonKind
import com.agrilink.app.ui.components.NavTab
import com.agrilink.app.ui.driver.DeliveryScreen
import com.agrilink.app.ui.driver.DeliveryViewModel
import com.agrilink.app.ui.driver.JobsScreen
import com.agrilink.app.ui.driver.JobsViewModel
import com.agrilink.app.ui.driver.TripsScreen
import com.agrilink.app.ui.driver.TripsTab
import com.agrilink.app.ui.driver.TripsViewModel
import com.agrilink.app.ui.farmer.AddProduceScreen
import com.agrilink.app.ui.farmer.AddProduceViewModel
import com.agrilink.app.ui.farmer.EditListingScreen
import com.agrilink.app.ui.farmer.EditListingViewModel
import com.agrilink.app.ui.farmer.FarmerTodayScreen
import com.agrilink.app.ui.farmer.FarmerTodayViewModel
import com.agrilink.app.ui.farmer.ProduceScreen
import com.agrilink.app.ui.farmer.ProduceViewModel
import com.agrilink.app.ui.nav.DeepLink
import com.agrilink.app.ui.nav.Routes
import com.agrilink.app.ui.orders.HandoverScreen
import com.agrilink.app.ui.orders.OrderDetailScreen
import com.agrilink.app.ui.orders.OrderDetailViewModel
import com.agrilink.app.ui.orders.OrdersScreen
import com.agrilink.app.ui.orders.OrdersTab
import com.agrilink.app.ui.orders.OrdersViewModel
import com.agrilink.app.ui.orders.PayScreen
import com.agrilink.app.ui.orders.PayViewModel
import com.agrilink.app.ui.orders.ReportScreen
import com.agrilink.app.ui.orders.ReportViewModel
import com.agrilink.app.ui.shared.AccountScreen
import com.agrilink.app.ui.shared.AccountViewModel
import com.agrilink.app.ui.shared.DisputeScreen
import com.agrilink.app.ui.shared.DisputeViewModel
import com.agrilink.app.ui.shared.NotificationsScreen
import com.agrilink.app.ui.shared.NotificationsViewModel
import com.agrilink.app.ui.shared.WalletScreen
import com.agrilink.app.ui.shared.WalletViewModel
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val farmerTabRoutes = listOf(Routes.FARMER_TODAY, Routes.FARMER_PRODUCE, Routes.FARMER_ORDERS, Routes.FARMER_WALLET, Routes.ACCOUNT)
private val buyerTabRoutes = listOf(Routes.BUYER_MARKET, Routes.BUYER_ORDERS, Routes.BUYER_ALERTS, Routes.ACCOUNT)
private val driverTabRoutes = listOf(Routes.DRIVER_JOBS, Routes.DRIVER_TRIPS, Routes.DRIVER_EARNINGS, Routes.ACCOUNT)

private fun tabsFor(role: String?, unread: Int): List<NavTab> = when (role) {
    "FARMER" -> listOf(
        NavTab(Routes.FARMER_TODAY, R.string.tab_today, Icons.Outlined.Home),
        NavTab(Routes.FARMER_PRODUCE, R.string.tab_produce, Icons.Outlined.Agriculture),
        NavTab(Routes.FARMER_ORDERS, R.string.tab_orders, Icons.Outlined.Inventory2),
        NavTab(Routes.FARMER_WALLET, R.string.tab_money, Icons.Outlined.AccountBalanceWallet),
        NavTab(Routes.ACCOUNT, R.string.tab_account, Icons.Outlined.AccountCircle),
    )
    "BUYER" -> listOf(
        NavTab(Routes.BUYER_MARKET, R.string.tab_market, Icons.Outlined.Storefront),
        NavTab(Routes.BUYER_ORDERS, R.string.tab_orders, Icons.Outlined.ReceiptLong),
        NavTab(Routes.BUYER_ALERTS, R.string.tab_alerts, Icons.Outlined.Notifications, badge = unread),
        NavTab(Routes.ACCOUNT, R.string.tab_account, Icons.Outlined.AccountCircle),
    )
    "DRIVER" -> listOf(
        NavTab(Routes.DRIVER_JOBS, R.string.tab_jobs, Icons.Outlined.Work),
        NavTab(Routes.DRIVER_TRIPS, R.string.tab_trips, Icons.Outlined.LocalShipping),
        NavTab(Routes.DRIVER_EARNINGS, R.string.tab_earnings, Icons.Outlined.Payments),
        NavTab(Routes.ACCOUNT, R.string.tab_account, Icons.Outlined.AccountCircle),
    )
    else -> emptyList()
}

private fun NavBackStackEntry.arg(name: String): String = arguments?.getString(name).orEmpty()

private var tabHome: String = Routes.LANGUAGE

private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(tabHome) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Sends the user to the screen a notification refers to. */
private fun NavHostController.openReference(role: String?, type: String?, id: String?) {
    when {
        type == "ORDER" && id != null && role == "DRIVER" -> goTab(Routes.DRIVER_TRIPS)
        type == "ORDER" && id != null -> navigate(Routes.order(id))
        type == "DISPUTE" && id != null -> navigate(Routes.dispute(id))
        type == "DELIVERY" && id != null && role == "DRIVER" -> navigate(Routes.delivery(id))
        type == "WALLET" || type == "PAYOUT" -> goTab(if (role == "DRIVER") Routes.DRIVER_EARNINGS else Routes.FARMER_WALLET)
        type == "VERIFICATION" -> navigate(Routes.verification(true))
        else -> Unit
    }
}

@Composable
fun AgriLinkRoot(deepLink: DeepLink?, onDeepLinkHandled: () -> Unit) {
    val container = rememberContainer()
    val nav = rememberNavController()
    val session by container.session.session.collectAsState()
    val chosenLanguage by container.settings.language.collectAsState()
    val user = session?.user
    val role = user?.role
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    var unread by remember { mutableIntStateOf(0) }
    val authViewModel = appViewModel { AuthViewModel(it.auth, it.settings) }

    val start = remember {
        when {
            container.session.user != null -> Routes.home(container.session.user?.role)
            container.settings.language.value == null -> Routes.LANGUAGE
            container.settings.onboardingRole.value == null -> Routes.ROLE
            else -> Routes.phone(true)
        }
    }

    // Local-notification permission (Android 13+), asked once the person is signed in.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(user?.id) {
        if (user != null && Build.VERSION.SDK_INT >= 33 && !SystemNotifications.canPost(container.context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Unread badge.
    LaunchedEffect(user?.id) {
        while (user != null) {
            (container.notifications.unreadCount() as? ApiResult.Ok)?.let { unread = it.value }
            delay(30_000)
        }
        unread = 0
    }

    // Signed out (or the session expired): back to the sign-in screen.
    LaunchedEffect(session == null) {
        if (session == null && route != null && route !in Routes.authRoutes) {
            nav.navigate(Routes.phone(true)) { popUpTo(0) { inclusive = true } }
        }
    }
    LaunchedEffect(Unit) {
        container.session.expired.collect { snackbar.showSnackbar(container.context.getString(R.string.session_expired)) }
    }

    // Notification tapped while the app is open or closed.
    LaunchedEffect(deepLink, user?.id, route) {
        val link = deepLink ?: return@LaunchedEffect
        if (user == null || route == null || route in Routes.authRoutes) return@LaunchedEffect
        nav.openReference(role, link.type, link.id)
        onDeepLinkHandled()
    }

    val tabRoutes = when (role) { "FARMER" -> farmerTabRoutes; "BUYER" -> buyerTabRoutes; "DRIVER" -> driverTabRoutes; else -> emptyList() }
    val showBar = route in tabRoutes
    tabHome = Routes.home(role)

    Scaffold(
        containerColor = Organic.Bg,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (showBar) AgriBottomBar(tabsFor(role, unread), route, nav::goTab) },
    ) { inner ->
        val bottom = PaddingValues(bottom = inner.calculateBottomPadding())
        NavHost(nav, startDestination = start, modifier = Modifier.padding(bottom)) {
            // ------------------------------------------------------------------ onboarding & sign-in
            composable(Routes.LANGUAGE) {
                LanguageScreen(
                    initial = chosenLanguage,
                    onContinue = { code ->
                        container.settings.setLanguage(code)
                        LocaleHelper.apply(code)
                        nav.navigate(Routes.ROLE)
                    },
                )
            }
            composable(Routes.ROLE) {
                RoleScreen(
                    onChosen = { chosen -> container.settings.setOnboardingRole(chosen); nav.navigate(Routes.phone(false)) },
                    onSignIn = { nav.navigate(Routes.phone(true)) },
                    onBack = { if (!nav.popBackStack()) nav.navigate(Routes.LANGUAGE) },
                )
            }
            composable(Routes.PHONE, arguments = listOf(navArgument("login") { type = NavType.BoolType })) { entry ->
                val login = entry.arguments?.getBoolean("login") ?: true
                PhoneScreen(
                    viewModel = authViewModel, login = login,
                    onBack = { if (!nav.popBackStack()) nav.navigate(Routes.LANGUAGE) },
                    onCodeSent = { nav.navigate(Routes.OTP) },
                    onSignedIn = { signedIn(nav, container.session.user?.role) },
                    onSwitchMode = { nav.navigate(if (login) Routes.ROLE else Routes.phone(true)) { popUpTo(Routes.PHONE) { inclusive = true } } },
                )
            }
            composable(Routes.OTP) {
                OtpScreen(authViewModel, onBack = { nav.popBackStack() }, onDone = { newAccount ->
                    if (newAccount) nav.navigate(Routes.SETUP) { popUpTo(0) { inclusive = true } } else signedIn(nav, container.session.user?.role)
                })
            }
            composable(Routes.SETUP) {
                val setupRole = container.session.user?.role ?: "BUYER"
                val vm = appViewModel { SetupViewModel(setupRole, it.profile, it.market, it.session.user?.fullName.orEmpty()) }
                SetupScreen(vm, setupRole, edit = false, onBack = null, onDone = { nav.navigate(Routes.verification(false)) { popUpTo(Routes.SETUP) { inclusive = true } } })
            }
            composable(Routes.VERIFICATION, arguments = listOf(navArgument("fromAccount") { type = NavType.BoolType })) { entry ->
                val fromAccount = entry.arguments?.getBoolean("fromAccount") ?: false
                val vm = appViewModel { VerificationViewModel(it.profile, it.auth) }
                VerificationScreen(
                    vm, role ?: "BUYER",
                    onBack = if (fromAccount) ({ nav.popBackStack() }) else null,
                    onContinue = { if (fromAccount) nav.popBackStack() else signedIn(nav, role) },
                    onAddProduce = if (role == "FARMER") ({ signedIn(nav, role); nav.navigate(Routes.PRODUCE_ADD) }) else null,
                )
            }
            composable(Routes.UNSUPPORTED) {
                Column(Modifier.fillMaxSize().background(Organic.Bg).padding(28.dp), verticalArrangement = Arrangement.Center) {
                    Text(stringResource(R.string.unsupported_title), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.unsupported_body), style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 12.dp))
                    AgriButton(stringResource(R.string.account_sign_out), { signOutNow(container) }, kind = ButtonKind.Secondary)
                }
            }

            // ------------------------------------------------------------------ farmer tabs
            composable(Routes.FARMER_TODAY) {
                val vm = appViewModel { FarmerTodayViewModel(it.orders, it.wallet, it.session, it.outbox) }
                FarmerTodayScreen(
                    vm, unread,
                    onOpenOrder = { nav.navigate(Routes.order(it)) },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                    onVerification = { nav.navigate(Routes.verification(true)) },
                    onWallet = { nav.goTab(Routes.FARMER_WALLET) },
                    onAddProduce = { nav.navigate(Routes.PRODUCE_ADD) },
                )
            }
            composable(Routes.FARMER_PRODUCE) {
                val vm = appViewModel { ProduceViewModel(it.market) }
                ProduceScreen(vm, onAdd = { nav.navigate(Routes.PRODUCE_ADD) }, onEdit = { nav.navigate(Routes.produceEdit(it)) })
            }
            composable(Routes.FARMER_ORDERS) {
                val vm = appViewModel { OrdersViewModel(it.orders, OrdersTab.NEW) }
                OrdersScreen(
                    vm, listOf(OrdersTab.NEW to R.string.orders_new, OrdersTab.ACTIVE_NOT_NEW to R.string.orders_active, OrdersTab.DONE to R.string.orders_done),
                    role = "FARMER", onOpen = { nav.navigate(Routes.order(it)) },
                )
            }
            composable(Routes.FARMER_WALLET) {
                val vm = appViewModel { WalletViewModel(it.wallet, it.session) }
                WalletScreen(vm, R.string.wallet_title_farmer)
            }
            composable(Routes.PRODUCE_ADD) {
                val vm = appViewModel { AddProduceViewModel(it.market, it.profile) }
                AddProduceScreen(vm, onClose = { nav.popBackStack() }, onDone = { nav.navigate(Routes.FARMER_PRODUCE) { popUpTo(Routes.PRODUCE_ADD) { inclusive = true }; launchSingleTop = true } })
            }
            composable(Routes.PRODUCE_EDIT, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arg("id")
                val vm = appViewModel(key = "edit-$id") { EditListingViewModel(id, it.market) }
                EditListingScreen(vm, onBack = { nav.popBackStack() })
            }

            // ------------------------------------------------------------------ buyer tabs
            composable(Routes.BUYER_MARKET) {
                val vm = appViewModel { MarketViewModel(it.market, it.orders, it.profile, it.session) }
                MarketScreen(
                    vm, unread,
                    onOpenListing = { nav.navigate(Routes.listing(it)) },
                    onOpenOrder = { nav.navigate(Routes.order(it)) },
                    onNotifications = { nav.goTab(Routes.BUYER_ALERTS) },
                )
            }
            composable(Routes.BUYER_ORDERS) {
                val vm = appViewModel { OrdersViewModel(it.orders, OrdersTab.ACTIVE) }
                OrdersScreen(vm, listOf(OrdersTab.ACTIVE to R.string.orders_active, OrdersTab.DONE to R.string.orders_done), role = "BUYER", onOpen = { nav.navigate(Routes.order(it)) })
            }
            composable(Routes.BUYER_ALERTS) {
                val vm = appViewModel { NotificationsViewModel(it.notifications) }
                NotificationsScreen(vm, onBack = null, onOpen = { n -> openFrom(nav, role, n) })
            }
            composable(Routes.LISTING, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arg("id")
                val vm = appViewModel(key = "listing-$id") { ListingDetailViewModel(id, it.market, it.orders, it.profile) }
                ListingDetailScreen(vm, onBack = { nav.popBackStack() }, onOrder = { listingId, qty -> nav.navigate(Routes.checkout(listingId, qty)) })
            }
            composable(Routes.CHECKOUT, arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("quantity") { type = NavType.StringType })) { entry ->
                val id = entry.arg("id")
                val qty = entry.arg("quantity").toDoubleOrNull() ?: 1.0
                val vm = appViewModel(key = "checkout-$id") { CheckoutViewModel(id, qty, it.market, it.orders, it.profile, it.session) }
                CheckoutScreen(vm, onBack = { nav.popBackStack() }, onPlaced = { orderId ->
                    nav.navigate(Routes.order(orderId)) { popUpTo(Routes.BUYER_MARKET) }
                })
            }

            // ------------------------------------------------------------------ driver tabs
            composable(Routes.DRIVER_JOBS) {
                val vm = appViewModel { JobsViewModel(it.delivery, it.profile) }
                JobsScreen(vm, unread, onOpenJob = { nav.navigate(Routes.delivery(it)) }, onNotifications = { nav.navigate(Routes.NOTIFICATIONS) })
            }
            composable(Routes.DRIVER_TRIPS) {
                val vm = appViewModel { TripsViewModel(it.delivery, TripsTab.ACTIVE) }
                TripsScreen(vm, onOpenTrip = { nav.navigate(Routes.delivery(it)) })
            }
            composable(Routes.DRIVER_EARNINGS) {
                val vm = appViewModel { WalletViewModel(it.wallet, it.session) }
                WalletScreen(vm, R.string.wallet_title_driver)
            }
            composable(Routes.DELIVERY, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arg("id")
                val vm = appViewModel(key = "delivery-$id") { DeliveryViewModel(id, it.delivery, it.profile) }
                DeliveryScreen(vm, onBack = { nav.popBackStack() })
            }

            // ------------------------------------------------------------------ shared
            composable(Routes.ACCOUNT) {
                val vm = appViewModel { AccountViewModel(it.auth, it.session) }
                AccountScreen(vm, user, onEditProfile = { nav.navigate(Routes.PROFILE_EDIT) }, onVerification = { nav.navigate(Routes.verification(true)) })
            }
            composable(Routes.PROFILE_EDIT) {
                val editRole = role ?: "BUYER"
                val vm = appViewModel(key = "profile-edit") { SetupViewModel(editRole, it.profile, it.market, it.session.user?.fullName.orEmpty()) }
                SetupScreen(vm, editRole, edit = true, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
            }
            composable(Routes.NOTIFICATIONS) {
                val vm = appViewModel(key = "notifications-screen") { NotificationsViewModel(it.notifications) }
                NotificationsScreen(vm, onBack = { nav.popBackStack() }, onOpen = { n -> openFrom(nav, role, n) })
            }
            composable(Routes.ORDER, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arg("id")
                val vm = appViewModel(key = "order-$id") { OrderDetailViewModel(id, it.orders, it.delivery, it.outbox) }
                OrderDetailScreen(
                    vm, role ?: "BUYER", onBack = { nav.popBackStack() },
                    onPay = { nav.navigate(Routes.pay(it)) },
                    onReport = { nav.navigate(Routes.report(it)) },
                    onHandover = { orderId, kind -> nav.navigate(Routes.handover(orderId, kind)) },
                    onOpenDispute = { nav.navigate(Routes.dispute(it)) },
                    onOpenListing = { nav.navigate(Routes.listing(it)) },
                )
            }
            composable(Routes.PAY, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arg("id")
                val vm = appViewModel(key = "pay-$id") { PayViewModel(id, it.orders, it.session) }
                PayScreen(vm, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
            }
            composable(Routes.REPORT, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arg("id")
                val vm = appViewModel(key = "report-$id") { ReportViewModel(id, it.orders, it.profile) }
                ReportScreen(vm, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
            }
            composable(Routes.HANDOVER, arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("kind") { type = NavType.StringType })) { entry ->
                val id = entry.arg("id")
                val kind = entry.arg("kind")
                val vm = appViewModel(key = "order-$id") { OrderDetailViewModel(id, it.orders, it.delivery, it.outbox) }
                val state by vm.state.collectAsState()
                val orderState: Load<com.agrilink.app.data.api.dto.OrderDto> = when (val s = state) {
                    Load.Loading -> Load.Loading
                    is Load.Failed -> s
                    is Load.Ready -> Load.Ready(s.value.order)
                }
                LaunchedEffect(Unit) { while (true) { delay(10_000); vm.refresh() } }
                HandoverScreen(orderState, kind, onBack = { nav.popBackStack() }, onRetry = vm::refresh, onReport = if (role == "BUYER") ({ nav.navigate(Routes.report(id)) }) else null)
            }
            composable(Routes.DISPUTE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arg("id")
                val vm = appViewModel(key = "dispute-$id") { DisputeViewModel(id, it.orders) }
                DisputeScreen(vm, onBack = { nav.popBackStack() }, onOpenOrder = { nav.navigate(Routes.order(it)) })
            }
        }
    }
}

private fun signedIn(nav: NavHostController, role: String?) {
    nav.navigate(Routes.home(role)) { popUpTo(0) { inclusive = true } }
}

private fun openFrom(nav: NavHostController, role: String?, n: NotificationDto) = nav.openReference(role, n.referenceType, n.referenceId)

private fun signOutNow(container: com.agrilink.app.di.AppContainer) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { container.auth.logout() }
}
