package ink.blavingad.hcb.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** HCB's public v3 API: works without auth for transparent organizations (v4 only serves members). */
@Serializable data class V3Balances(val balance_cents: Long? = null, val fee_balance_cents: Long? = null, val incoming_balance_cents: Long? = null)
@Serializable data class V3User(val id: String = "", val full_name: String? = null, val photo: String? = null)
@Serializable data class V3Org(
    val id: String = "", val name: String = "", val slug: String = "", val website: String? = null, val transparent: Boolean = false,
    val logo: String? = null, val background_image: String? = null, val public_message: String? = null, val donation_link: String? = null,
    val balances: V3Balances? = null, val users: List<V3User>? = null, val created_at: String? = null,
)
@Serializable data class V3Receipts(val count: Int = 0, val missing: Boolean = false)
@Serializable data class V3Ref(val id: String = "")
@Serializable data class V3Tx(
    val id: String = "", val amount_cents: Long = 0, val memo: String = "", val date: String? = null, val type: String? = null,
    val pending: Boolean = false, val receipts: V3Receipts? = null, val user: V3User? = null, val tags: List<Tag>? = null,
    val organization: V3Ref? = null,
) {
    /** Map onto the v4 model so the normal rows / icons / logos render it. */
    fun toTransaction() = Transaction(
        id = id, date = date, amount_cents = amount_cents, memo = memo, pending = pending,
        missing_receipt = receipts?.missing == true, tags = tags,
        card_charge = if (type == "card_charge") CardCharge(merchant = Merchant(name = memo)) else null,
        donation = if (type == "donation") Donation() else null,
        transfer = if (type == "transfer") Transfer() else null,
        check = if (type == "check") Check() else null,
        ach_transfer = if (type == "ach_transfer") AchTransfer() else null,
        invoice = if (type == "invoice") Invoice() else null,
    )
}

class PublicApi(private val http: OkHttpClient, private val json: Json) {
    private val base = "https://hcb.hackclub.com/api/v3"
    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url("$base/$path").header("Accept", "application/json").build()).execute().use { r ->
            val t = r.body.string()
            if (!r.isSuccessful) throw ApiException(r.code, if (r.code == 404) "This organization isn't public" else "HTTP ${r.code}")
            t
        }
    }
    suspend fun org(slug: String): V3Org = json.decodeFromString(V3Org.serializer(), get("organizations/$slug"))
    /** page-numbered; fake a cursor (= next page number) so it plugs into Paged. */
    suspend fun transactions(slug: String, after: String?, per: Int = 30): Paginated<Transaction> {
        val page = after?.substringAfter("#p")?.toIntOrNull() ?: 1
        val list = json.decodeFromString(ListSerializer(V3Tx.serializer()), get("organizations/$slug/transactions?per_page=$per&page=$page"))
        return Paginated(has_more = list.size >= per, data = list.mapIndexed { i, t ->
            t.toTransaction().let { if (i == list.lastIndex) it.copy(code = "#p${page + 1}") else it }
        })
    }

    /** hcb.gg short links: read the redirect target without following it. */
    suspend fun resolveShort(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            http.newBuilder().followRedirects(false).followSslRedirects(false).build()
                .newCall(Request.Builder().url(url).head().build()).execute().use { it.header("Location") }
        }.getOrNull()
    }
}
