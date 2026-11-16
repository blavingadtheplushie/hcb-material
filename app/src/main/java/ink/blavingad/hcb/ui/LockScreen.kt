package ink.blavingad.hcb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import ink.blavingad.hcb.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tryUnlock: () -> Unit = { scope.launch { if (ctx.authenticate("Unlock HCB")) onUnlocked() } }
    LaunchedEffect(Unit) { tryUnlock() }
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.surface), contentAlignment = Alignment.Center) {
        BgShape(MaterialShapes.Cookie12Sided, cs.primary.copy(alpha = .08f), Modifier.size(420.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            HcbLogoCookie(112.dp)
            Text("HCB is locked", style = MaterialTheme.typography.headlineSmallEmphasized, color = cs.onSurface)
            Button(tryUnlock, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Outlined.Fingerprint, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Unlock")
            }
        }
    }
}

/** Shown over the app while it sits in the recents screen (when "Hide in recents" is on). */
@Composable
fun PrivacyCover() {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.surface), contentAlignment = Alignment.Center) {
        BgShape(MaterialShapes.Cookie12Sided, cs.primary.copy(alpha = .08f), Modifier.size(420.dp))
        HcbLogoCookie(128.dp)
    }
}

/** The HCB building logo inside a themed cookie (same as the launcher icon, but in the phone's colours). */
@Composable
fun HcbLogoCookie(size: Dp) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(size).clip(MaterialShapes.Cookie9Sided.toShape()).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_hcb_logo), "HCB", Modifier.size(size * .58f), tint = cs.onPrimaryContainer)
    }
}
