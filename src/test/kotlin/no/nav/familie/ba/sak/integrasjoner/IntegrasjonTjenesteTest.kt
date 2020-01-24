package no.nav.familie.ba.sak.integrasjoner

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.ba.sak.HttpTestBase
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.objectMapper
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper


@SpringBootTest(classes = [ApplicationConfig::class], properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:18085/api"])
@ActiveProfiles("dev", "mock-oauth")
@TestInstance(Lifecycle.PER_CLASS)
class IntegrasjonTjenesteTest : HttpTestBase(
        18085
) {
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

        assert(mockJournalpostForVedtakId == journalPostId)

        val request = mockServer.takeRequest()

        val expectedUri = "$integrasjonerUri/arkiv/v1"

        Assertions.assertEquals(expectedUri, request.requestUrl.toString())
        Assertions.assertEquals(HttpMethod.POST.toString(), request.method)
        val body = request.body.readUtf8()
        val mapper = jacksonObjectMapper()
        val arkiverDokumentRequest = mapper.readValue(body, ArkiverDokumentRequest::class.java)
        Assertions.assertEquals(mockFnr, arkiverDokumentRequest.fnr)
        Assertions.assertEquals(1, arkiverDokumentRequest.dokumenter.size)
        Assertions.assertEquals(IntegrasjonTjeneste.VEDTAK_DOKUMENT_TYPE, arkiverDokumentRequest.dokumenter[0].dokumentType)
        Assertions.assertEquals(IntegrasjonTjeneste.VEDTAK_FILTYPE, arkiverDokumentRequest.dokumenter[0].filType)
        Assertions.assertEquals(IntegrasjonTjeneste.VEDTAK_FILNAVN, arkiverDokumentRequest.dokumenter[0].filnavn)

        Assertions.assertTrue(arkiverDokumentRequest.dokumenter[0].dokument.foldIndexed(true) { index, acc, byte ->
            acc && byte == mockPdf[index]
        })
        Assertions.assertEquals(mockJournalpostForVedtakId, journalPostId)
    }
}