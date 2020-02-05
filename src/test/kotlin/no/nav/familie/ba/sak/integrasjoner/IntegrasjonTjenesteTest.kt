package no.nav.familie.ba.sak.integrasjoner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.ba.sak.HttpTestBase
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.arkivering.FilType
import no.nav.familie.kontrakter.felles.objectMapper
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles


@SpringBootTest(classes = [ApplicationConfig::class], properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:18085/api"])
@ActiveProfiles("dev", "mock-oauth")
@TestInstance(Lifecycle.PER_CLASS)
class IntegrasjonTjenesteTest : HttpTestBase(18085) {

    @Autowired
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @Value("\${FAMILIE_INTEGRASJONER_API_URL}")
    lateinit var integrasjonerUri: String

    @Test
    @Tag("integration")
    fun `Iverksett vedtak på aktiv behandling`() {
        mockServer.enqueue(journalpostOkResponse())
        val journalPostId = integrasjonTjeneste.lagerJournalpostForVedtaksbrev(mockFnr, mockPdf)

        assertThat(mockJournalpostForVedtakId).isEqualTo(journalPostId)

        val request = mockServer.takeRequest()

        assertThat(HttpMethod.POST.toString()).isEqualTo(request.method)
        val body = request.body.readUtf8()
        val mapper = jacksonObjectMapper()
        val arkiverDokumentRequest = mapper.readValue(body, ArkiverDokumentRequest::class.java)
        assertThat(mockFnr).isEqualTo(arkiverDokumentRequest.fnr)
        assertThat(1).isEqualTo(arkiverDokumentRequest.dokumenter.size)
        assertThat(IntegrasjonTjeneste.VEDTAK_DOKUMENT_TYPE).isEqualTo(arkiverDokumentRequest.dokumenter[0].dokumentType)
        assertThat(FilType.PDFA).isEqualTo(arkiverDokumentRequest.dokumenter[0].filType)
        assertThat(arkiverDokumentRequest.dokumenter[0].dokument).isEqualTo(mockPdf)
        assertThat(mockJournalpostForVedtakId).isEqualTo(journalPostId)
    }

    private fun journalpostOkResponse(): MockResponse {
        val responseBody = Ressurs.success(ArkiverDokumentResponse(mockJournalpostForVedtakId, true))
        return mockResponse.setResponseCode(201).setBody(objectMapper.writeValueAsString(responseBody))
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev`() {
        mockServer.enqueue(distribusjonOkResponse())
        assertDoesNotThrow { integrasjonTjeneste.distribuerVedtaksbrev("123456789") }

        mockServer.enqueue(blankResponse())
        assertThrows<IllegalArgumentException> { integrasjonTjeneste.distribuerVedtaksbrev("123456789") }

        mockServer.enqueue(failureResponse())
        assertThrows<IllegalArgumentException> { integrasjonTjeneste.distribuerVedtaksbrev("123456789") }

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
        val mockJournalpostForVedtakId = "453491843"
        val mockFnr = "12345678910"
        val mockPdf = "mock data".toByteArray()
        val mockResponse: MockResponse = MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
    }
}
