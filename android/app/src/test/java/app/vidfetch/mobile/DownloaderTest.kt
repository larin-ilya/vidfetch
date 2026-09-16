package app.vidfetch.mobile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.vidfetch.mobile.data.AppDatabase
import app.vidfetch.mobile.data.DownloadEntity
import app.vidfetch.mobile.data.Downloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Downloads a real file from an in-process HTTP server and checks it lands on disk + in Room. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloaderTest {

    @Test
    fun downloadsRealFileAndPersistsResult() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = MockWebServer()
        val payload = ByteArray(200_000) { (it % 251).toByte() }
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/octet-stream")
                .setBody(Buffer().write(payload))
        )
        server.start()
        val url = server.url("/sample.bin").toString()

        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.downloadDao()
        val id = dao.insert(
            DownloadEntity(
                url = url, title = "sample.bin", kind = "video", fileName = "sample.bin",
                filePath = null, totalBytes = -1, downloadedBytes = 0, status = "QUEUED",
                error = null, createdAt = 0, updatedAt = 0,
            )
        )

        Downloader(context, dao).start(id)

        var row: DownloadEntity? = null
        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline) {
            delay(150)
            row = dao.get(id)
            if (row?.status == "DONE" || row?.status == "ERROR") break
        }

        assertEquals("DONE", row?.status)
        assertEquals(200_000L, row?.downloadedBytes)
        val file = File(row!!.filePath!!)
        assertTrue(file.exists())
        assertEquals(200_000L, file.length())

        server.shutdown()
        db.close()
    }
}
