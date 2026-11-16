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
import androidx.glance.layout.Column
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
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 48.dp), DpSize(180.dp, 110.dp)))

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
        val big = LocalSize.current.height >= 100.dp
        Column(
            GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(GlanceTheme.colors.widgetBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp).clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_hcb_logo), null, GlanceModifier.size(16.dp), colorFilter = ColorFilter.tint(GlanceTheme.colors.primary))
                Spacer(GlanceModifier.width(6.dp))
                Text("HCB", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            }
            Spacer(GlanceModifier.height(2.dp))
            Text(
                when { !s.signedIn -> "Sign in"; s.hide -> "••••"; s.total == null -> "…"; else -> money(s.total) },
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = if (big) 28.sp else 22.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            s.missing?.takeIf { it > 0 }?.let { n ->
                Text("$n missing receipt${if (n == 1) "" else "s"}", style = TextStyle(color = GlanceTheme.colors.error, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            }
            if (big && !s.hide) {
                Spacer(GlanceModifier.height(8.dp))
                s.orgs.take(3).forEach { (name, cents) ->
                    Row(GlanceModifier.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, GlanceModifier.defaultWeight(), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp), maxLines = 1)
                        Text(money(cents), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                    }
                }
            }
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
