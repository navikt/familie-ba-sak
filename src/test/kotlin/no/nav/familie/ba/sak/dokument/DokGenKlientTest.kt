package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Disabled("DokGen must be available")
class DokGenKlientTest(@Autowired
                       private val dokumentService: DokumentService,
                       @Autowired
                       private val dokGenKlient: DokGenKlient) {

    private val vedtak = Vedtak(
            behandling = Behandling(fagsak = Fagsak(personIdent = PersonIdent(
                    "12345678910"), aktørId = AktørId("1")),
                                    journalpostID = "",
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    kategori = BehandlingKategori.NASJONAL,
                                    underkategori = BehandlingUnderkategori.ORDINÆR),
            ansvarligSaksbehandler = "ansvarligSaksbehandler",
            vedtaksdato = LocalDate.now()
    )

    private val avslagVedtak = Vedtak(
            behandling = Behandling(fagsak = Fagsak(personIdent = PersonIdent(
                    "12345678910"), aktørId = AktørId("1")),
                                    journalpostID = "",
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    kategori = BehandlingKategori.NASJONAL,
                                    underkategori = BehandlingUnderkategori.ORDINÆR),
            ansvarligSaksbehandler = "ansvarligSaksbehandler",
            vedtaksdato = LocalDate.now()
    )

    private val opphørtVedtak = Vedtak(
            behandling = Behandling(fagsak = Fagsak(personIdent = PersonIdent(
                    "12345678910"), aktørId = AktørId("1")),
                                    journalpostID = "",
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    kategori = BehandlingKategori.NASJONAL,
                                    underkategori = BehandlingUnderkategori.ORDINÆR),
            ansvarligSaksbehandler = "ansvarligSaksbehandler",
            vedtaksdato = LocalDate.now()
    )

    @Test
    fun `Test å hente Markdown og konvertere til html når dokgen kjører lokalt`() {
        val markdown = dokumentService.hentStønadBrevMarkdown(vedtak = vedtak,
                                                              behandlingResultatType = BehandlingResultatType.INNVILGET
        )
        val htmlResponse = dokGenKlient.lagHtmlFraMarkdown(BehandlingResultatType.INNVILGET.brevMal, markdown)
        assert(htmlResponse.startsWith("<html>"))
    }

    @Test
    fun `Test å generer Markdown for avslag brev`() {
        val markdown = dokumentService.hentStønadBrevMarkdown(vedtak = avslagVedtak,
                                                              behandlingResultatType = BehandlingResultatType.AVSLÅTT)
        assert(markdown.startsWith("<br>Du har ikke rett til barnetrygd fordi ."))
    }

    @Test
    fun `Test å generer Markdown for opphørt brev`() {
        val markdown = dokumentService.hentStønadBrevMarkdown(vedtak = opphørtVedtak,
                                                              behandlingResultatType = BehandlingResultatType.OPPHØRT)
        assert(markdown.startsWith("<br>Barnetrygden din stanses fra"))
    }
}
