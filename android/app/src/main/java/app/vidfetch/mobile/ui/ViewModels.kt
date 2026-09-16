package app.vidfetch.mobile.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vidfetch.mobile.VidFetchApp
import app.vidfetch.mobile.data.LinkAnalyzer
import app.vidfetch.mobile.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun example() {
        url = "https://www.w3schools.com/html/mov_bbb.mp4"
        state = HomeState.Idle
    }

    fun analyze() {
        val target = url.trim()
        if (target.isBlank()) {
            state = HomeState.Error("Вставьте ссылку")
            return
        }
        state = HomeState.Loading
        viewModelScope.launch {
            try {
                val info = withContext(Dispatchers.IO) { LinkAnalyzer.analyze(target) }
                url = info.url
                state = if (info.kind == "page") {
                    HomeState.Error("Это веб-страница, а не файл. Нужна прямая ссылка на файл (mp4, mp3, …).")
                } else {
                    HomeState.Ready(
                        title = info.title,
                        kind = info.kind,
                        sizeText = if (info.totalBytes > 0) humanBytes(info.totalBytes) else "размер неизвестен",
                    )
                }
            } catch (e: Exception) {
                state = HomeState.Error("Не удалось прочитать ссылку: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun startDownload() {
        val ready = state as? HomeState.Ready ?: return
        val target = url.trim()
        val fileName = target.substringAfterLast('/').substringBefore('?').ifBlank { "file_${System.currentTimeMillis()}" }
        viewModelScope.launch {
            repo.enqueue(target, ready.title, ready.kind, fileName)
            state = HomeState.Idle
        }
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
