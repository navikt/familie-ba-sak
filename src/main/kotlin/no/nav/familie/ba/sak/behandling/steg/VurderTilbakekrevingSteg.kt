package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.tilbakekreving.RestTilbakekreving
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VurderTilbakekrevingSteg(val featureToggleService: FeatureToggleService, val tilbakekrevingService: TilbakekrevingService) :
        BehandlingSteg<RestTilbakekreving?> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling, restTilbakekreving: RestTilbakekreving?): StegType {
        
        if (featureToggleService.isEnabled(FeatureToggleConfig.TILBAKEKREVING) &&
            !tilbakekrevingService.søkerHarÅpenTilbakekreving(behandling.fagsak.id)) {

            tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id)
            if (restTilbakekreving != null) {
                tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id)
            }
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType = StegType.VURDER_TILBAKEKREVING
}