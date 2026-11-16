package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.checkDeposits
import ink.blavingad.hcb.data.checks
import ink.blavingad.hcb.data.invoices
import ink.blavingad.hcb.data.wires
import ink.blavingad.hcb.ui.EmptyState
import ink.blavingad.hcb.ui.ListBottom
import ink.blavingad.hcb.ui.LocalNav
import ink.blavingad.hcb.ui.RefreshBox
import ink.blavingad.hcb.ui.SectionHeader
import ink.blavingad.hcb.ui.SegmentedItem
import ink.blavingad.hcb.ui.money
import ink.blavingad.hcb.ui.prettyDate
import ink.blavingad.hcb.ui.rememberLoadable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private data class Row(val icon: ImageVector, val title: String, val sub: String, val amount: String?, val status: String?)

private fun pretty(s: String?) = s?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }

@Composable
fun MoneyHistoryScreen(org: String) {
    val c = LocalContext.current.container
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val checks = rememberLoadable("hist-checks", org) { runCatching { c.api.checks(org) }.getOrNull() }
    val deposits = rememberLoadable("hist-dep", org) { runCatching { c.api.checkDeposits(org) }.getOrNull() }
    val wires = rememberLoadable("hist-wires", org) { runCatching { c.api.wires(org) }.getOrNull() }
    val invoices = rememberLoadable("hist-inv", org) { runCatching { c.api.invoices(org) }.getOrNull() }
    val sections = listOf(
        "Invoices" to invoices.data?.map { Row(Icons.Outlined.RequestQuote, it.to ?: "Sponsor", listOfNotNull(it.item_description, it.due_date?.let { d -> "due " + prettyDate(d) }).joinToString(" · "), (it.item_amount ?: it.amount_due)?.let { a -> money(a) }, it.status) },
        "Check deposits" to deposits.data?.map { Row(Icons.Outlined.PhotoCamera, "Check deposit", listOfNotNull(prettyDate(it.created_at), it.rejection?.reason?.let { r -> "rejected: $r" }, it.estimated_arrival_date?.let { d -> "arrives " + prettyDate(d) }).joinToString(" · "), money(it.amount_cents), pretty(it.status)) },
        "Checks" to checks.data?.map { Row(Icons.Outlined.Mail, it.recipient_name ?: "Check", listOfNotNull(it.payment_for ?: it.memo, it.check_number?.let { n -> "#$n" }).joinToString(" · "), null, pretty(it.status)) },
        "Wires" to wires.data?.map { Row(Icons.Outlined.Public, it.recipient_name ?: "Wire", listOfNotNull(it.payment_for, it.recipient_country).joinToString(" · "), "%.2f %s".format(it.amount_cents / 100.0, it.currency ?: ""), pretty(it.state)) },
    )
    val loading = listOf(checks, deposits, wires, invoices).any { it.loading }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(title = { Text("Money moves") }, subtitle = { Text("Invoices, checks, deposits, wires") },
                navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer))
        },
    ) { pad ->
        RefreshBox(loading, { scope.launch { listOf(checks, deposits, wires, invoices).map { async { it.load() } }.forEach { it.await() } } }, Modifier.fillMaxSize().padding(pad)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = ListBottom) {
                if (sections.all { it.second == null } && loading) item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
                else if (sections.all { it.second.isNullOrEmpty() }) item { EmptyState(Icons.Outlined.History, "Nothing yet", "Invoices, checks and wires you send show up here.") }
                sections.forEach { (title, rows) ->
                    if (!rows.isNullOrEmpty()) {
                        item(key = "h$title") { SectionHeader(title) }
                        itemsIndexed(rows, key = { i, _ -> "$title$i" }) { i, r ->
                            SegmentedItem(i, rows.size) {
                                ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    leadingContent = { Icon(r.icon, null) },
                                    headlineContent = { Text(r.title) },
                                    supportingContent = { Text(listOfNotNull(r.status, r.sub.takeIf { it.isNotBlank() }).joinToString(" · ")) },
                                    trailingContent = r.amount?.let { a -> { Text(a, style = MaterialTheme.typography.titleMediumEmphasized) } })
                            }
                        }
                    }
                }
            }
        }
    }
}
