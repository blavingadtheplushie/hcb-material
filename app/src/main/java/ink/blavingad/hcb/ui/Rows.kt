package ink.blavingad.hcb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.data.Card
import ink.blavingad.hcb.data.Transaction

fun Transaction.icon() = when {
    card_charge != null -> Icons.Outlined.CreditCard
    donation != null -> Icons.Outlined.Favorite
    transfer != null -> Icons.Outlined.SwapHoriz
    check != null || invoice != null -> Icons.Outlined.Description
    else -> Icons.Outlined.AccountBalance
}

@Composable
fun TxRow(t: Transaction, index: Int, count: Int, showOrg: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    SegmentedItem(index, count, onClick) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Box(Modifier.size(40.dp).clip(MaterialShapes.Cookie6Sided.toShape()).background(if (t.amount_cents >= 0) cs.tertiaryContainer else cs.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(t.icon(), null, Modifier.size(20.dp), tint = if (t.amount_cents >= 0) cs.onTertiaryContainer else cs.onSecondaryContainer)
                }
            },
            headlineContent = { Text(t.title(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                val bits = buildList {
                    if (showOrg) t.organization?.name?.let(::add)
                    if (t.pending) add("Pending")
                    if (t.declined) add("Declined")
                    if (t.reversed) add("Reversed")
                    if (isEmpty()) add(t.kind())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (t.missing_receipt) {
                        Icon(Icons.Outlined.ReceiptLong, "Missing receipt", Modifier.size(14.dp), tint = cs.error)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(bits.joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            trailingContent = {
                Text(money(t.amount_cents, signed = true),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = when { t.declined -> cs.onSurfaceVariant; t.amount_cents > 0 -> cs.tertiary; else -> cs.onSurface })
            },
        )
    }
}

/** Card art: a gradient in the phone's palette, frozen cards in cool grey. */
@Composable
fun CardArt(card: Card, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val frozen = card.status == "frozen"
    val inactive = card.status != "active" && !frozen
    val brush = when {
        frozen -> Brush.linearGradient(listOf(cs.surfaceContainerHighest, cs.secondaryContainer))
        inactive -> Brush.linearGradient(listOf(cs.surfaceContainerHigh, cs.surfaceContainerHighest))
        card.type == "physical" -> Brush.linearGradient(listOf(cs.primary, cs.tertiary))
        else -> Brush.linearGradient(listOf(cs.primaryContainer, cs.tertiaryContainer))
    }
    val fg = when { frozen || inactive -> cs.onSurfaceVariant; card.type == "physical" -> cs.onPrimary; else -> cs.onPrimaryContainer }
    Box(modifier.fillMaxWidth().aspectRatio(1.586f).clip(RoundedCornerShape(24.dp)).background(brush).padding(20.dp)) {
        Column(Modifier.align(Alignment.TopStart)) {
            Text(card.organization?.name ?: "HCB", color = fg, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(card.name ?: (card.type.replaceFirstChar { it.uppercase() } + " card"), color = fg.copy(alpha = .8f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        if (frozen) Icon(Icons.Outlined.AcUnit, "Frozen", Modifier.align(Alignment.TopEnd), tint = fg)
        Column(Modifier.align(Alignment.BottomStart)) {
            Text("•••• " + (card.last4 ?: "····"), color = fg, style = MaterialTheme.typography.headlineSmallEmphasized, fontFamily = FontFamily.Monospace)
            if (card.exp_month != null && card.exp_year != null)
                Text("%02d/%02d".format(card.exp_month, card.exp_year % 100), color = fg.copy(alpha = .8f), style = MaterialTheme.typography.labelLarge)
        }
        Text(card.status.replaceFirstChar { it.uppercase() }, color = fg, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
fun MissingBanner(count: Int, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(onClick = onClick, color = cs.errorContainer, contentColor = cs.onErrorContainer, shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Warning, null)
            Column(Modifier.weight(1f)) {
                Text(if (count == 1) "1 receipt missing" else "$count receipts missing", style = MaterialTheme.typography.titleMediumEmphasized)
                Text("Upload them so your org stays happy", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun Spacer16() = Spacer(Modifier.height(16.dp))
