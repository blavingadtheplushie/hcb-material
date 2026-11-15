package ink.blavingad.hcb.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ink.blavingad.hcb.container
import ink.blavingad.hcb.ui.screens.CardScreen
import ink.blavingad.hcb.ui.screens.CardsScreen
import ink.blavingad.hcb.ui.screens.HomeScreen
import ink.blavingad.hcb.ui.screens.InvitationScreen
import ink.blavingad.hcb.ui.screens.LoginScreen
import ink.blavingad.hcb.ui.screens.OrgScreen
import ink.blavingad.hcb.ui.screens.ReceiptsScreen
import ink.blavingad.hcb.ui.screens.SettingsScreen
import ink.blavingad.hcb.ui.screens.TransactionScreen
import kotlinx.coroutines.launch

val LocalNav = staticCompositionLocalOf<NavHostController> { error("no nav") }
val LocalSnack = staticCompositionLocalOf<(String) -> Unit> { {} }

private enum class Tab(val route: String, val label: String, val icon: ImageVector, val selected: ImageVector) {
    Home("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Cards("cards", "Cards", Icons.Outlined.CreditCard, Icons.Filled.CreditCard),
    Receipts("receipts", "Receipts", Icons.Outlined.Receipt, Icons.Filled.Receipt),
    Settings("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

@Composable
fun AppRoot(onSignIn: (Boolean) -> Unit) {
    val c = LocalContext.current.container
    val tokens by c.auth.tokens.collectAsStateWithLifecycle()
    if (tokens == null) { LoginScreen(onSignIn); return }

    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = Tab.entries.any { it.route == route }

    CompositionLocalProvider(LocalNav provides nav, LocalSnack provides { m: String -> scope.launch { snack.showSnackbar(m) }; Unit }) {
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                if (showBar) ShortNavigationBar {
                    Tab.entries.forEach { t ->
                        ShortNavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                if (route != t.route) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = { Icon(if (route == t.route) t.selected else t.icon, null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            },
        ) { pad ->
            NavHost(
                nav, startDestination = "home", modifier = Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding()),
                enterTransition = AppMotion.enter, exitTransition = AppMotion.exit,
                popEnterTransition = AppMotion.popEnter, popExitTransition = AppMotion.popExit,
                predictivePopEnterTransition = AppMotion.predictivePopEnter,
                predictivePopExitTransition = AppMotion.predictivePopExit,
            ) {
                composable("home", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) { UnderlayFrame { HomeScreen() } }
                composable("cards", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) { UnderlayFrame { CardsScreen() } }
                composable("receipts", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) { UnderlayFrame { ReceiptsScreen() } }
                composable("settings", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) { UnderlayFrame { SettingsScreen() } }
                composable("org/{id}") { CardFrame { OrgScreen(it.arguments?.getString("id")!!) } }
                composable("tx/{org}/{id}") { CardFrame { TransactionScreen(it.arguments?.getString("org")!!, it.arguments?.getString("id")!!) } }
                composable("card/{id}") { CardFrame { CardScreen(it.arguments?.getString("id")!!) } }
                composable("invite/{id}") { CardFrame { InvitationScreen(it.arguments?.getString("id")!!) } }
            }
        }
    }
}
