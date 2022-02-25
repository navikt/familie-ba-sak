package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import java.time.YearMonth

fun erStartPåUtvidetSammeMåned(
    andelTilkjentYtelser: List<AndelTilkjentYtelse>,
    fom: YearMonth?,
) = andelTilkjentYtelser.any { it.stønadFom == fom && it.type == YtelseType.UTVIDET_BARNETRYGD }

fun beregnTomHvisDenIkkeErSatt(
    andreEndredeAndelerPåBehandling: List<EndretUtbetalingAndel>,
    endretUtbetalingAndel: EndretUtbetalingAndel,
    andelTilkjentYtelser: List<AndelTilkjentYtelse>
) {
    val førsteEndringEtterDenneEndringen = andreEndredeAndelerPåBehandling.filter {
        it.fom?.isAfter(endretUtbetalingAndel.fom)
            ?: false
    }.sortedBy { it.fom }.firstOrNull()

    if (førsteEndringEtterDenneEndringen != null) {
        endretUtbetalingAndel.tom = førsteEndringEtterDenneEndringen.fom?.minusMonths(1)
    } else {
        val sisteTomAndeler = andelTilkjentYtelser.filter {
            it.aktør == endretUtbetalingAndel.person?.aktør
        }.maxOf { it.stønadTom }

        endretUtbetalingAndel.tom = sisteTomAndeler
    }
}
