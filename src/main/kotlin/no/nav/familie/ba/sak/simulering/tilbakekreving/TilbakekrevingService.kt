package no.nav.familie.ba.sak.simulering.tilbakekreving

import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import org.springframework.stereotype.Service

@Service
class TilbakekrevingService(
        private val tilbakekrevingRepository: TilbakekrevingRepository,
        private val vedtakRepository: VedtakRepository,
) {

    fun lagreTilbakekreving(restTilbakekreving: RestTilbakekreving): Tilbakekreving {
        val vedtak = vedtakRepository.finnVedtak(restTilbakekreving.vedtakId)
        val tidligereTilbakekreving = tilbakekrevingRepository.findByVedtakId(vedtak.id)

        if (tidligereTilbakekreving != null) {
            tidligereTilbakekreving.begrunnelse = restTilbakekreving.begrunnelse
            tidligereTilbakekreving.valg = restTilbakekreving.valg
            tidligereTilbakekreving.varsel = restTilbakekreving.varsel
            return tilbakekrevingRepository.save(tidligereTilbakekreving)
        } else {
            return tilbakekrevingRepository.save(Tilbakekreving(
                    begrunnelse = restTilbakekreving.begrunnelse,
                    vedtak = vedtak,
                    valg = restTilbakekreving.valg,
                    varsel = restTilbakekreving.varsel,
                    tilbakekrevingsbehandlingId = null,
            ))
        }
    }
}