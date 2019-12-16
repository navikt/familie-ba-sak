package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.DokGenKlient
import no.nav.familie.ba.sak.behandling.DokGenService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DokGenKlientTest {

    private val dokgenKlient = DokGenKlient("http://localhost:8080")
    private val dokGenService = DokGenService(dokgenKlient)
    private val behandlingVedtak = BehandlingVedtak(
        behandling = Behandling(fagsak = Fagsak(), journalpostID = "", type = BehandlingType.FØRSTEGANGSBEHANDLING),
        ansvarligSaksbehandler = "ansvarligSaksbehandler",
        vedtaksdato = LocalDate.now(),
        stønadFom = LocalDate.now(),
        stønadTom = LocalDate.MAX
    )

    @Test
    fun `Test å hente Markdown og konvertere til html når dokgen kjører lokalt`() {
        dokgenKlient.runCatching {
            dokGenService.hentOgSettStønadBrevMarkdown(behandlingVedtak)
            val htmlResponse = lagHtmlFraMarkdown(behandlingVedtak.stønadBrevMarkdown)
            assert(htmlResponse.startsWith("<html>"))
        }
    }
}