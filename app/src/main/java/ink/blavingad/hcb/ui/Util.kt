package ink.blavingad.hcb.ui

import android.content.Context
import android.content.ActivityNotFoundException
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

fun Context.openUrl(url: String) {
    try { CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, url.toUri()) }
    catch (_: ActivityNotFoundException) {}
}
