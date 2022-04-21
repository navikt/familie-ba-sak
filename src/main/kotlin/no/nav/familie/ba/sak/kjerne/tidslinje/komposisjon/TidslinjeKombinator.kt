package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

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
