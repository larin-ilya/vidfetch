package app.vidfetch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.vidfetch.mobile.data.ThemeMode
import app.vidfetch.mobile.ui.DownloadRow
import app.vidfetch.mobile.ui.DownloadStatus
import app.vidfetch.mobile.ui.DownloadsContent
import app.vidfetch.mobile.ui.HistoryContent
import app.vidfetch.mobile.ui.HomeContent
import app.vidfetch.mobile.ui.HomeState
import app.vidfetch.mobile.ui.ScreenState
import app.vidfetch.mobile.ui.SettingsContent
import org.junit.Rule
import org.junit.Test

private val sampleRows = listOf(
    DownloadRow(1, "sample-6s.mp3", "AUDIO · sample-6s.mp3", 0.42f, DownloadStatus.DOWNLOADING, "42%", "246 КБ / 586 КБ"),
    DownloadRow(2, "trailer.mp4", "VIDEO · trailer.mp4", 0.78f, DownloadStatus.DOWNLOADING, "78%", "8.1 МБ / 10.4 МБ"),
    DownloadRow(3, "podcast.m4a", "AUDIO · podcast.m4a", 0.5f, DownloadStatus.PAUSED, "50%", "3.0 МБ / 6.0 МБ"),
)

private val doneRows = listOf(
    DownloadRow(4, "sample-6s.mp3", "AUDIO · sample-6s.mp3", 1f, DownloadStatus.DONE, "100%", "586 КБ"),
    DownloadRow(5, "clip.mp4", "VIDEO · clip.mp4", 1f, DownloadStatus.DONE, "100%", "4.2 МБ"),
)

/** Headless snapshots of every screen and state (phone, light + dark). */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false,
    )

    @Test
    fun homeIdle() {
        paparazzi.snapshot {
            AppPreview("home", false) {
                HomeContent("", HomeState.Idle, {}, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun homeLoading() {
        paparazzi.snapshot {
            AppPreview("home", false) {
                HomeContent("https://download.samplelib.com/mp3/sample-6s.mp3", HomeState.Loading, {}, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun homeReady() {
        paparazzi.snapshot {
            AppPreview("home", false) {
                HomeContent("https://download.samplelib.com/mp3/sample-6s.mp3", HomeState.Ready("sample-6s.mp3", "audio", "586 КБ"), {}, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun homeError() {
        paparazzi.snapshot {
            AppPreview("home", false) {
                HomeContent("не-ссылка", HomeState.Error("Вставьте прямую ссылку http(s):// на файл"), {}, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun downloadsContent() {
        paparazzi.snapshot { AppPreview("downloads", false) { DownloadsContent(ScreenState.CONTENT, sampleRows, { _, _ -> }) } }
    }

    @Test
    fun downloadsEmpty() {
        paparazzi.snapshot { AppPreview("downloads", false) { DownloadsContent(ScreenState.EMPTY, emptyList(), { _, _ -> }) } }
    }

    @Test
    fun downloadsError() {
        paparazzi.snapshot { AppPreview("downloads", false) { DownloadsContent(ScreenState.ERROR, emptyList(), { _, _ -> }) } }
    }

    @Test
    fun historyContent() {
        paparazzi.snapshot { AppPreview("history", false) { HistoryContent(ScreenState.CONTENT, doneRows, { _, _ -> }) } }
    }

    @Test
    fun historyEmpty() {
        paparazzi.snapshot { AppPreview("history", false) { HistoryContent(ScreenState.EMPTY, emptyList(), { _, _ -> }) } }
    }

    @Test
    fun settings() {
        paparazzi.snapshot {
            AppPreview("settings", false) {
                SettingsContent(ThemeMode.SYSTEM, "Русский", "Внутренняя память/VidFetch", 2, {}, {}, {})
            }
        }
    }

    @Test
    fun downloadsDark() {
        paparazzi.snapshot { AppPreview("downloads", true) { DownloadsContent(ScreenState.CONTENT, sampleRows, { _, _ -> }) } }
    }

    @Test
    fun settingsDark() {
        paparazzi.snapshot {
            AppPreview("settings", true) {
                SettingsContent(ThemeMode.DARK, "Русский", "Внутренняя память/VidFetch", 2, {}, {}, {})
            }
        }
    }

    @Test
    fun homeReadyDark() {
        paparazzi.snapshot {
            AppPreview("home", true) {
                HomeContent("https://download.samplelib.com/mp3/sample-6s.mp3", HomeState.Ready("sample-6s.mp3", "audio", "586 КБ"), {}, {}, {}, {}, {})
            }
        }
    }
}
