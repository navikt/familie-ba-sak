package no.nav.familie.ba.sak.simulering.tilbakekkreving

import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import org.springframework.stereotype.Service

@Service
class TilbakekrevingService(
        private val tilbakekrevingRepository: TilbakekrevingRepository,
        private val vedtakRepository: VedtakRepository,
) {

    fun lagreTilbakekreving(tilbakekrevingDto: TilbakekrevingDto): Tilbakekreving {
        val vedtak = vedtakRepository.finnVedtak(tilbakekrevingDto.vedtakId)
        val tilbakekreving = Tilbakekreving(
                beskrivelse = tilbakekrevingDto.beskrivelse,
                vedtak = vedtak,
                type = tilbakekrevingDto.type,
                varsel = tilbakekrevingDto.varsel,
        )
        return tilbakekrevingRepository.save(tilbakekreving)
    }
}