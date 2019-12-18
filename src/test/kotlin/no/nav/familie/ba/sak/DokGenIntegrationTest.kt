package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.DokGenService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Tag("integration")
class DokGenIntegrationTest{
    class DokGenTestService: DokGenService("mock_dokgen_uri", RestTemplate()){
        override fun utførRequest(httpMethod: HttpMethod, mediaType: MediaType, requestUrl: URI, requestBody: Any?): ResponseEntity<String>{
            if(requestUrl.path.matches(Regex(".+create-markdown"))){
                assert(mediaType== MediaType.APPLICATION_JSON)
                assert(requestBody is String)
                val mapper= ObjectMapper()
                val felter= mapper.readValue(requestBody as String, Map::class.java)
                assert(felter.contains("belop"))
                assert(felter.contains("startDato"))
                assert(felter.contains("begrunnelse"))
                assert(felter.contains("etterbetaling"))
                assert(felter.contains("antallTimer"))
                assert(felter.contains("stotteProsent"))
                assert(felter.contains("enhet"))
                assert(felter.contains("saksbehandler"))
           }else if(requestUrl.path.matches(Regex(".+to-html"))){
                assert(mediaType== MediaType.TEXT_MARKDOWN)
                assert(requestBody is String)
            }else{
                fail("Invalid URI")
            }

            return ResponseEntity<String>("mockup_response", HttpStatus.OK)
        }
    }

    class DokGenUtførRequestTestService: DokGenService("mock_dokgen_uri", RestTemplate()){
        fun testUtførRequest(): ResponseEntity<String> {
            return utførRequest(HttpMethod.POST, MediaType.TEXT_MARKDOWN, URI.create("mock_dokgen_uri"), "");
        }
    }

    class DokGenTestNullBodyService: DokGenService("mock_dokgen_uri", RestTemplate()){
        override fun utførRequest(httpMethod: HttpMethod, mediaType: MediaType, requestUrl: URI, requestBody: Any?): ResponseEntity<String>{
            return ResponseEntity<String>(null, HttpStatus.OK)
        }
    }

    @Test
    @Tag("integration")
    fun `Test generer markdown`(){
        val dokgen= DokGenTestService()
        val markdown= dokgen.hentStønadBrevMarkdown(BehandlingVedtak(
                id= 1,
                behandling= Behandling(
                        id= 1,
                        fagsak = Fagsak(),
                        journalpostID = "invalid",
                        type= BehandlingType.FØRSTEGANGSBEHANDLING,
                        saksnummer = null,
                        aktiv = true
                ),
                ansvarligSaksbehandler = "whoknows",
                vedtaksdato = LocalDate.MIN,
                stønadFom = LocalDate.MIN,
                stønadTom = LocalDate.MIN
        ))

        assert(markdown.equals("mockup_response"))
    }

    @Test
    @Tag("integration")
    fun `Test generer html`(){
        val dokgen= DokGenTestService()
        val html= dokgen.lagHtmlFraMarkdown("markdown")
        assert(html.equals("mockup_response"))
    }

    @Test
    @Tag("integration")
    fun `Test utførRequest`(){
        val dokgen= DokGenUtførRequestTestService()
        assertThrows(Exception::class.java){
          dokgen.testUtførRequest()
        }
    }

    @Test
    @Tag("integration")
    fun `Test null response`(){
        val dokgen= DokGenTestNullBodyService()
        val html= dokgen.lagHtmlFraMarkdown("markdown")
        assert(html.isEmpty())

        val markdown= dokgen.hentStønadBrevMarkdown(BehandlingVedtak(
                id= 1,
                behandling= Behandling(
                        id= 1,
                        fagsak = Fagsak(),
                        journalpostID = "invalid",
                        type= BehandlingType.FØRSTEGANGSBEHANDLING,
                        saksnummer = null,
                        aktiv = true
                ),
                ansvarligSaksbehandler = "whoknows",
                vedtaksdato = LocalDate.MIN,
                stønadFom = LocalDate.MIN,
                stønadTom = LocalDate.MIN
        ))
        assert(markdown.isEmpty())
    }

}