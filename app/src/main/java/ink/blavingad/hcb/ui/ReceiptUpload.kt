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
    var pending by remember { mutableStateOf<Uri?>(null) }

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
        if (ok && u != null) send(u, "receipt.jpg")
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) send(u, "receipt") }

    return remember {
        ReceiptUploader(
            camera = {
                val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                val f = File(dir, "receipt-${System.currentTimeMillis()}.jpg")
                val u = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
                pending = u
                takePicture.launch(u)
            },
            file = { pick.launch(arrayOf("image/*", "application/pdf")) },
            busyState = { busy },
        )
    }
}
