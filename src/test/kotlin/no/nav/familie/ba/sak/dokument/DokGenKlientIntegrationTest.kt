package no.nav.familie.ba.sak.dokument


import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.restDomene.DocFormat
import no.nav.familie.ba.sak.dokument.domene.DokumentHeaderFelter
import no.nav.familie.ba.sak.dokument.domene.DokumentRequest
import no.nav.familie.ba.sak.dokument.domene.MalMedData
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Tag("integration")
class DokGenKlientIntegrationTest {

    class DokGenTestKlient(restTemplate: RestTemplate) : DokGenKlient("mock://familie-ba-dokgen.adeo.no", restTemplate) {
        override fun <T : Any> utførRequest(request: RequestEntity<Any>, responseType: Class<T>): ResponseEntity<T> {
            super.utførRequest(request, responseType)
            if (request.url.path.matches(Regex(".+create-doc"))) {
                return when ((request.body!! as DokumentRequest).docFormat) {
                    DocFormat.HTML -> ResponseEntity.ok(responseType.cast("<HTML><H1>Vedtaksbrev HTML (Mock)</H1></HTML>"))
                    DocFormat.PDF -> ResponseEntity.ok(responseType.cast("Vedtaksbrev PDF".toByteArray()))
                    else -> ResponseEntity(responseType.cast("mockup_response"), HttpStatus.OK)
                }
            } else {
                fail("Invalid URI")
            }
        }
    }

    @Test
    @Tag("integration")
    fun `Test generer pdf`() {
        val restTemplate: RestTemplate = mockk(relaxed = true)
        val dokgen = DokGenTestKlient(restTemplate)
        val pdf = dokgen.lagPdfForMal(MalMedData("Innvilget", "fletteFelter"), testDokumentHeaderFelter)
        assert(pdf.contentEquals("Vedtaksbrev PDF".toByteArray()))
    }

    @Test
    @Tag("integration")
    fun `fss fallback`() {
        val restTemplate: RestTemplate = mockk()
        every {
            restTemplate.exchange(any(),
                                  ByteArray::class.java)
        } throws RuntimeException("of some kind") andThen ResponseEntity.ok("Vedtaksbrev PDF".toByteArray())

        val dokgen = DokGenTestKlient(restTemplate)
        val pdf = dokgen.lagPdfForMal(MalMedData("Innvilget", "fletteFelter"), testDokumentHeaderFelter)

        verify(exactly = 2) {
            restTemplate.exchange(any(), ByteArray::class.java)
        }
        assert(pdf.contentEquals("Vedtaksbrev PDF".toByteArray()))
    }
}

val testDokumentHeaderFelter = DokumentHeaderFelter(navn = "Mockersen",
                                                    dokumentDato = LocalDate.now().toString(),
                                                    fodselsnummer = "1234",
                                                    målform = "NB")