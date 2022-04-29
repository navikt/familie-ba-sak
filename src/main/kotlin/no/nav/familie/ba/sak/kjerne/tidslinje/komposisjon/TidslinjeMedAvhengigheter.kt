package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed

val MANGLER_AVHENGIGHETER = IllegalArgumentException("Det er ikke sendt med noen avhengigheter")

abstract class TidslinjeMedAvhengigheter<I, T : Tidsenhet>(
    private val foregåendeTidslinjer: Collection<Tidslinje<*, T>>
) : Tidslinje<I, T>() {

    init {
        if (foregåendeTidslinjer.isEmpty()) {
            throw MANGLER_AVHENGIGHETER
        }
    }
}

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.fraOgMed() = this
    .map { it.fraOgMed() }
    .minsteEllerNull() ?: throw MANGLER_AVHENGIGHETER

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.tilOgMed() = this
    .map { it.tilOgMed() }
    .størsteEllerNull() ?: throw MANGLER_AVHENGIGHETER
