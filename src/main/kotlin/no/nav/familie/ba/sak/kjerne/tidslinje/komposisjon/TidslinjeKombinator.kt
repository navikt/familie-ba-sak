package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

/**
 * Extension-metode for å kombinere en map av tidslinjer med en annen map av tidslinjer
 * Nøklene (K), og tidsenhet (T) må være av samme type for alle tidslinjer i begge map'er
 * Alle tidslinjer i en og samme map må ha innhold av samme type
 * Tidslinjene i hver av map'ene kan ha ulik innholdstype, hhv V(enstre) og H(øyre)
 * mapKombinator-funksjonen tar i en nøkkel og returnerer en kombinator-funksjon.
 * Kombintor-funksjonen tar inn (nullable) av innholdet V og H og returner (nullable) R
 * Resultatet er en map med settet av nøkler fra BEGGE map'ene.
 * Resultat-tidslinjene har tidsenheten T og innholdet R
 * Der hvor den ene map'en mangler tidslinje for en nøkkel, gjøres kombinasjonen med TomTidslinje,
 * som i praksis betyr kombinator-funksjonen mottar null som innhold
 */
fun <K, V, H, R, T : Tidsenhet> Map<K, Tidslinje<V, T>>.kombinerForFellesNøklerMed(
    tidslinjeMap: Map<K, Tidslinje<H, T>>,
    mapKombinator: (K) -> (V?, H?) -> R?
): Map<K, Tidslinje<R, T>> {
    return this
        .filterKeys { tidslinjeMap.containsKey(it) }
        .mapValues { (key, venstre) ->
            val høyre: Tidslinje<H, T> = tidslinjeMap.getValue(key)
            val kombinator: (V?, H?) -> R? = mapKombinator(key)
            val resultat: Tidslinje<R, T> = venstre.kombinerMed(høyre, kombinator)
            resultat
        }
}

/**
 * Extension-metode for å kombinere en map av tidslinjer med en annen map av tidslinjer
 * Nøklene (K), og tidsenhet (T) må være av samme type for alle tidslinjer i begge map'er
 * Alle tidslinjer i en og samme map må ha innhold av samme type
 * Tidslinjene i hver av map'ene kan ha ulik innholdstype, hhv V(enstre) og H(øyre)
 * mapKombinator-funksjonen tar i en nøkkel og returnerer en kombinator-funksjon.
 * Kombintor-funksjonen tar inn (nullable) av innholdet V og H og returner (nullable) R
 * Resultatet er en map med settet av nøkler som er FELLES for begge map'ene.
 * Resultat-tidslinjene har tidsenheten T og innholdet R
 */
fun <K, V, H, R, T : Tidsenhet> Map<K, Tidslinje<V, T>>.kombinerForAlleNøklerMed(
    tidslinjeMap: Map<K, Tidslinje<H, T>>,
    mapKombinator: (K) -> (V?, H?) -> R?
): Map<K, Tidslinje<R, T>> {

    val alleKeys = keys + tidslinjeMap.keys

    return alleKeys
        .map {
            val høyre: Tidslinje<H, T> = tidslinjeMap[it] ?: TomTidslinje()
            val venstre: Tidslinje<V, T> = this[it] ?: TomTidslinje()
            val kombinator: (V?, H?) -> R? = mapKombinator(it)
            val resultat: Tidslinje<R, T> = venstre.kombinerMed(høyre, kombinator)
            it to resultat
        }.toMap()
}

/**
 * Extension-metode for å kombinere to tidslinjer
 * Kombinasjonen baserer seg på TidslinjeSomStykkerOppTiden, som itererer gjennom alle tidspunktene
 * fra minste fraOgMed til største fraOgMed() fra begge tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv V og H
 * Kombintor-funksjonen tar inn (nullable) av V og H og returner (nullable) R
 * Resultatet er en tidslnije med tidsenhet T og innhold R
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
 * Extension-metode for å kombinere tre tidslinjer
 * Kombinasjonen baserer seg på TidslinjeSomStykkerOppTiden, som itererer gjennom alle tidspunktene
 * fra minste fraOgMed til største fraOgMed() fra alle tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv A, B og C
 * Kombintor-funksjonen tar inn (nullable) av A, B og C og returner (nullable) R
 * Resultatet er en tidslnije med tidsenhet T og innhold R
 */
fun <A, B, C, R, T : Tidsenhet> Tidslinje<A, T>.snittKombinerMed(
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
