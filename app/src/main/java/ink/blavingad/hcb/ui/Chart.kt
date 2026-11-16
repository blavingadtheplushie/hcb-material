package ink.blavingad.hcb.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.data.BalancePoint

/** Smooth area chart meant to sit half-faded behind a card's content. Draws in from the left once. */
@Composable
fun AreaChart(values: List<Double>, color: Color, modifier: Modifier = Modifier, fill: Float = .22f, line: Float = .55f) {
    if (values.size < 2) return
    val reveal = remember(values.size) { Animatable(0f) }
    LaunchedEffect(values.size) { reveal.animateTo(1f, tween(900)) }
    Canvas(modifier) {
        val min = values.min(); val max = values.max()
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val top = size.height * .12f
        val h = size.height - top
        val pts = values.mapIndexed { i, v ->
            Offset(size.width * i / (values.size - 1), top + h * (1f - ((v - min) / span).toFloat()))
        }
        val p = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val a = pts[i - 1]; val b = pts[i]; val mx = (a.x + b.x) / 2
                cubicTo(mx, a.y, mx, b.y, b.x, b.y)
            }
        }
        val area = Path().apply { addPath(p); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        clipRect(right = size.width * reveal.value) {
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = fill), Color.Transparent)))
            drawPath(p, color.copy(alpha = line), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Rounded bars (e.g. daily spending), faded, bottom-aligned. */
@Composable
fun BarsChart(values: List<Double>, color: Color, modifier: Modifier = Modifier, alpha: Float = .28f) {
    if (values.isEmpty() || values.all { it <= 0 }) return
    val grow = remember(values.size) { Animatable(0f) }
    LaunchedEffect(values.size) { grow.animateTo(1f, tween(700)) }
    Canvas(modifier) {
        val max = values.max().takeIf { it > 0 } ?: 1.0
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val bh = (size.height * .85f * (v / max).toFloat() * grow.value).coerceAtLeast(if (v > 0) 3.dp.toPx() else 1.dp.toPx())
            drawRoundRect(color.copy(alpha = if (v > 0) alpha else alpha / 3), Offset(i * (w + gap), size.height - bh), Size(w, bh), CornerRadius(w / 2, w / 2))
        }
    }
}

/** Downsample a balance series to ~n points (keeps last). */
fun List<BalancePoint>.series(n: Int = 60): List<Double> {
    if (isEmpty()) return emptyList()
    if (size <= n) return map { it.amount.toDouble() }
    val step = size.toDouble() / n
    return (0 until n).map { this[(it * step).toInt()].amount.toDouble() } + last().amount.toDouble()
}

/** Sum several orgs' balance series on a common daily axis (carrying each org's last value forward). */
fun sumSeries(all: List<List<BalancePoint>>, days: Int = 90): List<Double> {
    if (all.isEmpty()) return emptyList()
    val today = java.time.LocalDate.now()
    val axis = (days downTo 0).map { today.minusDays(it.toLong()) }
    val totals = DoubleArray(axis.size)
    all.forEach { s ->
        val sorted = s.mapNotNull { p -> runCatching { java.time.LocalDate.parse(p.date) to p.amount }.getOrNull() }.sortedBy { it.first }
        var j = 0; var cur = 0L
        axis.forEachIndexed { i, d ->
            while (j < sorted.size && !sorted[j].first.isAfter(d)) { cur = sorted[j].second; j++ }
            totals[i] += cur.toDouble()
        }
    }
    return totals.toList()
}
