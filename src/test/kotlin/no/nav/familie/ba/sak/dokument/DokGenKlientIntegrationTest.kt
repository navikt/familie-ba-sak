package no.nav.familie.ba.sak.dokument

import com.fasterxml.jackson.databind.ObjectMapper
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

    class DokGenTestKlient : DokGenKlient("mock_dokgen_uri", RestTemplate()) {
        override fun <T : Any> utførRequest(request: RequestEntity<Any>, responseType: Class<T>): ResponseEntity<T> {
            if (request.url.path.matches(Regex(".+create-markdown"))) {
                assert(request.body is String)
                val mapper = ObjectMapper()
                val felter = mapper.readValue(request.body as String, Map::class.java)
                assert(felter.contains("belop"))
                assert(felter.contains("startDato"))
                assert(felter.contains("fodselsnummer"))
                assert(felter.contains("fodselsdato"))
                assert(felter.contains("etterbetaling"))
                assert(felter.contains("enhet"))
                assert(felter.contains("saksbehandler"))
            } else if (request.url.path.matches(Regex(".+create-doc"))) {
                return when ((request.body!! as DokumentRequest).docFormat) {
                    DocFormat.HTML -> ResponseEntity.ok(responseType.cast("<HTML><H1>Vedtaksbrev HTML (Mock)</H1></HTML>"))
                    DocFormat.PDF -> ResponseEntity.ok(responseType.cast("Vedtaksbrev PDF".toByteArray()))
                    else -> ResponseEntity(responseType.cast("mockup_response"), HttpStatus.OK)
                }
            } else {
                fail("Invalid URI")
            }

            return ResponseEntity(responseType.cast("mockup_response"), HttpStatus.OK)
        }
    }

    @Test
    @Tag("integration")
    fun `Test generer pdf`() {
        val dokgen = DokGenTestKlient()
        val pdf = dokgen.lagPdfForMal(MalMedData("Innvilget", "fletteFelter"), testDokumentHeaderFelter)
        assert(pdf.contentEquals("Vedtaksbrev PDF".toByteArray()))
    }
}

val testDokumentHeaderFelter = DokumentHeaderFelter(navn = "Mockersen",
                                                    dokumentDato = LocalDate.now().toString(),
                                                    fodselsnummer = "1234",
                                                    returadresse = "retur")