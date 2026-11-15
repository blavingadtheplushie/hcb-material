package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.blavingad.hcb.container
import ink.blavingad.hcb.ui.Avatar
import ink.blavingad.hcb.ui.BentoGrid
import ink.blavingad.hcb.ui.CenterLoading
import ink.blavingad.hcb.ui.EmptyState
import ink.blavingad.hcb.ui.ErrorState
import ink.blavingad.hcb.ui.ListBottom
import ink.blavingad.hcb.ui.LocalNav
import ink.blavingad.hcb.ui.MissingBanner
import ink.blavingad.hcb.ui.RefreshBox
import ink.blavingad.hcb.ui.SectionHeader
import ink.blavingad.hcb.ui.SegmentedItem
import ink.blavingad.hcb.ui.money
import ink.blavingad.hcb.ui.rememberLoadable
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val c = LocalContext.current.container
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val hide by c.prefs.hideBalances.collectAsStateWithLifecycle()
    val user = rememberLoadable("user") { c.api.user() }
    val orgs = rememberLoadable("orgs") { c.api.organizations() }
    val invites = rememberLoadable("inv") { runCatching { c.api.invitations() }.getOrDefault(emptyList()).filter { !it.accepted } }
    var missing by remember { mutableStateOf(0) }
    val balances = remember { mutableStateMapOf<String, Long?>() }
    LaunchedEffect(Unit) { missing = runCatching { c.api.missingReceiptCount() }.getOrDefault(0) }
    LaunchedEffect(orgs.data) {
        orgs.data?.map { o -> async { balances[o.id] = runCatching { c.api.orgBalance(o.id).balance_cents }.getOrNull() } }?.forEach { it.await() }
    }
    val cards = rememberLoadable("homeCards") { c.api.myCards() }
    val recent = rememberLoadable("homeRecent", orgs.data?.map { it.id }) { kotlinx.coroutines.coroutineScope {
        orgs.data.orEmpty().map { o -> async { runCatching { c.api.orgTransactions(o.id, null, null, null).data.map { it.copy(organization = it.organization ?: o) } }.getOrDefault(emptyList()) } }
            .flatMap { it.await() }.sortedByDescending { it.date ?: "" }
    } }
    val refreshAll: () -> Unit = {
        scope.launch {
            listOf(async { user.load() }, async { orgs.load() }, async { invites.load() }, async { cards.load() }, async { recent.load() },
                async { missing = runCatching { c.api.missingReceiptCount() }.getOrDefault(missing) }).forEach { it.await() }
        }
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(user.data?.name?.substringBefore(' ')?.let { "Hey $it" } ?: "HCB") },
                subtitle = { Text(orgs.data?.size?.let { if (it == 1) "1 organization" else "$it organizations" } ?: "Loading…") },
                actions = {
                    user.data?.let { u ->
                        androidx.compose.material3.IconButton({
                            nav.navigate("settings") {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }, Modifier.padding(end = 8.dp_())) { Avatar(u.avatar, u.name, size = 36.dp_()) }
                    }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
    ) { pad ->
        when {
            orgs.data == null && orgs.error != null -> ErrorState(orgs.error!!, refreshAll, Modifier.padding(pad))
            orgs.data == null -> CenterLoading(Modifier.padding(pad))
            else -> RefreshBox(orgs.loading || user.loading, refreshAll, Modifier.fillMaxSize().padding(pad)) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = ListBottom) {
                    item {
                        val month = java.time.LocalDate.now().toString().take(7)
                        val list0 = orgs.data!!
                        BentoGrid(
                            total = if (list0.all { balances.containsKey(it.id) }) list0.sumOf { balances[it.id] ?: 0L } else null,
                            orgCount = list0.size, missing = missing,
                            activeCards = cards.data?.count { it.status == "active" },
                            spentMonth = recent.data?.filter { (it.date ?: "").startsWith(month) && it.amount_cents < 0 && !it.declined }?.sumOf { -it.amount_cents },
                            last = recent.data?.firstOrNull(), hide = hide,
                            onReceipts = { nav.navigate("receipts") }, onCards = { nav.navigate("cards") },
                            onLast = { recent.data?.firstOrNull()?.let { t -> nav.navigate("tx/${t.organization?.id}/${t.id}") } },
                        )
                    }
                    val inv = invites.data.orEmpty()
                    if (inv.isNotEmpty()) {
                        item { SectionHeader("Invitations") }
                        itemsIndexed(inv, key = { _, i -> "i" + i.id }) { i, it ->
                            SegmentedItem(i, inv.size, { nav.navigate("invite/${it.id}") }, color = MaterialTheme.colorScheme.secondaryContainer) {
                                ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    leadingContent = { Icon(Icons.Outlined.Mail, null) },
                                    headlineContent = { Text(it.organization?.name ?: "Organization invite") },
                                    supportingContent = { Text(it.sender?.name?.let { n -> "From $n" } ?: "Tap to respond") })
                            }
                        }
                    }
                    val list = orgs.data!!
                    item { SectionHeader("Organizations") }
                    if (list.isEmpty()) item { EmptyState(Icons.Outlined.AccountBalance, "No organizations yet", "When you join an org on HCB it shows up here.") }
                    itemsIndexed(list, key = { _, o -> o.id }) { i, o ->
                        SegmentedItem(i, list.size, { nav.navigate("org/${o.id}") }) {
                            ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = { Avatar(o.icon, o.name, square = true) },
                                headlineContent = { Text(o.name) },
                                supportingContent = { if (o.playground_mode == true) Text("Playground") else if (o.financially_frozen == true) Text("Frozen") else Text(o.slug) },
                                trailingContent = {
                                    val b = balances[o.id]
                                    Text(if (hide) "••••" else if (balances.containsKey(o.id)) money(b) else "…", style = MaterialTheme.typography.titleMediumEmphasized)
                                })
                        }
                    }
                }
            }
        }
    }
}

private fun Int.dp_() = androidx.compose.ui.unit.Dp(this.toFloat())
