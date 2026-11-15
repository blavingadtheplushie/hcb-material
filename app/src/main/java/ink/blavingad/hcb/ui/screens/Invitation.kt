package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.container
import ink.blavingad.hcb.ui.Avatar
import ink.blavingad.hcb.ui.CenterLoading
import ink.blavingad.hcb.ui.ErrorState
import ink.blavingad.hcb.ui.LocalNav
import ink.blavingad.hcb.ui.LocalSnack
import ink.blavingad.hcb.ui.rememberLoadable
import kotlinx.coroutines.launch

@Composable
fun InvitationScreen(id: String) {
    val c = LocalContext.current.container
    val nav = LocalNav.current
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val inv = rememberLoadable(id) { c.api.invitation(id) }
    var busy by remember { mutableStateOf(false) }
    fun respond(accept: Boolean) {
        busy = true
        scope.launch {
            runCatching { c.api.respondInvitation(id, accept) }
                .onSuccess { snack(if (accept) "Welcome aboard" else "Invitation declined"); nav.popBackStack() }
                .onFailure { snack(it.message ?: "Failed"); busy = false }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Invitation") }, navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { pad ->
        val i = inv.data
        if (i == null) { if (inv.error != null) ErrorState(inv.error!!, { scope.launch { inv.load() } }, Modifier.padding(pad)) else CenterLoading(Modifier.padding(pad)); return@Scaffold }
        Column(Modifier.fillMaxSize().padding(pad).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Avatar(i.organization?.icon, i.organization?.name ?: "?", size = 96.dp, square = true)
            Spacer(Modifier.height(16.dp))
            Text(i.organization?.name ?: "An organization", style = MaterialTheme.typography.headlineMediumEmphasized, textAlign = TextAlign.Center)
            Text(listOfNotNull(i.sender?.name?.let { "${it} invited you" }, i.role?.let { "as $it" }).joinToString(" ").ifBlank { "You've been invited" },
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { respond(false) }, modifier = Modifier.weight(1f).height(56.dp), enabled = !busy, shapes = ButtonDefaults.shapes()) { Text("Decline") }
                Button(onClick = { respond(true) }, modifier = Modifier.weight(1f).height(56.dp), enabled = !busy, shapes = ButtonDefaults.shapes()) { Text("Accept") }
            }
        }
    }
}
