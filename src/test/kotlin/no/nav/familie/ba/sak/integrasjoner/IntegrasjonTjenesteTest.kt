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
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
        val mockJournalpostForVedtakId = "453491843"
        val responseBody = Ressurs.success(ArkiverDokumentResponse(mockJournalpostForVedtakId, true))
        val mockFnr = "12345678910"
        val mockPdf = "mock data".toByteArray()
        val response: MockResponse = MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setResponseCode(201)
                .setBody(objectMapper.writeValueAsString(responseBody))

        mockServer.enqueue(response)
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
}
