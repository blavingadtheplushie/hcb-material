package ink.blavingad.hcb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.data.Card
import ink.blavingad.hcb.container
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
                    MerchantIcon(t, t.icon(), if (t.amount_cents >= 0) cs.onTertiaryContainer else cs.onSecondaryContainer, 20.dp)
                }
            },
            headlineContent = { Text(t.listTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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

/** Card art. Flat colours only, no gradients.
 *  Virtual cards: style A "cookie" (big cropped shape, picked from the card id). Dark theme = dark card + dim shape.
 *  Physical cards: style C "minimal" (surface + primary outline + small flower). Frozen/inactive: grey, shape as outline. */
private val cookieShapes = listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Cookie12Sided, MaterialShapes.Cookie7Sided, MaterialShapes.Sunny,
    MaterialShapes.SoftBurst, MaterialShapes.Flower, MaterialShapes.Clover8Leaf, MaterialShapes.Cookie6Sided)

private fun Color.lum() = 0.2126f * red + 0.7152f * green + 0.0722f * blue

@Composable
fun CardArt(card: Card, modifier: Modifier = Modifier, grant: ink.blavingad.hcb.data.CardGrant? = null, showOwner: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val frozen = card.status == "frozen"
    val inactive = card.status != "active" && !frozen
    val dim = frozen || inactive
    val physical = card.type == "physical"
    val seed = (card.id.hashCode() and 0x7fffffff)
    // dark theme: keep cards dark (no big bright slab). Light theme: if primaryContainer is far from the surface
    // (monochrome / black & white themes make it near-black), use a light surface card with a tinted cookie instead.
    val dark = cs.surface.lum() < .5f
    val soft = dark || kotlin.math.abs(cs.primaryContainer.lum() - cs.surface.lum()) > .45f
    val bg = when { physical -> cs.surface; dim && dark -> cs.surfaceContainerLow; dim || soft -> cs.surfaceContainerHigh; else -> cs.primaryContainer }
    val fg = when { dim -> cs.onSurfaceVariant; physical || soft -> cs.onSurface; else -> cs.onPrimaryContainer }
    val shapeColor = if (soft) androidx.compose.ui.graphics.lerp(bg, cs.primary, if (dark) .28f else .16f) else cs.primary
    val dimLine = if (dark) cs.outlineVariant else cs.outline
    val cardShape = RoundedCornerShape(24.dp)
    Box(modifier.fillMaxWidth().aspectRatio(1.586f).clip(cardShape).background(bg)
        .then(if (physical) Modifier.border(2.dp, if (dim) cs.outline else cs.primary, cardShape) else Modifier)) {
        when {
            physical -> {
                val m = Modifier.align(Alignment.TopEnd).padding(18.dp).size(34.dp)
                if (dim) Box(m.border(2.dp, cs.outline, MaterialShapes.Flower.toShape())) else Box(m.clip(MaterialShapes.Flower.toShape()).background(cs.primary))
            }
            else -> {
                val sh = cookieShapes[seed % cookieShapes.size]
                val m = Modifier.align(Alignment.BottomEnd).offset(x = 64.dp, y = 76.dp).size(230.dp).rotate((seed / 8 % 60).toFloat())
                if (dim) Box(m.border(3.dp, dimLine, sh.toShape())) else Box(m.clip(sh.toShape()).background(shapeColor))
            }
        }
        Box(Modifier.fillMaxSize().padding(20.dp)) {
            Column(Modifier.align(Alignment.TopStart).padding(end = 72.dp)) {
                Text(card.organization?.name ?: "HCB", color = fg, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                card.name?.takeIf { it.isNotBlank() }?.let { Text(it, color = fg.copy(alpha = .8f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Row(Modifier.align(Alignment.TopEnd).padding(end = if (physical) 44.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (frozen) Surface(color = cs.surfaceContainerHighest, contentColor = cs.onSurface, shape = RoundedCornerShape(50)) { Icon(Icons.Outlined.AcUnit, "Frozen", Modifier.padding(4.dp).size(18.dp)) }
                if (grant != null) Surface(color = cs.tertiaryContainer, contentColor = cs.onTertiaryContainer, shape = RoundedCornerShape(50)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CardGiftcard, null, Modifier.size(16.dp))
                        Text(" Grant" + (grant.balance_cents?.let { " · " + money(it) } ?: ""), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (physical) Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(width = 40.dp, height = 30.dp).clip(RoundedCornerShape(6.dp)).background(if (dim) cs.outlineVariant else cs.primary.copy(alpha = .85f)))
                Icon(Icons.Outlined.Contactless, "Physical card", tint = fg.copy(alpha = .7f))
            }
            Column(Modifier.align(Alignment.BottomStart)) {
                Text("•••• " + (card.last4 ?: "····"), color = fg, style = MaterialTheme.typography.headlineSmallEmphasized, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showOwner) card.user?.name?.let { Text(it.uppercase(), color = fg.copy(alpha = .85f), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)) }
                    if (card.exp_month != null && card.exp_year != null)
                        Text("%02d/%02d".format(card.exp_month, card.exp_year % 100), color = fg.copy(alpha = .8f), style = MaterialTheme.typography.labelLarge)
                }
            }
            if (card.status != "active") Surface(color = cs.surfaceContainerHighest, contentColor = cs.onSurface, shape = RoundedCornerShape(50), modifier = Modifier.align(Alignment.BottomEnd)) {
                Text(card.status.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

/** Big decorative expressive shape for card backgrounds. */
@Composable
fun BgShape(shape: androidx.graphics.shapes.RoundedPolygon, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.rotate(18f).clip(shape.toShape()).background(color))
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
