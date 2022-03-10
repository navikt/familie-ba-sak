package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje

data class DifferanseBeregningTidslinje(
    val utbetalingTema: Tidslinje<Int>,
    val erSekundærlandTidslinje: Tidslinje<Boolean>
)
