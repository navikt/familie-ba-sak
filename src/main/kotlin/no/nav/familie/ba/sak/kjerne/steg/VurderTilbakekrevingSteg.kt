package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VurderTilbakekrevingSteg(
    val tilbakekrevingService: TilbakekrevingService,
    val simuleringService: SimuleringService,
) : BehandlingSteg<TilbakekrevingDto?> {
    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: TilbakekrevingDto?,
    ): StegType {
        if (!tilbakekrevingService.søkerHarÅpenTilbakekreving(behandling.fagsak.id)) {
            tilbakekrevingService.validerRestTilbakekreving(data, behandling.id)
            if (data != null) {
                tilbakekrevingService.lagreTilbakekreving(data, behandling.id)
            }
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType = StegType.VURDER_TILBAKEKREVING
}
