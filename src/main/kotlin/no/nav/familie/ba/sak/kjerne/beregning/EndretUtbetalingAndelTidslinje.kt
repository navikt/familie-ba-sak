package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje

fun List<EndretUtbetalingAndel>.tilTidslinje(): Tidslinje<EndretUtbetalingAndel> =
    this
        .map {
            Periode(
                verdi = it,
                fom = it.fom?.førsteDagIInneværendeMåned(),
                tom = it.tom?.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()
