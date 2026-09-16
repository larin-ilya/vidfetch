package app.vidfetch.mobile

import android.app.Application
import app.vidfetch.mobile.data.AppDatabase
import app.vidfetch.mobile.data.DownloadRepository
import app.vidfetch.mobile.data.Downloader
import app.vidfetch.mobile.data.SettingsStore

class VidFetchApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { DownloadRepository(database.downloadDao(), Downloader(this, database.downloadDao())) }
    val settings by lazy { SettingsStore(this) }
}
