package ink.blavingad.hcb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.blavingad.hcb.container

@Composable
fun LoginScreen(onSignIn: (Boolean) -> Unit) {
    val auth = LocalContext.current.container.auth
    val busy by auth.busy.collectAsStateWithLifecycle()
    val error by auth.error.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    // a random expansion of "HCB" from github.com/hackclub/hcb-expansions
    val phrase = androidx.compose.runtime.remember {
        runCatching { ctx.assets.open("hcb_phrases.txt").bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }.random() }.getOrDefault("Hack Club's fiscal sponsorship platform")
    }
    Surface(color = cs.surface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            ink.blavingad.hcb.ui.HcbLogoCookie(140.dp)
            Spacer(Modifier.height(32.dp))
            Text("HCB", style = MaterialTheme.typography.displayLargeEmphasized, color = cs.onSurface)
            Text(phrase, style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            if (error != null) Text(error!!, color = cs.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp))
            Button(onClick = { onSignIn(false) }, enabled = !busy, shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(64.dp), contentPadding = ButtonDefaults.contentPaddingFor(64.dp)) {
                if (busy) LoadingIndicator(Modifier.size(28.dp), color = cs.onPrimary)
                else Text("Sign in with HCB", style = MaterialTheme.typography.titleMediumEmphasized)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { onSignIn(true) }, enabled = !busy, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Create an account")
            }
            Spacer(Modifier.height(16.dp))
            Text("Unofficial client · not affiliated with Hack Club", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }
    }
}
