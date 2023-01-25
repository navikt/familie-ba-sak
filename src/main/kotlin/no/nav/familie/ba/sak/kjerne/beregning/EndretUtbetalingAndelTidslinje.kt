package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt

class EndretUtbetalingAndelTidslinje(
    private val endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>
) : Tidslinje<EndretUtbetalingAndelMedAndelerTilkjentYtelse, Måned>() {

    override fun lagPerioder(): Collection<Periode<EndretUtbetalingAndelMedAndelerTilkjentYtelse, Måned>> {
        return endretUtbetalingAndeler.map {
            Periode(
                fraOgMed = it.fom?.tilTidspunkt() ?: throw Feil("Endret utbetaling andel har ingen fom-dato: $it"),
                tilOgMed = it.tom?.tilTidspunkt() ?: throw Feil("Endret utbetaling andel har ingen tom-dato: $it"),
                innhold = it
            )
        }
    }
}
