package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CurrencyExchange
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
    val snack = ink.blavingad.hcb.ui.LocalSnack.current
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
            item {
                val amoled by c.prefs.amoled.collectAsStateWithLifecycle()
                val logos by c.prefs.logos.collectAsStateWithLifecycle()
                Spacer(Modifier.height(8.dp))
                SegmentedItem(0, 2, { c.prefs.setAmoled(!amoled) }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.DarkMode, null) },
                        headlineContent = { Text("Pure black") }, supportingContent = { Text("True black background in dark mode, for OLED screens") },
                        trailingContent = { Switch(amoled, { c.prefs.setAmoled(it) }) })
                }
                SegmentedItem(1, 2, { c.prefs.setLogos(!logos) }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Storefront, null) },
                        headlineContent = { Text("Merchant logos") }, supportingContent = { Text("Brand icons on card charges (Simple Icons)") },
                        trailingContent = { Switch(logos, { c.prefs.setLogos(it) }) })
                }
            }
            item {
                val cardStyle by c.prefs.cardStyle.collectAsStateWithLifecycle()
                Spacer(Modifier.height(8.dp))
                SegmentedItem(0, 1, { c.prefs.setCardStyle(if (cardStyle == "collage") "cookie" else "collage") }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.CreditCard, null) },
                        headlineContent = { Text("Card design") },
                        supportingContent = { Text((if (cardStyle == "collage") "Collage" else "Cookie") + " for virtual cards · physical cards keep the outline design") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                                listOf("cookie" to "Cookie", "collage" to "Collage").forEach { (k, l) ->
                                    ToggleButton(cardStyle == k, { c.prefs.setCardStyle(k) }) { Text(l) }
                                }
                            }
                        })
                }
            }
            item {
                val conv by c.prefs.convertTo.collectAsStateWithLifecycle()
                var convMenu by remember { mutableStateOf(false) }
                Spacer(Modifier.height(8.dp))
                SegmentedItem(0, 1, { convMenu = true }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.CurrencyExchange, null) },
                        headlineContent = { Text("Converted amount") },
                        supportingContent = { Text(if (conv.isEmpty()) "Off" else "$conv · Visa rate (0 % fee) for card charges, ECB otherwise, on the payment date") },
                        trailingContent = {
                            androidx.compose.foundation.layout.Box {
                                Icon(Icons.Outlined.ExpandMore, null)
                                DropdownMenu(convMenu, { convMenu = false }) {
                                    DropdownMenuItem({ Text("Off") }, { c.prefs.setConvertTo(""); convMenu = false })
                                    ink.blavingad.hcb.data.Fx.currencies.forEach { cur ->
                                        DropdownMenuItem({ Text("$cur · " + runCatching { java.util.Currency.getInstance(cur).displayName }.getOrDefault(cur)) }, { c.prefs.setConvertTo(cur); convMenu = false })
                                    }
                                }
                            }
                        })
                }
            }
            item { SectionHeader("Security & notifications") }
            item {
                val lock by c.prefs.lock.collectAsStateWithLifecycle()
                val notify by c.prefs.notify.collectAsStateWithLifecycle()
                val toggleLock: (Boolean) -> Unit = { v -> scope.launch { if (ctx.authenticate(if (v) "Turn on app lock" else "Turn off app lock")) { c.prefs.setLock(v); c.unlocked.value = true } } }
                val secure by c.prefs.secure.collectAsStateWithLifecycle()
                val relock by c.prefs.relockAfter.collectAsStateWithLifecycle()
                var relockMenu by remember { mutableStateOf(false) }
                val relockLabel: (Int) -> String = { s -> when { s == 0 -> "Immediately"; s < 60 -> "After $s s"; else -> "After ${s / 60} min" } }
                SegmentedItem(0, if (lock) 3 else 2, { toggleLock(!lock) }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Fingerprint, null) },
                        headlineContent = { Text("App lock") }, supportingContent = { Text("Fingerprint, face or screen lock to open the app") },
                        trailingContent = { Switch(lock, toggleLock) })
                }
                if (lock) SegmentedItem(1, 3, { relockMenu = true }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Timer, null) },
                        headlineContent = { Text("Lock again") }, supportingContent = { Text(relockLabel(relock) + " in the background") },
                        trailingContent = {
                            androidx.compose.foundation.layout.Box {
                                Icon(Icons.Outlined.ExpandMore, null)
                                DropdownMenu(relockMenu, { relockMenu = false }) {
                                    listOf(0, 30, 60, 300, 900).forEach { s -> DropdownMenuItem({ Text(relockLabel(s)) }, { c.prefs.setRelockAfter(s); relockMenu = false }) }
                                }
                            }
                        })
                }
                SegmentedItem(if (lock) 2 else 1, if (lock) 3 else 2, { c.prefs.setSecure(!secure) }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.ScreenLockPortrait, null) },
                        headlineContent = { Text("Block screenshots") }, supportingContent = { Text("Shows a cover instead of the app in recents") },
                        trailingContent = { Switch(secure, { c.prefs.setSecure(it) }) })
                }
            }
            item { SectionHeader("Notifications") }
            item {
                val notify by c.prefs.notify.collectAsStateWithLifecycle()
                val income by c.prefs.notifyIncome.collectAsStateWithLifecycle()
                val every by c.prefs.notifyEvery.collectAsStateWithLifecycle()
                var everyMenu by remember { mutableStateOf(false) }
                val everyLabel: (Int) -> String = { m -> if (m < 60) "Every $m min" else if (m == 60) "Every hour" else "Every ${m / 60} h" }
                val n = if (notify) 4 else 2
                SegmentedItem(0, n, { c.prefs.setNotify(!notify) }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Notifications, null) },
                        headlineContent = { Text("Transaction notifications") }, supportingContent = { Text("Tap one to add a receipt") },
                        trailingContent = { Switch(notify, { c.prefs.setNotify(it) }) })
                }
                if (notify) {
                    SegmentedItem(1, n, { everyMenu = true }) {
                        ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Schedule, null) },
                            headlineContent = { Text("Check for new transactions") }, supportingContent = { Text(everyLabel(every)) },
                            trailingContent = {
                                androidx.compose.foundation.layout.Box {
                                    Icon(Icons.Outlined.ExpandMore, null)
                                    DropdownMenu(everyMenu, { everyMenu = false }) {
                                        listOf(15, 30, 60, 180, 360).forEach { m -> DropdownMenuItem({ Text(everyLabel(m)) }, {
                                            c.prefs.setNotifyEvery(m); everyMenu = false
                                            ink.blavingad.hcb.notify.TxWorker.schedule(ctx, m, replace = true)
                                        }) }
                                    }
                                }
                            })
                    }
                    SegmentedItem(2, n, { c.prefs.setNotifyIncome(!income) }) {
                        ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Savings, null) },
                            headlineContent = { Text("Incoming money too") }, supportingContent = { Text("Donations, transfers and other deposits") },
                            trailingContent = { Switch(income, { c.prefs.setNotifyIncome(it) }) })
                    }
                }
                SegmentedItem(n - 1, n, {
                    ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Tune, null) },
                        headlineContent = { Text("System notification settings") }, supportingContent = { Text("Sound, vibration, lock screen") })
                }
            }
            item { SectionHeader("Links") }
            item {
                SegmentedItem(0, 1, {
                    val i = if (android.os.Build.VERSION.SDK_INT >= 31)
                        android.content.Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, android.net.Uri.parse("package:${ctx.packageName}"))
                    else android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${ctx.packageName}"))
                    runCatching { ctx.startActivity(i) }
                }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Link, null) },
                        headlineContent = { Text("Open HCB links in the app") },
                        supportingContent = { Text("Turn on hcb.hackclub.com and hcb.gg under \"Open by default\"") })
                }
            }
            item { SectionHeader("Storage") }
            item {
                SegmentedItem(0, 1, {
                    ink.blavingad.hcb.ui.MemCache.clear()
                    ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                    coil3.SingletonImageLoader.get(ctx).let { it.memoryCache?.clear(); it.diskCache?.clear() }
                    snack("Cache cleared")
                }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.CleaningServices, null) },
                        headlineContent = { Text("Clear cache") }, supportingContent = { Text("Saved screens, images and logos. Everything reloads from HCB.") })
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
                SegmentedItem(0, 2, { ctx.openUrl("https://github.com/blavingadtheplushie/hcb-material") }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.Code, null) },
                        headlineContent = { Text("HCB ${BuildConfig.VERSION_NAME}") },
                        supportingContent = { Text("Unofficial client · GPLv3") })
                }
                SegmentedItem(1, 2, { ctx.openUrl("https://github.com/blavingadtheplushie/hcb-material/issues/new") }) {
                    ListItem(colors = clear, leadingContent = { Icon(Icons.Outlined.BugReport, null) },
                        headlineContent = { Text("Report a bug") })
                }
            }
        }
    }
}
