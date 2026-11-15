package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.Transaction
import ink.blavingad.hcb.ui.ListBottom
import ink.blavingad.hcb.ui.LocalNav
import ink.blavingad.hcb.ui.RefreshBox
import ink.blavingad.hcb.ui.SectionHeader
import ink.blavingad.hcb.ui.openUrl
import ink.blavingad.hcb.ui.rememberLoadable
import ink.blavingad.hcb.ui.rememberPaged
import ink.blavingad.hcb.ui.rememberReceiptUploader
import kotlinx.coroutines.launch

@Composable
fun ReceiptsScreen() {
    val ctx = LocalContext.current
    val c = ctx.container
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val missing = rememberPaged<Transaction>("missing", idOf = { it.id }) { after -> c.api.missingReceipts(after) }
    val bin = rememberLoadable("bin") { c.api.receipts(null) }
    val upload = rememberReceiptUploader(null) { scope.launch { bin.load() } }
    val list = rememberLazyListState()
    val snack = ink.blavingad.hcb.ui.LocalSnack.current
    var viewing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ink.blavingad.hcb.data.Receipt?>(null) }
    viewing?.let { ink.blavingad.hcb.ui.ReceiptViewer(it) { viewing = null } }
    var deleting by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ink.blavingad.hcb.data.Receipt?>(null) }
    deleting?.let { r ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            icon = { Icon(androidx.compose.material.icons.Icons.Outlined.Delete, null) },
            title = { Text("Delete this receipt?") },
            text = { Text(r.filename ?: "It will be removed from your receipt bin.") },
            confirmButton = { androidx.compose.material3.TextButton({
                deleting = null
                scope.launch {
                    runCatching { c.api.deleteReceipt(r.id) }
                        .onSuccess { bin.data = bin.data?.filterNot { it.id == r.id }; snack("Receipt deleted"); bin.load() }
                        .onFailure { snack(it.message ?: "Delete failed") }
                }
            }) { Text("Delete") } },
            dismissButton = { androidx.compose.material3.TextButton({ deleting = null }) { Text("Cancel") } },
        )
    }
    InfiniteLoader(list, missing)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(title = { Text("Receipts") },
                subtitle = { missing.items?.let { Text(if (missing.total == 0) "All caught up" else "${missing.total} missing") } },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer))
        },
    ) { pad ->
        RefreshBox(missing.loading && missing.items != null || bin.loading && bin.data != null, { scope.launch { missing.refresh(); bin.load() } }, Modifier.fillMaxSize().padding(pad)) {
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = ListBottom) {
                item {
                    SectionHeader("Receipt bin")
                    Text("Snap receipts now, attach them to transactions later on HCB.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 32.dp))
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = upload.camera, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) { Icon(Icons.Outlined.PhotoCamera, null); Text("  Camera") }
                        FilledTonalButton(onClick = upload.file, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) { Icon(Icons.Outlined.AttachFile, null); Text("  File") }
                    }
                    if (upload.busy) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { LoadingIndicator() }
                    val r = bin.data.orEmpty()
                    if (r.isNotEmpty()) LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(r, key = { it.id }) { x ->
                            Column(Modifier.size(110.dp, 170.dp).animateItem()) {
                                Surface(onClick = { viewing = x }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(110.dp, 136.dp)) {
                                    AsyncImage(x.preview_url ?: x.url, x.filename, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)))
                                }
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text(x.filename ?: "", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    androidx.compose.material3.IconButton({ deleting = x }, Modifier.size(28.dp)) {
                                        Icon(androidx.compose.material.icons.Icons.Outlined.Delete, "Delete receipt", Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                item { SectionHeader("Missing receipts") }
                txList(missing, showOrg = true) { t -> t.organization?.id?.let { nav.navigate("tx/$it/${t.id}") } }
            }
        }
    }
}
