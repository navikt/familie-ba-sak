package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth

object EndringUtil {

    internal fun Tidslinje<Boolean, Måned>.tilFørsteEndringstidspunkt() = this.perioder().filter { it.innhold == true }.minOfOrNull { it.fraOgMed }?.tilYearMonth()

    internal fun Tidslinje<Boolean, Dag>.tilFørsteEndringstidspunktForDagtidslinje() = this.perioder().filter { it.innhold == true }.minOfOrNull { it.fraOgMed }?.tilYearMonth()
}
