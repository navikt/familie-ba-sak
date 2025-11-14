package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
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
                    it.personer.intersect(endretUtbetalingAndel.personer).isNotEmpty()
            }.sortedBy { it.fom }
            .firstOrNull()

    if (førsteEndringEtterDenneEndringen != null) {
        return førsteEndringEtterDenneEndringen.fom?.minusMonths(1)
    } else {
        val sisteTomAndeler =
            andelTilkjentYtelser
                .filter { endretUtbetalingAndel.personer.any { person -> person.aktør == it.aktør } }
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
        endretUtbetalingAndel.personer.associate { person ->
            person.aktør to
                andreEndredeAndelerPåBehandling
                    .filter { it.fom?.isAfter(endretUtbetalingAndel.fom) == true && it.personer.contains(person) }
                    .sortedBy { it.fom }
                    .firstOrNull()
                    ?.fom
                    ?.minusMonths(1)
        }

    val sisteTomAndelerPerAktør =
        andelTilkjentYtelser
            .filter { endretUtbetalingAndel.personer.any { person -> person.aktør == it.aktør } }
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
                personer = endretUtbetalingAndel.personer.filter { periode.verdi.contains(it.aktør) }.toMutableSet(),
                fom = periode.fom?.toYearMonth(),
                tom = periode.tom?.toYearMonth(),
            )
        }
