package ink.blavingad.hcb.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val AUTH = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

fun Context.findActivity(): FragmentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is FragmentActivity) return c; c = c.baseContext }
    return null
}

fun Context.canAuthenticate(): Boolean =
    BiometricManager.from(this).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

/** Fingerprint / face / screen lock. Returns true when verified, or when the phone has no lock at all. */
suspend fun Context.authenticate(title: String, subtitle: String? = null): Boolean {
    val act = findActivity() ?: return false
    if (!canAuthenticate()) return true
    return suspendCancellableCoroutine { cont ->
        val prompt = BiometricPrompt(act, ContextCompat.getMainExecutor(act), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { if (cont.isActive) cont.resume(true) }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { if (cont.isActive) cont.resume(false) }
        })
        val info = BiometricPrompt.PromptInfo.Builder().setTitle(title).apply { subtitle?.let(::setSubtitle) }
            .setAllowedAuthenticators(AUTH).build()
        prompt.authenticate(info)
    }
}
