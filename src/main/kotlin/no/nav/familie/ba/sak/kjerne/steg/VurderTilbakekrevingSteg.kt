package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VurderTilbakekrevingSteg(
    val tilbakekrevingService: TilbakekrevingService,
    val simuleringService: SimuleringService,
    val unleashService: UnleashNextMedContextService,
) : BehandlingSteg<RestTilbakekreving?> {
    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RestTilbakekreving?,
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
