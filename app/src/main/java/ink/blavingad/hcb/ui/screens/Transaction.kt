package ink.blavingad.hcb.ui.screens

import android.net.Uri
import ink.blavingad.hcb.ui.parseDate
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.automirrored.outlined.Label
import ink.blavingad.hcb.data.memoSuggestions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.Transaction
import ink.blavingad.hcb.ui.Avatar
import ink.blavingad.hcb.ui.CenterLoading
import ink.blavingad.hcb.ui.ErrorState
import ink.blavingad.hcb.ui.ListBottom
import ink.blavingad.hcb.ui.LocalNav
import ink.blavingad.hcb.ui.LocalSnack
import ink.blavingad.hcb.ui.Pill
import ink.blavingad.hcb.ui.SectionHeader
import ink.blavingad.hcb.ui.SegmentedItem
import ink.blavingad.hcb.ui.kind
import ink.blavingad.hcb.ui.money
import ink.blavingad.hcb.ui.openUrl
import ink.blavingad.hcb.ui.prettyDate
import ink.blavingad.hcb.ui.rememberReceiptUploader
import ink.blavingad.hcb.ui.prettyDateTime
import ink.blavingad.hcb.ui.rememberLoadable
import ink.blavingad.hcb.ui.title
import kotlinx.coroutines.launch

