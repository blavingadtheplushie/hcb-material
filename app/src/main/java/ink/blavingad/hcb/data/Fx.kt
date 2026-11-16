package ink.blavingad.hcb.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * USD → other currency, the way HCB does it:
 * card charges use the Visa rate with a 0 % bank fee (HCB covers FX fees),
 * everything else the ECB reference rate. Rates are taken at the payment date and cached forever.
 */
class Fx(context: Context, private val http: OkHttpClient, private val json: Json) {
    private val cache = context.getSharedPreferences("fx", Context.MODE_PRIVATE)

    data class Rate(val rate: Double, val source: String, val date: LocalDate)

    suspend fun rate(to: String, date: LocalDate, card: Boolean): Rate? = withContext(Dispatchers.IO) {
        if (to == "USD") return@withContext Rate(1.0, "", date)
        val day = minOf(date, LocalDate.now())
        val ecb = ecb(to, day)
        if (card) visa(to, day, ecb?.rate)?.let { return@withContext it }
        ecb
    }

    private fun cached(key: String): Rate? = cache.getString(key, null)?.split('|')?.takeIf { it.size == 3 }?.let {
        Rate(it[0].toDouble(), it[1], LocalDate.parse(it[2]))
    }
    private fun store(key: String, r: Rate) = cache.edit { putString(key, "${r.rate}|${r.source}|${r.date}") }

    private fun ecb(to: String, day: LocalDate): Rate? {
        val key = "ecb:$to:$day"
        cached(key)?.let { return it }
        return runCatching {
            val body = http.newCall(Request.Builder().url("https://api.frankfurter.dev/v1/$day?base=USD&symbols=$to").build()).execute().use { if (it.isSuccessful) it.body.string() else null } ?: return null
            val o = json.parseToJsonElement(body).jsonObject
            val r = o["rates"]!!.jsonObject[to]!!.jsonPrimitive.doubleOrNull!!
            Rate(r, "ECB", LocalDate.parse(o["date"]!!.jsonPrimitive.content)).also { if (day < LocalDate.now()) store(key, it) }
        }.getOrNull()
    }

    private val visaDate = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    /** Visa publishes one rate per day; today's may not exist yet, so walk back a few days. */
    private fun visa(to: String, day: LocalDate, ecbHint: Double?): Rate? {
        val key = "visa:$to:$day"
        cached(key)?.let { return it }
        for (back in 0L..3L) {
            val d = day.minusDays(back).format(visaDate).replace("/", "%2F")
            val url = "https://usa.visa.com/cmsapi/fx/rates?amount=1&fee=0&utcConvertedDate=$d&exchangedate=$d&fromCurr=$to&toCurr=USD"
            val body = runCatching {
                http.newCall(Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36")
                    .header("Accept", "application/json")
                    .header("Referer", "https://usa.visa.com/support/consumer/travel-support/exchange-rate-calculator.html").build())
                    .execute().use { if (it.isSuccessful) it.body.string() else null }
            }.getOrNull() ?: continue
            val raw = runCatching {
                val o = json.parseToJsonElement(body).jsonObject
                val ov = o["originalValues"] as? JsonObject
                (ov?.get("fxRateVisa") ?: o["fxRateVisa"] ?: ov?.get("fxRateWithAdditionalFee"))?.jsonPrimitive?.content?.toDoubleOrNull()
            }.getOrNull() ?: continue
            if (raw <= 0) continue
            // Visa's from/to wording is ambiguous: keep whichever orientation matches the ECB ballpark.
            val r = if (ecbHint == null) raw else listOf(raw, 1 / raw).minBy { abs(it - ecbHint) / ecbHint }
            if (ecbHint != null && abs(r - ecbHint) / ecbHint > 0.15) return null
            return Rate(r, "Visa", day.minusDays(back)).also { store(key, it) }
        }
        return null
    }

    companion object {
        val currencies = listOf("EUR", "GBP", "CHF", "CAD", "AUD", "NZD", "JPY", "CNY", "HKD", "SGD", "INR", "KRW", "IDR", "MYR", "PHP", "THB",
            "BRL", "MXN", "ZAR", "TRY", "ILS", "SEK", "NOK", "DKK", "ISK", "PLN", "CZK", "HUF", "RON")
    }
}
