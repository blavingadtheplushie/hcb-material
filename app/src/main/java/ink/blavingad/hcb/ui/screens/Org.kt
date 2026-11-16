package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.Transaction
import ink.blavingad.hcb.data.balanceByDate
import ink.blavingad.hcb.ui.MoneyActions
import ink.blavingad.hcb.ui.series
import ink.blavingad.hcb.ui.Avatar
import ink.blavingad.hcb.ui.CardArt
import ink.blavingad.hcb.ui.CenterLoading
import ink.blavingad.hcb.ui.EmptyState
import ink.blavingad.hcb.ui.ErrorState
import ink.blavingad.hcb.ui.ListBottom
import ink.blavingad.hcb.ui.LocalNav
import ink.blavingad.hcb.ui.Paged
import ink.blavingad.hcb.ui.RefreshBox
import ink.blavingad.hcb.ui.SectionHeader
import ink.blavingad.hcb.ui.SegmentedItem
import ink.blavingad.hcb.ui.TxRow
import ink.blavingad.hcb.ui.dayHeader
import ink.blavingad.hcb.ui.money
import ink.blavingad.hcb.ui.openUrl
import ink.blavingad.hcb.ui.parseDate
import ink.blavingad.hcb.ui.rememberLoadable
import ink.blavingad.hcb.ui.rememberPaged
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private val filters = listOf("" to "All", "expenses" to "Expenses", "revenue" to "Income", "missing_receipts" to "Missing receipts")

