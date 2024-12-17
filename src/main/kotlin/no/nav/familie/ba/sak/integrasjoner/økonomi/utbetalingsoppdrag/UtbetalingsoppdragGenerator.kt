package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.integrasjoner.pdl.logger
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
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.objectMapper
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
                toggleId = FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = vedtak.behandling.id,
            )

        val forrigeAndeler =
            if (forrigeTilkjentYtelse == null) {
                emptyList()
            } else if (vedtak.behandling.opprettetÅrsak != BehandlingÅrsak.OPPDATER_UTVIDET_KLASSEKODE) {
                forrigeTilkjentYtelse.tilAndelData(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            } else {
                andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder.finnForrigeAndelerForOppdaterUtvidetKlassekodeBehandling(forrigeTilkjentYtelse, skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            }

        val nyeAndeler =
            if (vedtak.behandling.opprettetÅrsak != BehandlingÅrsak.OPPDATER_UTVIDET_KLASSEKODE) {
                tilkjentYtelse.tilAndelData(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            } else {
                andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder.finnNyeAndelerForOppdaterUtvidetKlassekodeBehandling(tilkjentYtelse, skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            }

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
                toggleId = FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = behandling.id,
            )

        val sisteAndelPerKjede =
            andelTilkjentYtelseRepository
                .hentSisteAndelPerIdentOgType(fagsakId = behandling.fagsak.id)
                .associateBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type.tilYtelseType(skalBrukeNyKlassekodeForUtvidetBarnetrygd)) }

        return if (unleashNextMedContextService.isEnabled(FeatureToggleConfig.BRUK_OVERSTYRING_AV_FOM_SISTE_ANDEL_UTVIDET)) {
            overstyrSisteUtvidetBarnetrygdAndel(behandling, sisteAndelPerKjede, skalBrukeNyKlassekodeForUtvidetBarnetrygd)
        } else {
            sisteAndelPerKjede.mapValues { it.value.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd) }
        }
    }

    private fun overstyrSisteUtvidetBarnetrygdAndel(
        behandling: Behandling,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean,
    ): Map<IdentOgType, AndelDataLongId> {
        val tilkjenteYtelserHvorUtbetalingsoppdragInneholderOppdatertKlassekodeForUtvidetBarnetrygd = tilkjentYtelseRepository.findByOppdatertUtvidetBarnetrygdIUtbetalingsoppdrag(behandling.fagsak.id)
        return sisteAndelPerKjede.mapValues {
            if (tilkjenteYtelserHvorUtbetalingsoppdragInneholderOppdatertKlassekodeForUtvidetBarnetrygd.isNotEmpty() && it.key.type == YtelsetypeBA.UTVIDET_BARNETRYGD) {
                // Finner siste utbetalingsoppdraget som innehold kjedelementer med oppdatert utvidet klassekode
                val sistOversendteUtbetalingsoppdragMedUtvidetBarnetrygd = tilkjenteYtelserHvorUtbetalingsoppdragInneholderOppdatertKlassekodeForUtvidetBarnetrygd.maxBy { tilkjentYtelse -> tilkjentYtelse.behandling.aktivertTidspunkt }.utbetalingsoppdrag

                // Finner det siste kjedelementet med oppdatert utvidet klassekode
                val sistOversendteUtvidetBarnetrygdKjedeelement =
                    objectMapper
                        .readValue(sistOversendteUtbetalingsoppdragMedUtvidetBarnetrygd, Utbetalingsoppdrag::class.java)
                        .utbetalingsperiode
                        .filter { utbetalingsperiode -> utbetalingsperiode.klassifisering == YtelsetypeBA.UTVIDET_BARNETRYGD.klassifisering }
                        .maxByOrNull { utvidetPeriode -> utvidetPeriode.vedtakdatoFom }!!

                if (it.value.stønadFom != sistOversendteUtvidetBarnetrygdKjedeelement.vedtakdatoFom.toYearMonth()) {
                    logger.warn("Overstyrer vedtakFom i andelDataLongId da fom til siste andel per kjede ikke stemmer overens med siste kjedelement oversendt til Oppdrag")
                    // Oppdaterer fom i AndelDataLongId til samme fom som sist oversendte, da det ikke er 1-1 mellom fom på siste andel og fom på siste kjedelement oversendt til Oppdrag.
                    it.value.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd).copy(fom = sistOversendteUtvidetBarnetrygdKjedeelement.vedtakdatoFom.toYearMonth())
                } else {
                    it.value.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
                }
            } else {
                it.value.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            }
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
