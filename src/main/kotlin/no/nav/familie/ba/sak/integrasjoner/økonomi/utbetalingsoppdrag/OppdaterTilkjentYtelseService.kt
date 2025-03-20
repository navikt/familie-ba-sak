package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.internal.AndelTilkjentYtelseKorreksjon
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.PatchetAndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilPatchetAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.førerTilOpphør
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeIdLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
class OppdaterTilkjentYtelseService(
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val patchetAndelTilkjentYtelseRepository: PatchetAndelTilkjentYtelseRepository,
    private val clockProvider: ClockProvider,
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

    @Transactional
    fun oppdaterTilkjentYtelseMedKorrigerteAndeler(
        tilkjentYtelse: TilkjentYtelse,
        andelTilkjentYtelseKorreksjoner: List<AndelTilkjentYtelseKorreksjon>,
    ) {
        val andelerSomSkalSlettes = andelTilkjentYtelseKorreksjoner.map { it.andelMedFeil }
        val andelerSomSkalOpprettes = andelTilkjentYtelseKorreksjoner.map { it.korrigertAndel }

        val andelerSomSkalSlettesGruppertPåId = andelerSomSkalSlettes.groupBy { it.id }
        if (andelerSomSkalSlettesGruppertPåId.any { it.value.size > 1 }) throw Feil("Den samme andelen forekommer flere ganger blant andelene som er markert for sletting. Dette betyr at det finnes en splitt i utbetalingsoppdragene oversendt til Oppdrag som ikke eksisterer i andelene.")

        patchetAndelTilkjentYtelseRepository.saveAll(andelerSomSkalSlettes.map { it.tilPatchetAndelTilkjentYtelse() })

        tilkjentYtelse.andelerTilkjentYtelse.removeAll(andelerSomSkalSlettes.toSet())
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerSomSkalOpprettes.toSet())
    }

    private fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
        tilkjentYtelse: TilkjentYtelse,
        utbetalingsoppdrag: Utbetalingsoppdrag,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ) {
        val opphør = Opphør.opprettFor(utbetalingsoppdrag, tilkjentYtelse.behandling)
        tilkjentYtelse.utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
        tilkjentYtelse.stønadTom = utledStønadTom(tilkjentYtelse.andelerTilkjentYtelse, endretUtbetalingAndeler)
        tilkjentYtelse.stønadFom = if (opphør.erRentOpphør) null else tilkjentYtelse.andelerTilkjentYtelse.minOf { it.stønadFom }
        tilkjentYtelse.endretDato = LocalDate.now(clockProvider.get())
        tilkjentYtelse.opphørFom = opphør.opphørsdato?.toYearMonth()
    }

    private fun utledStønadTom(
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

    private fun oppdaterAndelerMedPeriodeOffset(
        tilkjentYtelse: TilkjentYtelse,
        andelerMedPeriodeId: List<AndelMedPeriodeIdLongId>,
    ) {
        val andelerPåId = andelerMedPeriodeId.associateBy { it.id }
        val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse
        val andelerSomSkalSendesTilOppdrag = andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
        if (tilkjentYtelse.behandling.erOppdaterUtvidetKlassekode()) {
            if (andelerMedPeriodeId.size != andelerSomSkalSendesTilOppdrag.size && andelerMedPeriodeId.size != andelerSomSkalSendesTilOppdrag.size + 1) {
                throw IllegalStateException(
                    "Antallet andeler med oppdatert periodeOffset, forrigePeriodeOffset og kildeBehandlingId fra ny generator skal være likt eller ha en andel mer enn andelene med kalkulertUtbetalingsbeløp != 0. Generator gir ${andelerMedPeriodeId.size} andeler men det er ${andelerSomSkalSendesTilOppdrag.size} andeler med kalkulertUtbetalingsbeløp != 0",
                )
            }
        } else {
            if (andelerMedPeriodeId.size != andelerSomSkalSendesTilOppdrag.size) {
                throw IllegalStateException(
                    "Antallet andeler med oppdatert periodeOffset, forrigePeriodeOffset og kildeBehandlingId fra ny generator skal være likt antallet andeler med kalkulertUtbetalingsbeløp != 0. Generator gir ${andelerMedPeriodeId.size} andeler men det er ${andelerSomSkalSendesTilOppdrag.size} andeler med kalkulertUtbetalingsbeløp != 0",
                )
            }
        }

        andelerSomSkalSendesTilOppdrag.forEach { andel ->
            val andelMedOffset = andelerPåId[andel.id]
            if (andelMedOffset == null) {
                throw IllegalStateException(
                    "Feil ved oppdaterig av offset på andeler. Finner ikke andel med id ${andel.id} blandt andelene med oppdatert offset fra ny generator. Ny generator returnerer andeler med ider ${andelerPåId.values.map { it.id }}",
                )
            }
            andel.periodeOffset = andelMedOffset.periodeId
            andel.forrigePeriodeOffset = andelMedOffset.forrigePeriodeId
            andel.kildeBehandlingId = andelMedOffset.kildeBehandlingId
        }
    }
}
