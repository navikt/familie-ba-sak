package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

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
        if (unleashService.isEnabled(FeatureToggle.VALIDER_IKKE_AVREGNING)) {
            validerIkkeAvregning(behandling)
        }
        if (!tilbakekrevingService.søkerHarÅpenTilbakekreving(behandling.fagsak.id)) {
            tilbakekrevingService.validerRestTilbakekreving(data, behandling.id)
            if (data != null) {
                tilbakekrevingService.lagreTilbakekreving(data, behandling.id)
            }
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType = StegType.VURDER_TILBAKEKREVING

    private fun validerIkkeAvregning(behandling: Behandling) {
        val brukerHarEtterbetaling = simuleringService.hentEtterbetaling(behandling.id) > BigDecimal.ZERO
        if (brukerHarEtterbetaling) {
            val brukerHarFeilutbetaling = simuleringService.hentFeilutbetaling(behandling.id) > BigDecimal.ZERO

            val brukerHarÅpenTilbakekreving by lazy {
                tilbakekrevingService.søkerHarÅpenTilbakekreving(behandling.fagsak.id)
            }

            if (brukerHarFeilutbetaling || brukerHarÅpenTilbakekreving) {
                throw FunksjonellFeil(
                    melding =
                        "Løsningen i dag legger opp til automatisk avregning der feilutbetalinger trekkes mot etterbetalinger. " +
                            "Dette har vi ikke hjemmel for. Du må derfor splitte saken for å gå videre.",
                )
            }
        }
    }
}
