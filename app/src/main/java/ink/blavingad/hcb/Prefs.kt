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
    val amoled = MutableStateFlow(p.getBoolean("amoled", false))
    val secure = MutableStateFlow(p.getBoolean("secure", false))
    val logos = MutableStateFlow(p.getBoolean("logos", true))
    val notifyIncome = MutableStateFlow(p.getBoolean("notifyIncome", true))
    /** seconds in background before the app lock kicks in again (0 = immediately) */
    val relockAfter = MutableStateFlow(p.getInt("relock", 30))
    /** background check interval in minutes (Android minimum is 15) */
    val notifyEvery = MutableStateFlow(p.getInt("notifyEvery", 15))
    var lastSeenTx: String?
        get() = p.getString("lastTx", null)
        set(v) { p.edit { putString("lastTx", v) } }
    fun setLock(v: Boolean) { lock.value = v; p.edit { putBoolean("lock", v) } }
    fun setNotify(v: Boolean) { notify.value = v; p.edit { putBoolean("notify", v) } }
    fun setTheme(m: ThemeMode) { theme.value = m; p.edit { putString("theme", m.name) } }
    fun setHide(v: Boolean) { hideBalances.value = v; p.edit { putBoolean("hide", v) } }
    fun setAmoled(v: Boolean) { amoled.value = v; p.edit { putBoolean("amoled", v) } }
    fun setSecure(v: Boolean) { secure.value = v; p.edit { putBoolean("secure", v) } }
    fun setLogos(v: Boolean) { logos.value = v; p.edit { putBoolean("logos", v) } }
    fun setNotifyIncome(v: Boolean) { notifyIncome.value = v; p.edit { putBoolean("notifyIncome", v) } }
    fun setRelockAfter(v: Int) { relockAfter.value = v; p.edit { putInt("relock", v) } }
    fun setNotifyEvery(v: Int) { notifyEvery.value = v; p.edit { putInt("notifyEvery", v) } }
}
