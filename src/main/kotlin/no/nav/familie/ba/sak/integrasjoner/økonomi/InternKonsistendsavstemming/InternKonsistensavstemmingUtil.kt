package no.nav.familie.ba.sak.integrasjoner.økonomi.InternKonsistendsavstemming

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag

fun erForskjellMellomAndelerOgOppdrag(
    andeler: List<AndelTilkjentYtelse>,
    utbetalingsoppdrag: Utbetalingsoppdrag?,
    fagsakId: Long
): Boolean {
    val oppdragsperioder =
        utbetalingsoppdrag?.utbetalingsperiode
            ?.filter { it.opphør == null }
            ?: emptyList()

    val startFørsteOppdragsperiode = oppdragsperioder.minOfOrNull { it.vedtakdatoFom } ?: TIDENES_MORGEN

    val sumUtbetalingFraAndeler = andeler
        .filter { it.stønadFom.isSameOrAfter(startFørsteOppdragsperiode.toYearMonth()) }
        .sumOf { it.kalkulertUtbetalingsbeløp }
        .toBigDecimal()

    val sumUtbetalingFraOppdrag = oppdragsperioder.sumOf { it.sats }

    val erForskjellMellomAndelerOgOppdrag = sumUtbetalingFraOppdrag != sumUtbetalingFraAndeler

    if (erForskjellMellomAndelerOgOppdrag) {
        secureLogger.info(
            "Fagsak $fagsakId har ulikt utbetalingsbeløp i andelene tilkjent ytelse og utbetalingsoppdraget." +
                "\nSum utbetaling i utbetalingsperiodene=$oppdragsperioder" +
                "\nSum utbetaling i andelene=$sumUtbetalingFraAndeler"
        )
    }

    return erForskjellMellomAndelerOgOppdrag
}
