package app.vidfetch.mobile

import app.vidfetch.mobile.data.LinkAnalyzer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Proves the app accepts links: reads a real file URL, and falls back to GET when HEAD is rejected. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LinkAnalyzerTest {

    @Test
    fun acceptsDirectFileViaHead() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "video/mp4")
                .setBody(Buffer().write(ByteArray(50_000) { 1 }))
        )
        server.start()
        val url = server.url("/movie.mp4").toString()

        val info = LinkAnalyzer.analyze(url)

        assertEquals("movie.mp4", info.title)
        assertEquals("video", info.kind)
        assertEquals(50_000L, info.totalBytes)
        server.shutdown()
    }

    @Test
    fun fallsBackToGetWhenHeadRejected() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(405))                                  // HEAD rejected
        server.enqueue(                                                                      // GET (Range)
            MockResponse()
                .setHeader("Content-Type", "audio/mpeg")
                .setHeader("Content-Range", "bytes 0-0/9999")
                .setBody(Buffer().write(ByteArray(1)))
        )
        server.start()
        val url = server.url("/song.mp3").toString()

        val info = LinkAnalyzer.analyze(url)

        assertEquals("audio", info.kind)
        assertEquals(9999L, info.totalBytes)
        server.shutdown()
    }

    @Test
    fun normalizesBareHost() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setHeader("Content-Type", "video/mp4").setBody(Buffer().write(ByteArray(10)))
        )
        server.start()
        val hostPort = server.hostName + ":" + server.port
        val bare = "http://$hostPort/clip.mp4"
        // normalize() must leave a valid http URL untouched
        assertEquals(bare, LinkAnalyzer.normalize(bare))
        assertTrue(LinkAnalyzer.normalize("example.com/a.mp4").startsWith("https://"))
        server.shutdown()
    }
}
