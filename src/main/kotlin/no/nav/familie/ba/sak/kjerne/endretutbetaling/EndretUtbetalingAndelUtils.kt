package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import java.time.YearMonth

fun beregnGyldigTomIFremtiden(
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
                .filter {
                    endretUtbetalingAndel.personer.any { person -> person.aktør == it.aktør }
                }.groupBy { it.aktør }
                .minOf { (_, andelerForAktør) -> andelerForAktør.maxOf { it.stønadTom } }

        return sisteTomAndeler
    }
}
