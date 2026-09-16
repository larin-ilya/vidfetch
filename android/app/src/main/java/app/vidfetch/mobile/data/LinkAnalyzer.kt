package app.vidfetch.mobile.data

import java.net.HttpURLConnection
import java.net.URL

data class LinkInfo(
    val url: String,
    val title: String,
    val kind: String,     // "video" | "audio" | "page"
    val totalBytes: Long, // -1 if unknown
)

/**
 * Robust link reader: many hosts reject HEAD, so we fall back to a cheap
 * `GET` with `Range: bytes=0-0`, and we read the total size from `Content-Range`.
 */
object LinkAnalyzer {

    fun normalize(raw: String): String {
        val t = raw.trim()
        return when {
            t.startsWith("http://") || t.startsWith("https://") -> t
            t.contains("://") -> t
            t.isNotBlank() -> "https://$t"
            else -> t
        }
    }

    fun analyze(rawUrl: String): LinkInfo {
        val url = normalize(rawUrl)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw IllegalArgumentException("Вставьте ссылку http(s)://")
        }
        head(url)?.let { return it }
        return get(url)
    }

    private fun open(url: String, method: String, range: Boolean): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) VidFetch/1.1")
            if (range) setRequestProperty("Range", "bytes=0-0")
        }

    private fun head(url: String): LinkInfo? {
        val c = open(url, "HEAD", range = false)
        return try {
            c.connect()
            if (c.responseCode in 200..299) build(url, c) else null
        } catch (e: Exception) {
            null
        } finally {
            c.disconnect()
        }
    }

    private fun get(url: String): LinkInfo {
        val c = open(url, "GET", range = true)
        return try {
            c.connect()
            val code = c.responseCode
            if (code in 200..299) build(url, c) else throw RuntimeException("HTTP $code")
        } finally {
            c.disconnect()
        }
    }

    private fun build(url: String, c: HttpURLConnection): LinkInfo {
        val type = (c.contentType ?: "").substringBefore(';').trim().lowercase()
        val disposition = c.getHeaderField("Content-Disposition") ?: ""
        val dispName = Regex("""filename\*?=(?:UTF-8'')?["']?([^"';]+)""")
            .find(disposition)?.groupValues?.getOrNull(1)?.trim()
        val urlName = url.substringAfterLast('/').substringBefore('?').ifBlank { "file" }
        val title = if (!dispName.isNullOrBlank()) dispName else urlName

        val contentRange = c.getHeaderField("Content-Range")
        val totalFromRange = contentRange?.substringAfterLast('/')?.trim()?.toLongOrNull()
        var len = c.contentLengthLong
        if (totalFromRange != null && totalFromRange > 0) len = totalFromRange

        val lower = title.lowercase()
        val kind = when {
            type.startsWith("audio") -> "audio"
            type.startsWith("video") -> "video"
            type == "text/html" || type == "application/xhtml+xml" -> "page"
            lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") || lower.endsWith(".ogg") -> "audio"
            lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") -> "video"
            else -> "video"
        }
        return LinkInfo(url, title, kind, len)
    }
}
