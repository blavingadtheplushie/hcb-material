package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Notifications
import ink.blavingad.hcb.ui.authenticate
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.blavingad.hcb.BuildConfig
import ink.blavingad.hcb.ThemeMode
import ink.blavingad.hcb.container
import ink.blavingad.hcb.ui.Avatar
import ink.blavingad.hcb.ui.ListBottom
import ink.blavingad.hcb.ui.SectionHeader
import ink.blavingad.hcb.ui.SegmentedItem
import ink.blavingad.hcb.ui.openUrl
import ink.blavingad.hcb.ui.rememberLoadable
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    val user = rememberLoadable("user") { c.api.user() }
    val theme by c.prefs.theme.collectAsStateWithLifecycle()
    val hide by c.prefs.hideBalances.collectAsStateWithLifecycle()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val clear = ListItemDefaults.colors(containerColor = Color.Transparent)
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(title = { Text("Settings") }, scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer))
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = ListBottom) {
            item {
                user.data?.let { u ->
                    SegmentedItem(0, 1, color = MaterialTheme.colorScheme.secondaryContainer) {
                        ListItem(colors = clear, leadingContent = { Avatar(u.avatar, u.name, size = 56.dp) },
                            headlineContent = { Text(u.name, style = MaterialTheme.typography.titleLargeEmphasized) },
                            supportingContent = { u.email?.let { Text(it) } })
                    }
                }
            }
            item { SectionHeader("Appearance") }
            item {
                val modes = ThemeMode.entries
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    modes.forEachIndexed { i, m ->
                        ToggleButton(checked = theme == m, onCheckedChange = { c.prefs.setTheme(m) }, modifier = Modifier.weight(1f),
                            shapes = when (i) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() }) { Text(m.name) }
                    }
                }
                Text("Colours follow your wallpaper (Material You).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
            }
            item {
                SegmentedItem(0, 1, { c.prefs.setHide(!hide) }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.VisibilityOff, null) },
                        headlineContent = { Text("Hide balances") }, supportingContent = { Text("For screenshots and nosy friends") },
                        trailingContent = { Switch(hide, { c.prefs.setHide(it) }) })
                }
            }
            item { SectionHeader("Security & notifications") }
            item {
                val lock by c.prefs.lock.collectAsStateWithLifecycle()
                val notify by c.prefs.notify.collectAsStateWithLifecycle()
                val toggleLock: (Boolean) -> Unit = { v -> scope.launch { if (ctx.authenticate(if (v) "Turn on app lock" else "Turn off app lock")) { c.prefs.setLock(v); c.unlocked.value = true } } }
                SegmentedItem(0, 2, { toggleLock(!lock) }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Fingerprint, null) },
                        headlineContent = { Text("App lock") }, supportingContent = { Text("Fingerprint, face or screen lock to open the app") },
                        trailingContent = { Switch(lock, toggleLock) })
                }
                SegmentedItem(1, 2, { c.prefs.setNotify(!notify) }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Notifications, null) },
                        headlineContent = { Text("Transaction notifications") }, supportingContent = { Text("Checked every 15 min · tap to add a receipt") },
                        trailingContent = { Switch(notify, { c.prefs.setNotify(it) }) })
                }
            }
            item { SectionHeader("Account") }
            item {
                SegmentedItem(0, 3, { ctx.openUrl("https://hcb.hackclub.com/my/settings") }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.ManageAccounts, null) }, headlineContent = { Text("Account settings on HCB") })
                }
                SegmentedItem(1, 3, { ctx.openUrl("https://help.hcb.hackclub.com") }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, null) }, headlineContent = { Text("HCB help center") })
                }
                SegmentedItem(2, 3, { scope.launch { c.auth.logout() } }, color = MaterialTheme.colorScheme.errorContainer) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.AutoMirrored.Outlined.Logout, null) }, headlineContent = { Text("Sign out") })
                }
            }
            item { SectionHeader("About") }
            item {
                SegmentedItem(0, 1, { ctx.openUrl("https://github.com/blavingadtheplushie/hcb-material") }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Code, null) },
                        headlineContent = { Text("HCB Material ${BuildConfig.VERSION_NAME}") },
                        supportingContent = { Text("Unofficial, GPLv3 · made by a plushie octopus") })
                }
            }
        }
    }
}
