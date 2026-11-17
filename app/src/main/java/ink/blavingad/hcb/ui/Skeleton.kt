package ink.blavingad.hcb.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

/*
 * Skeleton loaders, M3 Expressive flavour, flat colours only (no gradient sweep):
 * placeholders "breathe" (alpha pulse) in a cascade from top to bottom, and the leading
 * shapes slowly spin + bounce like the expressive LoadingIndicator.
 */

/** Alpha pulse; `index` staggers it so a list breathes as a wave. */
@Composable
private fun pulse(index: Int): Float {
    val t = rememberInfiniteTransition(label = "sk")
    val a by t.animateFloat(.45f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse,
        StartOffset((index % 8) * 110)), label = "a")
    return a
}

@Composable
fun SkeletonBlock(width: Dp?, height: Dp, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(50),
                  color: Color = MaterialTheme.colorScheme.surfaceContainerHighest, index: Int = 0) {
    val a = pulse(index)
    Box((if (width != null) modifier.width(width) else modifier.fillMaxWidth()).height(height)
        .graphicsLayer { alpha = a }.clip(shape).background(color))
}

/** A polygon placeholder that spins and bounces softly. */
@Composable
fun SkeletonShape(poly: RoundedPolygon, size: Dp, color: Color, modifier: Modifier = Modifier, index: Int = 0) {
    val t = rememberInfiniteTransition(label = "shape")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing), initialStartOffset = StartOffset(index * 400)), label = "r")
    val s by t.animateFloat(.88f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset((index % 8) * 110)), label = "s")
    val a = pulse(index)
    Box(modifier.size(size).graphicsLayer { rotationZ = rot; scaleX = s; scaleY = s; alpha = a }.clip(poly.toShape()).background(color))
}

private val rowShapes = listOf(MaterialShapes.Cookie6Sided, MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf, MaterialShapes.SoftBurst)

/** Inline value placeholder (amounts, counts) that sits where text will appear. */
@Composable
fun SkeletonText(width: Dp, height: Dp = 18.dp, color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f), index: Int = 0) =
    SkeletonBlock(width, height, Modifier.padding(vertical = 4.dp), color = color, index = index)

/** A transaction-like row inside a segmented group. */
@Composable
fun SkeletonRow(i: Int, count: Int, square: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val c = cs.onSurface.copy(alpha = .10f)
    SegmentedItem(i, count) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (square) SkeletonBlock(40.dp, 40.dp, shape = RoundedCornerShape(12.dp), color = cs.secondaryContainer, index = i)
            else SkeletonShape(rowShapes[i % rowShapes.size], 40.dp, cs.secondaryContainer, index = i)
            Column(Modifier.padding(start = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(listOf(150, 110, 170, 130, 95)[i % 5].dp, 14.dp, color = c, index = i)
                SkeletonBlock(listOf(80, 120, 70, 100, 90)[i % 5].dp, 10.dp, color = c, index = i)
            }
            SkeletonBlock(listOf(60, 48, 72, 54)[i % 4].dp, 16.dp, color = c, index = i)
        }
    }
}

fun LazyListScope.skeletonRows(count: Int = 6, square: Boolean = false, header: Boolean = true, key: String = "sk") {
    if (header) item(key = "${key}h") { SkeletonHeader() }
    items(count, key = { "$key$it" }) { SkeletonRow(it, count, square) }
}

@Composable
fun SkeletonHeader(index: Int = 0) {
    SkeletonBlock(90.dp, 12.dp, Modifier.padding(start = 32.dp, top = 24.dp, bottom = 12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .25f), index = index)
}

/** Big colourful tile placeholder with its decorative shape. */
@Composable
private fun SkeletonTile(modifier: Modifier, height: Dp, bg: Color, fg: Color, poly: RoundedPolygon, big: Boolean, index: Int) {
    Box(modifier.height(height).clip(RoundedCornerShape(28.dp)).background(bg)) {
        SkeletonShape(poly, if (big) 220.dp else 140.dp, fg.copy(alpha = .08f), Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp), index)
        Column(Modifier.padding(18.dp)) {
            SkeletonBlock(if (big) 110.dp else 70.dp, 12.dp, color = fg.copy(alpha = .18f), index = index)
            Spacer(Modifier.weight(1f))
            SkeletonBlock(if (big) 170.dp else 90.dp, if (big) 36.dp else 26.dp, color = fg.copy(alpha = .18f), index = index)
            if (big) { Spacer(Modifier.height(8.dp)); SkeletonBlock(130.dp, 10.dp, color = fg.copy(alpha = .14f), index = index) }
        }
    }
}

