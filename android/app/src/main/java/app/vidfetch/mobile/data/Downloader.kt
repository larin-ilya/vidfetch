package app.vidfetch.mobile.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Real, cancellable HTTP downloader. Streams a URL to the app's external files
 * directory and writes progress into Room, so the UI always reflects persisted state.
 */
class Downloader(
    private val context: Context,
    private val dao: DownloadDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Long, Job>()

    fun start(id: Long) {
        if (jobs.containsKey(id)) return
        jobs[id] = scope.launch { runDownload(id) }
    }

    fun cancel(id: Long) {
        jobs.remove(id)?.cancel()
    }

    private suspend fun runDownload(id: Long) {
        val entity = dao.get(id) ?: return
        val dir = context.getExternalFilesDir("downloads") ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val out = File(dir, entity.fileName)
        var conn: HttpURLConnection? = null
        try {
            dao.updateStatus(id, "DOWNLOADING", System.currentTimeMillis())
            conn = (URL(entity.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) VidFetch/1.1")
            }
            conn.connect()
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    var lastEmit = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        read += n
                        val now = System.currentTimeMillis()
                        if (now - lastEmit > 250) {
                            lastEmit = now
                            dao.updateProgress(
                                id, read, if (total > 0) total else -1, "DOWNLOADING", now,
                            )
                        }
                    }
                    val size = out.length()
                    dao.updateProgress(id, size, if (total > 0) total else size, "DOWNLOADING", System.currentTimeMillis())
                }
            }
            dao.get(id)?.let {
                dao.update(
                    it.copy(
                        filePath = out.absolutePath,
                        downloadedBytes = out.length(),
                        totalBytes = if (total > 0) total else out.length(),
                        status = "DONE",
                        error = null,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
        } catch (ex: Exception) {
            dao.get(id)?.let {
                dao.update(
                    it.copy(
                        status = "ERROR",
                        error = ex.message ?: ex.javaClass.simpleName,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
        } finally {
            jobs.remove(id)
            conn?.disconnect()
        }
    }
}
