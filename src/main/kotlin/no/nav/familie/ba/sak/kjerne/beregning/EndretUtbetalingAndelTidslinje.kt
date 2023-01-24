package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt

class EndretUtbetalingAndelTidslinje(
    private val endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>
) : Tidslinje<EndretUtbetalingAndelMedAndelerTilkjentYtelse, Måned>() {

    override fun lagPerioder(): Collection<Periode<EndretUtbetalingAndelMedAndelerTilkjentYtelse, Måned>> {
        return endretUtbetalingAndeler.map {
            Periode(
                fraOgMed = it.fom?.tilTidspunkt() ?: TIDENES_MORGEN.tilMånedTidspunkt(),
                tilOgMed = it.tom?.tilTidspunkt() ?: TIDENES_ENDE.tilMånedTidspunkt(),
                innhold = it
            )
        }
    }
}
