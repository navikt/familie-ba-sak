package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder

object EndringUtil {
    internal fun Tidslinje<Boolean>.tilFørsteEndringstidspunkt() =
        this
            .tilPerioder()
            .filter { it.verdi == true }
            .mapNotNull { it.fom }
            .minOfOrNull { it }
            ?.toYearMonth()
}
