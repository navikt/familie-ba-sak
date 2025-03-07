package no.nav.familie.ba.sak.kjerne.tidslinje.matematikk

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.joinIkkeNull
import no.nav.familie.tidslinje.Tidslinje

fun <K, V : Comparable<V>> minsteAvHver(
    aTidslinjer: Map<K, Tidslinje<V>>,
    bTidslinjer: Map<K, Tidslinje<V>>,
) = aTidslinjer.joinIkkeNull(bTidslinjer) { a, b -> minOf(a, b) }
