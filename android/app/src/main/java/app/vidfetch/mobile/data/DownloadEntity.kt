package app.vidfetch.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A download record persisted in the local Room database. */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val kind: String,          // "video" | "audio"
    val fileName: String,
    val filePath: String?,     // absolute path once finished
    val totalBytes: Long,      // -1 when unknown
    val downloadedBytes: Long,
    val status: String,        // QUEUED | DOWNLOADING | PAUSED | DONE | ERROR
    val error: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
