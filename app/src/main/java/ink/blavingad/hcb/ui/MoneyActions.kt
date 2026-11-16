package ink.blavingad.hcb.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.achTransfer
import ink.blavingad.hcb.data.parseCents
import ink.blavingad.hcb.data.sendCheck
import ink.blavingad.hcb.data.sendWire
import ink.blavingad.hcb.data.transfer

private enum class Sheet { Transfer, Ach, Check, Wire, Deposit, Invoice }

private fun amountOk(m: Map<String, String>) = parseCents(m["amount"] ?: "") != null
private fun cents(m: Map<String, String>) = parseCents(m["amount"] ?: "") ?: error("Invalid amount")

/** Row of money actions under the balance hero + their sheets. */
@Composable
fun MoneyActions(orgId: String, orgName: String, onMoved: () -> Unit) {
    val c = LocalContext.current.container
    val nav = LocalNav.current
    var open by rememberSaveable { mutableStateOf<Sheet?>(null) }
    val items = listOf(
        Triple("Transfer", Icons.Outlined.SwapHoriz, Sheet.Transfer),
        Triple("ACH", Icons.Outlined.AccountBalance, Sheet.Ach),
        Triple("Check", Icons.Outlined.Mail, Sheet.Check),
        Triple("Wire", Icons.Outlined.Public, Sheet.Wire),
        Triple("Deposit", Icons.Outlined.PhotoCamera, Sheet.Deposit),
        Triple("Invoice", Icons.Outlined.RequestQuote, Sheet.Invoice),
    )
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (l, i, s) -> Chip(l, i) { open = s } }
        Chip("History", Icons.Outlined.History) { nav.navigate("money/$orgId") }
    }
    val close = { open = null }
    val sub = "from $orgName"
    val note = "Over \$500 has to be sent from the HCB website (sudo mode)."
    when (open) {
        Sheet.Transfer -> FormSheet("Transfer to an org", sub, listOf(
            Field("to", "Destination org (slug or ID)"), AMOUNT, Field("name", "What it's for"),
        ), "Send transfer", close, valid = ::amountOk) { m ->
            c.api.transfer(orgId, m["to"]!!.trim().substringAfterLast('/'), cents(m), m["name"]!!); onMoved(); "Transfer sent"
        }
        Sheet.Ach -> FormSheet("ACH transfer", "$sub · US bank accounts. $note", listOf(
            Field("recipient_name", "Recipient name"), Field("recipient_email", "Recipient email", type = KeyboardType.Email),
            Field("bank_name", "Bank name"), Field("routing_number", "Routing number", type = KeyboardType.Number),
            Field("account_number", "Account number", type = KeyboardType.Number), AMOUNT, Field("payment_for", "What it's for"),
        ), "Send ACH", close, valid = ::amountOk) { m ->
            c.api.achTransfer(orgId, m - "amount", cents(m)); onMoved(); "ACH transfer requested"
        }
        Sheet.Check -> FormSheet("Mail a check", "$sub · US addresses. $note", listOf(
            Field("recipient_name", "Recipient name"), Field("recipient_email", "Recipient email", type = KeyboardType.Email),
            Field("address_line1", "Address"), Field("address_line2", "Apt, suite", optional = true),
            Field("address_city", "City"), Field("address_state", "State (e.g. VT)"), Field("address_zip", "ZIP", type = KeyboardType.Number),
            AMOUNT, Field("memo", "Check memo"), Field("payment_for", "What it's for"),
        ), "Send check", close, valid = ::amountOk) { m ->
            c.api.sendCheck(orgId, m - "amount", cents(m)); onMoved(); "Check on its way"
        }
        Sheet.Wire -> FormSheet("International wire", "$sub · amount in the chosen currency. $note", listOf(
            Field("recipient_name", "Recipient name"), Field("recipient_email", "Recipient email", type = KeyboardType.Email),
            Field("currency", "Currency (EUR, GBP…)"), Field("account_number", "IBAN / account number"), Field("bic_code", "BIC / SWIFT"),
            Field("recipient_country", "Country code (FR, GB…)"), Field("address_line1", "Address"), Field("address_line2", "Address line 2", optional = true),
            Field("address_city", "City"), Field("address_state", "Region", optional = true), Field("address_postal_code", "Postal code"),
            Field("amount", "Amount"), Field("memo", "Memo"), Field("payment_for", "What it's for"),
        ), "Send wire", close, valid = ::amountOk) { m ->
            c.api.sendWire(orgId, (m - "amount").mapValues { (k, v) -> if (k == "currency" || k == "recipient_country") v.trim().uppercase() else v }, cents(m)); onMoved(); "Wire requested"
        }
        Sheet.Deposit -> CheckDepositSheet(orgId, orgName, close, onMoved)
        Sheet.Invoice -> InvoiceSheet(orgId, orgName, close)
        null -> {}
    }
}

@Composable
private fun Chip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), border = null)
}
