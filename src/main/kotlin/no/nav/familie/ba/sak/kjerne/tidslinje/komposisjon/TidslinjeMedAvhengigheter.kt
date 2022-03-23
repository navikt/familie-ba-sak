package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minste
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.største
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerNull

val MANGLER_AVHENGIGHETER = IllegalArgumentException("Det er ikke sendt med noen avhengigheter")

abstract class TidslinjeMedAvhengigheter<I, T : Tidsenhet>(
    private val foregåendeTidslinjer: Collection<Tidslinje<*, T>>
) : Tidslinje<I, T>() {

    init {
        if (foregåendeTidslinjer.isEmpty()) {
            throw MANGLER_AVHENGIGHETER
        }
    }

    override fun fraOgMed() = foregåendeTidslinjer
        .map { it.fraOgMed() }
        .minste()() ?: throw MANGLER_AVHENGIGHETER

    override fun tilOgMed() = foregåendeTidslinjer
        .map { it.tilOgMed() }
        .største() ?: throw MANGLER_AVHENGIGHETER
}