@Composable
fun TransactionScreen(org: String, id: String, mode: String? = null) {
    val ctx = LocalContext.current
    val c = ctx.container
    val nav = LocalNav.current
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val tx = rememberLoadable(org, id) { c.api.transaction(org, id) }
    val receipts = rememberLoadable("r", id) { c.api.receipts(id) }
    val comments = rememberLoadable("c", id) { c.api.comments(id) }
    var viewing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ink.blavingad.hcb.data.Receipt?>(null) }
    viewing?.let { ink.blavingad.hcb.ui.ReceiptViewer(it) { viewing = null } }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<String?>(null) }
    var tagging by remember { mutableStateOf(false) }
    val suggestions = rememberLoadable("memo", id, renaming != null) { if (renaming != null) c.api.memoSuggestions(org, id) else emptyList() }
    var comment by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val upload = rememberReceiptUploader(id) { scope.launch { receipts.load(); tx.load() } }
    var autoDone by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(mode) {
        if (!autoDone) { autoDone = true; when (mode) { "camera" -> upload.camera(); "file" -> upload.file() } }
    }

    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val t = tx.data
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(t?.title() ?: "") },
                subtitle = { t?.let { Text(it.kind()) } },
                navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton({ menu = true }) { Icon(Icons.Outlined.MoreVert, "More") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text("Rename") }, { menu = false; renaming = t?.memo ?: "" }, leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                        DropdownMenuItem({ Text("Tags") }, { menu = false; tagging = true }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Label, null) })
                        DropdownMenuItem({ Text("Open on HCB") }, { menu = false; ctx.openUrl("https://hcb.hackclub.com/hcb/${id.removePrefix("txn_")}") }, leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) })
                    }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
    ) { pad ->
        if (t == null) { if (tx.error != null) ErrorState(tx.error!!, { scope.launch { tx.load() } }, Modifier.padding(pad)) else CenterLoading(Modifier.padding(pad)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = ListBottom) {
            item { AmountHero(t) }
            val rows = details(t)
            item { SectionHeader("Details") }
            itemsIndexed(rows) { i, (k, v) ->
                SegmentedItem(i, rows.size) {
                    val wallet = t.card_charge?.wallet?.takeIf { k == "Card" && it.isNotBlank() }
                    ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        overlineContent = { Text(k) }, headlineContent = { Text(v) },
                        trailingContent = wallet?.let { w -> { ink.blavingad.hcb.ui.WalletIcon(w, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f)) } })
                }
            }
            item {
                SectionHeader("Receipts")
                val list = receipts.data
                if (t.missing_receipt) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ReceiptLong, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text("This transaction needs a receipt", Modifier.padding(start = 12.dp).weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                if (list == null) Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) { LoadingIndicator() }
                else if (list.isNotEmpty()) LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.id }) { r ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(onClick = { viewing = r }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(120.dp, 150.dp)) {
                                AsyncImage(r.preview_url ?: r.url, r.filename, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)))
                            }
                            IconButton({ scope.launch {
                                runCatching { c.api.deleteReceipt(r.id) }.onSuccess { snack("Receipt deleted"); receipts.load() }.onFailure { snack(it.message ?: "Delete failed") }
                            } }) { Icon(Icons.Outlined.Delete, "Delete receipt") }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = upload.camera, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) { Icon(Icons.Outlined.PhotoCamera, null); Text("  Camera") }
                    FilledTonalButton(onClick = upload.file, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) { Icon(Icons.Outlined.AttachFile, null); Text("  File") }
                }
                if (upload.busy) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { LoadingIndicator() }
                if (t.missing_receipt) TextButton({ scope.launch {
                    runCatching { c.api.markNoReceipt(id) }.onSuccess { snack("Marked as no receipt"); tx.load() }.onFailure { snack(it.message ?: "Failed") }
                } }, Modifier.padding(horizontal = 16.dp)) { Text("I don't have a receipt") }
            }
            item { SectionHeader("Comments") }
            val cl = comments.data.orEmpty()
            itemsIndexed(cl, key = { _, k -> k.id }) { i, k ->
                SegmentedItem(i, cl.size) {
                    ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Avatar(k.user?.avatar, k.user?.name ?: "?") },
                        overlineContent = { Text(listOfNotNull(k.user?.name, prettyDateTime(k.created_at)).joinToString(" · ")) },
                        headlineContent = { Text(k.content) })
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(comment, { comment = it }, Modifier.weight(1f), placeholder = { Text("Add a comment") }, shape = RoundedCornerShape(28.dp))
                    IconButton(enabled = comment.isNotBlank() && !busy, onClick = {
                        busy = true
                        scope.launch {
                            runCatching { c.api.addComment(id, comment.trim()) }
                                .onSuccess { comment = ""; comments.load() }.onFailure { snack(it.message ?: "Couldn't comment") }
                            busy = false
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
                }
            }
        }
    }

    if (tagging && t != null) ink.blavingad.hcb.ui.TagSheet(org, t, { tagging = false }) { nt -> tagging = false; tx.data = nt.copy(organization = nt.organization ?: t.organization); scope.launch { tx.load() } }
    renaming?.let { value ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename transaction") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value, { renaming = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ink.blavingad.hcb.ui.MemoSuggestions(suggestions.data.orEmpty().filter { it != value }) { renaming = it }
            } },
            confirmButton = { Button({
                val m = value.trim(); renaming = null
                scope.launch { runCatching { c.api.renameTransaction(org, id, m) }.onSuccess { tx.data = it.copy(organization = it.organization ?: t?.organization); tx.load() }.onFailure { snack(it.message ?: "Rename failed") } }
            }, shapes = ButtonDefaults.shapes()) { Text("Save") } },
            dismissButton = { TextButton({ renaming = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AmountHero(t: Transaction) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(money(t.amount_cents, signed = true), style = MaterialTheme.typography.displayLargeEmphasized,
            color = if (t.amount_cents > 0) cs.tertiary else cs.onSurface, textAlign = TextAlign.Center)
        ConvertedAmount(t)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (t.pending) Pill("Pending", cs.secondaryContainer, cs.onSecondaryContainer)
            if (t.declined) Pill("Declined", cs.errorContainer, cs.onErrorContainer)
            if (t.reversed) Pill("Reversed", cs.surfaceContainerHighest, cs.onSurface)
            if (t.lost_receipt) Pill("No receipt", cs.surfaceContainerHighest, cs.onSurface)
            t.tags.orEmpty().forEach { val (bg, fg) = ink.blavingad.hcb.ui.tagColors(it.color); Pill((it.emoji?.let { e -> "$e " } ?: "") + it.label, bg, fg) }
        }
    }
}

