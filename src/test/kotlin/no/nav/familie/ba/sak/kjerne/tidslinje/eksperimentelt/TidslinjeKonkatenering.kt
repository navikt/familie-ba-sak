package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

fun <I, T : Tidsenhet> konkatener(vararg tidslinje: Tidslinje<I, T>) =
    tidslinje.toList().snittKombiner {
        when {
            it.all { it == null } -> null
            else -> it.single { it != null }
        }
    }
