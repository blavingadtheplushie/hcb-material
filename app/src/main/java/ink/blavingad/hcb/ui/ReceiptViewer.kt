package ink.blavingad.hcb.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.positionChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

private fun Receipt.isPdf(): Boolean {
    val n = (filename ?: url ?: "").substringBefore('?').lowercase()
    return n.endsWith(".pdf")
}

/** Downloads the receipt into cache/receipts/ (signed storage URL, no auth needed). */
private suspend fun download(ctx: Context, r: Receipt): File = withContext(Dispatchers.IO) {
    val dir = File(ctx.cacheDir, "receipts").apply { mkdirs() }
    val ext = (r.filename?.substringAfterLast('.', "") ?: "").ifBlank { if (r.isPdf()) "pdf" else "jpg" }
    val f = File(dir, "${r.id.ifBlank { "receipt" }}.$ext")
    if (f.exists() && f.length() > 0) return@withContext f
    val url = r.url ?: error("No file")
    ctx.container.http.newCall(Request.Builder().url(url).build()).execute().use { res ->
        if (!res.isSuccessful) error("Download failed (${res.code})")
        val tmp = File(dir, f.name + ".part")
        tmp.outputStream().use { out -> res.body.byteStream().copyTo(out) }
        tmp.renameTo(f)
    }
    f
}

/** Renders PDF pages one by one so the first page shows up right away. */
private fun renderPdf(f: File, width: Int) = flow {
    ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        PdfRenderer(fd).use { pdf ->
            for (i in 0 until minOf(pdf.pageCount, 30)) {
                val b = pdf.openPage(i).use { p ->
                    val w = width.coerceIn(400, 1600)
                    val h = (w.toFloat() * p.height / p.width).toInt().coerceAtLeast(1)
                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
                        it.eraseColor(android.graphics.Color.WHITE)
                        p.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
                emit(b)
            }
        }
    }
}.flowOn(Dispatchers.IO)

/**
 * Pinch-to-zoom + pan + double-tap container.
 * Zooms around the fingers' centroid, pans 1:1 with the finger, clamps to the content bounds and animates double taps.
 */
@Composable
private fun Zoomable(content: @Composable () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    var anim by remember { mutableStateOf<Job?>(null) }

    fun clamp(o: Offset, s: Float): Offset {
        val mx = (size.width * (s - 1f) / 2f).coerceAtLeast(0f)
        val my = (size.height * (s - 1f) / 2f).coerceAtLeast(0f)
        return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
    }
    fun animateTo(targetScale: Float, targetOffset: Offset) {
        anim?.cancel()
        val s0 = scale; val o0 = offset
        anim = scope.launch {
            animate(0f, 1f, animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)) { t, _ ->
                scale = s0 + (targetScale - s0) * t
                offset = o0 + (targetOffset - o0) * t
            }
        }
    }

    Box(
        Modifier.fillMaxSize().clipToBounds()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tap ->
                    if (scale > 1.05f) animateTo(1f, Offset.Zero)
                    else {
                        val target = 2.5f
                        val g = tap - Offset(size.width / 2f, size.height / 2f)
                        animateTo(target, clamp(g - (g - offset) * (target / scale), target))
                    }
                })
            }
            .pointerInput(Unit) {
                // only grab the gesture when pinching or already zoomed, so lists still scroll normally
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val ev = awaitPointerEvent()
                        if (ev.changes.size > 1 || scale > 1.01f) {
                            anim?.cancel()
                            val zoom = ev.calculateZoom()
                            val pan = ev.calculatePan()
                            val g = ev.calculateCentroid(useCurrent = true).let {
                                if (it == Offset.Unspecified) Offset.Zero else it - Offset(size.width / 2f, size.height / 2f)
                            }
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            val z = newScale / scale
                            // keep the point under the fingers fixed, then follow the finger 1:1
                            offset = clamp(g - (g - offset) * z + pan, newScale)
                            scale = newScale
                            ev.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (ev.changes.any { it.pressed })
                    if (scale < 1.02f && scale != 1f) animateTo(1f, Offset.Zero)
                }
            }
            .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReceiptViewer(r: Receipt, title: String? = null, subtitle: String? = null, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdf = remember(r.id) { r.isPdf() }
    var pages by remember(r.id) { mutableStateOf<List<Bitmap>?>(null) }
    var error by remember(r.id) { mutableStateOf<String?>(null) }
    val widthPx = (ctx.resources.displayMetrics.widthPixels * 1.5f).toInt()
    var fullLoaded by remember(r.id) { mutableStateOf(false) }

    if (pdf) LaunchedEffect(r.id) {
        runCatching {
            val f = download(ctx, r)
            renderPdf(f, widthPx).collect { b -> pages = pages.orEmpty() + b }
        }.onFailure { if (pages.isNullOrEmpty()) error = it.message ?: "Couldn't open this PDF" }
    }

    fun share() = scope.launch {
        runCatching {
            val f = download(ctx, r)
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = if (pdf) "application/pdf" else "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(send, r.filename ?: "Receipt"))
        }.onFailure { error = it.message ?: "Share failed" }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(title ?: "Receipt", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val sub = subtitle ?: listOfNotNull(
                                prettyDateTime(r.created_at).ifBlank { null }?.let { "Uploaded $it" },
                                r.uploader?.name?.ifBlank { null }?.let { "by $it" },
                            ).joinToString(" ").ifBlank { null }
                            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    navigationIcon = { IconButton(onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Close") } },
                    actions = {
                        IconButton({ share() }) { Icon(Icons.Outlined.Share, "Share") }
                        IconButton({ r.url?.let(ctx::openUrlInBrowser) }) { Icon(Icons.Outlined.OpenInNew, "Open externally") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.surfaceContainerLowest), contentAlignment = Alignment.Center) {
                when {
                    error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(32.dp)) {
                        Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton({ r.url?.let(ctx::openUrlInBrowser) }) { Text("Open in browser") }
                    }
                    !pdf -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Zoomable {
                            // small preview (already cached from the thumbnail) shows instantly, full-res fades in on top
                            if (!fullLoaded && r.preview_url != null) AsyncImage(r.preview_url, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                            AsyncImage(
                                ImageRequest.Builder(ctx).data(r.url ?: r.preview_url).crossfade(200).build(), r.filename,
                                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                                onSuccess = { fullLoaded = true },
                                onError = { if (r.preview_url == null) error = "Couldn't load this receipt" else fullLoaded = false },
                            )
                        }
                        if (!fullLoaded && r.preview_url == null) LoadingIndicator()
                    }
                    pages == null -> LoadingIndicator()
                    else -> Zoomable {
                        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                            itemsIndexed(pages!!) { _, b ->
                                Image(b.asImageBitmap(), null, Modifier.fillMaxWidth().background(Color.White), contentScale = ContentScale.FillWidth)
                            }
                        }
                    }
                }
            }
        }
    }
}
