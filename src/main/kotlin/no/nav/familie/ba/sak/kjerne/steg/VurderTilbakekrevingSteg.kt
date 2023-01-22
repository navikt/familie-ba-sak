package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.vedtakSimuleringMottakereTilSimuleringPerioder
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class VurderTilbakekrevingSteg(
    val featureToggleService: FeatureToggleService,
    val tilbakekrevingService: TilbakekrevingService,
    val simuleringService: SimuleringService
) : BehandlingSteg<RestTilbakekreving?> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling, data: RestTilbakekreving?): StegType {
        if (!tilbakekrevingService.søkerHarÅpenTilbakekreving(behandling.fagsak.id)) {
            tilbakekrevingService.validerRestTilbakekreving(data, behandling.id)
            if (data != null) {
                tilbakekrevingService.lagreTilbakekreving(data, behandling.id)
            }
        }

        if (!featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING)) {
            val finnesFeilutbetaling = simuleringService.hentFeilutbetaling(behandling.id) != BigDecimal.ZERO
            when {
                featureToggleService.isEnabled(FeatureToggleConfig.ENDRINGER_I_VALIDERING_FOR_MIGRERINGSBEHANDLING) -> {
                    // manuelle migreringer kan ikke fortsettes om det finnes en feilutbetaling
                    // eller en etterbetaling større enn 220 KR
                    validerNårToggelenErPå(behandling, finnesFeilutbetaling)
                }
                else -> {
                    validerNårToggelenErAv(behandling, finnesFeilutbetaling)
                }
            }
        }
        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun validerNårToggelenErPå(behandling: Behandling, finnesFeilutbetaling: Boolean) {
        if (behandling.erManuellMigrering() && (
            finnesFeilutbetaling || finnesPerioderMedEtterbetalingStørreEnnMaksBeløp(behandling.id)
            )
        ) {
            kastException(behandling)
        }
    }

    private fun validerNårToggelenErAv(behandling: Behandling, finnesFeilutbetaling: Boolean) {
        val finnesEtterbetaling = simuleringService.hentEtterbetaling(behandling.id) != BigDecimal.ZERO
        when {
            behandling.erManuellMigreringForEndreMigreringsdato() &&
                (finnesFeilutbetaling || finnesEtterbetaling) -> kastException(behandling)
            behandling.erHelmanuellMigrering() && (
                finnesFeilutbetaling ||
                    finnesPerioderMedEtterbetalingStørreEnnMaksBeløp(behandling.id)
                ) -> kastException(behandling)
        }
    }

    private fun kastException(behandling: Behandling) {
        throw FunksjonellFeil(
            frontendFeilmelding = "Utbetalingen må være lik utbetalingen i Infotrygd. " +
                "Du må tilbake og gjøre nødvendige endringer for å komme videre i behandlingen",
            melding = "Migreringsbehandling med årsak ${behandling.opprettetÅrsak.visningsnavn} kan ikke fortsette " +
                "når det finnes feilutbetaling/etterbetaling"
        )
    }

    private fun finnesPerioderMedEtterbetalingStørreEnnMaksBeløp(behandlinId: Long): Boolean {
        val simuleringMottaker = simuleringService.hentSimuleringPåBehandling(behandlinId)
        val simuleringPerioder = vedtakSimuleringMottakereTilSimuleringPerioder(
            simuleringMottaker,
            featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ)
        )
        return simuleringPerioder.any { it.etterbetaling > BigDecimal(HELMANUELL_MIGRERING_MAKS_ETTERBETALING) }
    }

    companion object {
        const val HELMANUELL_MIGRERING_MAKS_ETTERBETALING = 220
    }

    override fun stegType(): StegType = StegType.VURDER_TILBAKEKREVING
}
