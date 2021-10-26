package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.tilbakekreving.RestTilbakekrevingsVedtak
import no.nav.familie.ba.sak.tilbakekreving.RestTilbakekrevingsbehandling
import org.springframework.stereotype.Service

@Service
class TilbakekrevingsbehandlingService(private val tilbakekrevingKlient: TilbakekrevingKlient) {

    fun hentRestTilbakekrevingsbehandlinger(fagsakId: Long): List<RestTilbakekrevingsbehandling> {
        val behandlinger = tilbakekrevingKlient.hentTilbakekrevingsbehandlinger(fagsakId)
        return behandlinger.map {
            RestTilbakekrevingsbehandling(
                behandlingId = it.behandlingId,
                opprettetTidspunkt = it.opprettetTidspunkt,
                aktiv = it.aktiv,
                årsak = it.årsak,
                type = it.type,
                status = it.status,
                resultat = it.resultat,
                vedtakForBehandling = if (it.vedtaksdato != null) {
                    listOf(
                        RestTilbakekrevingsVedtak(
                            aktiv = true,
                            vedtaksdato = it.vedtaksdato!!
                        )
                    )
                } else emptyList(),
                vedtaksdato = it.vedtaksdato!!
            )
        }
    }
}
