package ink.blavingad.hcb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import ink.blavingad.hcb.container

/** HCB roles (OrganizerPosition::HasRole): reader < member < manager. */
val ROLES = listOf("reader" to "Reader", "member" to "Member", "manager" to "Manager")

private val roleHelp = mapOf(
    "reader" to "Can see everything, can't spend or move money.",
    "member" to "Can get cards, upload receipts and spend.",
    "manager" to "Full access: money moves, cards, inviting people.",
)

/** Invite someone to an org by email (POST /organizations/:id/invitations, needs manager). */
@Composable
fun InviteSheet(orgId: String, orgName: String, onDismiss: () -> Unit, onInvited: () -> Unit) {
    val c = LocalContext.current.container
    FormSheet(
        title = "Invite to $orgName",
        subtitle = "They'll get an email from HCB to join the team.",
        fields = listOf(Field("email", "Email", type = KeyboardType.Email)),
        submit = "Send invite",
        onDismiss = onDismiss,
        valid = { (it["email"] ?: "").contains('@') },
        extra = { v ->
            LaunchedEffect(Unit) { if (v["role"] == null) v["role"] = "member" }
            val role = v["role"] ?: "member"
            Column(verticalArrangement = Arrangement.spacedBy(6.dp_())) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    ROLES.forEachIndexed { i, (k, l) ->
                        ToggleButton(checked = role == k, onCheckedChange = { v["role"] = k }, modifier = Modifier.weight(1f),
                            shapes = when (i) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                ROLES.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }) { Text(l) }
                    }
                }
                Text(roleHelp[role] ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    ) { v ->
        c.api.invite(orgId, v["email"]!!.trim(), v["role"] ?: "member")
        onInvited()
        "Invite sent to ${v["email"]!!.trim()}"
    }
}

private fun Int.dp_() = androidx.compose.ui.unit.Dp(this.toFloat())
