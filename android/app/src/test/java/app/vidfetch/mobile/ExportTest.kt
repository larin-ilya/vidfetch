package app.vidfetch.mobile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.vidfetch.mobile.data.AppDatabase
import app.vidfetch.mobile.data.DownloadEntity
import app.vidfetch.mobile.data.DownloadRepository
import app.vidfetch.mobile.data.Downloader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Proves the journal export ("admin") really writes the stored records to a CSV file. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExportTest {

    @Test
    fun exportsJournalToCsv() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.downloadDao()
        dao.insert(
            DownloadEntity(url = "https://x/a.mp3", title = "A", kind = "audio", fileName = "a.mp3",
                filePath = "/a.mp3", totalBytes = 10, downloadedBytes = 10, status = "DONE",
                error = null, createdAt = 1, updatedAt = 1)
        )
        dao.insert(
            DownloadEntity(url = "https://x/b.mp4", title = "B", kind = "video", fileName = "b.mp4",
                filePath = null, totalBytes = -1, downloadedBytes = 0, status = "DOWNLOADING",
                error = null, createdAt = 2, updatedAt = 2)
        )

        val repo = DownloadRepository(dao, Downloader(context, dao))
        val file = repo.exportCsv(context)

        assertTrue(file.exists())
        val lines = file.readLines().filter { it.isNotBlank() }
        assertEquals(3, lines.size) // header + 2 records
        assertTrue(lines[0].startsWith("id,title,kind"))
        assertTrue(lines.any { it.contains("a.mp3") })
        assertTrue(lines.any { it.contains("b.mp4") })
        db.close()
    }
}
