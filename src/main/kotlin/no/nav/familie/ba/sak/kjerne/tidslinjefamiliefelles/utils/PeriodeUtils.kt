package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.utils

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Periode
import java.time.YearMonth

fun <V> Periode<V>.splitPerMåned(tilOgMedMåned: YearMonth): List<Periode<V>> {
    val førsteMåned = (this.fom ?: PRAKTISK_TIDLIGSTE_DAG).toYearMonth()
    val sisteMåned = setOfNotNull(this.tom?.toYearMonth(), tilOgMedMåned).minOrNull()!!
    return førsteMåned.rangeTo(sisteMåned).map {
        Periode(
            verdi = this.verdi,
            fom = it.førsteDagIInneværendeMåned(),
            tom = it.sisteDagIInneværendeMåned(),
        )
    }
}
