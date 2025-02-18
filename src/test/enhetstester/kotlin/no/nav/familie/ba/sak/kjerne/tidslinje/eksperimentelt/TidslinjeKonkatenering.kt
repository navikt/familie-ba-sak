package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullOgIkkeTom
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombinerUtenNullOgIkkeTom
import no.nav.familie.tidslinje.Tidslinje as FamilieFellesTidslinje

/**
 * Funksjon for å kjede sammen tidslinjer
 * Vil krasje med exception fra Iterable.single() hvis to eller flere tidslinjer overlapper
 */
fun <I, T : Tidsenhet> konkatenerTidslinjer(vararg tidslinje: Tidslinje<I, T>): Tidslinje<I, T> = tidslinje.toList().kombinerUtenNullOgIkkeTom { it.single() }

operator fun <I, T : Tidsenhet> Tidslinje<I, T>.plus(tidslinje: Tidslinje<I, T>): Tidslinje<I, T> = konkatenerTidslinjer(this, tidslinje)

fun <V> konkatenerTidslinjer(vararg tidslinje: FamilieFellesTidslinje<V>): FamilieFellesTidslinje<V> = tidslinje.toList().kombinerUtenNullOgIkkeTom { it.single() }
