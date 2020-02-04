package no.nav.familie.ba.sak

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

abstract class HttpTestBase(
        private val port: Int
) {

    var mockServer: MockWebServer = MockWebServer()
    val url = "http://localhost:$port"

    @BeforeAll
    fun prepare() {
        mockServer.start(port)
        mockServer.url(url)
    }

    @AfterAll
    fun tearDown() {
        mockServer.close()
    }
}
