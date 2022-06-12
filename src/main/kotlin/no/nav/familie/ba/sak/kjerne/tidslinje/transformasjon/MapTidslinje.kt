package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

/**
 * Extension-metode for å map'e innhold fra en type og verdi til en annen
 * Hvis det nå oppstår tilgrensende perioder med samme innhold, slås de sammen
 */
fun <I, T : Tidsenhet, R> Tidslinje<I, T>.map(mapper: (I?) -> R?): Tidslinje<R, T> {
    val tidslinje = this
    return object : TidslinjeSomStykkerOppTiden<R, T>(listOf(tidslinje)) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>) =
            mapper(tidslinje.innholdForTidspunkt(tidspunkt))
    }
}
