package no.nav.familie.ba.sak.integrasjoner

import no.nav.familie.ba.sak.HttpTestBase
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.kontrakter.felles.Ressurs

import no.nav.familie.kontrakter.felles.objectMapper
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles


@SpringBootTest(classes = [ApplicationConfig::class], properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:18085/api"])
@ActiveProfiles("dev", "mock-oauth")
@TestInstance(Lifecycle.PER_CLASS)
class DistribuerVedtaksbrevTest : HttpTestBase(18085) {

    @Autowired
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev returnerer normalt ved vellykket integrasjonskall`() {
        mockServer.enqueue(distribusjonOkResponse())
        assertDoesNotThrow { integrasjonTjeneste.distribuerVedtaksbrev("123456789") }
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis integrasjoner gir blank response`() {
        mockServer.enqueue(blankResponse())
        assertThrows<IllegalArgumentException> { integrasjonTjeneste.distribuerVedtaksbrev("123456789") }
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis integrasjoner gir failure response`() {
        mockServer.enqueue(failureResponse())
        assertThrows<IllegalArgumentException> { integrasjonTjeneste.distribuerVedtaksbrev("123456789") }
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis responsekoden ikke er 2xx`() {
        mockServer.enqueue(non2xxResponse())
        assertThrows<IntegrasjonException> { integrasjonTjeneste.distribuerVedtaksbrev("123456789") }
    }

    private fun distribusjonOkResponse() = mockResponse.setResponseCode(200)
        .setBody(objectMapper.writeValueAsString(Ressurs.success("1234567")))

    private fun blankResponse() = mockResponse.setResponseCode(200)
        .setBody(objectMapper.writeValueAsString(Ressurs.success("")))

    private fun failureResponse() = mockResponse.setResponseCode(200)
        .setBody(objectMapper.writeValueAsString(Ressurs.failure<Any>("")))

    private fun non2xxResponse() = mockResponse.setResponseCode(400)

    companion object {
        val mockResponse: MockResponse = MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
    }
}
