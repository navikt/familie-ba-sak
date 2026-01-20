package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingsbehandlingDto
import org.springframework.stereotype.Service

@Service
class TilbakekrevingsbehandlingService(
    private val tilbakekrevingKlient: TilbakekrevingKlient,
) {
    fun hentTilbakekrevingsbehandlingerDto(fagsakId: Long): List<TilbakekrevingsbehandlingDto> {
        val behandlinger = tilbakekrevingKlient.hentTilbakekrevingsbehandlinger(fagsakId)
        return behandlinger.map {
            TilbakekrevingsbehandlingDto(
                behandlingId = it.behandlingId,
                opprettetTidspunkt = it.opprettetTidspunkt,
                aktiv = it.aktiv,
                årsak = it.årsak,
                type = it.type,
                status = it.status,
                resultat = it.resultat,
                vedtaksdato = it.vedtaksdato,
            )
        }
    }
}
