package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.Periode as FamilieFellesPeriode
import no.nav.familie.tidslinje.Tidslinje as FamilieFellesTidslinje

class EndretUtbetalingAndelTidslinje(
    private val endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
) : Tidslinje<EndretUtbetalingAndel, Måned>() {
    override fun lagPerioder(): Collection<Periode<EndretUtbetalingAndel, Måned>> =
        endretUtbetalingAndeler.map {
            Periode(
                fraOgMed = it.fom?.tilTidspunkt() ?: throw Feil("Endret utbetaling andel har ingen fom-dato: $it"),
                tilOgMed = it.tom?.tilTidspunkt() ?: throw Feil("Endret utbetaling andel har ingen tom-dato: $it"),
                innhold = it,
            )
        }
}

fun List<EndretUtbetalingAndel>.tilTidslinje(): FamilieFellesTidslinje<EndretUtbetalingAndel> =
    this
        .map {
            FamilieFellesPeriode(
                verdi = it,
                fom = it.fom?.førsteDagIInneværendeMåned(),
                tom = it.tom?.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()
