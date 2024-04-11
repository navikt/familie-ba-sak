package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeIdLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class UtbetalingsoppdragGeneratorService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
) {
    @Transactional
    fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        vedtak: Vedtak,
        saksbehandlerId: String,
        erSimulering: Boolean = false,
    ): BeregnetUtbetalingsoppdragLongId {
        val forrigeTilkjentYtelse = hentForrigeTilkjentYtelse(vedtak.behandling)
        val nyTilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = vedtak.behandling.id)
        val endretMigreringsDato =
            beregnOmMigreringsDatoErEndret(
                vedtak.behandling,
                forrigeTilkjentYtelse?.andelerTilkjentYtelse?.minOfOrNull { it.stønadFom },
            )
        val sisteAndelPerKjede = hentSisteAndelTilkjentYtelse(vedtak.behandling.fagsak)
        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                nyTilkjentYtelse = nyTilkjentYtelse,
                sisteAndelPerKjede = sisteAndelPerKjede,
                erSimulering = erSimulering,
                endretMigreringsDato = endretMigreringsDato,
            )

        if (!erSimulering) {
            oppdaterTilkjentYtelse(nyTilkjentYtelse, beregnetUtbetalingsoppdrag)
        }

        return beregnetUtbetalingsoppdrag
    }

    private fun oppdaterTilkjentYtelse(
        tilkjentYtelse: TilkjentYtelse,
        beregnetUtbetalingsoppdrag: BeregnetUtbetalingsoppdragLongId,
    ) {
        secureLogger.info("Oppdaterer TilkjentYtelse med utbetalingsoppdrag og offsets på andeler for behandling ${tilkjentYtelse.behandling.id}")

        oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
            tilkjentYtelse = tilkjentYtelse,
            utbetalingsoppdrag = beregnetUtbetalingsoppdrag.utbetalingsoppdrag,
            endretUtbetalingAndeler = endretUtbetalingAndelHentOgPersisterService.hentForBehandling(tilkjentYtelse.behandling.id),
        )
        oppdaterAndelerMedPeriodeOffset(
            tilkjentYtelse = tilkjentYtelse,
            andelerMedPeriodeId = beregnetUtbetalingsoppdrag.andeler,
        )
        tilkjentYtelseRepository.save(tilkjentYtelse)
    }

    private fun hentForrigeTilkjentYtelse(behandling: Behandling): TilkjentYtelse? =
        behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling = behandling)
            ?.let { tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandlingId = it.id) }

    private fun hentSisteAndelTilkjentYtelse(fagsak: Fagsak) =
        andelTilkjentYtelseRepository.hentSisteAndelPerIdentOgType(fagsakId = fagsak.id)
            .associateBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type.tilYtelseType()) }

    private fun beregnOmMigreringsDatoErEndret(
        behandling: Behandling,
        forrigeTilstandFraDato: YearMonth?,
    ): YearMonth? {
        val erMigrertSak =
            behandlingHentOgPersisterService.hentBehandlinger(behandling.fagsak.id)
                .any { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD }

        if (!erMigrertSak) {
            return null
        }

        val nyttTilstandFraDato =
            behandlingService.hentMigreringsdatoPåFagsak(fagsakId = behandling.fagsak.id)
                ?.toYearMonth()
                ?.plusMonths(1)

        return if (forrigeTilstandFraDato != null &&
            nyttTilstandFraDato != null &&
            forrigeTilstandFraDato.isAfter(nyttTilstandFraDato)
        ) {
            nyttTilstandFraDato
        } else {
            null
        }
    }
}

private fun utledOpphør(
    utbetalingsoppdrag: no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag,
    behandling: Behandling,
): Opphør {
    val erRentOpphør =
        utbetalingsoppdrag.utbetalingsperiode.isNotEmpty() && utbetalingsoppdrag.utbetalingsperiode.all { it.opphør != null }
    var opphørsdato: LocalDate? = null
    if (erRentOpphør) {
        opphørsdato = utbetalingsoppdrag.utbetalingsperiode.minOf { it.opphør!!.opphørDatoFom }
    }
    if (behandling.type == BehandlingType.REVURDERING) {
        val opphørPåRevurdering = utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør != null }
        if (opphørPåRevurdering.isNotEmpty()) {
            opphørsdato = opphørPåRevurdering.maxOfOrNull { it.opphør!!.opphørDatoFom }
        }
    }
    return Opphør(erRentOpphør = erRentOpphør, opphørsdato = opphørsdato)
}

private fun utledStønadTom(
    tilkjentYtelse: TilkjentYtelse,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
): YearMonth? {
    val andelerMedEndringer = tilkjentYtelse.andelerTilkjentYtelse.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(endretUtbetalingAndeler)

    return andelerMedEndringer.filterNot { it ->
        val harEndringSomFørerTilOpphørVed0Prosent =
            it.endreteUtbetalinger.any { endretUtbetaling ->
                endretUtbetaling.årsak in
                    listOf(
                        Årsak.ALLEREDE_UTBETALT,
                        Årsak.ENDRE_MOTTAKER,
                        Årsak.ETTERBETALING_3ÅR,
                    )
            }
        harEndringSomFørerTilOpphørVed0Prosent && it.prosent == BigDecimal.ZERO
    }.maxOfOrNull { it.stønadTom }
}

fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
    tilkjentYtelse: TilkjentYtelse,
    utbetalingsoppdrag: no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel> = emptyList(),
) {
    val opphør = utledOpphør(utbetalingsoppdrag, tilkjentYtelse.behandling)

    tilkjentYtelse.utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
    tilkjentYtelse.stønadTom = utledStønadTom(tilkjentYtelse, endretUtbetalingAndeler)
    tilkjentYtelse.stønadFom =
        if (opphør.erRentOpphør) null else tilkjentYtelse.andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
    tilkjentYtelse.endretDato = LocalDate.now()
    tilkjentYtelse.opphørFom = opphør.opphørsdato?.toYearMonth()
}

fun oppdaterAndelerMedPeriodeOffset(
    tilkjentYtelse: TilkjentYtelse,
    andelerMedPeriodeId: List<AndelMedPeriodeIdLongId>,
) {
    val andelerPåId = andelerMedPeriodeId.associateBy { it.id }
    val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse
    val andelerSomSkalSendesTilOppdrag = andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
    if (andelerMedPeriodeId.size != andelerSomSkalSendesTilOppdrag.size) {
        error("Antallet andeler med oppdatert periodeOffset, forrigePeriodeOffset og kildeBehandlingId fra ny generator skal være likt antallet andeler med kalkulertUtbetalingsbeløp != 0. Generator gir ${andelerMedPeriodeId.size} andeler men det er ${andelerSomSkalSendesTilOppdrag.size} andeler med kalkulertUtbetalingsbeløp != 0")
    }
    andelerSomSkalSendesTilOppdrag.forEach { andel ->
        val andelMedOffset =
            andelerPåId[andel.id]
                ?: error("Feil ved oppdaterig av offset på andeler. Finner ikke andel med id ${andel.id} blandt andelene med oppdatert offset fra ny generator. Ny generator returnerer andeler med ider [${andelerPåId.values.map { it.id }}]")
        andel.periodeOffset = andelMedOffset.periodeId
        andel.forrigePeriodeOffset = andelMedOffset.forrigePeriodeId
        andel.kildeBehandlingId = andelMedOffset.kildeBehandlingId
    }
}

data class Opphør(val erRentOpphør: Boolean, val opphørsdato: LocalDate?)
