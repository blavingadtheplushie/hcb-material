package ink.blavingad.hcb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ink.blavingad.hcb.container
import ink.blavingad.hcb.data.TAG_COLORS
import ink.blavingad.hcb.data.Transaction
import ink.blavingad.hcb.data.createTag
import ink.blavingad.hcb.data.deleteTag
import ink.blavingad.hcb.data.setTags
import ink.blavingad.hcb.data.tags
import kotlinx.coroutines.launch

/** HCB tag colour names → a tone from the current scheme, so tags follow the phone's palette. */
@Composable
fun tagColors(color: String?): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return when (color) {
        "red", "orange" -> cs.errorContainer to cs.onErrorContainer
        "yellow", "green" -> cs.tertiaryContainer to cs.onTertiaryContainer
        "cyan", "blue" -> cs.primaryContainer to cs.onPrimaryContainer
        "purple" -> cs.secondaryContainer to cs.onSecondaryContainer
        else -> cs.surfaceContainerHighest to cs.onSurface
    }
}

@Composable
fun TagSheet(org: String, tx: Transaction, onDismiss: () -> Unit, onSaved: (Transaction) -> Unit) {
    val c = LocalContext.current.container
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val all = rememberLoadable("tags", org) { c.api.tags(org) }
    val selected = remember { mutableStateListOf<String>().apply { addAll(tx.tags.orEmpty().map { it.id }) } }
    var label by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("blue") }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row { Text("Tags", style = MaterialTheme.typography.headlineSmallEmphasized, modifier = Modifier.weight(1f))
                TextButton({ editing = !editing }) { Text(if (editing) "Done" else "Manage") } }
            val list = all.data
            if (list == null) Text(all.error ?: "Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else if (list.isEmpty()) Text("No tags in this org yet. Create one below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                list.orEmpty().forEach { tag ->
                    val (bg, fg) = tagColors(tag.color)
                    val name = (tag.emoji?.let { "$it " } ?: "") + tag.label
                    if (editing) InputChip(selected = false, onClick = {
                        scope.launch { runCatching { c.api.deleteTag(tag.id) }.onSuccess { selected.remove(tag.id); all.load() }.onFailure { snack(it.message ?: "Couldn't delete") } }
                    }, label = { Text(name) }, trailingIcon = { Icon(Icons.Outlined.Close, "Delete", Modifier.size(16.dp)) })
                    else FilterChip(tag.id in selected, { if (tag.id in selected) selected.remove(tag.id) else selected.add(tag.id) }, { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = bg, selectedLabelColor = fg))
                }
            }
            Text("New tag", style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(emoji, { emoji = it.take(4) }, Modifier.size(width = 72.dp, height = 64.dp), placeholder = { Text("🏷️") }, singleLine = true)
                OutlinedTextField(label, { label = it }, Modifier.weight(1f), placeholder = { Text("Label") }, singleLine = true)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TAG_COLORS.forEach { col ->
                    val (bg, fg) = tagColors(col)
                    FilterChip(color == col, { color = col }, { Text(col) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = bg, selectedLabelColor = fg))
                }
            }
            TextButton(enabled = label.isNotBlank() && !busy, onClick = {
                busy = true
                scope.launch {
                    runCatching { c.api.createTag(org, label.trim(), color, emoji.trim().ifBlank { null }) }
                        .onSuccess { t -> selected.add(t.id); label = ""; emoji = ""; all.load() }
                        .onFailure { snack(it.message ?: "Couldn't create tag") }
                    busy = false
                }
            }) { Text("Create tag") }
            Button(enabled = !busy, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shapes = ButtonDefaults.shapes(), onClick = {
                busy = true
                scope.launch {
                    runCatching { c.api.setTags(org, tx.id, selected.toList()) }
                        .onSuccess { snack("Tags saved"); sheet.hide(); onSaved(it) }
                        .onFailure { snack(it.message ?: "Couldn't save tags") }
                    busy = false
                }
            }) { Text("Save") }
        }
    }
}

/** Chips under the rename field. */
@Composable
fun MemoSuggestions(list: List<String>, onPick: (String) -> Unit) {
    if (list.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        list.forEach { s -> SuggestionChip({ onPick(s) }, { Text(s, maxLines = 1) }) }
    }
}

