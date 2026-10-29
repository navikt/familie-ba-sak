package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.tidslinje.Tidslinje

class Utbetalingstidslinje(
    private val utbetalingsperioder: Set<Utbetalingsperiode>,
    val tidslinje: Tidslinje<Utbetalingsperiode>,
) {
    fun erTidslinjeForPeriodeId(periodeId: Long): Boolean = utbetalingsperioder.any { it.periodeId == periodeId }
}
