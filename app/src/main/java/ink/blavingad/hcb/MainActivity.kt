package ink.blavingad.hcb

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.blavingad.hcb.ui.AppRoot
import ink.blavingad.hcb.ui.LockScreen
import ink.blavingad.hcb.ui.ReceiptShortcut
import ink.blavingad.hcb.ui.theme.HcbTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var authLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        // Auth Tab hands the hcb:// redirect straight back; older browsers use a Custom Tab
        // and the redirect then arrives through the hcb:// intent filter (onNewIntent).
        authLauncher = AuthTabIntent.registerActivityResultLauncher(this) { result ->
            when (result.resultCode) {
                AuthTabIntent.RESULT_OK -> result.resultUri?.let(::completeSignIn)
                AuthTabIntent.RESULT_CANCELED -> container.auth.cancelled()
                else -> container.auth.cancelled("Sign-in couldn't be verified. Try again.")
            }
        }
        handleIntent(intent)
        setContent {
            val mode by container.prefs.theme.collectAsStateWithLifecycle()
            val lockOn by container.prefs.lock.collectAsStateWithLifecycle()
            val unlocked by container.unlocked.collectAsStateWithLifecycle()
            val amoled by container.prefs.amoled.collectAsStateWithLifecycle()
            val secure by container.prefs.secure.collectAsStateWithLifecycle()
            val covered by this.covered.collectAsStateWithLifecycle()
            androidx.compose.runtime.LaunchedEffect(secure) { applySecure() }
            HcbTheme(mode, amoled) {
                // window background follows the theme, so nothing white ever flashes (recents, resize, startup)
                val bg = androidx.compose.material3.MaterialTheme.colorScheme.surface
                androidx.compose.runtime.SideEffect { window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bg.toArgb())) }
                androidx.compose.foundation.layout.Box {
                    if (lockOn && !unlocked && container.auth.tokens.value != null) LockScreen { container.unlocked.value = true }
                    else AppRoot(onSignIn = ::startSignIn)
                    // privacy cover shown in recents instead of the real screen (and instead of a blank white card)
                    if (covered && secure) ink.blavingad.hcb.ui.PrivacyCover()
                }
            }
        }
    }

    private val covered = kotlinx.coroutines.flow.MutableStateFlow(false)
    private var foreground = false

    /** FLAG_SECURE only while in the foreground: screenshots stay blocked, but the recents
     *  thumbnail is our themed cover instead of Android's blank white card. */
    private fun applySecure() {
        val f = android.view.WindowManager.LayoutParams.FLAG_SECURE
        if (container.prefs.secure.value && foreground) window.addFlags(f) else window.clearFlags(f)
    }
    override fun onResume() { super.onResume(); foreground = true; covered.value = false; applySecure(); resetStaleSignIn() }
    override fun onPause() {
        foreground = false
        if (container.prefs.secure.value) covered.value = true
        applySecure()
        super.onPause()
    }

    private var stoppedAt = 0L
    override fun onStop() { super.onStop(); stoppedAt = System.currentTimeMillis() }
    override fun onStart() {
        super.onStart()
        // relock after 30 s in the background (not when coming back from the camera / sign-in)
        val skip = container.skipRelock; container.skipRelock = false
        val delay = container.prefs.relockAfter.value * 1000L
        if (!skip && stoppedAt != 0L && System.currentTimeMillis() - stoppedAt >= delay) container.unlocked.value = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        intent?.getStringExtra("receipt_tx")?.let { tx ->
            container.receiptShortcut.value = ReceiptShortcut(intent.getStringExtra("receipt_org") ?: "", tx, intent.getStringExtra("receipt_mode") ?: "camera")
            intent.removeExtra("receipt_tx")
        }
        if (container.auth.isCallback(data)) { completeSignIn(data!!); intent.data = null }
        else if (ink.blavingad.hcb.ui.isHcbLink(data)) { container.deepLink.value = data.toString(); intent?.data = null }
    }

    private fun completeSignIn(uri: Uri) {
        container.scope.launch { container.auth.handleCallback(uri) }
    }

    private val ui = android.os.Handler(android.os.Looper.getMainLooper())
    private var signInStartedAt = 0L

    private fun startSignIn(signup: Boolean) {
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val uri = container.auth.buildAuthorizeUri(dark, signup)
        container.auth.busy.value = true
        container.skipRelock = true
        signInStartedAt = System.currentTimeMillis()
        val launched = runCatching { AuthTabIntent.Builder().build().launch(authLauncher, uri, "hcb") }.isSuccess
        if (!launched) { openInBrowser(uri); return }
        // some browsers accept the Auth Tab intent but never show anything: if we're still
        // in the foreground shortly after, fall back to a Custom Tab / plain browser.
        val started = signInStartedAt
        ui.postDelayed({
            if (foreground && signInStartedAt == started && container.auth.busy.value) openInBrowser(uri)
        }, 2500)
    }

    private fun openInBrowser(uri: Uri) {
        signInStartedAt = System.currentTimeMillis()
        container.skipRelock = true
        try { CustomTabsIntent.Builder().build().launchUrl(this, uri) }
        catch (_: Exception) {
            try { startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)) }
            catch (_: Exception) { container.auth.cancelled("No web browser found to sign in.") }
        }
        // never leave the button spinning if the browser didn't open either
        val started = signInStartedAt
        ui.postDelayed({ if (foreground && signInStartedAt == started && container.auth.busy.value && !container.auth.exchanging) container.auth.cancelled() }, 2500)
    }

    /** Back from the browser without a redirect (closed the tab, Custom Tab has no result): stop the spinner. */
    private fun resetStaleSignIn() {
        if (!container.auth.busy.value || signInStartedAt == 0L) return
        ui.postDelayed({
            if (foreground && container.auth.busy.value && !container.auth.exchanging && System.currentTimeMillis() - signInStartedAt > 2000) container.auth.cancelled()
        }, 700)
    }
}
