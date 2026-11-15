package ink.blavingad.hcb.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.Card
import ink.blavingad.hcb.data.CardSecrets
import ink.blavingad.hcb.data.cardSecrets
import kotlinx.coroutines.launch

private fun CardSecrets.pretty() = number.chunked(4).joinToString(" ")
private fun CardSecrets.exp() = "%02d/%02d".format(exp_month, exp_year % 100)

private fun Context.copySensitive(label: String, value: String) {
    val clip = ClipData.newPlainText(label, value)
    if (Build.VERSION.SDK_INT >= 33) clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}

/** Show / copy / share virtual card number + CVC, behind fingerprint / face / screen lock. */
@Composable
fun CardDetailsSection(card: Card) {
    if (card.type != "virtual" || card.status == "canceled") return
    val ctx = LocalContext.current
    val c = ctx.container
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    var secrets by remember(card.id) { mutableStateOf<CardSecrets?>(null) }
    var loading by remember { mutableStateOf(false) }

    suspend fun fetch(): CardSecrets? {
        secrets?.let { return it }
        if (!ctx.authenticate("Card details", "•••• ${card.last4 ?: ""}")) return null
        loading = true
        return try { c.api.cardSecrets(card.id, c.http).also { secrets = it } }
        catch (e: Exception) { snack(e.message ?: "Couldn't load card details"); null } finally { loading = false }
    }

    SectionHeader("Card details")
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(enabled = !loading, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes(), onClick = {
            if (secrets != null) secrets = null else scope.launch { fetch() }
        }) {
            if (loading) LoadingIndicator(Modifier.padding(end = 8.dp))
            Icon(if (secrets != null) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
            Text(if (secrets != null) "  Hide" else "  Show")
        }
        FilledTonalButton(enabled = !loading, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes(), onClick = {
            scope.launch {
                val s = fetch() ?: return@launch
                val text = "Card: ${s.pretty()}\nExpires: ${s.exp()}\nCVC: ${s.cvc}" + (card.user?.name?.let { "\nName: $it" } ?: "")
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share card details"))
            }
        }) { Icon(Icons.Outlined.Share, null); Text("  Share") }
    }
    val s = secrets ?: return
    val rows = listOf("Card number" to s.pretty(), "Expires" to s.exp(), "CVC" to s.cvc)
    Column(Modifier.padding(top = 8.dp)) {
        rows.forEachIndexed { i, (k, v) ->
            SegmentedItem(i, rows.size, { ctx.copySensitive(k, v.replace(" ", "")); snack("$k copied") }) {
                ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    overlineContent = { Text(k) }, headlineContent = { Text(v, fontFamily = FontFamily.Monospace) },
                    trailingContent = { IconButton({ ctx.copySensitive(k, v.replace(" ", "")); snack("$k copied") }) { Icon(Icons.Outlined.ContentCopy, "Copy") } })
            }
        }
    }
}
