package app.vidfetch.mobile.ui

import app.vidfetch.mobile.data.DownloadEntity

enum class ScreenState { LOADING, CONTENT, EMPTY, ERROR }

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, DONE, ERROR }

data class DownloadRow(
    val id: Long,
    val title: String,
    val subtitle: String,
    val progress: Float,
    val status: DownloadStatus,
    val percentText: String,
    val detail: String,
)

sealed interface HomeState {
    data object Idle : HomeState
    data object Loading : HomeState
    data class Error(val message: String) : HomeState
    data class Ready(val title: String, val kind: String, val sizeText: String) : HomeState
}

fun humanBytes(b: Long): String {
    if (b < 0) return "—"
    val units = listOf("Б", "КБ", "МБ", "ГБ")
    var v = b.toDouble()
    var i = 0
    while (v >= 1024 && i < units.size - 1) {
        v /= 1024
        i++
    }
    val s = if (v >= 100) v.toInt().toString() else String.format("%.1f", v)
    return "$s ${units[i]}"
}

fun DownloadEntity.toRow(): DownloadRow {
    val st = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.QUEUED)
    val progress = when {
        totalBytes > 0 -> (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
        st == DownloadStatus.DONE -> 1f
        else -> 0f
    }
    val percentText = when {
        totalBytes > 0 -> "${(progress * 100).toInt()}%"
        st == DownloadStatus.DONE -> "100%"
        else -> "—"
    }
    val detail = buildString {
        append(humanBytes(downloadedBytes))
        if (totalBytes > 0) append(" / ").append(humanBytes(totalBytes))
        if (!error.isNullOrBlank()) append(" · ").append(error)
    }
    return DownloadRow(
        id = id,
        title = title,
        subtitle = kind.uppercase() + " · " + fileName,
        progress = progress,
        status = st,
        percentText = percentText,
        detail = detail,
    )
}
