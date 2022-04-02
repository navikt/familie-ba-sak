package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.TomTidslinje

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
