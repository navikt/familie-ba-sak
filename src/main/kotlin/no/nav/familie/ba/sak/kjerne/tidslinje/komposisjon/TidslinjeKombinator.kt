package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

/**
 * Extension-metode for å kombinere to tidslinjer
 * Kombinasjonen baserer seg på TidslinjeSomStykkerOppTiden, som itererer gjennom alle tidspunktene
 * fra minste fraOgMed til største tilOgMed fra begge tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv V og H
 * Kombintor-funksjonen tar inn (nullable) av V og H og returnerer (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <V, H, R, T : Tidsenhet> Tidslinje<V, T>.snittKombinerMed(
    høyreTidslinje: Tidslinje<H, T>,
    kombinator: (V?, H?) -> R?
): Tidslinje<R, T> {
    val venstreTidslinje = this
    return object : TidslinjeSomStykkerOppTiden<R, T>(venstreTidslinje, høyreTidslinje) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): R? =
            kombinator(
                venstreTidslinje.innholdForTidspunkt(tidspunkt),
                høyreTidslinje.innholdForTidspunkt(tidspunkt)
            )
    }
}

/**
 * Extension-metode for å kombinere liste av tidslinjer
 * Kombinasjonen baserer seg på TidslinjeSomStykkerOppTiden, som itererer gjennom alle tidspunktene
 * fra minste fraOgMed til største fraOgMed() fra alle tidslinjene
 * Innhold (I) og tidsenhet (T) må være av samme type
 * Kombintor-funksjonen tar inn Iterable<I> og returner (nullable) R
 * Null-verdier fjernes før de sendes til kombinator-funksjonen, som betyr at en tom iterator kan bli sendt
 * Resultatet er en tidslnije med tidsenhet T og innhold R
 */
fun <I, R, T : Tidsenhet> Collection<Tidslinje<I, T>>.snittKombinerUtenNull(
    listeKombinator: (Iterable<I>) -> R?
): Tidslinje<R, T> {
    val tidslinjer = this
    return object : TidslinjeSomStykkerOppTiden<R, T>(tidslinjer) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): R? =
            listeKombinator(tidslinjer.map { it.innholdForTidspunkt(tidspunkt) }.filterNotNull())
    }
}
