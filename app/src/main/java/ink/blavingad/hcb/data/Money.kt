package ink.blavingad.hcb.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class BalancePoint(val date: String = "", val amount: Long = 0)

@Serializable
data class OutCheck(
    val id: String = "", val status: String? = null, val recipient_name: String? = null, val recipient_email: String? = null,
    val memo: String? = null, val payment_for: String? = null, val check_number: String? = null, val created_at: String? = null,
    val address_city: String? = null, val address_state: String? = null, val sender: User? = null,
)

@Serializable
data class OutWire(
    val id: String = "", val amount_cents: Long = 0, val currency: String? = null, val state: String? = null,
    val memo: String? = null, val payment_for: String? = null, val recipient_name: String? = null,
    val recipient_country: String? = null, val created_at: String? = null, val sender: User? = null,
)

@Serializable
data class Rejection(val reason: String? = null, val description: String? = null)

@Serializable
data class CheckDeposit(
    val id: String = "", val status: String? = null, val amount_cents: Long = 0, val created_at: String? = null,
    val estimated_arrival_date: String? = null, val rejection: Rejection? = null,
    val front_url: String? = null, val back_url: String? = null, val submitter: User? = null,
)

@Serializable
data class InvoiceDoc(
    val id: String = "", val status: String? = null, val to: String? = null, val amount_due: Long? = null,
    val memo: String? = null, val due_date: String? = null, val item_amount: Long? = null,
    val item_description: String? = null, val sponsor_id: Long? = null, val created_at: String? = null,
)

@Serializable
data class SponsorDoc(
    val id: String = "", val name: String = "", val contact_email: String? = null, val slug: String? = null,
    val address_line1: String? = null, val address_line2: String? = null, val address_city: String? = null,
    val address_state: String? = null, val address_postal_code: String? = null, val address_country: String? = null,
)

val TAG_COLORS = listOf("muted", "red", "orange", "yellow", "green", "cyan", "blue", "purple")

private fun obj(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    pairs.forEach { (k, v) ->
        when (v) {
            null -> {}
            is String -> if (v.isNotBlank()) put(k, v)
            is Number -> put(k, v)
            is Boolean -> put(k, v)
            is JsonElement -> put(k, v)
            is Map<*, *> -> put(k, obj(*v.entries.map { it.key.toString() to it.value }.toTypedArray()))
            else -> put(k, v.toString())
        }
    }
}
private fun JsonObject.body() = toString().toRequestBody("application/json".toMediaType())
private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

// ---- charts ----
suspend fun Api.balanceByDate(org: String) = get("organizations/$org/balance_by_date", ListSerializer(BalancePoint.serializer()))

// ---- transactions extras ----
suspend fun Api.memoSuggestions(org: String, tx: String): List<String> = runCatching {
    (json.parseToJsonElement(raw("organizations/$org/transactions/$tx/memo_suggestions")) as JsonArray).mapNotNull { e ->
        when (e) {
            is JsonPrimitive -> e.content
            is JsonObject -> (e["memo"] as? JsonPrimitive)?.content ?: (e["name"] as? JsonPrimitive)?.content
            else -> null
        }
    }.filter { it.isNotBlank() }.distinct()
}.getOrDefault(emptyList())

suspend fun Api.tags(org: String) = get("tags?organization_id=${enc(org)}", ListSerializer(Tag.serializer()))
suspend fun Api.createTag(org: String, label: String, color: String, emoji: String?) =
    json.decodeFromString(Tag.serializer(), raw("tags", "POST", obj("organization_id" to org, "label" to label, "color" to color, "emoji" to emoji).body()))
suspend fun Api.deleteTag(id: String) { raw("tags/$id", "DELETE") }
suspend fun Api.setTags(org: String, tx: String, tagIds: List<String>) =
    json.decodeFromString(Transaction.serializer(), raw("organizations/$org/transactions/$tx", "PATCH",
        buildJsonObject { put("tag_ids", buildJsonArray { tagIds.forEach { add(JsonPrimitive(it)) } }) }.body()))

// ---- outgoing money ----
suspend fun Api.transfer(org: String, to: String, amountCents: Long, name: String) {
    raw("organizations/$org/transfers", "POST", obj("to_organization_id" to to, "amount_cents" to amountCents, "name" to name).body())
}

suspend fun Api.achTransfer(org: String, f: Map<String, String>, amountCents: Long) {
    val a = f + ("amount_money" to "%.2f".format(java.util.Locale.US, amountCents / 100.0))
    raw("ach_transfers", "POST", obj("organization_id" to org, "ach_transfer" to (a + mapOf("send_email_notification" to "true"))).body())
}

suspend fun Api.sendCheck(org: String, f: Map<String, String>, amountCents: Long) {
    raw("checks", "POST", obj("organization_id" to org, "check" to (f + mapOf("amount_cents" to amountCents.toString(), "send_email_notification" to "true"))).body())
}

suspend fun Api.sendWire(org: String, f: Map<String, String>, amountCents: Long) {
    raw("wires", "POST", obj("organization_id" to org, "wire" to (f + mapOf("amount_cents" to amountCents.toString(), "send_email_notification" to "true"))).body())
}

suspend fun Api.depositCheck(org: String, front: ByteArray, back: ByteArray, amountCents: Long): CheckDeposit {
    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("organization_id", org)
        .addFormDataPart("amount_cents", amountCents.toString())
        .addFormDataPart("front", "front.jpg", front.toRequestBody("image/jpeg".toMediaType()))
        .addFormDataPart("back", "back.jpg", back.toRequestBody("image/jpeg".toMediaType()))
        .build()
    return json.decodeFromString(CheckDeposit.serializer(), raw("check_deposits", "POST", body))
}

suspend fun Api.checks(org: String) = get("checks?organization_id=${enc(org)}", ListSerializer(OutCheck.serializer()))
suspend fun Api.checkDeposits(org: String) = get("check_deposits?organization_id=${enc(org)}", ListSerializer(CheckDeposit.serializer()))
suspend fun Api.wires(org: String) = get("wires?organization_id=${enc(org)}&limit=50", Paginated.serializer(OutWire.serializer())).data

// ---- invoices + sponsors ----
suspend fun Api.invoices(org: String) = get("invoices?organization_id=${enc(org)}", ListSerializer(InvoiceDoc.serializer()))
suspend fun Api.sponsors(org: String) = get("sponsors?organization_id=${enc(org)}", ListSerializer(SponsorDoc.serializer()))
suspend fun Api.createSponsor(org: String, f: Map<String, String>) =
    json.decodeFromString(SponsorDoc.serializer(), raw("sponsors", "POST", obj("organization_id" to org, "sponsor" to f).body()))
suspend fun Api.createInvoice(org: String, sponsorId: String, dueDate: String, description: String, amountCents: Long) =
    json.decodeFromString(InvoiceDoc.serializer(), raw("invoices", "POST", obj(
        "organization_id" to org, "sponsor_id" to sponsorId,
        "invoice" to mapOf("due_date" to dueDate, "item_description" to description,
            "item_amount" to "%.2f".format(java.util.Locale.US, amountCents / 100.0)),
    ).body()))

/** "12.5" / "12,50" / "$1,234.56" → cents, null if invalid. */
fun parseCents(s: String): Long? {
    val t = s.trim().removePrefix("$").replace(" ", "")
    val norm = if (t.count { it == ',' } == 1 && '.' !in t && t.substringAfter(',').length <= 2) t.replace(',', '.') else t.replace(",", "")
    val d = norm.toBigDecimalOrNull() ?: return null
    if (d.signum() <= 0) return null
    return d.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
}
