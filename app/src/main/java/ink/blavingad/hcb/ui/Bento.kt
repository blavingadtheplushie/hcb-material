package ink.blavingad.hcb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import ink.blavingad.hcb.data.Transaction

@Composable
private fun Tile(
    modifier: Modifier, height: Dp, bg: Color, fg: Color, shape: RoundedPolygon, icon: ImageVector,
    label: String, value: String, sub: String? = null, big: Boolean = false, onClick: (() -> Unit)? = null,
    chart: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Box(Modifier.height(height).clip(RoundedCornerShape(28.dp)).background(bg)) {
            if (chart == null) BgShape(shape, fg.copy(alpha = .08f), Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp).size(if (big) 220.dp else 140.dp))
            chart?.invoke(this)
            Column(Modifier.padding(18.dp).fillMaxWidth(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(18.dp), tint = fg)
                    Text("  $label", color = fg.copy(alpha = .85f), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                Box(Modifier.weight(1f))
                Text(value, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = if (big) MaterialTheme.typography.displaySmallEmphasized else MaterialTheme.typography.headlineSmallEmphasized)
                sub?.let { Text(it, color = fg.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
    if (onClick != null) Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(28.dp), color = Color.Transparent) { content() }
    else Box(modifier) { content() }
}

@Composable
fun BentoGrid(
    total: Long?, orgCount: Int, missing: Int, activeCards: Int?, spentMonth: Long?, last: Transaction?, hide: Boolean,
    balanceSeries: List<Double> = emptyList(), dailySpend: List<Double> = emptyList(),
    onReceipts: () -> Unit, onCards: () -> Unit, onLast: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    fun m(v: Long?) = if (hide) "••••" else if (v == null) "…" else money(v)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(Modifier.fillMaxWidth(), 150.dp, cs.primaryContainer, cs.onPrimaryContainer, MaterialShapes.Cookie12Sided,
            Icons.Outlined.AccountBalanceWallet, "Total balance", m(total), if (orgCount == 1) "across 1 organization" else "across $orgCount organizations", big = true,
            chart = if (balanceSeries.size > 1 && !hide) ({
                AreaChart(balanceSeries, cs.onPrimaryContainer, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(110.dp))
            }) else null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(Modifier.weight(1f), 120.dp, if (missing > 0) cs.errorContainer else cs.surfaceContainerHigh, if (missing > 0) cs.onErrorContainer else cs.onSurface,
                MaterialShapes.Sunny, Icons.Outlined.ReceiptLong, "Receipts", if (missing == 0) "All done" else "$missing missing", onClick = onReceipts)
            Tile(Modifier.weight(1f), 120.dp, cs.secondaryContainer, cs.onSecondaryContainer, MaterialShapes.Pill,
                Icons.Outlined.CreditCard, "Cards", activeCards?.let { "$it active" } ?: "…", onClick = onCards)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(Modifier.weight(1f), 120.dp, cs.tertiaryContainer, cs.onTertiaryContainer, MaterialShapes.Flower,
                Icons.Outlined.TrendingDown, "Spent this month", m(spentMonth),
                chart = if (dailySpend.any { it > 0 } && !hide) ({
                    BarsChart(dailySpend, cs.onTertiaryContainer, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(56.dp).padding(horizontal = 14.dp, vertical = 10.dp))
                }) else null)
            Tile(Modifier.weight(1f), 120.dp, cs.surfaceContainerHighest, cs.onSurface, MaterialShapes.SoftBurst,
                Icons.Outlined.CreditCard, "Latest", if (last == null) "…" else if (hide) "••••" else money(last.amount_cents, signed = true),
                last?.title(), onClick = if (last != null) onLast else null)
        }
    }
}
