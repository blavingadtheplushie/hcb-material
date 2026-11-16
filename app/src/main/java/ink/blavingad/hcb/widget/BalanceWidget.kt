package ink.blavingad.hcb.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ColorFilter
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ink.blavingad.hcb.MainActivity
import ink.blavingad.hcb.R
import ink.blavingad.hcb.container
import ink.blavingad.hcb.ui.money
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Home-screen widget (hcb-mobile#123): total balance across orgs + missing receipts. Material You via GlanceTheme. */
class BalanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 48.dp), DpSize(180.dp, 110.dp), DpSize(180.dp, 180.dp)))

    private data class Snapshot(val signedIn: Boolean, val total: Long?, val missing: Int?, val orgs: List<Pair<String, Long?>>, val hide: Boolean)

    private suspend fun load(ctx: Context): Snapshot {
        val c = ctx.applicationContext.container
        val hide = c.prefs.hideBalances.value
        if (c.auth.tokens.value == null) return Snapshot(false, null, null, emptyList(), hide)
        return runCatching {
            coroutineScope {
                val orgs = c.api.organizations()
                val missing = async { runCatching { c.api.missingReceiptCount() }.getOrNull() }
                val bal = orgs.map { o -> async { o.name to runCatching { c.api.orgBalance(o.id).balance_cents }.getOrNull() } }.awaitAll()
                Snapshot(true, bal.sumOf { it.second ?: 0L }, missing.await(), bal.sortedByDescending { it.second ?: 0L }, hide)
            }
        }.getOrElse { Snapshot(true, null, null, emptyList(), hide) }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = load(context)
        provideContent { GlanceTheme { Content(s) } }
    }

    @Composable
    private fun Content(s: Snapshot) {
        val size = LocalSize.current
        val big = size.height >= 100.dp
        val c = GlanceTheme.colors
        val amount = when { !s.signedIn -> "Sign in"; s.hide -> "••••"; s.total == null -> "…"; else -> money(s.total) }
        Box(
            GlanceModifier.fillMaxSize().cornerRadius(28.dp).background(c.widgetBackground).clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.BottomEnd,
        ) {
            // decorative expressive shapes, anchored bottom-right (clipped by the rounded widget)
            Image(ImageProvider(R.drawable.widget_shape_clover), null,
                GlanceModifier.size(if (big) 150.dp else 90.dp).padding(start = 30.dp, top = 30.dp),
                colorFilter = ColorFilter.tint(c.secondaryContainer))
            Column(GlanceModifier.fillMaxSize().padding(14.dp), verticalAlignment = if (big) Alignment.Top else Alignment.CenterVertically) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(GlanceModifier.size(if (big) 36.dp else 30.dp), contentAlignment = Alignment.Center) {
                        Image(ImageProvider(R.drawable.widget_shape_cookie), null, GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(c.primary))
                        Image(ImageProvider(R.drawable.ic_hcb_logo), null, GlanceModifier.size(if (big) 18.dp else 15.dp), colorFilter = ColorFilter.tint(c.onPrimary))
                    }
                    Spacer(GlanceModifier.width(10.dp))
                    if (big) {
                        Text("HCB", GlanceModifier.defaultWeight(), style = TextStyle(color = c.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                        s.missing?.takeIf { it > 0 }?.let { n -> MissingChip(n) }
                    } else {
                        Column(GlanceModifier.defaultWeight()) {
                            Text(amount, style = TextStyle(color = c.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                            s.missing?.takeIf { it > 0 }?.let { n ->
                                Text("$n missing receipt${if (n == 1) "" else "s"}", style = TextStyle(color = c.error, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                            }
                        }
                    }
                }
                if (big) {
                    Spacer(GlanceModifier.height(6.dp))
                    Text(amount, style = TextStyle(color = c.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    Spacer(GlanceModifier.defaultWeight())
                    if (!s.hide) {
                        val rows = s.orgs.take(if (size.height >= 170.dp) 3 else 1)
                        rows.forEachIndexed { i, (name, cents) ->
                            if (i > 0) Spacer(GlanceModifier.height(4.dp))
                            Row(
                                GlanceModifier.fillMaxWidth().cornerRadius(16.dp).background(c.surfaceVariant).padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(name, GlanceModifier.defaultWeight(), style = TextStyle(color = c.onSurfaceVariant, fontSize = 13.sp), maxLines = 1)
                                Spacer(GlanceModifier.width(8.dp))
                                Text(money(cents), style = TextStyle(color = c.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MissingChip(n: Int) {
        Row(
            GlanceModifier.cornerRadius(12.dp).background(GlanceTheme.colors.errorContainer).padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$n receipt${if (n == 1) "" else "s"}", style = TextStyle(color = GlanceTheme.colors.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }

    companion object {
        suspend fun refresh(ctx: Context) { runCatching { BalanceWidget().updateAll(ctx) } }
        suspend fun installed(ctx: Context) = runCatching { GlanceAppWidgetManager(ctx).getGlanceIds(BalanceWidget::class.java).isNotEmpty() }.getOrDefault(false)
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}