private fun details(t: Transaction): List<Pair<String, String>> = buildList {
    add("Date" to prettyDate(t.date))
    t.card_charge?.let { cc ->
        cc.merchant?.let { m -> add("Merchant" to listOfNotNull(m.name, m.country).joinToString(" · ")) }
        cc.spent_at?.let { add("Spent at" to prettyDateTime(it)) }
        cc.card?.let { k -> add("Card" to listOfNotNull(k.user?.name, k.last4?.let { "•••• $it" }).joinToString(" · ")) }
        cc.decline_reason?.let { add("Decline reason" to it) }
    }
    t.donation?.let { d ->
        d.donor?.name?.let { add("Donor" to it) }
        d.donor?.email?.let { add("Email" to it) }
        d.message?.takeIf { it.isNotBlank() }?.let { add("Message" to it) }
        if (d.refunded == true) add("Status" to "Refunded")
    }
    t.transfer?.let { x ->
        x.from?.name?.let { add("From" to it) }; x.to?.name?.let { add("To" to it) }
        x.status?.let { add("Status" to it) }; x.sender?.name?.let { add("Sent by" to it) }
    }
    t.check?.let { x -> x.recipient_name?.let { add("Recipient" to it) }; x.memo?.let { add("Check memo" to it) }; x.status?.let { add("Status" to it) } }
    t.ach_transfer?.let { x -> x.recipient_name?.let { add("Recipient" to it) }; x.bank_name?.let { add("Bank" to it) }; x.payment_for?.let { add("For" to it) } }
    t.check_deposit?.let { x -> x.status?.let { add("Status" to it.replace('_', ' ')) }; x.estimated_arrival_date?.let { add("Arrives" to prettyDate(it)) }; x.rejection?.reason?.let { add("Rejected" to it) } }
    t.wire_transfer?.let { x -> x.recipient_name?.let { add("Recipient" to it) }; x.recipient_country?.let { add("Country" to it) }; x.payment_for?.let { add("For" to it) }; x.state?.let { add("Status" to it) } }
    t.invoice?.let { x -> x.sponsor?.name?.let { add("Sponsor" to it) }; x.description?.let { add("Description" to it) }; x.due_date?.let { add("Due" to prettyDate(it)) } }
}

@Composable
private fun ConvertedAmount(t: Transaction) {
    val c = androidx.compose.ui.platform.LocalContext.current.container
    val to by c.prefs.convertTo.collectAsStateWithLifecycle()
    val hide by c.prefs.hideBalances.collectAsStateWithLifecycle()
    if (to.isEmpty() || to == "USD" || t.amount_cents == 0L) return
    val day = remember(t.id, t.date, t.card_charge?.spent_at) {
        t.card_charge?.spent_at?.let { runCatching { java.time.OffsetDateTime.parse(it).toLocalDate() }.getOrNull() } ?: parseDate(t.date) ?: java.time.LocalDate.now()
    }
    val card = t.card_charge != null
    var rate by remember(t.id, to) { mutableStateOf<ink.blavingad.hcb.data.Fx.Rate?>(null) }
    LaunchedEffect(t.id, to, day) { rate = c.fx.rate(to, day, card) }
    val r = rate ?: return
    val fmt = remember(to) { java.text.NumberFormat.getCurrencyInstance().apply { currency = java.util.Currency.getInstance(to) } }
    val v = t.amount_cents / 100.0 * r.rate
    val txt = if (hide) "≈ ••••" else "≈ " + (if (v < 0) "−" else "") + fmt.format(kotlin.math.abs(v))
    Text("$txt · ${r.source} ${prettyDate(r.date.toString())}", style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
}
