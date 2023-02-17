package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.ENDRINGER_I_VALIDERING_FOR_MIGRERINGSBEHANDLING
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.IKKE_STOPP_MIGRERINGSBEHANDLING
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.MIGRERING_MED_FEILUTBETALING_UNDER_BELØPSGRENSE
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.vedtakSimuleringMottakereTilSimuleringPerioder
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

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

        if (behandling.erManuellMigrering() && !featureToggleService.isEnabled(IKKE_STOPP_MIGRERINGSBEHANDLING)) {
            validerEtterbetalingForManuellMigrering(behandling)
            validerFeilutbetalingForManuellMigrering(behandling)
        }
        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun validerEtterbetalingForManuellMigrering(behandling: Behandling) {
        val beløpsgrense = when {
            behandling.erHelmanuellMigrering() || featureToggleService.isEnabled(ENDRINGER_I_VALIDERING_FOR_MIGRERINGSBEHANDLING)
            -> HELMANUELL_MIGRERING_MAKS_ETTERBETALING
            else -> 0
        }
        if (finnesEtterbetalingStørreEnnBeløpsgrense(behandling.id, beløpsgrense)) kastException(behandling)
    }

    private fun validerFeilutbetalingForManuellMigrering(behandling: Behandling) {
        val finnesFeilutbetaling = simuleringService.hentFeilutbetaling(behandling.id) != BigDecimal.ZERO
        if (!finnesFeilutbetaling) return

        when {
            featureToggleService.isEnabled(MIGRERING_MED_FEILUTBETALING_UNDER_BELØPSGRENSE, true) &&
                erNegativePerioderesultaterPåMaks1KroneOgTotalFeilutbetalingMindreEnnBeløpsgrense(behandling.id) -> return
            else -> kastException(behandling)
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

    private fun finnesEtterbetalingStørreEnnBeløpsgrense(behandlinId: Long, beløpsgrense: Int): Boolean {
        val simuleringPerioder = vedtakSimuleringMottakereTilSimuleringPerioder(
            økonomiSimuleringMottakere = simuleringService.hentSimuleringPåBehandling(behandlinId),
            erManuelPosteringTogglePå = featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ),
            erMigreringsbehandling = false // ikke relevant for etterbetaling
        ).filter { it.fom.isSameOrBefore(februar2023) }

        return if (beløpsgrense == HELMANUELL_MIGRERING_MAKS_ETTERBETALING) {
            simuleringPerioder.any { it.etterbetaling > BigDecimal(HELMANUELL_MIGRERING_MAKS_ETTERBETALING) }
        } else {
            simuleringPerioder.sumOf { it.etterbetaling } > BigDecimal(beløpsgrense)
        }
    }

    private fun erNegativePerioderesultaterPåMaks1KroneOgTotalFeilutbetalingMindreEnnBeløpsgrense(behandlingId: Long): Boolean {
        val simuleringPerioder = vedtakSimuleringMottakereTilSimuleringPerioder(
            økonomiSimuleringMottakere = simuleringService.hentSimuleringPåBehandling(behandlingId),
            erManuelPosteringTogglePå = featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ),
            erMigreringsbehandling = false // ikke relevant for feilutbetaling
        ).filter { it.fom.isSameOrBefore(februar2023) }
        return simuleringPerioder.all { it.resultat <= BigDecimal.ZERO && it.resultat >= BigDecimal(-1) } &&
            simuleringService.hentFeilutbetaling(behandlingId) < BigDecimal(HELMANUELL_MIGRERING_FEILUTBETALING_BELØPSGRENSE)
    }

    companion object {
        const val HELMANUELL_MIGRERING_MAKS_ETTERBETALING = 220
        const val HELMANUELL_MIGRERING_FEILUTBETALING_BELØPSGRENSE = 100
        val februar2023 = LocalDate.of(2023, 2, 1)
    }

    override fun stegType(): StegType = StegType.VURDER_TILBAKEKREVING
}
