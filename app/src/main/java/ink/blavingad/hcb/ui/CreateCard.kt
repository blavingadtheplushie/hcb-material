package ink.blavingad.hcb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.Card
import ink.blavingad.hcb.data.createCard
import kotlinx.coroutines.launch

@Composable
fun CreateCardSheet(orgId: String, orgName: String, onDismiss: () -> Unit, onCreated: (Card) -> Unit) {
    val c = LocalContext.current.container
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var physical by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val ship = remember { mutableStateMapOf<String, String>() }
    val fields = listOf(
        "shipping_name" to "Name on the package", "shipping_address_line1" to "Address", "shipping_address_line2" to "Apt, suite (optional)",
        "shipping_address_city" to "City", "shipping_address_state" to "State", "shipping_address_postal_code" to "ZIP code",
    )
    val ok = !physical || fields.filter { it.first != "shipping_address_line2" }.all { !ship[it.first].isNullOrBlank() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New card", style = MaterialTheme.typography.headlineSmallEmphasized)
            Text("for $orgName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val opts = listOf(false to "Virtual", true to "Physical")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                opts.forEachIndexed { i, (v, l) ->
                    ToggleButton(checked = physical == v, onCheckedChange = { physical = v }, modifier = Modifier.weight(1f),
                        shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes()) {
                        Icon(if (v) Icons.Outlined.LocalShipping else Icons.Outlined.CreditCard, null); Text("  $l")
                    }
                }
            }
            Text(if (physical) "Shipped by HCB, US addresses only. You need a birthday and a verified phone number on your HCB account."
                 else "Ready instantly. You need a birthday and a verified phone number on your HCB account.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (physical) fields.forEach { (k, l) ->
                OutlinedTextField(ship[k] ?: "", { ship[k] = it }, Modifier.fillMaxWidth(), label = { Text(l) }, singleLine = true)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(enabled = ok && !busy, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shapes = ButtonDefaults.shapes(), onClick = {
                busy = true; error = null
                scope.launch {
                    runCatching {
                        c.api.createCard(orgId, if (physical) "physical" else "virtual",
                            if (physical) ship.toMap() + ("shipping_address_country" to "US") else emptyMap())
                    }.onSuccess { k -> snack("Card created"); sheet.hide(); onCreated(k) }
                     .onFailure { error = it.message ?: "Couldn't create the card" }
                    busy = false
                }
            }) {
                if (busy) LoadingIndicator(Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.onPrimary)
                Text(if (physical) "Order card" else "Create card")
            }
        }
    }
}
