package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt

/**
 * Extension-metode for å kombinere en Collection av tidslinjer
 * Kombinasjonen baserer seg på TidslinjeSomStykkerOppTiden, som itererer gjennom alle tidspunktene
 * fra minste fraOgMed til største fraOgMed() fra alle tidslinjene
 * Tidsenhet (T) og innhold <I> må være av samme type
 * Kombintor-funksjonen tar inn en Iterable av (nullable) I og returner (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <I, R, T : Tidsenhet> Collection<Tidslinje<I, T>>.kombiner(kombinator: (Iterable<I?>) -> R?): Tidslinje<R, T> {
    val tidslinjer = this
    return object : TidslinjeSomStykkerOppTiden<R, T>(tidslinjer) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): R? =
            kombinator(tidslinjer.map { it.innholdForTidspunkt(tidspunkt) })
    }
}
