package ink.blavingad.hcb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** key, label, optional?, keyboard */
data class Field(val key: String, val label: String, val optional: Boolean = false, val type: KeyboardType = KeyboardType.Text, val multiline: Boolean = false)

/** Generic bottom-sheet form: fields + a big submit button with busy state and inline error. */
@Composable
fun FormSheet(
    title: String, subtitle: String?, fields: List<Field>, submit: String,
    onDismiss: () -> Unit, extra: (@Composable (SnapshotStateMap<String, String>) -> Unit)? = null,
    valid: (Map<String, String>) -> Boolean = { true },
    onSubmit: suspend (Map<String, String>) -> String,
) {
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val v = remember { mutableStateMapOf<String, String>() }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val ok = fields.filter { !it.optional }.all { !v[it.key].isNullOrBlank() } && valid(v)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmallEmphasized)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            extra?.invoke(v)
            fields.forEach { f ->
                OutlinedTextField(v[f.key] ?: "", { v[f.key] = it }, Modifier.fillMaxWidth(),
                    label = { Text(if (f.optional) "${f.label} (optional)" else f.label) }, singleLine = !f.multiline,
                    minLines = if (f.multiline) 2 else 1, keyboardOptions = KeyboardOptions(keyboardType = f.type))
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(enabled = ok && !busy, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shapes = ButtonDefaults.shapes(), onClick = {
                busy = true; error = null
                scope.launch {
                    runCatching { onSubmit(v.toMap()) }
                        .onSuccess { msg -> snack(msg); sheet.hide(); onDismiss() }
                        .onFailure { error = it.message ?: "Something went wrong" }
                    busy = false
                }
            }) {
                if (busy) LoadingIndicator(Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.onPrimary)
                Text(submit)
            }
        }
    }
}

val AMOUNT = Field("amount", "Amount (USD)", type = KeyboardType.Decimal)
