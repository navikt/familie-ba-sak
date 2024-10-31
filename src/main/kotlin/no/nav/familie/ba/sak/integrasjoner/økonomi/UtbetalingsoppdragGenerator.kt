package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class UtbetalingsoppdragGenerator(
    private val utbetalingsgenerator: Utbetalingsgenerator,
    private val justerUtbetalingsoppdragService: JusterUtbetalingsoppdragService,
    private val unleashNextMedContextService: UnleashNextMedContextService,
    private val behandlingsinformasjonUtleder: BehandlingsinformasjonUtleder,
) {
    fun lagUtbetalingsoppdrag(
        saksbehandlerId: String,
        vedtak: Vedtak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        nyTilkjentYtelse: TilkjentYtelse,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        erSimulering: Boolean,
        endretMigreringsDato: YearMonth? = null,
    ): BeregnetUtbetalingsoppdragLongId {
        val beregnetUtbetalingsoppdrag =
            utbetalingsgenerator.lagUtbetalingsoppdrag(
                behandlingsinformasjon =
                    behandlingsinformasjonUtleder.utled(
                        saksbehandlerId,
                        vedtak,
                        forrigeTilkjentYtelse,
                        sisteAndelPerKjede,
                        erSimulering,
                        endretMigreringsDato,
                    ),
                forrigeAndeler = forrigeTilkjentYtelse?.tilAndelData() ?: emptyList(),
                nyeAndeler = nyTilkjentYtelse.tilAndelData(),
                sisteAndelPerKjede = sisteAndelPerKjede.mapValues { it.value.tilAndelDataLongId() },
            )
        return justerUtbetalingsoppdragService.justerBeregnetUtbetalingsoppdragVedBehov(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            behandling = vedtak.behandling,
        )
    }

    private fun TilkjentYtelse.tilAndelData(): List<AndelDataLongId> =
        this.andelerTilkjentYtelse.map { it.tilAndelDataLongId() }

    private fun AndelTilkjentYtelse.tilAndelDataLongId(): AndelDataLongId {
        // Skrur på ny klassekode for enkelte fagsaker til å begynne med.
        val skalBrukeNyKlassekodeForUtvidetBarnetrygd =
            unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = this.behandlingId,
            )
        return AndelDataLongId(
            id = id,
            fom = periode.fom,
            tom = periode.tom,
            beløp = kalkulertUtbetalingsbeløp,
            personIdent = aktør.aktivFødselsnummer(),
            type = type.tilYtelseType(skalBrukeNyKlassekodeForUtvidetBarnetrygd),
            periodeId = periodeOffset,
            forrigePeriodeId = forrigePeriodeOffset,
            kildeBehandlingId = kildeBehandlingId,
        )
    }
}
