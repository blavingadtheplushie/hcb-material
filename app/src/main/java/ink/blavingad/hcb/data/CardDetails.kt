package ink.blavingad.hcb.data

import ink.blavingad.hcb.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable data class StripeNonce(val private_nonce: String, val public_nonce: String)
@Serializable data class EphemeralKey(val ephemeralKeySecret: String, val stripe_id: String)
@Serializable data class CardSecrets(val number: String = "", val cvc: String = "", val exp_month: Int = 0, val exp_year: Int = 0)

/** Same flow as the official app (lib/useStripeCardDetails.ts): Stripe nonce → HCB ephemeral key → Stripe card. Virtual cards only. */
suspend fun Api.cardSecrets(cardId: String, http: okhttp3.OkHttpClient): CardSecrets = withContext(Dispatchers.IO) {
    val nonce = http.newCall(Request.Builder().url("https://api.stripe.com/v1/ephemeral_key_nonces")
        .header("Authorization", "Bearer ${BuildConfig.STRIPE_PK}").post(ByteArray(0).toRequestBody()).build()).execute().use {
        if (!it.isSuccessful) throw ApiException(it.code, "Stripe nonce failed (${it.code})")
        json.decodeFromString(StripeNonce.serializer(), it.body.string())
    }
    val key = get("cards/$cardId/ephemeral_keys?nonce=${nonce.public_nonce}", EphemeralKey.serializer())
    val url = "https://api.stripe.com/v1/issuing/cards/${key.stripe_id}?expand[0]=number&expand[1]=cvc&ephemeral_key_private_nonce=${nonce.private_nonce}"
    http.newCall(Request.Builder().url(url).header("Authorization", "Bearer ${key.ephemeralKeySecret}")
        .header("Stripe-Version", "2020-03-02").build()).execute().use {
        if (!it.isSuccessful) throw ApiException(it.code, "Stripe card failed (${it.code})")
        json.decodeFromString(CardSecrets.serializer(), it.body.string())
    }
}
