package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.Transaction
import ink.blavingad.hcb.ui.CenterLoading
import ink.blavingad.hcb.ui.ErrorState
import ink.blavingad.hcb.ui.ListBottom
import ink.blavingad.hcb.ui.LocalNav
import ink.blavingad.hcb.ui.LocalSnack
import ink.blavingad.hcb.ui.Pill
import ink.blavingad.hcb.ui.RefreshBox
import ink.blavingad.hcb.ui.SectionHeader
import ink.blavingad.hcb.ui.money
import ink.blavingad.hcb.ui.openUrl
import ink.blavingad.hcb.ui.rememberLoadable
import ink.blavingad.hcb.ui.rememberPaged
import kotlinx.coroutines.launch

/** Read-only view of a transparent organization you're not a member of (public v3 API). */
@Composable
fun PublicOrgScreen(slug: String) {
    val ctx = LocalContext.current
    val c = ctx.container
    val nav = LocalNav.current
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val org = rememberLoadable("pub", slug) { c.public.org(slug) }
    val txs = rememberPaged<Transaction>("pub", slug, idOf = { it.code ?: it.id }) { after -> c.public.transactions(slug, after) }
    val list = rememberLazyListState()
    InfiniteLoader(list, txs)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val o = org.data
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(o?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { Text(o?.balances?.balance_cents?.let { money(it) + " · transparent" } ?: "…") },
                navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton({ ctx.openUrl("https://hcb.hackclub.com/$slug") }) { Icon(Icons.Outlined.OpenInNew, "Open on HCB") } },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
    ) { pad ->
        if (o == null) { if (org.error != null) ErrorState(org.error!!, { scope.launch { org.load() } }, Modifier.padding(pad)) else CenterLoading(Modifier.padding(pad)); return@Scaffold }
        RefreshBox(txs.loading && txs.items != null || org.loading, { scope.launch { org.load(); txs.refresh() } }, Modifier.fillMaxSize().padding(pad)) {
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = ListBottom) {
                item { BalanceHero(o.balances?.balance_cents, o.balances?.fee_balance_cents) }
                item {
                    val cs = MaterialTheme.colorScheme
                    androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Public, null, Modifier.padding(end = 6.dp), tint = cs.onSurfaceVariant)
                        Pill("Public finances", cs.secondaryContainer, cs.onSecondaryContainer)
                    }
                    o.public_message?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    }
                }
                item { SectionHeader("Activity") }
                txList(txs, showOrg = false) { snack("Transaction details are only visible to members") }
            }
        }
    }
}
