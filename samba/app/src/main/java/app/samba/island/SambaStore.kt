package app.samba.island

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Semua ukuran dalam dp. offsetX dihitung dari tengah layar, offsetY dari tepi atas. */
data class IslandConfig(
    val enabled: Boolean = false,
    val offsetX: Int = 0,
    val offsetY: Int = 8,
    val width: Int = 120,
    val height: Int = 34,
)

/** Satu sumber kebenaran untuk Activity dan Service (keduanya satu proses). */
object SambaStore {
    private const val FILE = "samba_prefs"
    private var loaded = false
    private val _config = MutableStateFlow(IslandConfig())
    val config: StateFlow<IslandConfig> = _config.asStateFlow()

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val d = IslandConfig()
        _config.value = IslandConfig(
            enabled = p.getBoolean("enabled", d.enabled),
            offsetX = p.getInt("offsetX", d.offsetX),
            offsetY = p.getInt("offsetY", d.offsetY),
            width = p.getInt("width", d.width),
            height = p.getInt("height", d.height),
        )
    }

    fun update(context: Context, change: (IslandConfig) -> IslandConfig) {
        val next = change(_config.value)
        _config.value = next
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", next.enabled)
            .putInt("offsetX", next.offsetX)
            .putInt("offsetY", next.offsetY)
            .putInt("width", next.width)
            .putInt("height", next.height)
            .apply()
    }
}
