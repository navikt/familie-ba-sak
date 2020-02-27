package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakResultat
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Tag("integration")
class DokGenIntegrationTest {

    class DokGenTestService : DokGenService("mock_dokgen_uri", RestTemplate()) {
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

    class DokGenTestNullBodyService : DokGenService("mock_dokgen_uri", RestTemplate()) {
        override fun <T : Any> utførRequest(request: RequestEntity<String>, responseType: Class<T>): ResponseEntity<T> {
            return ResponseEntity<T>(null, HttpStatus.OK)
        }
    }

    @Test
    @Tag("integration")
    fun `Test generer markdown`() {
        val dokgen = DokGenTestService()
        val markdown = dokgen.hentStønadBrevMarkdown(Vedtak(
                id = 1,
                behandling = Behandling(
                        id = 1,
                        fagsak = Fagsak(personIdent = PersonIdent(""), aktørId = AktørId("1")),
                        journalpostID = "invalid",
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                        aktiv = true,
                        kategori = BehandlingKategori.NASJONAL,
                        underkategori = BehandlingUnderkategori.ORDINÆR
                ),
                ansvarligSaksbehandler = "whoknows",
                vedtaksdato = LocalDate.MIN,
                resultat = VedtakResultat.INNVILGET,
                begrunnelse = ""
        ))

        assert(markdown == "mockup_response")
    }

    @Test
    @Tag("integration")
    fun `Test generer html`() {
        val dokgen = DokGenTestService()
        val html = dokgen.lagHtmlFraMarkdown("Innvilget", "markdown")
        assert(html == "<HTML><H1>Vedtaksbrev HTML (Mock)</H1></HTML>")
    }

    @Test
    @Tag("integration")
    fun `Test generer pdf`() {
        val dokgen = DokGenTestService()
        val pdf = dokgen.lagPdfFraMarkdown("Innvilget", "markdown")
        assert(pdf.contentEquals("Vedtaksbrev PDF".toByteArray()))
    }

    @Test
    @Tag("integration")
    fun `Test null response`() {
        val dokgen = DokGenTestNullBodyService()
        val html = dokgen.lagHtmlFraMarkdown("Innvilget", "markdown")
        assert(html.isEmpty())

        val markdown = dokgen.hentStønadBrevMarkdown(Vedtak(
                id = 1,
                behandling = Behandling(
                        id = 1,
                        fagsak = Fagsak(personIdent = PersonIdent(""), aktørId = AktørId("1")),
                        journalpostID = "invalid",
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                        aktiv = true,
                        kategori = BehandlingKategori.NASJONAL,
                        underkategori = BehandlingUnderkategori.ORDINÆR
                ),
                ansvarligSaksbehandler = "whoknows",
                vedtaksdato = LocalDate.MIN,
                resultat = VedtakResultat.INNVILGET,
                begrunnelse = ""
        ))
        assert(markdown.isEmpty())
    }

}
