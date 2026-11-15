package ink.blavingad.hcb

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

enum class ThemeMode { System, Light, Dark }

class Prefs(context: Context) {
    private val p = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val theme = MutableStateFlow(runCatching { ThemeMode.valueOf(p.getString("theme", "System")!!) }.getOrDefault(ThemeMode.System))
    val hideBalances = MutableStateFlow(p.getBoolean("hide", false))
    val lock = MutableStateFlow(p.getBoolean("lock", false))
    val notify = MutableStateFlow(p.getBoolean("notify", true))
    var lastSeenTx: String?
        get() = p.getString("lastTx", null)
        set(v) { p.edit { putString("lastTx", v) } }
    fun setLock(v: Boolean) { lock.value = v; p.edit { putBoolean("lock", v) } }
    fun setNotify(v: Boolean) { notify.value = v; p.edit { putBoolean("notify", v) } }
    fun setTheme(m: ThemeMode) { theme.value = m; p.edit { putString("theme", m.name) } }
    fun setHide(v: Boolean) { hideBalances.value = v; p.edit { putBoolean("hide", v) } }
}
