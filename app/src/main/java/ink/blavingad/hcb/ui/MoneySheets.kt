package ink.blavingad.hcb.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.FileProvider
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import coil3.compose.AsyncImage
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.createInvoice
import ink.blavingad.hcb.data.createSponsor
import ink.blavingad.hcb.data.depositCheck
import ink.blavingad.hcb.data.parseCents
import ink.blavingad.hcb.data.sponsors
import java.io.File

@Composable
fun CheckDepositSheet(orgId: String, orgName: String, onDismiss: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val snack = LocalSnack.current
    var front by rememberSaveable { mutableStateOf<String?>(null) }
    var back by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var side by rememberSaveable { mutableStateOf("front") }
    val shoot = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = pending; pending = null
        if (ok && u != null) { if (side == "front") front = u else back = u }
    }
    fun take(s: String) {
        try {
            side = s
            val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
            val u = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", File(dir, "check-$s-${System.currentTimeMillis()}.jpg"))
            pending = u.toString(); c.skipRelock = true; shoot.launch(u)
        } catch (e: Exception) { pending = null; snack(e.message ?: "Couldn't open the camera") }
    }
    FormSheet("Deposit a check", "into $orgName · sign the back and write \"For deposit to HCB\"", listOf(AMOUNT), "Deposit check", onDismiss,
        extra = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("front" to front, "back" to back).forEach { (s, u) ->
                    Surface(onClick = { take(s) }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.weight(1f).aspectRatio(1.6f)) {
                        Box(contentAlignment = Alignment.Center) {
                            if (u != null) AsyncImage(Uri.parse(u), null, Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
                            else androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.PhotoCamera, null); Text(if (s == "front") "Front" else "Back")
                            }
                        }
                    }
                }
            }
        },
        valid = { parseCents(it["amount"] ?: "") != null && front != null && back != null },
    ) { m ->
        val cr = ctx.contentResolver
        val f = cr.openInputStream(Uri.parse(front!!))!!.use { it.readBytes() }
        val b = cr.openInputStream(Uri.parse(back!!))!!.use { it.readBytes() }
        c.api.depositCheck(orgId, f, b, parseCents(m["amount"]!!)!!); onDone(); "Check submitted"
    }
}

@Composable
fun InvoiceSheet(orgId: String, orgName: String, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val sponsors = rememberLoadable("sponsors", orgId) { runCatching { c.api.sponsors(orgId) }.getOrDefault(emptyList()) }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) } // sponsor id, "new" for a new one
    val isNew = chosen == "new" || (sponsors.data?.isEmpty() == true)
    val base = listOf(
        Field("item_description", "Description"), AMOUNT,
        Field("due", "Due date (YYYY-MM-DD)", type = KeyboardType.Number),
    )
    val newSponsor = listOf(
        Field("name", "Sponsor name"), Field("contact_email", "Sponsor email", type = KeyboardType.Email),
        Field("address_line1", "Address"), Field("address_line2", "Address line 2", optional = true),
        Field("address_city", "City"), Field("address_state", "State / region"), Field("address_postal_code", "Postal code"),
        Field("address_country", "Country code (US, FR…)"),
    )
    FormSheet("New invoice", "for $orgName · HCB emails it to the sponsor", if (isNew) newSponsor + base else base, "Send invoice", onDismiss,
        extra = {
            val list = sponsors.data.orEmpty()
            if (list.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                list.forEach { s -> FilterChip(chosen == s.id, { chosen = s.id }, { Text(s.name) }) }
                FilterChip(chosen == "new", { chosen = "new" }, { Text("+ New sponsor") })
            }
        },
        valid = { m -> parseCents(m["amount"] ?: "") != null && runCatching { java.time.LocalDate.parse(m["due"]!!.trim()) }.isSuccess && (isNew || chosen != null) },
    ) { m ->
        val sid = if (isNew) c.api.createSponsor(orgId, newSponsor.associate { it.key to (m[it.key] ?: "") }.mapValues { (k, v) -> if (k == "address_country") v.trim().uppercase() else v }).id else chosen!!
        c.api.createInvoice(orgId, sid, m["due"]!!.trim(), m["item_description"]!!, parseCents(m["amount"]!!)!!)
        "Invoice sent"
    }
}

