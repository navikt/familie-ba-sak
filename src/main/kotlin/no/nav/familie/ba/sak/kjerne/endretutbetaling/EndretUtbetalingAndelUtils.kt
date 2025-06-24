package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.personident.Aktør
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
