package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeMedAvhengigheter
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

/**
 * Extension-metode for å map'e innhold fra en type og verdi til en annen
 */
fun <I, T : Tidsenhet, R> Tidslinje<I, T>.map(mapper: (I?) -> R?): Tidslinje<R, T> {
    val tidslinje = this
    return object : TidslinjeMedAvhengigheter<R, T>(listOf(tidslinje)) {
        override fun lagPerioder() = tidslinje.perioder().map {
            Periode(it.fraOgMed, it.tilOgMed, mapper(it.innhold))
        }
    }
}
