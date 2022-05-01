package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

/**
 * Funksjon for å kjede sammen tidslinjer
 * Vil krasje med exception fra Iterable.single() hvis to eller flere tidslinjer overlapper
 */
fun <I, T : Tidsenhet> konkatenerTidslinjer(vararg tidslinje: Tidslinje<I, T>) =
    tidslinje.toList().snittKombiner {
        when {
            it.all { it == null } -> null
            else -> it.single { it != null }
        }
    }
