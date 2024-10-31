package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import org.springframework.stereotype.Service

@Service
class JusterUtbetalingsoppdragService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val unleashNextMedContextService: UnleashNextMedContextService,
) {
    fun justerBeregnetUtbetalingsoppdragVedBehov(
        beregnetUtbetalingsoppdrag: BeregnetUtbetalingsoppdragLongId,
        behandling: Behandling,
    ): BeregnetUtbetalingsoppdragLongId {
        // For fagsaker vi ikke har skrudd på ny klassekode for, returnerer vi det originale utbetalingsoppdraget.
        if (!unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = behandling.id,
            )
        ) {
            return beregnetUtbetalingsoppdrag
        }
        val erFagsakOverPåNyKlassekodeForUtvidetBarnetrygd = tilkjentYtelseRepository.fagsakHarTattIBrukNyKlassekodeForUtvidetBarnetrygd(fagsakId = behandling.fagsak.id)
        // Fagsak er over på ny klassekode for utvidet barnetrygd. Trenger ikke gjøre noen justeringer.
        if (erFagsakOverPåNyKlassekodeForUtvidetBarnetrygd) return beregnetUtbetalingsoppdrag

        val utvidetBarnetrygdErOpphørt = beregnetUtbetalingsoppdrag.utbetalingsoppdrag.utbetalingsperiode.any { it.opphør != null && it.klassifisering == YtelsetypeBA.UTVIDET_BARNETRYGD.klassifisering }
        // Fagsak er ikke over på ny klassekode for utvidet barnetrygd, men det finnes heller ikke noe opphør på en utvidet-kjede. Trenger ikke gjøre noen justeringer.
        if (!utvidetBarnetrygdErOpphørt) return beregnetUtbetalingsoppdrag

        // Når fagsak ikke er over på ny klassekode for utvidet barnetrygd og vi opphører en utvidet kjede, må vi bruke gammel klassekode.
        return beregnetUtbetalingsoppdrag.copy(
            utbetalingsoppdrag =
                beregnetUtbetalingsoppdrag.utbetalingsoppdrag.copy(
                    utbetalingsperiode =
                        beregnetUtbetalingsoppdrag.utbetalingsoppdrag.utbetalingsperiode.map {
                            if (it.opphør != null && it.klassifisering == YtelsetypeBA.UTVIDET_BARNETRYGD.klassifisering) {
                                it.copy(klassifisering = YtelsetypeBA.UTVIDET_BARNETRYGD_GAMMEL.klassifisering)
                            } else {
                                it
                            }
                        },
                ),
        )
    }
}
