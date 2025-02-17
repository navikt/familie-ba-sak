package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
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
    private val andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder: AndelDataForOppdaterUtvidetKlassekodeBehandlingUtleder,
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
                toggle = FeatureToggle.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = vedtak.behandling.id,
            )

        val forrigeAndeler =
            if (forrigeTilkjentYtelse == null) {
                emptyList()
            } else if (!vedtak.behandling.erOppdaterUtvidetKlassekode()) {
                forrigeTilkjentYtelse.tilAndelData(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            } else {
                andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder.finnForrigeAndelerForOppdaterUtvidetKlassekodeBehandling(forrigeTilkjentYtelse, skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            }

        val nyeAndeler = tilkjentYtelse.tilAndelData(skalBrukeNyKlassekodeForUtvidetBarnetrygd)

        val beregnetUtbetalingsoppdrag =
            utbetalingsgenerator.lagUtbetalingsoppdrag(
                behandlingsinformasjon = behandlingsinformasjon,
                forrigeAndeler = forrigeAndeler,
                nyeAndeler = nyeAndeler,
                sisteAndelPerKjede = sisteAndelPerKjede,
            )

        return klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            behandling = vedtak.behandling,
        )
    }

    private fun hentSisteAndelTilkjentYtelse(
        behandling: Behandling,
    ): Map<IdentOgType, AndelDataLongId> {
        val skalBrukeNyKlassekodeForUtvidetBarnetrygd =
            unleashNextMedContextService.isEnabled(
                toggle = FeatureToggle.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = behandling.id,
            )

        val sisteAndelPerKjede =
            andelTilkjentYtelseRepository
                .hentSisteAndelPerIdentOgType(fagsakId = behandling.fagsak.id)
                .associateBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type.tilYtelseType(skalBrukeNyKlassekodeForUtvidetBarnetrygd)) }

        val tilkjenteYtelserMedOppdatertUtvidetBarnetrygdKlassekodeIUtbetalingsoppdrag = tilkjentYtelseRepository.findByOppdatertUtvidetBarnetrygdKlassekodeIUtbetalingsoppdrag(behandling.fagsak.id)

        return if (tilkjenteYtelserMedOppdatertUtvidetBarnetrygdKlassekodeIUtbetalingsoppdrag.isNotEmpty() && unleashNextMedContextService.isEnabled(FeatureToggle.BRUK_OVERSTYRING_AV_FOM_SISTE_ANDEL_UTVIDET)) {
            SisteUtvidetAndelOverstyrer.overstyrSisteUtvidetBarnetrygdAndel(
                sisteAndelPerKjede = sisteAndelPerKjede,
                tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag = tilkjenteYtelserMedOppdatertUtvidetBarnetrygdKlassekodeIUtbetalingsoppdrag,
                skalBrukeNyKlassekodeForUtvidetBarnetrygd = skalBrukeNyKlassekodeForUtvidetBarnetrygd,
            )
        } else {
            sisteAndelPerKjede.mapValues { it.value.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd) }
        }
    }

    private fun hentForrigeTilkjentYtelse(behandling: Behandling): TilkjentYtelse? =
        behandlingHentOgPersisterService
            .hentForrigeBehandlingSomErIverksatt(behandling = behandling)
            ?.let { tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandlingId = it.id) }
}

fun TilkjentYtelse.tilAndelData(skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean): List<AndelDataLongId> = this.andelerTilkjentYtelse.map { it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd) }

fun AndelTilkjentYtelse.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean): AndelDataLongId =
    AndelDataLongId(
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
