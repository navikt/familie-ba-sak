package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class DokGenService(
    private val dokgenKlient: DokGenKlient
) {
    fun hentOgSettStønadBrevMarkdown(behandlingVedtak: BehandlingVedtak) {
        val fletteFelter = mapTilBrevfelter(behandlingVedtak)
        behandlingVedtak.stønadBrevMarkdown =  dokgenKlient.hentMarkdownForMal("Innvilget", fletteFelter)
    }

    private fun mapTilBrevfelter(vedtak: BehandlingVedtak): String {
        val datoFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val brevfelter = "{\"belop\": %s,\n" + // TODO
            "\"startDato\": \"%s\",\n" +
            "\"begrunnelse\": \"%s\",\n" +
            "\"etterbetaling\": %s,\n" +
            "\"antallTimer\": %s,\n" +
            "\"stotteProsent\": %s,\n" +
            "\"enhet\": \"%s\",\n" +
            "\"saksbehandler\": \"%s\"}"

        return String.format(
            brevfelter, 123, vedtak.stønadFom.format(datoFormat), "begrunnelse", false, 1, 100, "enhet", vedtak.ansvarligSaksbehandler
        )
    }
}