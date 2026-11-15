package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    var tab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val cards = rememberLoadable("pubcards", slug, tab == 1) {
        if (tab == 1) c.public.cards(slug, org.data?.let { ink.blavingad.hcb.data.Organization(id = it.id, name = it.name, slug = it.slug) }) else null
    }
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
        RefreshBox(txs.loading && txs.items != null || org.loading, { scope.launch { org.load(); if (tab == 1) cards.load() else txs.refresh() } }, Modifier.fillMaxSize().padding(pad)) {
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = ListBottom) {
                item { BalanceHero(o.balances?.balance_cents, o.balances?.fee_balance_cents) }
                item {
                    val cs = MaterialTheme.colorScheme
                    androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Public, null, Modifier.padding(end = 6.dp), tint = cs.onSurfaceVariant)
                        Pill("Public finances", cs.secondaryContainer, cs.onSecondaryContainer)
                    }
                    o.public_message?.takeIf { it.isNotBlank() }?.let { msg ->
                        ink.blavingad.hcb.ui.Markdown(msg, Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { url -> if (ink.blavingad.hcb.ui.isHcbLink(android.net.Uri.parse(url))) c.deepLink.value = url else ctx.openUrl(url) }
                    }
                }
                item {
                    val labels = listOf("Activity", "Cards", "Team")
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        labels.forEachIndexed { i, l ->
                            androidx.compose.material3.ToggleButton(checked = tab == i, onCheckedChange = { tab = i }, modifier = Modifier.weight(1f),
                                shapes = when (i) { 0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes(); labels.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes() }) { Text(l) }
                        }
                    }
                }
                when (tab) {
                    0 -> txList(txs, showOrg = false) { snack("Transaction details are only visible to members") }
                    1 -> {
                        val cl = cards.data
                        if (cl == null) item {
                            if (cards.error != null) ErrorState(cards.error!!, { scope.launch { cards.load() } })
                            else androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { androidx.compose.material3.LoadingIndicator() }
                        }
                        else if (cl.isEmpty()) item { ink.blavingad.hcb.ui.EmptyState(androidx.compose.material.icons.Icons.Outlined.CreditCard, "No cards in this org") }
                        else items(cl.size, key = { cl[it].id }) { i ->
                            androidx.compose.material3.Surface(onClick = { snack("Card details are only visible to members") }, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).animateItem()) {
                                ink.blavingad.hcb.ui.CardArt(cl[i], showOwner = true)
                            }
                        }
                    }
                    else -> {
                        val users = o.users.orEmpty().sortedWith(compareByDescending<ink.blavingad.hcb.data.V3User> { it.admin }.thenBy { it.full_name ?: "" })
                        if (users.isEmpty()) item { ink.blavingad.hcb.ui.EmptyState(androidx.compose.material.icons.Icons.Outlined.Groups, "No team info") }
                        else items(users.size, key = { users[it].id }) { i ->
                            val u = users[i]
                            ink.blavingad.hcb.ui.SegmentedItem(i, users.size) {
                                androidx.compose.material3.ListItem(colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                    leadingContent = { ink.blavingad.hcb.ui.Avatar(u.photo, u.full_name ?: "?") },
                                    headlineContent = { Text(u.full_name ?: "Unknown") },
                                    trailingContent = { if (u.admin) Text("Manager", style = MaterialTheme.typography.labelLarge) })
                            }
                        }
                    }
                }
            }
        }
    }
}
