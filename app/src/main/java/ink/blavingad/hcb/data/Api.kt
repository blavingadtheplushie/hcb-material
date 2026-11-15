package ink.blavingad.hcb.data

import ink.blavingad.hcb.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class ApiException(val code: Int, message: String) : Exception(message)

fun errorMessage(json: Json, text: String): String? = runCatching {
    val o = json.parseToJsonElement(text) as? JsonObject ?: return null
    listOf("error_description", "message", "error").firstNotNullOfOrNull { k -> (o[k] as? JsonPrimitive)?.content }
        ?: (o["messages"] as? JsonArray)?.joinToString(", ") { it.jsonPrimitive.content }
}.getOrNull()

class Api(private val http: OkHttpClient, private val auth: Auth, val json: Json) {

    suspend fun raw(path: String, method: String = "GET", body: RequestBody? = null, retry: Boolean = true): String = withContext(Dispatchers.IO) {
        val token = auth.accessToken()
        val b = Request.Builder().url("${BuildConfig.API_BASE}/${path.trimStart('/')}")
            .header("Accept", "application/json")
            .method(method, body ?: if (method == "GET" || method == "DELETE") null else ByteArray(0).toRequestBody())
        if (token != null) b.header("Authorization", "Bearer $token")
        http.newCall(b.build()).execute().use { res ->
            val text = res.body.string()
            if (res.code == 401 && retry && token != null) {
                if (auth.accessToken(forceRefresh = true) != null) return@withContext raw(path, method, body, false)
            }
            if (!res.isSuccessful) throw ApiException(res.code, errorMessage(json, text) ?: "HTTP ${res.code}")
            text
        }
    }

    suspend fun <T> get(path: String, s: KSerializer<T>): T = json.decodeFromString(s, raw(path))
    private suspend fun <T> send(path: String, method: String, s: KSerializer<T>, body: RequestBody? = null): T =
        json.decodeFromString(s, raw(path, method, body).ifBlank { "{}" })

    private fun jsonBody(o: JsonObject) = o.toString().toRequestBody("application/json".toMediaType())

    // ---- endpoints (HCB v4) ----
    suspend fun user() = get("user", User.serializer())
    suspend fun organizations() = get("user/organizations", ListSerializer(Organization.serializer()))
    suspend fun organization(id: String) = get("organizations/$id?expand=balance_cents,users", Organization.serializer())
    suspend fun orgBalance(id: String) = get("organizations/$id?expand=balance_cents", Organization.serializer())
    suspend fun invitations() = get("user/invitations", ListSerializer(Invitation.serializer()))
    suspend fun invitation(id: String) = get("user/invitations/$id?expand=organization,sender", Invitation.serializer())
    suspend fun respondInvitation(id: String, accept: Boolean) { raw("user/invitations/$id/${if (accept) "accept" else "reject"}", "POST") }

    private fun page(base: String, after: String?, limit: Int = 30): String {
        val sep = if ('?' in base) '&' else '?'
        return "$base${sep}limit=$limit" + (after?.let { "&after=${java.net.URLEncoder.encode(it, "UTF-8")}" } ?: "")
    }
    private val txPage = Paginated.serializer(Transaction.serializer())

    suspend fun orgTransactions(id: String, after: String?, search: String?, filter: String?): Paginated<Transaction> {
        val q = buildList {
            if (!search.isNullOrBlank()) add("filters[search]=" + java.net.URLEncoder.encode(search, "UTF-8"))
            if (!filter.isNullOrBlank()) add("filters[$filter]=true")
        }.joinToString("&")
        return get(page("organizations/$id/transactions" + if (q.isNotEmpty()) "?$q" else "", after), txPage)
    }
    suspend fun missingReceipts(after: String?) = get(page("user/transactions/missing_receipt?expand=organization", after), txPage)
    suspend fun missingReceiptCount() = get("user/transactions/missing_receipt?limit=1", txPage).total_count
    suspend fun transaction(org: String, id: String) = get("organizations/$org/transactions/$id", Transaction.serializer())
    suspend fun renameTransaction(org: String, id: String, memo: String) =
        send("organizations/$org/transactions/$id", "PATCH", Transaction.serializer(), jsonBody(buildJsonObject { put("memo", memo) }))
    suspend fun markNoReceipt(id: String) { raw("transactions/$id/mark_no_receipt", "POST") }

    suspend fun myCards() = get("user/cards?expand=organization", ListSerializer(Card.serializer()))
    suspend fun orgCards(id: String) = get("organizations/$id/cards?expand=user", ListSerializer(Card.serializer()))
    suspend fun card(id: String) = get("cards/$id?expand=organization,user,total_spent_cents", Card.serializer())
    suspend fun cardTransactions(id: String, after: String?) = get(page("cards/$id/transactions", after), txPage)
    suspend fun setFrozen(id: String, frozen: Boolean) {
        try { raw("cards/$id/${if (frozen) "freeze" else "defrost"}", "POST") }
        catch (e: ApiException) {
            if (e.code != 404) throw e
            raw("cards/$id", "PATCH", jsonBody(buildJsonObject { put("status", if (frozen) "frozen" else "active") }))
        }
    }

    suspend fun receipts(txId: String?) = get(if (txId != null) "receipts?transaction_id=$txId" else "receipts", ListSerializer(Receipt.serializer()))
    suspend fun uploadReceipt(bytes: ByteArray, filename: String, mime: String, txId: String?): Receipt {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, bytes.toRequestBody(mime.toMediaType()))
            .apply { if (txId != null) addFormDataPart("transaction_id", txId) }
            .build()
        return send("receipts", "POST", Receipt.serializer(), body)
    }
    suspend fun deleteReceipt(id: String) { raw("receipts/$id", "DELETE") }

    suspend fun comments(txId: String) = get("comments?transaction_id=$txId", ListSerializer(Comment.serializer()))
    suspend fun addComment(txId: String, content: String): Comment {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("content", content).addFormDataPart("transaction_id", txId).build()
        return send("comments", "POST", Comment.serializer(), body)
    }
}
