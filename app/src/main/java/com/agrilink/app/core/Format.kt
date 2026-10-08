package com.agrilink.app.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/** Number, money and weight formatting that does not depend on the device locale (ETB 21,968 everywhere). */
object Format {
    private fun grouped(pattern: String) = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))

    /** "ETB 21,968", or "ETB 2,760.50" when there are cents. */
    fun etb(amount: Double, withCurrency: Boolean = true): String {
        val hasCents = abs(amount - amount.roundToLong()) > 0.004
        val text = grouped(if (hasCents) "#,##0.00" else "#,##0").format(amount)
        return if (withCurrency) "ETB $text" else text
    }

    /** Compact for tiles: ETB 1.84M, ETB 412K. */
    fun etbCompact(amount: Double): String = when {
        abs(amount) >= 1_000_000 -> "ETB ${trim(amount / 1_000_000, 2)}M"
        abs(amount) >= 100_000 -> "ETB ${(amount / 1000).roundToLong()}K"
        else -> etb(amount)
    }

    fun kg(value: Double): String = if (value >= 1000) "${trim(value / 1000, 2)} t" else "${trim(value, 1)} kg"

    /** Quantity without trailing zeros: 400, 2.5. */
    fun number(value: Double, digits: Int = 2): String = trim(value, digits)

    private fun trim(value: Double, digits: Int): String {
        val text = grouped(if (digits <= 0) "#,##0" else "#,##0." + "#".repeat(digits)).format(value)
        return text
    }

    fun percent(value: Double): String = "${grouped("0.#").format(value)}%"

    /** "46" style price without currency for list tiles. */
    fun price(value: Double): String = etb(value, withCurrency = false)
}

object Dates {
    val zone: ZoneId = ZoneId.of("Africa/Addis_Ababa")
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun parse(iso: String?): Instant? = iso?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }

    private fun zoned(iso: String?): ZonedDateTime? = parse(iso)?.atZone(zone)

    /** "14:42" */
    fun time(iso: String?): String = zoned(iso)?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "—"

    /** "7 Oct, 14:42" */
    fun dateTime(iso: String?): String = zoned(iso)?.let { "${it.dayOfMonth} ${months[it.monthValue - 1]}, ${"%02d:%02d".format(it.hour, it.minute)}" } ?: "—"

    /** "Thu 9 Oct" from an instant or a plain yyyy-MM-dd date. */
    fun dayLabel(isoOrDate: String?): String {
        if (isoOrDate.isNullOrBlank()) return "—"
        val date = if (isoOrDate.length == 10) runCatching { LocalDate.parse(isoOrDate) }.getOrNull() else zoned(isoOrDate)?.toLocalDate()
        return date?.let { "${weekdays[it.dayOfWeek.value - 1]} ${it.dayOfMonth} ${months[it.monthValue - 1]}" } ?: "—"
    }

    fun today(now: Instant = Instant.now()): LocalDate = now.atZone(zone).toLocalDate()

    /** Milliseconds until [iso]; negative when it has passed; null when there is no deadline. */
    fun millisUntil(iso: String?, now: Instant = Instant.now()): Long? = parse(iso)?.let { it.toEpochMilli() - now.toEpochMilli() }

    /** "1:40", "12 min", "now". Negative or zero durations read "now". */
    fun countdown(millis: Long): String {
        if (millis <= 0) return "now"
        val totalMinutes = millis / 60_000
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h >= 24 -> "${h / 24} d ${h % 24} h"
            h >= 1 -> "$h:${"%02d".format(m)}"
            totalMinutes >= 1 -> "$totalMinutes min"
            else -> "<1 min"
        }
    }

    /** "just now", "5 min ago", "3 h ago" or a date. */
    fun ago(iso: String?, now: Instant = Instant.now()): String {
        val then = parse(iso) ?: return "—"
        val diff = now.toEpochMilli() - then.toEpochMilli()
        return when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000} min ago"
            diff < 86_400_000 -> "${diff / 3_600_000} h ago"
            else -> dateTime(iso)
        }
    }
}

/** Ethiopian (Ge'ez) calendar, shown next to Gregorian dates as in the design. */
object EthiopianCalendar {
    val monthsAm = listOf("መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት", "መጋቢት", "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜ")
    private const val EPOCH_JDN = 1723856

    data class EthDate(val year: Int, val month: Int, val day: Int) {
        fun format(): String = "${monthsAm[month - 1]} $day, $year"
    }

    private fun jdn(year: Int, month: Int, day: Int): Int {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365 * y + floor(y / 4.0).toInt() - floor(y / 100.0).toInt() + floor(y / 400.0).toInt() - 32045
    }

    fun from(date: LocalDate): EthDate {
        val days = jdn(date.year, date.monthValue, date.dayOfMonth) - EPOCH_JDN
        val r = days % 1461
        val n = (r % 365) + 365 * (r / 1460)
        return EthDate(
            year = 4 * (days / 1461) + r / 365 - r / 1460,
            month = n / 30 + 1,
            day = n % 30 + 1,
        )
    }
}

object Phone {
    /** "+251 91 123 4567" for display; anything unexpected is returned unchanged. */
    fun display(e164: String?): String {
        if (e164.isNullOrBlank()) return "—"
        val m = Regex("^\\+251(\\d{2})(\\d{3})(\\d{4})$").matchEntire(e164) ?: return e164
        return "+251 ${m.groupValues[1]} ${m.groupValues[2]} ${m.groupValues[3]}"
    }

    /** "+251 91 ••• 2140" as in the design's code screen. */
    fun mask(e164: String?): String {
        if (e164.isNullOrBlank()) return "—"
        val m = Regex("^\\+251(\\d{2})\\d{3}(\\d{4})$").matchEntire(e164) ?: return e164
        return "+251 ${m.groupValues[1]} ••• ${m.groupValues[2]}"
    }

    /** Local format the user typed, normalised to the digits we need to decide whether it can be valid. */
    fun looksValid(input: String): Boolean {
        val digits = input.filter { it.isDigit() }
        val national = when {
            digits.startsWith("251") -> digits.drop(3)
            digits.startsWith("0") -> digits.drop(1)
            else -> digits
        }
        return national.length == 9 && (national.startsWith("9") || national.startsWith("7"))
    }

    /** Normalises to +2519XXXXXXXX for display after sign-up; the server does the authoritative check. */
    fun normalize(input: String): String {
        val digits = input.filter { it.isDigit() }
        val national = when {
            digits.startsWith("251") -> digits.drop(3)
            digits.startsWith("0") -> digits.drop(1)
            else -> digits
        }
        return "+251$national"
    }
}