@Composable
fun SkeletonBento() {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkeletonTile(Modifier.fillMaxWidth(), 150.dp, cs.primaryContainer, cs.onPrimaryContainer, MaterialShapes.Cookie12Sided, true, 0)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonTile(Modifier.weight(1f), 120.dp, cs.surfaceContainerHigh, cs.onSurface, MaterialShapes.Sunny, false, 1)
            SkeletonTile(Modifier.weight(1f), 120.dp, cs.secondaryContainer, cs.onSecondaryContainer, MaterialShapes.Pill, false, 2)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonTile(Modifier.weight(1f), 120.dp, cs.tertiaryContainer, cs.onTertiaryContainer, MaterialShapes.Flower, false, 3)
            SkeletonTile(Modifier.weight(1f), 120.dp, cs.surfaceContainerHighest, cs.onSurface, MaterialShapes.SoftBurst, false, 4)
        }
    }
}

fun LazyListScope.homeSkeleton() {
    item(key = "skb") { SkeletonBento() }
    skeletonRows(4, square = true, key = "sko")
}

/** Org page: balance hero + action buttons + rows. */
fun LazyListScope.orgSkeleton() {
    item(key = "skhero") {
        val cs = MaterialTheme.colorScheme
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(28.dp)).background(cs.primaryContainer)) {
                SkeletonShape(MaterialShapes.Cookie9Sided, 200.dp, cs.onPrimaryContainer.copy(alpha = .08f), Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp))
                Column(Modifier.padding(24.dp)) {
                    SkeletonBlock(70.dp, 12.dp, color = cs.onPrimaryContainer.copy(alpha = .18f))
                    Spacer(Modifier.height(14.dp))
                    SkeletonBlock(190.dp, 40.dp, color = cs.onPrimaryContainer.copy(alpha = .18f))
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { SkeletonBlock(null, 40.dp, Modifier.weight(1f), color = cs.secondaryContainer, index = it + 1) }
            }
        }
    }
    skeletonRows(6, key = "skt")
}

/** Payment-card shaped placeholders. */
fun LazyListScope.cardsSkeleton(count: Int = 3) {
    items(count, key = { "skc$it" }) { i ->
        val cs = MaterialTheme.colorScheme
        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().aspectRatio(1.586f).clip(RoundedCornerShape(24.dp)).background(cs.surfaceContainerHigh)) {
            SkeletonShape(rowShapes[i % rowShapes.size], 220.dp, cs.onSurface.copy(alpha = .07f), Modifier.align(Alignment.BottomEnd).offset(x = 50.dp, y = 50.dp), i)
            Column(Modifier.padding(22.dp).align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBlock(150.dp, 18.dp, color = cs.onSurface.copy(alpha = .12f), index = i)
                SkeletonBlock(90.dp, 12.dp, color = cs.onSurface.copy(alpha = .10f), index = i)
            }
        }
    }
}

/** Transaction details: amount hero + detail rows. */
fun LazyListScope.txSkeleton() {
    item(key = "skamt") {
        val cs = MaterialTheme.colorScheme
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonShape(MaterialShapes.Cookie9Sided, 72.dp, cs.secondaryContainer)
            SkeletonBlock(180.dp, 44.dp, color = cs.onSurface.copy(alpha = .12f))
            SkeletonBlock(120.dp, 12.dp, color = cs.onSurface.copy(alpha = .10f))
        }
    }
    item(key = "skdh") { SkeletonHeader() }
    items(5, key = { "skd$it" }) { i ->
        SegmentedItem(i, 5) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(60.dp, 10.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .10f), index = i)
                SkeletonBlock(listOf(160, 120, 190, 100, 140)[i].dp, 14.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f), index = i)
            }
        }
    }
}

/** Full-screen skeleton list wrapper used instead of CenterLoading. */
@Composable
fun SkeletonScreen(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    androidx.compose.foundation.lazy.LazyColumn(modifier, userScrollEnabled = false, contentPadding = ListBottom) { content() }
}
