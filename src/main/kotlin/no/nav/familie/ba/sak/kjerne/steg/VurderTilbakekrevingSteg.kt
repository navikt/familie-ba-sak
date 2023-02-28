package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.IKKE_STOPP_MIGRERINGSBEHANDLING
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.domene.SimuleringsPeriode
import no.nav.familie.ba.sak.kjerne.simulering.hentTotalEtterbetaling
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
    val simuleringService: SimuleringService,
    val persongrunnlagService: PersongrunnlagService
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
            val antallBarn = persongrunnlagService.hentBarna(behandling.id).size

            validerEtterbetalingForManuellMigrering(behandling, antallBarn)
            validerFeilutbetalingForManuellMigrering(behandling, antallBarn)
        }
        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun validerEtterbetalingForManuellMigrering(behandling: Behandling, antallBarn: Int) {
        val finnesEtterBetaling = hentTotalEtterbetalingFørMars2023(behandling.id) != BigDecimal.ZERO
        if (!finnesEtterBetaling) return

        when {
            behandling.erHelmanuellMigrering() -> {
                // manuelle migreringer kan ikke fortsettes om det finnes en etterbetaling
                // større enn 220 KR
                if (!finnesPerioderMedEtterbetalingStørreEnnMaksBeløp(behandlinId = behandling.id)) return
            }
            else -> {
                val simuleringsperioderFørMars2023 = hentSimuleringsperioderFørMars2023(behandling.id)
                if (
                    simuleringsperioderFørMars2023.harKunPositiveResultater() &&
                    simuleringsperioderFørMars2023.harMaks1KroneIResultatPerBarn(antallBarn) &&
                    simuleringsperioderFørMars2023.harTotaltAvvikUnderBeløpsgrense()
                ) {
                    return
                }
            }
        }
        kastException(behandling)
    }

    private fun validerFeilutbetalingForManuellMigrering(behandling: Behandling, antallBarn: Int) {
        val finnesFeilutbetaling = simuleringService.hentFeilutbetaling(behandling.id) != BigDecimal.ZERO
        if (!finnesFeilutbetaling) return

        val simuleringsperioderFørMars2023 = hentSimuleringsperioderFørMars2023(behandling.id)
        if (
            simuleringsperioderFørMars2023.harKunNegativeResultater() &&
            simuleringsperioderFørMars2023.harMaks1KroneIResultatPerBarn(antallBarn) &&
            simuleringsperioderFørMars2023.harTotaltAvvikUnderBeløpsgrense()
        ) {
            return
        }
        kastException(behandling)
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
        val simuleringPerioder = hentSimuleringsperioderFørMars2023(behandlinId)
        return simuleringPerioder.any { it.etterbetaling > BigDecimal(HELMANUELL_MIGRERING_MAKS_ETTERBETALING_PER_PERIODE) }
    }

    private fun hentSimuleringsperioderFørMars2023(behandlingId: Long): List<SimuleringsPeriode> {
        return vedtakSimuleringMottakereTilSimuleringPerioder(
            økonomiSimuleringMottakere = simuleringService.hentSimuleringPåBehandling(behandlingId),
            erManuelPosteringTogglePå = featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ)
        ).filter { it.fom.isSameOrBefore(februar2023) }
    }

    private fun hentTotalEtterbetalingFørMars2023(behandlingId: Long) =
        hentTotalEtterbetaling(hentSimuleringsperioderFørMars2023(behandlingId), null)

    private fun List<SimuleringsPeriode>.harKunPositiveResultater(): Boolean {
        return all { it.resultat >= BigDecimal.ZERO }
    }

    private fun List<SimuleringsPeriode>.harKunNegativeResultater(): Boolean {
        return all { it.resultat <= BigDecimal.ZERO }
    }

    private fun List<SimuleringsPeriode>.harMaks1KroneIResultatPerBarn(antallBarn: Int): Boolean {
        return all { it.resultat.abs() <= BigDecimal(antallBarn) }
    }

    private fun List<SimuleringsPeriode>.harTotaltAvvikUnderBeløpsgrense(): Boolean {
        return sumOf { it.resultat }.abs() < BigDecimal(MANUELL_MIGRERING_BELØPSGRENSE_FOR_TOTALT_AVVIK)
    }

    companion object {
        const val HELMANUELL_MIGRERING_MAKS_ETTERBETALING_PER_PERIODE = 220
        const val MANUELL_MIGRERING_BELØPSGRENSE_FOR_TOTALT_AVVIK = 100
        val februar2023 = LocalDate.of(2023, 2, 1)
    }

    override fun stegType(): StegType = StegType.VURDER_TILBAKEKREVING
}