@Composable
fun OrgScreen(id: String) {
    val c = LocalContext.current.container
    val ctx = LocalContext.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val hide by c.prefs.hideBalances.collectAsStateWithLifecycle()
    val org = rememberLoadable(id) { c.api.organization(id) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("") }
    val txs = rememberPaged<Transaction>(id, search, filter, idOf = { it.id }) { after -> c.api.orgTransactions(id, after, search, filter) }
    val cards = rememberLoadable(id, tab == 1) { if (tab == 1) c.api.orgCards(id) else null }
    // v4 has no description: borrow the public message from v3 when the org is transparent
    val desc = rememberLoadable("desc", id, org.data?.transparent == true) { org.data?.takeIf { it.transparent == true }?.let { runCatching { c.public.org(it.slug).public_message }.getOrNull() } }
    val history = rememberLoadable("bal", id) { runCatching { c.api.balanceByDate(id) }.getOrDefault(emptyList()) }
    val list = rememberLazyListState()
    InfiniteLoader(list, txs)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val o = org.data
    var creating by rememberSaveable { mutableStateOf(false) }
    if (creating && o != null) ink.blavingad.hcb.ui.CreateCardSheet(o.id, o.name, onDismiss = { creating = false }) { k ->
        creating = false; scope.launch { cards.load() }; nav.navigate("card/${k.id}")
    }
    Scaffold(
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(tab == 1 && o != null,
                enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { creating = true },
                    icon = { Icon(androidx.compose.material.icons.Icons.Filled.Add, null) },
                    text = { Text("New card") },
                    expanded = list.firstVisibleItemIndex < 2,
                )
            }
        },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(o?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { Text(if (o?.balance_cents == null) "…" else if (hide) "Balance hidden" else money(o.balance_cents) + " available") },
                navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { if (o != null) IconButton({ ctx.openUrl("https://hcb.hackclub.com/${o.slug}") }) { Icon(Icons.Outlined.OpenInNew, "Open on HCB") } },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
    ) { pad ->
        if (o == null) { if (org.error != null) ErrorState(org.error!!, { scope.launch { org.load() } }, Modifier.padding(pad)) else CenterLoading(Modifier.padding(pad)); return@Scaffold }
        RefreshBox(txs.loading && txs.items != null || org.loading, { scope.launch { org.load(); if (tab == 0) txs.refresh() else if (tab == 1) cards.load() } }, Modifier.fillMaxSize().padding(pad)) {
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = ListBottom) {
                item {
                    BalanceHero(if (hide) null else o.balance_cents, o.fee_balance_cents, history.data?.series().orEmpty())
                    MoneyActions(o.id, o.name, onMoved = { scope.launch { org.load(); txs.refresh() } })
                    desc.data?.takeIf { it.isNotBlank() }?.let { msg ->
                        ink.blavingad.hcb.ui.Markdown(msg, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { url -> if (ink.blavingad.hcb.ui.isHcbLink(android.net.Uri.parse(url))) c.deepLink.value = url else ctx.openUrl(url) }
                    }
                }
                item {
                    val labels = listOf("Activity", "Cards", "Team")
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        labels.forEachIndexed { i, l ->
                            ToggleButton(checked = tab == i, onCheckedChange = { tab = i }, modifier = Modifier.weight(1f),
                                shapes = when (i) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); labels.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() }) { Text(l) }
                        }
                    }
                }
                when (tab) {
                    0 -> {
                        item {
                            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true,
                                placeholder = { Text("Search transactions") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                                shape = RoundedCornerShape(28.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { search = query.trim() }))
                            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                filters.forEach { (k, l) -> FilterChip(selected = filter == k, onClick = { filter = k }, label = { Text(l) }) }
                            }
                        }
                        txList(txs, showOrg = false) { t -> nav.navigate("tx/$id/${t.id}") }
                    }
                    1 -> {
                        val cl = cards.data
                        if (cl == null) item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
                        else if (cl.isEmpty()) item { EmptyState(Icons.Outlined.CreditCard, "No cards in this org") }
                        else itemsIndexed(cl, key = { _, k -> k.id }) { _, k ->
                            Surface(onClick = { nav.navigate("card/${k.id}") }, shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                CardArt(k.copy(organization = k.organization ?: o), showOwner = true)
                            }
                        }
                    }
                    else -> {
                        val users = o.users.orEmpty()
                        if (users.isEmpty()) item { EmptyState(Icons.Outlined.Groups, "No team info") }
                        itemsIndexed(users, key = { _, u -> u.id }) { i, u ->
                            SegmentedItem(i, users.size) {
                                ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    leadingContent = { Avatar(u.avatar, u.name) },
                                    headlineContent = { Text(u.name) },
                                    supportingContent = { u.email?.let { Text(it) } },
                                    trailingContent = { u.role?.let { Text(it.replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.labelLarge) } })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceHero(balance: Long?, fees: Long?, series: List<Double> = emptyList()) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.primaryContainer, contentColor = cs.onPrimaryContainer, shape = RoundedCornerShape(32.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Box {
            ink.blavingad.hcb.ui.BgShape(androidx.compose.material3.MaterialShapes.Cookie12Sided, cs.onPrimaryContainer.copy(alpha = .08f),
                Modifier.align(Alignment.CenterEnd).offset(x = 60.dp).size(200.dp))
            if (balance != null && series.size > 1) ink.blavingad.hcb.ui.AreaChart(series, cs.onPrimaryContainer,
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(100.dp))
            Column(Modifier.padding(24.dp)) {
                Text("Balance", style = MaterialTheme.typography.labelLarge)
                Text(if (balance == null) "••••" else money(balance), style = MaterialTheme.typography.displayMediumEmphasized)
                if (fees != null && fees > 0) Text("${money(fees)} in pending fees", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Day-grouped transaction list with segmented groups; shared by org, card and receipts screens. */
fun androidx.compose.foundation.lazy.LazyListScope.txList(txs: Paged<Transaction>, showOrg: Boolean, onOpen: (Transaction) -> Unit) {
    val items = txs.items
    if (items == null) {
        if (txs.error != null) item { Text(txs.error!!, color = Color.Unspecified, modifier = Modifier.padding(32.dp)) }
        else item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
        return
    }
    if (items.isEmpty()) { item { EmptyState(Icons.Outlined.SearchOff, "Nothing here", "No transactions match.") }; return }
    val groups = items.groupBy { if (it.pending) null else parseDate(it.date) }
    groups.forEach { (day, list) ->
        item(key = "h$day") { SectionHeader(dayHeader(day), Modifier.animateItem()) }
        itemsIndexed(list, key = { _, t -> t.id }) { i, t -> Box(Modifier.animateItem()) { TxRow(t, i, list.size, showOrg) { onOpen(t) } } }
    }
    if (txs.hasMore) item(key = "more") { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(40.dp)) } }
}

@Composable
fun <T> InfiniteLoader(state: LazyListState, paged: Paged<T>) {
    val nearEnd by remember { derivedStateOf { state.layoutInfo.let { (it.visibleItemsInfo.lastOrNull()?.index ?: 0) >= it.totalItemsCount - 4 } } }
    LaunchedEffect(paged) {
        snapshotFlow { nearEnd && paged.items != null && paged.hasMore && !paged.loading && paged.error == null }.distinctUntilChanged().filter { it }.collect { paged.more() }
    }
}
