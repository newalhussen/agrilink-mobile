package com.agrilink.app.ui.nav

/** A screen to open from a notification: the server's `referenceType` and `referenceId`. */
data class DeepLink(val type: String, val id: String)

object Routes {
    const val LANGUAGE = "language"
    const val ROLE = "role"
    const val PHONE = "phone/{login}"
    const val OTP = "otp"
    const val SETUP = "setup"
    const val VERIFICATION = "verification/{fromAccount}"
    const val UNSUPPORTED = "unsupported"

    const val FARMER_TODAY = "farmer/today"
    const val FARMER_PRODUCE = "farmer/produce"
    const val FARMER_ORDERS = "farmer/orders"
    const val FARMER_WALLET = "farmer/wallet"
    const val BUYER_MARKET = "buyer/market"
    const val BUYER_ORDERS = "buyer/orders"
    const val BUYER_ALERTS = "buyer/alerts"
    const val DRIVER_JOBS = "driver/jobs"
    const val DRIVER_TRIPS = "driver/trips"
    const val DRIVER_EARNINGS = "driver/earnings"
    const val ACCOUNT = "account"

    const val NOTIFICATIONS = "notifications"
    const val PROFILE_EDIT = "profile/edit"
    const val LISTING = "listing/{id}"
    const val CHECKOUT = "checkout/{id}/{quantity}"
    const val ORDER = "order/{id}"
    const val PAY = "pay/{id}"
    const val REPORT = "report/{id}"
    const val HANDOVER = "handover/{id}/{kind}"
    const val DISPUTE = "dispute/{id}"
    const val PRODUCE_ADD = "produce/add"
    const val PRODUCE_EDIT = "produce/edit/{id}"
    const val DELIVERY = "delivery/{id}"

    fun phone(login: Boolean) = "phone/$login"
    fun verification(fromAccount: Boolean) = "verification/$fromAccount"
    fun listing(id: String) = "listing/$id"
    fun checkout(id: String, quantity: Double) = "checkout/$id/$quantity"
    fun order(id: String) = "order/$id"
    fun pay(id: String) = "pay/$id"
    fun report(id: String) = "report/$id"
    fun handover(id: String, kind: String) = "handover/$id/$kind"
    fun dispute(id: String) = "dispute/$id"
    fun produceEdit(id: String) = "produce/edit/$id"
    fun delivery(id: String) = "delivery/$id"

    fun home(role: String?): String = when (role) {
        "FARMER" -> FARMER_TODAY
        "BUYER" -> BUYER_MARKET
        "DRIVER" -> DRIVER_JOBS
        else -> UNSUPPORTED
    }

    val authRoutes = setOf(LANGUAGE, ROLE, PHONE, OTP, SETUP, UNSUPPORTED)
}
