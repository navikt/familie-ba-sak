package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom

/**
 * Extension-metode for å kombinere to tidslinjer
 * Kombinasjonen baserer seg på TidslinjeSomStykkerOppTiden, som itererer gjennom alle tidspunktene
 * fra minste fraOgMed til største tilOgMed fra begge tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv V og H
 * Kombintor-funksjonen tar inn (nullable) av V og H og returnerer (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <V, H, R, T : Tidsenhet> Tidslinje<V, T>.kombinerMed(
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
 * Extension-metode for å kombinere to tidslinjer
 * Kombinasjonen baserer seg på TidslinjeSomStykkerOppTiden, som itererer gjennom alle tidspunktene
 * fra minste fraOgMed til største tilOgMed fra begge tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv V og H
 * Hvis innholdet V eller H er null returneres null
 * Kombintor-funksjonen tar ellers V og H og returnerer (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <V, H, R, T : Tidsenhet> Tidslinje<V, T>.kombinerUtenNullMed(
    høyreTidslinje: Tidslinje<H, T>,
    kombinator: (V, H) -> R?
): Tidslinje<R, T> {
    val venstreTidslinje = this
    return object : TidslinjeSomStykkerOppTiden<R, T>(venstreTidslinje, høyreTidslinje) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): R? {
            val venstre = venstreTidslinje.innholdForTidspunkt(tidspunkt)
            val høyre = høyreTidslinje.innholdForTidspunkt(tidspunkt)

            return when {
                venstre == null || høyre == null -> null
                else -> kombinator(venstre, høyre)
            }
        }
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
fun <I, R, T : Tidsenhet> Collection<Tidslinje<I, T>>.kombinerUtenNull(
    listeKombinator: (Iterable<I>) -> R?
): Tidslinje<R, T> {
    if (this.isEmpty()) return tidslinje { emptyList() }
    val tidslinjer = this
    return object : TidslinjeSomStykkerOppTiden<R, T>(tidslinjer) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): R? =
            listeKombinator(tidslinjer.map { it.innholdForTidspunkt(tidspunkt) }.filterNotNull())
    }
}

/**
 * Extension-metode for å kombinere liste av tidslinjer
 * Kombinasjonen baserer seg på TidslinjeSomStykkerOppTiden, som itererer gjennom alle tidspunktene
 * fra minste fraOgMed til største fraOgMed() fra alle tidslinjene
 * Innhold (I) og tidsenhet (T) må være av samme type
 * Kombintor-funksjonen tar inn Iterable<I> og returner (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <I, R, T : Tidsenhet> Collection<Tidslinje<I, T>>.kombiner(
    listeKombinator: (Iterable<I>) -> R?
) = tidsrom().tidslinjeFraTidspunkt { tidspunkt ->
    this.map { it.innholdsresultatForTidspunkt(tidspunkt) }
        .filter { it.harVerdi }
        .map { it.verdi }
        .let { listeKombinator(it) }
        .tilVerdi()
}

/**
 * Extension-metode for å kombinere to tidslinjer
 * Kombinasjonen baserer seg på TidslinjeSomStykkerOppTiden, som itererer gjennom alle tidspunktene
 * fra minste fraOgMed til største tilOgMed fra begge tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv V og H
 * Kombintor-funksjonen tar inn tidspunktet og (nullable) av V og H og returnerer (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <V, H, R, T : Tidsenhet> Tidslinje<V, T>.tidspunktKombinerMed(
    høyreTidslinje: Tidslinje<H, T>,
    kombinator: (Tidspunkt<T>, V?, H?) -> R?
): Tidslinje<R, T> {
    val venstreTidslinje = this
    return object : TidslinjeSomStykkerOppTiden<R, T>(venstreTidslinje, høyreTidslinje) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): R? =
            kombinator(
                tidspunkt,
                venstreTidslinje.innholdForTidspunkt(tidspunkt),
                høyreTidslinje.innholdForTidspunkt(tidspunkt)
            )
    }
}

/**
 * Extension-metode for å kombinere tre tidslinjer
 * Kombinasjonen baserer seg på TidslinjeSomStykkerOppTiden, som itererer gjennom alle tidspunktene
 * fra minste fraOgMed til største fraOgMed() fra alle tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv A, B og C
 * Kombintor-funksjonen tar inn (nullable) av A, B og C og returner (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <A, B, C, R, T : Tidsenhet> Tidslinje<A, T>.kombinerMed(
    tidslinjeB: Tidslinje<B, T>,
    tidslinjeC: Tidslinje<C, T>,
    kombinator: (A?, B?, C?) -> R?
): Tidslinje<R, T> {
    val tidslinjeA = this
    return object : TidslinjeSomStykkerOppTiden<R, T>(tidslinjeA, tidslinjeB, tidslinjeC) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): R? =
            kombinator(
                tidslinjeA.innholdForTidspunkt(tidspunkt),
                tidslinjeB.innholdForTidspunkt(tidspunkt),
                tidslinjeC.innholdForTidspunkt(tidspunkt)
            )
    }
}
