package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerUtenNullOgIkkeTom

fun <V> konkatenerTidslinjer(vararg tidslinje: Tidslinje<V>): Tidslinje<V> = tidslinje.toList().kombinerUtenNullOgIkkeTom { it.single() }

operator fun <V> Tidslinje<V>.plus(tidslinje: Tidslinje<V>): Tidslinje<V> = konkatenerTidslinjer(this, tidslinje)
