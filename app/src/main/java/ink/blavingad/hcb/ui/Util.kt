package ink.blavingad.hcb.ui

import android.content.Context
import android.content.ActivityNotFoundException
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/** Opens a URL in a real browser, never in this app (avoids hcb links bouncing back to us). */
fun Context.openUrlInBrowser(url: String) {
    val pm = packageManager
    val probe = android.content.Intent(android.content.Intent.ACTION_VIEW, "https://example.com".toUri())
    val browser = pm.resolveActivity(probe, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        ?.takeIf { it != packageName && it != "android" }
        ?: pm.queryIntentActivities(probe, 0).map { it.activityInfo.packageName }.firstOrNull { it != packageName }
    try {
        val tab = CustomTabsIntent.Builder().setShowTitle(true).build()
        if (browser != null) tab.intent.setPackage(browser)
        tab.launchUrl(this, url.toUri())
    } catch (_: Exception) {}
}

fun Context.openUrl(url: String) {
    if (ink.blavingad.hcb.ui.isHcbLink(url.toUri())) { openUrlInBrowser(url); return }
    try { CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, url.toUri()) }
    catch (_: ActivityNotFoundException) {}
}
