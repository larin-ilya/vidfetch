package app.vidfetch.mobile.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    LIGHT("Светлая"),
    DARK("Тёмная"),
    SYSTEM("Системная"),
}

/** Persisted application settings (SharedPreferences). */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("vidfetch_settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)
    )
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "Русский") ?: "Русский")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _folder = MutableStateFlow(prefs.getString("folder", "Внутренняя память/VidFetch") ?: "Внутренняя память/VidFetch")
    val folder: StateFlow<String> = _folder.asStateFlow()

    private val _maxConcurrent = MutableStateFlow(prefs.getInt("maxConcurrent", 2))
    val maxConcurrent: StateFlow<Int> = _maxConcurrent.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        _theme.value = mode
        prefs.edit().putString("theme", mode.name).apply()
    }

    fun setLanguage(value: String) {
        _language.value = value
        prefs.edit().putString("language", value).apply()
    }

    fun setFolder(value: String) {
        _folder.value = value
        prefs.edit().putString("folder", value).apply()
    }

    fun setMaxConcurrent(value: Int) {
        _maxConcurrent.value = value
        prefs.edit().putInt("maxConcurrent", value).apply()
    }
}
