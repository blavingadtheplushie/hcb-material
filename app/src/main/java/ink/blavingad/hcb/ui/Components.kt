package ink.blavingad.hcb.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Simple async state holder: load once, refresh on demand. */
class Loadable<T>(private val loader: suspend () -> T) {
    var data by mutableStateOf<T?>(null)
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    suspend fun load() {
        loading = true
        try { data = loader(); error = null } catch (e: Exception) { error = e.message ?: e.toString() } finally { loading = false }
    }
}

@Composable
fun <T> rememberLoadable(vararg keys: Any?, loader: suspend () -> T): Loadable<T> {
    val l = remember(*keys) { Loadable(loader) }
    LaunchedEffect(l) { l.load() }
    return l
}

/** Cursor-paginated list (limit/after/has_more). */
class Paged<T>(private val idOf: (T) -> String, private val fetch: suspend (after: String?) -> ink.blavingad.hcb.data.Paginated<T>) {
    var items by mutableStateOf<List<T>?>(null)
    var hasMore by mutableStateOf(true)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var total by mutableStateOf(0)
    suspend fun refresh() { loadPage(reset = true) }
    suspend fun more() { if (hasMore && !loading) loadPage(reset = false) }
    private suspend fun loadPage(reset: Boolean) {
        loading = true
        try {
            val after = if (reset) null else items?.lastOrNull()?.let(idOf)
            val p = fetch(after)
            items = if (reset) p.data else (items.orEmpty() + p.data)
            hasMore = p.has_more; total = p.total_count; error = null
        } catch (e: Exception) { error = e.message ?: e.toString() } finally { loading = false }
    }
}

@Composable
fun <T> rememberPaged(vararg keys: Any?, idOf: (T) -> String, fetch: suspend (String?) -> ink.blavingad.hcb.data.Paginated<T>): Paged<T> {
    val p = remember(*keys) { Paged(idOf, fetch) }
    LaunchedEffect(p) { p.refresh() }
    return p
}

/** Expressive pull-to-refresh with the morphing LoadingIndicator and haptic ticks (BetterAttend style). */
@Composable
fun RefreshBox(refreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state) {
        snapshotFlow { state.distanceFraction >= 1f }.distinctUntilChanged().drop(1).collect { armed ->
            haptic.performHapticFeedback(if (armed) HapticFeedbackType.GestureThresholdActivate else HapticFeedbackType.SegmentFrequentTick)
        }
    }
    PullToRefreshBox(
        isRefreshing = refreshing, onRefresh = onRefresh, state = state, modifier = modifier,
        indicator = { PullToRefreshDefaults.LoadingIndicator(state = state, isRefreshing = refreshing, modifier = Modifier.align(Alignment.TopCenter)) },
    ) { content() }
}

/**
 * A row in a segmented group: big outer corners on the first/last item, small inner ones,
 * and the corners morph rounder while pressed.
 */
@Composable
fun SegmentedItem(
    index: Int, count: Int, onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val big = 24.dp; val small = 6.dp
    val top by animateDpAsState(if (pressed) big else if (index == 0) big else small, MaterialTheme.motionScheme.fastSpatialSpec(), label = "t")
    val bottom by animateDpAsState(if (pressed) big else if (index == count - 1) big else small, MaterialTheme.motionScheme.fastSpatialSpec(), label = "b")
    val haptic = LocalHapticFeedback.current
    Surface(
        color = color,
        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = if (index == count - 1) 0.dp else 2.dp)
            .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
            .then(if (onClick != null) Modifier.clickable(interactionSource = src, indication = androidx.compose.material3.ripple()) {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onClick()
            } else Modifier),
    ) { content() }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 8.dp))
}

@Composable
fun Avatar(url: String?, name: String, size: Dp = 40.dp, square: Boolean = false) {
    val shape = if (square) RoundedCornerShape(size / 3.5f) else CircleShape
    Box(Modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold,
            style = if (size > 48.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium)
        if (!url.isNullOrBlank()) AsyncImage(url, null, Modifier.size(size).clip(shape))
    }
}

@Composable
fun CenterLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(56.dp)) }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.CloudOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, shapes = androidx.compose.material3.ButtonDefaults.shapes()) { Text("Retry") }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String? = null) {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).clip(androidx.compose.material3.MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMediumEmphasized, textAlign = TextAlign.Center)
        if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun Pill(text: String, container: Color, content: Color) {
    Surface(color = container, contentColor = content, shape = CircleShape) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}

val ListBottom = PaddingValues(bottom = 24.dp)
