package ink.blavingad.hcb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import ink.blavingad.hcb.data.CardLocking

/**
 * Standing, non-dismissible "your cards are locked" notice (mirrors hcb-mobile#423).
 * HCB locks a cardholder's cards when a receipt is ~7 days overdue; uploading it unlocks them in seconds.
 * Renders nothing when the API doesn't send card_locking (feature off) or locked is false.
 */
@Composable
fun CardLockBanner(lock: CardLocking?, modifier: Modifier = Modifier) {
    if (lock?.locked != true) return
    val nav = LocalNav.current
    val n = lock.overdue_receipt_count
    Surface(
        onClick = {
            nav.navigateTab("receipts")
        },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.Lock, null, Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your cards are locked", style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    (if (n > 0) "$n overdue receipt${if (n == 1) "" else "s"}. " else "You have overdue receipts. ") +
                        "Upload ${if (n == 1) "it" else "them"} to unlock your cards in seconds. Recurring charges keep failing until you do.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Upload receipts")
        }
    }
}
