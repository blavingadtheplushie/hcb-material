package ink.blavingad.hcb.ui

import android.net.Uri
import androidx.navigation.NavHostController
import ink.blavingad.hcb.Container

private val HOSTS = setOf("hcb.hackclub.com", "hcb.gg", "www.hcb.gg", "bank.hackclub.com")
// first path segments that are app pages, not organizations
private val RESERVED = setOf("", "my", "users", "user", "auth", "api", "donations", "admin", "logout", "login", "signup", "apply",
    "invites", "invite", "settings", "receipts", "reimbursements", "cards", "stripe_cards", "grants", "card_grants", "docs", "help",
    "check_deposits", "transfers", "ach_transfers", "checks", "invoices", "sponsors", "mobile", "explore", "search", "events", "organizations")

fun isHcbLink(uri: Uri?): Boolean = uri != null && (uri.scheme == "https" || uri.scheme == "http") && uri.host?.lowercase() in HOSTS

/** Opens an hcb.hackclub.com / hcb.gg link inside the app. Returns false if it should go to the browser. */
suspend fun Container.openLink(raw: String, nav: NavHostController): Boolean {
    var uri = Uri.parse(raw)
    if (uri.host?.lowercase()?.endsWith("hcb.gg") == true) {
        // short links: let HCB tell us where they go
        val target = public.resolveShort(raw) ?: return false
        uri = Uri.parse(target)
        if (!isHcbLink(uri) || uri.host?.endsWith("hcb.gg") == true) return false
    }
    val seg = uri.pathSegments.orEmpty()
    val first = seg.firstOrNull()?.lowercase() ?: ""
    if (first == "hcb" && seg.size >= 2) {
        val id = "txn_" + seg[1].removePrefix("txn_")
        val t = runCatching { api.transactionById(id) }.getOrNull() ?: return false
        val org = t.organization?.id ?: return false
        nav.navigate("tx/$org/$id"); return true
    }
    if (first in RESERVED) return false
    val slug = seg.first()
    // member? full org view. otherwise the public (transparent) view.
    runCatching { api.orgBalance(slug) }.getOrNull()?.let { nav.navigate("org/${it.id}"); return true }
    runCatching { public.org(slug) }.getOrNull()?.takeIf { it.transparent }?.let { nav.navigate("porg/${it.slug}"); return true }
    return false
}
