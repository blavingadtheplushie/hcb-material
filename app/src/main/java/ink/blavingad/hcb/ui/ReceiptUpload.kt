package ink.blavingad.hcb.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import ink.blavingad.hcb.container
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReceiptUploader(val camera: () -> Unit, val file: () -> Unit, busyState: () -> Boolean) {
    private val b = busyState
    val busy: Boolean get() = b()
}

/** Camera (full-res via FileProvider) or any file (image/PDF) → POST /receipts. txId null = receipt bin. */
@Composable
fun rememberReceiptUploader(txId: String?, onDone: () -> Unit): ReceiptUploader {
    val ctx = LocalContext.current
    val c = ctx.container
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    // survives process death while the camera app is in front
    var pending by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }

    fun send(uri: Uri, fallbackName: String) {
        busy = true
        scope.launch {
            try {
                val (bytes, name, mime) = withContext(Dispatchers.IO) {
                    val cr = ctx.contentResolver
                    val name = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cur ->
                        if (cur.moveToFirst()) cur.getString(0) else null
                    } ?: fallbackName
                    Triple(cr.openInputStream(uri)!!.use { it.readBytes() }, name, cr.getType(uri) ?: "image/jpeg")
                }
                c.api.uploadReceipt(bytes, name, mime, txId)
                snack("Receipt uploaded")
                onDone()
            } catch (e: Exception) { snack(e.message ?: "Upload failed") } finally { busy = false }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = pending; pending = null
        if (ok && u != null) send(Uri.parse(u), "receipt.jpg")
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) send(u, "receipt") }
    val latest = androidx.compose.runtime.rememberUpdatedState(snack)

    return remember {
        ReceiptUploader(
            camera = {
                try {
                    val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                    val f = File(dir, "receipt-${System.currentTimeMillis()}.jpg")
                    val u = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
                    pending = u.toString()
                    c.skipRelock = true
                    takePicture.launch(u)
                } catch (e: Exception) {
                    pending = null
                    latest.value(if (e is android.content.ActivityNotFoundException) "No camera app found" else e.message ?: "Couldn't open the camera")
                }
            },
            file = {
                try { c.skipRelock = true; pick.launch(arrayOf("image/*", "application/pdf")) }
                catch (e: Exception) { latest.value("No file picker found") }
            },
            busyState = { busy },
        )
    }
}
