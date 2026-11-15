package ink.blavingad.hcb.ui

import ink.blavingad.hcb.data.Transaction
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

private val usd = NumberFormat.getCurrencyInstance(Locale.US).apply { currency = Currency.getInstance("USD") }

fun money(cents: Long?, signed: Boolean = false): String {
    if (cents == null) return "—"
    val s = usd.format(abs(cents) / 100.0)
    return when { cents < 0 -> "−$s"; signed && cents > 0 -> "+$s"; else -> s }
}

fun parseDate(s: String?): LocalDate? = s?.let {
    runCatching { LocalDate.parse(it.take(10)) }.getOrNull() ?: runCatching { OffsetDateTime.parse(it).toLocalDate() }.getOrNull()
}

fun prettyDate(s: String?): String = parseDate(s)?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: ""

fun prettyDateTime(s: String?): String = s?.let {
    runCatching { OffsetDateTime.parse(it).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)) }.getOrNull()
} ?: prettyDate(s)

fun dayHeader(d: LocalDate?): String {
    if (d == null) return "Pending"
    val today = LocalDate.now()
    return when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
    }
}

/** What the official app shows as the transaction's kind. */
fun Transaction.kind(): String = when {
    card_charge != null -> if (declined) "Declined card charge" else "Card charge"
    donation != null -> if (donation.recurring == true) "Recurring donation" else "Donation"
    transfer != null -> "HCB transfer"
    check != null -> "Check"
    ach_transfer != null -> "ACH transfer"
    invoice != null -> "Invoice"
    amount_cents >= 0 -> "Deposit"
    else -> "Transaction"
}

fun Transaction.title(): String = when {
    has_custom_memo -> memo
    card_charge?.merchant != null -> card_charge.merchant.smart_name ?: card_charge.merchant.name ?: memo
    donation?.donor?.name != null -> "Donation from ${donation.donor.name}"
    else -> memo.ifBlank { kind() }
}
