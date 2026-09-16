package app.vidfetch.mobile.data

import kotlinx.coroutines.flow.Flow

class DownloadRepository(
    private val dao: DownloadDao,
    private val downloader: Downloader,
) {
    fun observeActive(): Flow<List<DownloadEntity>> = dao.observeActive()
    fun observeDone(): Flow<List<DownloadEntity>> = dao.observeDone()
    fun observeAll(): Flow<List<DownloadEntity>> = dao.observeAll()

    suspend fun enqueue(url: String, title: String, kind: String, fileName: String): Long {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            DownloadEntity(
                url = url,
                title = title,
                kind = kind,
                fileName = fileName,
                filePath = null,
                totalBytes = -1,
                downloadedBytes = 0,
                status = "QUEUED",
                error = null,
                createdAt = now,
                updatedAt = now,
            )
        )
        downloader.start(id)
        return id
    }

    fun pause(id: Long) = downloader.cancel(id)

    fun resume(id: Long) = downloader.start(id)

    fun cancel(id: Long) = downloader.cancel(id)

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun all(): List<DownloadEntity> = dao.all()
}
