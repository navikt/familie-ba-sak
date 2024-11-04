package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.førerTilOpphør
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeIdLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class OppdaterTilkjentYtelseService(
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
        tilkjentYtelse: TilkjentYtelse,
        beregnetUtbetalingsoppdrag: BeregnetUtbetalingsoppdragLongId,
    ) {
        secureLogger.info(
            "Oppdaterer TilkjentYtelse med utbetalingsoppdrag og offsets på andeler for behandling ${tilkjentYtelse.behandling.id}",
        )

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
}

fun utledStønadTom(
    andelerTilkjentYtelse: Set<AndelTilkjentYtelse>,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
): YearMonth? {
    val andelerMedEndringer = andelerTilkjentYtelse.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(endretUtbetalingAndeler)

    val andelerMedRelevantUtbetaling =
        andelerMedEndringer.filterNot { andelTilkjentYtelseMedEndreteUtbetalinger ->
            andelTilkjentYtelseMedEndreteUtbetalinger.endreteUtbetalinger.any { endretUtbetaling ->
                endretUtbetaling.førerTilOpphør()
            }
        }

    return andelerMedRelevantUtbetaling.maxOfOrNull { it.stønadTom }
}

fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
    tilkjentYtelse: TilkjentYtelse,
    utbetalingsoppdrag: no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
) {
    val opphør = Opphør.opprettFor(utbetalingsoppdrag, tilkjentYtelse.behandling)

    tilkjentYtelse.utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
    tilkjentYtelse.stønadTom = utledStønadTom(tilkjentYtelse.andelerTilkjentYtelse, endretUtbetalingAndeler)
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
