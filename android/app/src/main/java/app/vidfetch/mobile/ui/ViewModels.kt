package app.vidfetch.mobile.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vidfetch.mobile.VidFetchApp
import app.vidfetch.mobile.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private data class HeadMeta(val title: String, val size: Long, val kind: String)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as VidFetchApp).repository

    var url by mutableStateOf("")
        private set
    var state by mutableStateOf<HomeState>(HomeState.Idle)
        private set

    fun onUrl(value: String) {
        url = value
        if (state is HomeState.Error) state = HomeState.Idle
    }

    fun demoUrl() {
        url = "https://download.samplelib.com/mp3/sample-6s.mp3"
        state = HomeState.Idle
    }

    fun analyze() {
        val target = url.trim()
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            state = HomeState.Error("Вставьте прямую ссылку http(s):// на файл")
            return
        }
        state = HomeState.Loading
        viewModelScope.launch {
            try {
                val meta = withContext(Dispatchers.IO) { head(target) }
                state = HomeState.Ready(meta.title, meta.kind, if (meta.size > 0) humanBytes(meta.size) else "размер неизвестен")
            } catch (e: Exception) {
                state = HomeState.Error("Не удалось прочитать ссылку: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun startDownload() {
        val ready = state as? HomeState.Ready ?: return
        val target = url.trim()
        val fileName = target.substringAfterLast('/').substringBefore('?').ifBlank { "video_${System.currentTimeMillis()}" }
        viewModelScope.launch {
            repo.enqueue(target, ready.title, ready.kind, fileName)
            state = HomeState.Idle
        }
    }

    fun reset() {
        state = HomeState.Idle
    }

    private fun head(u: String): HeadMeta {
        val c = (URL(u).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) VidFetch/1.1")
        }
        c.connect()
        val len = c.contentLengthLong
        val type = c.contentType ?: ""
        val name = u.substringAfterLast('/').substringBefore('?').ifBlank { "Файл" }
        val kind = if (type.startsWith("audio") || name.endsWith(".mp3") || name.endsWith(".m4a")) "audio" else "video"
        return HeadMeta(name, len, kind)
    }
}

class DownloadsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as VidFetchApp).repository
    val items: StateFlow<List<DownloadRow>> =
        repo.observeActive()
            .map { list -> list.map { it.toRow() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun pause(id: Long) = repo.pause(id)
    fun resume(id: Long) = repo.resume(id)
    fun cancel(id: Long) = repo.cancel(id)
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
}

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as VidFetchApp).repository
    val items: StateFlow<List<DownloadRow>> =
        repo.observeDone()
            .map { list -> list.map { it.toRow() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as VidFetchApp).settings
    val theme: StateFlow<ThemeMode> = settings.theme
    val language: StateFlow<String> = settings.language
    val folder: StateFlow<String> = settings.folder
    val maxConcurrent: StateFlow<Int> = settings.maxConcurrent

    fun setTheme(mode: ThemeMode) = settings.setTheme(mode)
    fun setLanguage(value: String) = settings.setLanguage(value)
    fun setFolder(value: String) = settings.setFolder(value)
    fun setMaxConcurrent(value: Int) = settings.setMaxConcurrent(value)
}
