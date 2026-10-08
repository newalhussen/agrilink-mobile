package com.agrilink.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.agrilink.app.R

@StringRes
fun orderStatusRes(status: String): Int = when (status) {
    "PENDING" -> R.string.order_status_pending
    "ACCEPTED" -> R.string.order_status_accepted
    "PAYMENT_PENDING" -> R.string.order_status_payment_pending
    "PAID" -> R.string.order_status_paid
    "READY_FOR_PICKUP" -> R.string.order_status_ready
    "PICKED_UP" -> R.string.order_status_picked_up
    "IN_TRANSIT" -> R.string.order_status_in_transit
    "DELIVERED" -> R.string.order_status_delivered
    "COMPLETED" -> R.string.order_status_completed
    "REJECTED" -> R.string.order_status_rejected
    "CANCELLED" -> R.string.order_status_cancelled
    "EXPIRED" -> R.string.order_status_expired
    "DISPUTED" -> R.string.order_status_disputed
    else -> R.string.order_status_unknown
}

fun orderStatusTone(status: String): Tone = when (status) {
    "PENDING" -> Tone.Neutral
    "ACCEPTED", "PAYMENT_PENDING", "PICKED_UP", "IN_TRANSIT" -> Tone.Accent
    "PAID", "READY_FOR_PICKUP", "DELIVERED" -> Tone.Sage
    "COMPLETED" -> Tone.SolidSage
    "DISPUTED" -> Tone.SolidDark
    else -> Tone.Outline
}

@Composable
fun OrderStatusTag(status: String) = Tag(stringResource(orderStatusRes(status)), orderStatusTone(status))

@StringRes
fun deliveryStatusRes(status: String): Int = when (status) {
    "OPEN" -> R.string.delivery_status_open
    "ASSIGNED" -> R.string.delivery_status_assigned
    "PICKED_UP" -> R.string.order_status_picked_up
    "IN_TRANSIT" -> R.string.order_status_in_transit
    "DELIVERED" -> R.string.delivery_status_delivered
    "CANCELLED" -> R.string.order_status_cancelled
    else -> R.string.order_status_unknown
}

@Composable
fun DeliveryStatusTag(status: String) = Tag(
    stringResource(deliveryStatusRes(status)),
    when (status) {
        "OPEN" -> Tone.Accent
        "ASSIGNED" -> Tone.Neutral
        "PICKED_UP", "IN_TRANSIT" -> Tone.Accent
        "DELIVERED" -> Tone.SolidSage
        else -> Tone.Outline
    },
)

@StringRes
fun verificationStatusRes(status: String): Int = when (status) {
    "PENDING" -> R.string.verification_pending
    "INFO_NEEDED" -> R.string.verification_info_needed
    "VERIFIED" -> R.string.verification_verified
    "REJECTED" -> R.string.verification_rejected
    else -> R.string.verification_unverified
}

fun verificationTone(status: String): Tone = when (status) {
    "VERIFIED" -> Tone.SolidSage
    "PENDING" -> Tone.Sage
    "INFO_NEEDED" -> Tone.Accent
    "REJECTED" -> Tone.SolidDark
    else -> Tone.Outline
}

@Composable
fun VerificationTag(status: String) = Tag(stringResource(verificationStatusRes(status)), verificationTone(status))

@StringRes
fun unitRes(unit: String): Int = when (unit) {
    "KG" -> R.string.unit_kg
    "QUINTAL" -> R.string.unit_quintal
    "TON" -> R.string.unit_ton
    "LITER" -> R.string.unit_liter
    "CRATE" -> R.string.unit_crate
    "SACK" -> R.string.unit_sack
    "TRAY" -> R.string.unit_tray
    "BUNCH" -> R.string.unit_bunch
    else -> R.string.unit_piece
}

@StringRes
fun paymentMethodRes(method: String): Int = when (method) {
    "TELEBIRR" -> R.string.pay_telebirr
    "CBE_BIRR" -> R.string.pay_cbe_birr
    "BANK_TRANSFER" -> R.string.pay_bank
    else -> R.string.pay_card
}
