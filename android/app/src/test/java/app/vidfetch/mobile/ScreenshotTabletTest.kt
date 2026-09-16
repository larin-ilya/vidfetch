package app.vidfetch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.vidfetch.mobile.data.ThemeMode
import app.vidfetch.mobile.ui.DownloadRow
import app.vidfetch.mobile.ui.DownloadStatus
import app.vidfetch.mobile.ui.DownloadsContent
import app.vidfetch.mobile.ui.HomeContent
import app.vidfetch.mobile.ui.HomeState
import app.vidfetch.mobile.ui.ScreenState
import app.vidfetch.mobile.ui.SettingsContent
import org.junit.Rule
import org.junit.Test

/** Snapshots on a tablet-sized device to demonstrate size adaptation. */
class ScreenshotTabletTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_10,
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false,
    )

    @Test
    fun homeTablet() {
        paparazzi.snapshot {
            AppPreview("home", false) {
                HomeContent("https://example.com/file.mp3", HomeState.Ready("file.mp3", "audio", "1.2 МБ"), {}, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun downloadsTablet() {
        paparazzi.snapshot {
            AppPreview("downloads", false) {
                DownloadsContent(
                    ScreenState.CONTENT,
                    listOf(DownloadRow(1, "trailer.mp4", "VIDEO · trailer.mp4", 0.6f, DownloadStatus.DOWNLOADING, "60%", "6.2 МБ / 10.4 МБ")),
                    { _, _ -> },
                )
            }
        }
    }
}
