package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

/**
 * Funksjon for å kjede sammen tidslinjer
 * Vil krasje med exception fra Iterable.single() hvis to eller flere tidslinjer overlapper
 */
fun <I, T : Tidsenhet> konkatenerTidslinjer(vararg tidslinje: Tidslinje<I, T>) =
    tidslinje.toList().kombiner {
        when {
            it.all { it == null } -> null
            else -> it.single { it != null }
        }
    }

operator fun <I, T : Tidsenhet> Tidslinje<I, T>.plus(tidslinje: Tidslinje<I, T>): Tidslinje<I, T> =
    konkatenerTidslinjer(this, tidslinje)
