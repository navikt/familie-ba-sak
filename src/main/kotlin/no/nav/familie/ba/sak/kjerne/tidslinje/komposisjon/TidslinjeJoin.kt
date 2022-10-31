package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

fun <K, V, H, R, T : Tidsenhet> Map<K, Tidslinje<V, T>>.outerJoin(
    høyreTidslinjer: Map<K, Tidslinje<H, T>>,
    kombinator: (V?, H?) -> R?
): Map<K, Tidslinje<R, T>> {
    val venstreTidslinjer = this
    val alleNøkler = venstreTidslinjer.keys + høyreTidslinjer.keys

    return alleNøkler.associateWith { nøkkel ->
        val venstreTidslinje = venstreTidslinjer.getOrDefault(nøkkel, TomTidslinje())
        val høyreTidslinje = høyreTidslinjer.getOrDefault(nøkkel, TomTidslinje())

        venstreTidslinje.kombinerMed(høyreTidslinje, kombinator)
    }
}

fun <K, V, H, R, T : Tidsenhet> Map<K, Tidslinje<V, T>>.leftJoin(
    høyreTidslinjer: Map<K, Tidslinje<H, T>>,
    kombinator: (V?, H?) -> R?
): Map<K, Tidslinje<R, T>> {
    val venstreTidslinjer = this
    val venstreNøkler = venstreTidslinjer.keys

    return venstreNøkler.associateWith { nøkkel ->
        val venstreTidslinje = venstreTidslinjer.getOrDefault(nøkkel, TomTidslinje())
        val høyreTidslinje = høyreTidslinjer.getOrDefault(nøkkel, TomTidslinje())

        venstreTidslinje.kombinerMed(høyreTidslinje, kombinator)
    }
}
