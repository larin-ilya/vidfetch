package app.vidfetch.mobile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.vidfetch.mobile.data.AppDatabase
import app.vidfetch.mobile.data.DownloadEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Proves that download records really persist in the on-disk Room database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomPersistenceTest {

    @Test
    fun recordsPersistAcrossReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "persist-test.db"
        context.deleteDatabase(name)

        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        db.downloadDao().insert(
            DownloadEntity(
                url = "https://example.com/y.mp3",
                title = "Тестовая запись",
                kind = "audio",
                fileName = "y.mp3",
                filePath = "/data/y.mp3",
                totalBytes = 100,
                downloadedBytes = 100,
                status = "DONE",
                error = null,
                createdAt = 1,
                updatedAt = 2,
            )
        )
        assertEquals(1, db.downloadDao().all().size)
        db.close()

        // Reopen: simulates an app restart — data must still be there.
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val rows = db.downloadDao().all()
        assertEquals(1, rows.size)
        assertEquals("Тестовая запись", rows[0].title)
        assertEquals("DONE", rows[0].status)
        assertEquals(100L, rows[0].downloadedBytes)
        db.close()
    }
}
