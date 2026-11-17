package ink.blavingad.hcb.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.content.edit
import ink.blavingad.hcb.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom

/** OAuth2 authorization code + PKCE against HCB's Doorkeeper, same client as the official app. */
class Auth(context: Context, private val http: OkHttpClient, private val json: Json) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    private val _tokens = MutableStateFlow(prefs.getString("tokens", null)?.let { runCatching { json.decodeFromString<Tokens>(it) }.getOrNull() })
    val tokens: StateFlow<Tokens?> = _tokens
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    /** true only while the code → token exchange is running */
    @Volatile var exchanging = false

    private fun save(t: Tokens?) {
        prefs.edit { if (t == null) remove("tokens") else putString("tokens", json.encodeToString(Tokens.serializer(), t)) }
        _tokens.value = t
    }

    private fun random(bytes: Int): String {
        val b = ByteArray(bytes); SecureRandom().nextBytes(b)
        return Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun buildAuthorizeUri(dark: Boolean, signup: Boolean = false): Uri {
        val verifier = random(48)
        val state = random(16)
        prefs.edit { putString("verifier", verifier); putString("state", state) }
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        error.value = null
        return Uri.parse("${BuildConfig.API_BASE}/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", BuildConfig.OAUTH_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.OAUTH_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "read write")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .appendQueryParameter("no_app_shell", "true")
            .appendQueryParameter("signup", signup.toString())
            .appendQueryParameter("theme", if (dark) "dark" else "light")
            .build()
    }

    fun isCallback(uri: Uri?) = uri?.scheme == "hcb" && (uri.getQueryParameter("code") != null || uri.getQueryParameter("error") != null)

    suspend fun handleCallback(uri: Uri) {
        uri.getQueryParameter("error")?.let { error.value = uri.getQueryParameter("error_description") ?: it; return }
        val code = uri.getQueryParameter("code") ?: return
        val state = uri.getQueryParameter("state")
        val expected = prefs.getString("state", null)
        if (expected != null && state != null && state != expected) { error.value = "Sign-in state mismatch. Try again."; return }
        val verifier = prefs.getString("verifier", null) ?: run { error.value = "Sign-in expired. Try again."; return }
        busy.value = true; exchanging = true
        try {
            val t = tokenRequest(FormBody.Builder()
                .add("grant_type", "authorization_code").add("code", code)
                .add("client_id", BuildConfig.OAUTH_CLIENT_ID)
                .add("redirect_uri", BuildConfig.OAUTH_REDIRECT_URI)
                .add("code_verifier", verifier).build())
            prefs.edit { remove("verifier"); remove("state") }
            save(t)
        } catch (e: Exception) {
            error.value = e.message ?: "Sign-in failed"
        } finally { exchanging = false; busy.value = false }
    }

    fun cancelled(msg: String? = null) { busy.value = false; if (msg != null) error.value = msg }

    private suspend fun tokenRequest(body: FormBody): Tokens = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("${BuildConfig.API_BASE}/oauth/token").post(body).header("Accept", "application/json").build()
        http.newCall(req).execute().use { res ->
            val text = res.body.string()
            if (!res.isSuccessful) throw ApiException(res.code, errorMessage(json, text) ?: "Token request failed (${res.code})")
            json.decodeFromString<Tokens>(text).let { if (it.created_at == 0L) it.copy(created_at = System.currentTimeMillis() / 1000) else it }
        }
    }

    /** Returns a valid access token, refreshing it if it's about to expire. */
    suspend fun accessToken(forceRefresh: Boolean = false): String? = mutex.withLock {
        val t = _tokens.value ?: return null
        val expired = t.expires_in != null && (t.created_at + t.expires_in - 60) * 1000 < System.currentTimeMillis()
        if (!expired && !forceRefresh) return t.access_token
        val rt = t.refresh_token ?: run { save(null); return null }
        return try {
            val nt = tokenRequest(FormBody.Builder().add("grant_type", "refresh_token").add("refresh_token", rt)
                .add("client_id", BuildConfig.OAUTH_CLIENT_ID).build())
            save(nt.copy(refresh_token = nt.refresh_token ?: rt)); nt.access_token
        } catch (e: ApiException) {
            if (e.code in 400..401) save(null)
            null
        }
    }

    suspend fun logout() {
        val t = _tokens.value
        save(null)
        ink.blavingad.hcb.ui.MemCache.clear()
        if (t != null) withContext(Dispatchers.IO) {
            runCatching {
                http.newCall(Request.Builder().url("${BuildConfig.API_BASE}/oauth/revoke")
                    .post(FormBody.Builder().add("token", t.access_token).add("client_id", BuildConfig.OAUTH_CLIENT_ID).build())
                    .build()).execute().close()
            }
        }
    }
}
