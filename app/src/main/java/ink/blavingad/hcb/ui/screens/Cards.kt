package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.Transaction
import ink.blavingad.hcb.ui.CardArt
import ink.blavingad.hcb.ui.CenterLoading
import ink.blavingad.hcb.ui.EmptyState
import ink.blavingad.hcb.ui.ErrorState
import ink.blavingad.hcb.ui.ListBottom
import ink.blavingad.hcb.ui.LocalNav
import ink.blavingad.hcb.ui.LocalSnack
import ink.blavingad.hcb.ui.RefreshBox
import ink.blavingad.hcb.ui.SectionHeader
import ink.blavingad.hcb.ui.SegmentedItem
import ink.blavingad.hcb.ui.money
import ink.blavingad.hcb.ui.rememberLoadable
import ink.blavingad.hcb.ui.rememberPaged
import kotlinx.coroutines.launch

@Composable
fun CardsScreen() {
    val c = LocalContext.current.container
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val cards = rememberLoadable("cards") { c.api.myCards().sortedBy { if (it.status == "active") 0 else if (it.status == "frozen") 1 else 2 } }
    val grants = rememberLoadable("grants") { c.api.cardGrants().filter { it.card_id != null }.associateBy { it.card_id!! } }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(title = { Text("Cards") },
                subtitle = { cards.data?.let { l -> Text("${l.count { it.status == "active" }} active") } },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer))
        },
    ) { pad ->
        val l = cards.data
        when {
            l == null && cards.error != null -> ErrorState(cards.error!!, { scope.launch { cards.load() } }, Modifier.padding(pad))
            l == null -> CenterLoading(Modifier.padding(pad))
            else -> RefreshBox(cards.loading, { scope.launch { cards.load() } }, Modifier.fillMaxSize().padding(pad)) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = ListBottom) {
                    if (l.isEmpty()) item { EmptyState(Icons.Outlined.CreditCard, "No cards yet", "Cards you own on HCB show up here.") }
                    items(l, key = { it.id }) { k ->
                        Surface(onClick = { nav.navigate("card/${k.id}") }, shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { CardArt(k, grant = grants.data?.get(k.id)) }
                    }
                }
            }
        }
    }
}

@Composable
fun CardScreen(id: String) {
    val c = LocalContext.current.container
    val nav = LocalNav.current
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val card = rememberLoadable(id) { c.api.card(id) }
    val grants = rememberLoadable("grants") { c.api.cardGrants().filter { it.card_id != null }.associateBy { it.card_id!! } }
    val txs = rememberPaged<Transaction>(id, idOf = { it.id }) { after -> c.api.cardTransactions(id, after) }
    var busy by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    InfiniteLoader(list, txs)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            MediumFlexibleTopAppBar(title = { Text(card.data?.name ?: "Card") },
                subtitle = { card.data?.last4?.let { Text("•••• $it") } },
                navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer))
        },
    ) { pad ->
        val k = card.data
        if (k == null) { if (card.error != null) ErrorState(card.error!!, { scope.launch { card.load() } }, Modifier.padding(pad)) else CenterLoading(Modifier.padding(pad)); return@Scaffold }
        RefreshBox(card.loading || (txs.loading && txs.items != null), { scope.launch { card.load(); txs.refresh() } }, Modifier.fillMaxSize().padding(pad)) {
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = ListBottom) {
                item { CardArt(k, Modifier.padding(16.dp), grant = grants.data?.get(k.id), showOwner = true) }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val frozen = k.status == "frozen"
                        if (k.status == "active" || frozen) Button(enabled = !busy, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes(), onClick = {
                            busy = true
                            scope.launch {
                                runCatching { c.api.setFrozen(id, !frozen) }
                                    .onSuccess { snack(if (frozen) "Card defrosted" else "Card frozen"); card.load() }
                                    .onFailure { snack(it.message ?: "Failed") }
                                busy = false
                            }
                        }) {
                            if (busy) LoadingIndicator(Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Icon(if (frozen) Icons.Outlined.LocalFireDepartment else Icons.Outlined.AcUnit, null)
                            Text(if (frozen) "  Defrost" else "  Freeze")
                        }
                    }
                }
                val info = listOfNotNull(
                    k.organization?.name?.let { "Organization" to it },
                    k.user?.name?.let { "Cardholder" to it },
                    "Type" to k.type.replaceFirstChar { it.uppercase() },
                    grants.data?.get(k.id)?.let { g -> "Grant" to ((g.balance_cents?.let { money(it) + " left" } ?: "Yes") + (g.purpose?.let { " · $it" } ?: "")) },
                    k.total_spent_cents?.let { "Total spent" to money(it) },
                )
                item { ink.blavingad.hcb.ui.CardDetailsSection(k) }
                item { SectionHeader("Card") }
                itemsIndexed(info) { i, (a, b) ->
                    SegmentedItem(i, info.size) { ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent), overlineContent = { Text(a) }, headlineContent = { Text(b) }) }
                }
                txList(txs, showOrg = false) { t -> nav.navigate("tx/${k.organization?.id ?: t.organization?.id}/${t.id}") }
            }
        }
    }
}
