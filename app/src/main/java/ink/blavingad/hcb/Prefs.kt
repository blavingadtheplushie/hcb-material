package ink.blavingad.hcb

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

enum class ThemeMode { System, Light, Dark }

class Prefs(context: Context) {
    private val p = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val theme = MutableStateFlow(runCatching { ThemeMode.valueOf(p.getString("theme", "System")!!) }.getOrDefault(ThemeMode.System))
    val hideBalances = MutableStateFlow(p.getBoolean("hide", false))
    fun setTheme(m: ThemeMode) { theme.value = m; p.edit { putString("theme", m.name) } }
    fun setHide(v: Boolean) { hideBalances.value = v; p.edit { putBoolean("hide", v) } }
}
