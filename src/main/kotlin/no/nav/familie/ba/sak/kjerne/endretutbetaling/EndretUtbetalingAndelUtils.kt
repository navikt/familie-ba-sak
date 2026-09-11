package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.YearMonth

fun beregnGyldigTom(
    andreEndredeAndelerPåBehandling: List<EndretUtbetalingAndel>,
    endretUtbetalingAndel: EndretUtbetalingAndel,
    andelTilkjentYtelser: List<AndelTilkjentYtelse>,
): YearMonth? {
    val førsteEndringEtterDenneEndringen =
        andreEndredeAndelerPåBehandling
            .filter {
                it.fom?.isAfter(endretUtbetalingAndel.fom) == true &&
                    it.aktører.intersect(endretUtbetalingAndel.aktører).isNotEmpty()
            }.sortedBy { it.fom }
            .firstOrNull()

    if (førsteEndringEtterDenneEndringen != null) {
        return førsteEndringEtterDenneEndringen.fom?.minusMonths(1)
    } else {
        val sisteTomAndeler =
            andelTilkjentYtelser
                .filter { it.aktør in endretUtbetalingAndel.aktører }
                .groupBy { it.aktør }
                .minOf { (_, andelerForAktør) -> andelerForAktør.maxOf { it.stønadTom } }

        return sisteTomAndeler
    }
}

fun beregnGyldigTomPerAktør(
    endretUtbetalingAndel: EndretUtbetalingAndel,
    andreEndredeAndelerPåBehandling: List<EndretUtbetalingAndel>,
    andelTilkjentYtelser: List<AndelTilkjentYtelse>,
): Map<Aktør, YearMonth?> {
    val førsteEndringEtterDenneEndringenPerAktør =
        endretUtbetalingAndel.aktører.associateWith { aktør ->
            andreEndredeAndelerPåBehandling
                .filter { it.fom?.isAfter(endretUtbetalingAndel.fom) == true && it.aktører.contains(aktør) }
                .sortedBy { it.fom }
                .firstOrNull()
                ?.fom
                ?.minusMonths(1)
        }

    val sisteTomAndelerPerAktør =
        andelTilkjentYtelser
            .filter { it.aktør in endretUtbetalingAndel.aktører }
            .filter { førsteEndringEtterDenneEndringenPerAktør[it.aktør] == null }
            .groupBy { it.aktør }
            .mapValues { (_, andelerForAktør) -> andelerForAktør.maxOfOrNull { it.stønadTom } }

    return førsteEndringEtterDenneEndringenPerAktør + sisteTomAndelerPerAktør
}

fun skalSplitteEndretUtbetalingAndel(
    endretUtbetalingAndel: EndretUtbetalingAndel,
    gyldigTomDatoPerAktør: Map<Aktør, YearMonth?>,
): Boolean =
    endretUtbetalingAndel.tom == null &&
        gyldigTomDatoPerAktør.values.distinctBy { it }.size > 1

fun splittEndretUbetalingAndel(
    endretUtbetalingAndel: EndretUtbetalingAndel,
    gyldigTomEtterDagensDatoPerAktør: Map<Aktør, YearMonth?>,
): List<EndretUtbetalingAndel> =
    gyldigTomEtterDagensDatoPerAktør
        .map { (aktør, tom) ->
            Periode(
                verdi = aktør,
                fom = endretUtbetalingAndel.fom?.førsteDagIInneværendeMåned(),
                tom = tom?.sisteDagIInneværendeMåned(),
            ).tilTidslinje()
        }.kombiner()
        .tilPerioderIkkeNull()
        .map { periode ->
            endretUtbetalingAndel.copy(
                id = 0,
                aktører = endretUtbetalingAndel.aktører.intersect(periode.verdi).toMutableSet(),
                fom = periode.fom?.toYearMonth(),
                tom = periode.tom?.toYearMonth(),
            )
        }
