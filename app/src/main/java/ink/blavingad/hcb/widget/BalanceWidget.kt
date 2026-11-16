package ink.blavingad.hcb.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import ink.blavingad.hcb.data.Transaction
import ink.blavingad.hcb.data.balanceByDate
import ink.blavingad.hcb.ui.downsample
import ink.blavingad.hcb.ui.sumSeries
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.glance.layout.ContentScale
import ink.blavingad.hcb.ui.money
import ink.blavingad.hcb.ui.parseDate
import ink.blavingad.hcb.ui.title
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Home-screen widget (hcb-mobile#123): total balance across orgs + missing receipts. Material You via GlanceTheme. */
class BalanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 48.dp), DpSize(180.dp, 110.dp), DpSize(180.dp, 180.dp), DpSize(180.dp, 250.dp)))

    private data class Snapshot(val signedIn: Boolean, val total: Long?, val missing: Int?, val txs: List<Transaction>, val hide: Boolean, val series: List<Double> = emptyList())

    private suspend fun load(ctx: Context): Snapshot {
        val c = ctx.applicationContext.container
        val hide = c.prefs.hideBalances.value
        if (c.auth.tokens.value == null) return Snapshot(false, null, null, emptyList(), hide)
        return runCatching {
            coroutineScope {
                val orgs = c.api.organizations()
                val missing = async { runCatching { c.api.missingReceiptCount() }.getOrNull() }
                val bal = orgs.map { o -> async { o.name to runCatching { c.api.orgBalance(o.id).balance_cents }.getOrNull() } }.awaitAll()
                // latest transactions across every org, newest first
                val txs = orgs.map { o -> async { runCatching { c.api.orgTransactions(o.id, null, null, null).data.take(6).map { t -> if (t.organization == null) t.copy(organization = o) else t } }.getOrDefault(emptyList()) } }
                    .awaitAll().flatten().filter { !it.declined }.sortedByDescending { it.date ?: "" }.take(6)
                val series = if (hide) emptyList() else runCatching {
                    sumSeries(orgs.map { o -> async { runCatching { c.api.balanceByDate(o.id) }.getOrDefault(emptyList()) } }.awaitAll()).downsample(60)
                }.getOrDefault(emptyList())
                Snapshot(true, bal.sumOf { it.second ?: 0L }, missing.await(), txs, hide, series)
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
            val chart = remember(s.series, big) { if (s.series.size >= 2 && !s.hide) chartBitmap(s.series, if (big) 600 else 400, if (big) 220 else 120) else null }
            if (chart != null) {
                // continuous balance history, white alpha mask tinted with primary; faded on the left so the amount stays readable
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                    Image(ImageProvider(chart), null,
                        GlanceModifier.fillMaxWidth().height(if (big) 110.dp else size.height).padding(top = if (big) 18.dp else 0.dp),
                        contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.tint(c.primary))
                }
            } else {
                // no chart (hidden balances / no data): expressive shape in the empty top-right corner, away from the rows
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                    Image(ImageProvider(R.drawable.widget_shape_clover), null,
                        GlanceModifier.size(if (big) 120.dp else 80.dp).padding(start = 24.dp, bottom = 24.dp),
                        colorFilter = ColorFilter.tint(c.secondaryContainer))
                }
            }
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
                    val rows = s.txs.take(when { size.height >= 240.dp -> 5; size.height >= 170.dp -> 3; else -> 1 })
                    if (rows.isEmpty() && s.signedIn) {
                        Text("No transactions yet", style = TextStyle(color = c.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
                    }
                    rows.forEachIndexed { i, t ->
                        if (i > 0) Spacer(GlanceModifier.height(4.dp))
                        TxRow(t, s.hide)
                    }
                }
            }
        }
    }

    @Composable
    private fun TxRow(t: Transaction, hide: Boolean) {
        val c = GlanceTheme.colors
        val ctx = LocalContext.current
        val open = Intent(Intent.ACTION_VIEW, Uri.parse("https://hcb.hackclub.com/hcb/${t.id.removePrefix("txn_")}"), ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val income = t.amount_cents > 0
        Row(
            GlanceModifier.fillMaxWidth().cornerRadius(16.dp).background(c.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp)
                .clickable(androidx.glance.appwidget.action.actionStartActivity(open)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight()) {
                Text(t.title(), style = TextStyle(color = c.onSecondaryContainer, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                val sub = listOfNotNull(shortDay(t.date), t.organization?.name, if (t.pending) "pending" else null).joinToString(" · ")
                if (sub.isNotEmpty()) Text(sub, style = TextStyle(color = c.onSecondaryContainer, fontSize = 11.sp), maxLines = 1)
            }
            Spacer(GlanceModifier.width(8.dp))
            Text(if (hide) "••••" else money(t.amount_cents, signed = true),
                style = TextStyle(color = if (income) c.primary else c.onSecondaryContainer, fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }

    private fun shortDay(s: String?): String? {
        val d = parseDate(s) ?: return null
        val today = LocalDate.now()
        return when (d) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> d.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
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

    /** Smooth area chart as an alpha mask (white), faded in from 40% width. Tinted by Glance. */
    private fun chartBitmap(v: List<Double>, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val min = v.min(); val max = v.max(); val span = (max - min).takeIf { it > 0 } ?: 1.0
        val top = h * .12f; val usable = h * .80f
        val pts = v.mapIndexed { i, x -> (i * w / (v.size - 1f)) to (top + usable * (1 - ((x - min) / span).toFloat())) }
        val line = Path().apply {
            moveTo(pts[0].first, pts[0].second)
            for (i in 1 until pts.size) {
                val (x0, y0) = pts[i - 1]; val (x1, y1) = pts[i]; val mx = (x0 + x1) / 2
                cubicTo(mx, y0, mx, y1, x1, y1)
            }
        }
        val area = Path(line).apply { lineTo(w.toFloat(), h.toFloat()); lineTo(0f, h.toFloat()); close() }
        fun fade(a: Int) = LinearGradient(0f, 0f, w.toFloat(), 0f, intArrayOf(0x00FFFFFF, 0x00FFFFFF, (a shl 24) or 0xFFFFFF),
            floatArrayOf(0f, .4f, 1f), Shader.TileMode.CLAMP)
        cv.drawPath(area, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; shader = fade(0x30) })
        cv.drawPath(line, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = h / 70f + 1.5f; strokeCap = Paint.Cap.ROUND; shader = fade(0x80) })
        return bmp
    }

    companion object {
        suspend fun refresh(ctx: Context) { runCatching { BalanceWidget().updateAll(ctx) } }
        suspend fun installed(ctx: Context) = runCatching { GlanceAppWidgetManager(ctx).getGlanceIds(BalanceWidget::class.java).isNotEmpty() }.getOrDefault(false)
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}
