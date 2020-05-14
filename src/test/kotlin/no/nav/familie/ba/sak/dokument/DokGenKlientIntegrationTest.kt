package no.nav.familie.ba.sak.dokument

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.dokument.domene.DokumentHeaderFelter
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
        override fun <T : Any> utførRequest(request: RequestEntity<String>, responseType: Class<T>): ResponseEntity<T> {
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
                if (request.body!!.matches(Regex(".+HTML.+"))) {
                    return ResponseEntity.ok(responseType.cast("<HTML><H1>Vedtaksbrev HTML (Mock)</H1></HTML>"))
                } else if (request.body!!.matches(Regex(".+PDF.+"))) {
                    return ResponseEntity.ok(responseType.cast("Vedtaksbrev PDF".toByteArray()))
                }
            } else {
                fail("Invalid URI")
            }

            return ResponseEntity(responseType.cast("mockup_response"), HttpStatus.OK)
        }
    }

    class DokumentServiceTest : DokumentService(mockk(), mockk(), mockk(), mockk(), mockk(), mockk()) {
        override fun hentStønadBrevMarkdown(vedtak: Vedtak,
                                            søknad: SøknadDTO?,
                                            behandlingResultatType: BehandlingResultatType): String {
            return "mockup_response"
        }
    }

    @Test
    @Tag("integration")
    fun `Test generer markdown`() {
        val dokumentService = DokumentServiceTest()
        val markdown = dokumentService.hentStønadBrevMarkdown(vedtak = lagVedtak(),
                                                              behandlingResultatType = BehandlingResultatType.INNVILGET)
        assert(markdown == "mockup_response")
    }

    @Test
    @Tag("integration")
    fun `Test generer html`() {
        val dokgen = DokGenTestKlient()
        val html = dokgen.lagHtmlFraMarkdown("Innvilget", "markdown", testDokumentHeaderFelter)
        assert(html == "<HTML><H1>Vedtaksbrev HTML (Mock)</H1></HTML>")
    }

    @Test
    @Tag("integration")
    fun `Test generer pdf`() {
        val dokgen = DokGenTestKlient()
        val pdf = dokgen.lagPdfFraMarkdown("Innvilget", "markdown", testDokumentHeaderFelter)
        assert(pdf.contentEquals("Vedtaksbrev PDF".toByteArray()))
    }
}

val testDokumentHeaderFelter = DokumentHeaderFelter(navn = "Mockersen",
                                                    dokumentDato = LocalDate.now().toString(),
                                                    fodselsnummer = "1234",
                                                    returadresse = "retur")