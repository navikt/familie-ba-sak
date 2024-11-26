package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import org.springframework.stereotype.Component

@Component
class UtbetalingsoppdragGenerator(
    private val utbetalingsgenerator: Utbetalingsgenerator,
    private val klassifiseringKorrigerer: KlassifiseringKorrigerer,
    private val unleashNextMedContextService: UnleashNextMedContextService,
    private val behandlingsinformasjonUtleder: BehandlingsinformasjonUtleder,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val andelDataForNyUtvidetKlassekodeBehandlingUtleder: AndelDataForNyUtvidetKlassekodeBehandlingUtleder,
) {
    fun lagUtbetalingsoppdrag(
        saksbehandlerId: String,
        vedtak: Vedtak,
        tilkjentYtelse: TilkjentYtelse,
        erSimulering: Boolean = false,
    ): BeregnetUtbetalingsoppdragLongId {
        val forrigeTilkjentYtelse = hentForrigeTilkjentYtelse(vedtak.behandling)
        val sisteAndelPerKjede = hentSisteAndelTilkjentYtelse(vedtak.behandling)

        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId,
                vedtak,
                forrigeTilkjentYtelse,
                sisteAndelPerKjede,
                erSimulering,
            )

        val skalBrukeNyKlassekodeForUtvidetBarnetrygd =
            unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = vedtak.behandling.id,
            )

        val forrigeAndler =
            if (forrigeTilkjentYtelse == null) {
                emptyList()
            } else if (vedtak.behandling.opprettetÅrsak != BehandlingÅrsak.NY_UTVIDET_KLASSEKODE) {
                forrigeTilkjentYtelse.tilAndelData(skalBrukeNyKlassekodeForUtvidetBarnetrygd) ?: emptyList()
            } else {
                andelDataForNyUtvidetKlassekodeBehandlingUtleder.finnForrigeAndelerForNyUtvidetKlassekodeBehandling(forrigeTilkjentYtelse, skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            }

        val nyeAndeler =
            if (vedtak.behandling.opprettetÅrsak != BehandlingÅrsak.NY_UTVIDET_KLASSEKODE) {
                tilkjentYtelse.tilAndelData(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            } else {
                andelDataForNyUtvidetKlassekodeBehandlingUtleder.finnNyeAndelerForNyUtvidetKlassekodeBehandling(tilkjentYtelse, skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            }

        val beregnetUtbetalingsoppdrag =
            utbetalingsgenerator.lagUtbetalingsoppdrag(
                behandlingsinformasjon = behandlingsinformasjon,
                forrigeAndeler = forrigeAndler,
                nyeAndeler = nyeAndeler,
                sisteAndelPerKjede = sisteAndelPerKjede.mapValues { it.value.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd) },
            )

        return klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            behandling = vedtak.behandling,
        )
    }

    private fun hentSisteAndelTilkjentYtelse(behandling: Behandling): Map<IdentOgType, AndelTilkjentYtelse> {
        val skalBrukeNyKlassekodeForUtvidetBarnetrygd =
            unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = behandling.id,
            )
        return andelTilkjentYtelseRepository
            .hentSisteAndelPerIdentOgType(fagsakId = behandling.fagsak.id)
            .associateBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type.tilYtelseType(skalBrukeNyKlassekodeForUtvidetBarnetrygd)) }
    }

    private fun hentForrigeTilkjentYtelse(behandling: Behandling): TilkjentYtelse? =
        behandlingHentOgPersisterService
            .hentForrigeBehandlingSomErIverksatt(behandling = behandling)
            ?.let { tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandlingId = it.id) }
}

fun TilkjentYtelse.tilAndelData(skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean): List<AndelDataLongId> =
    this.andelerTilkjentYtelse.map { it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd) }

fun AndelTilkjentYtelse.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean): AndelDataLongId {
    // Skrur på ny klassekode for enkelte fagsaker til å begynne med.
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
