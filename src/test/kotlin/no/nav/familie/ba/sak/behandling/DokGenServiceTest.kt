package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
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
class DokGenServiceTest(@Autowired
                        private val dokGenService: DokGenService) {

    private val vedtak = Vedtak(
            behandling = Behandling(fagsak = Fagsak(personIdent = PersonIdent("12345678910")),
                                    journalpostID = "",
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING),
            ansvarligSaksbehandler = "ansvarligSaksbehandler",
            vedtaksdato = LocalDate.now(),
            resultat = VedtakResultat.INNVILGET
    )

    private val avslagVedtak= Vedtak(
            behandling = Behandling(fagsak = Fagsak(personIdent = PersonIdent("12345678910")),
                                    journalpostID = "",
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING),
            ansvarligSaksbehandler = "ansvarligSaksbehandler",
            vedtaksdato = LocalDate.now(),
            resultat = VedtakResultat.AVSLÅTT
    )

    @Test
    fun `Test å hente Markdown og konvertere til html når dokgen kjører lokalt`() {
        val markdown= dokGenService.hentStønadBrevMarkdown(vedtak)
        val htmlResponse= dokGenService.lagHtmlFraMarkdown(markdown)
        assert(htmlResponse.startsWith("<html>"))
    }

    @Test
    fun `Test å generer Markdown for avslag brev`(){
        val markdown= dokGenService.hentStønadBrevMarkdown(avslagVedtak)
        assert(markdown.startsWith("<br>Du har ikke rett til barnetrygd fordi ."))
    }
}
