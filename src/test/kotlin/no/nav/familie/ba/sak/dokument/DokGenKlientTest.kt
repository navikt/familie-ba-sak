package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
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
                       private val dokGenKlient: DokGenKlient) {

    private val vedtak = Vedtak(
            behandling = Behandling(fagsak = Fagsak(personIdent = PersonIdent("12345678910"), aktørId = AktørId("1")),
                                    journalpostID = "",
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    kategori = BehandlingKategori.NASJONAL,
                                    underkategori = BehandlingUnderkategori.ORDINÆR,
                                    resultat = BehandlingResultat.INNVILGET),
            ansvarligSaksbehandler = "ansvarligSaksbehandler",
            vedtaksdato = LocalDate.now()
    )

    private val avslagVedtak = Vedtak(
            behandling = Behandling(fagsak = Fagsak(personIdent = PersonIdent("12345678910"), aktørId = AktørId("1")),
                                    journalpostID = "",
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    kategori = BehandlingKategori.NASJONAL,
                                    underkategori = BehandlingUnderkategori.ORDINÆR,
                                    resultat = BehandlingResultat.AVSLÅTT),
            ansvarligSaksbehandler = "ansvarligSaksbehandler",
            vedtaksdato = LocalDate.now()
    )

    private val opphørtVedtak= Vedtak(
            behandling = Behandling(fagsak = Fagsak(personIdent = PersonIdent("12345678910"), aktørId = AktørId("1")),
                                    journalpostID = "",
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    kategori = BehandlingKategori.NASJONAL,
                                    underkategori = BehandlingUnderkategori.ORDINÆR),
            ansvarligSaksbehandler = "ansvarligSaksbehandler",
            vedtaksdato = LocalDate.now(),
            resultat = VedtakResultat.OPPHØRT,
            begrunnelse = ""
    )

    @Test
    fun `Test å hente Markdown og konvertere til html når dokgen kjører lokalt`() {
        val markdown = dokGenKlient.hentStønadBrevMarkdown(vedtak)
        val htmlResponse = dokGenKlient.lagHtmlFraMarkdown(vedtak.behandling.resultat.toDokGenTemplate(), markdown)
        assert(htmlResponse.startsWith("<html>"))
    }

    @Test
    fun `Test å generer Markdown for avslag brev`() {
        val markdown = dokGenKlient.hentStønadBrevMarkdown(avslagVedtak)
        assert(markdown.startsWith("<br>Du har ikke rett til barnetrygd fordi ."))
    }

    @Test
    fun `Test å generer Markdown for opphørt brev`(){
        val markdown= dokGenKlient.hentStønadBrevMarkdown(opphørtVedtak)
        assert(markdown.startsWith("<br>Barnetrygden din stanses fra"))
    }
}
