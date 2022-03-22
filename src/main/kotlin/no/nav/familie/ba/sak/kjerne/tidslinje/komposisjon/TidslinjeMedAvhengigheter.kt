package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerNull

abstract class TidslinjeMedAvhengigheter<DATA, T : Tidsenhet>(
    private val foregåendeTidslinjer: Collection<Tidslinje<*, T>>
) : Tidslinje<DATA, T>() {

    init {
        if (foregåendeTidslinjer.isEmpty()) {
            throw IllegalArgumentException("Det er ikke sendt med noen avhengigheter")
        }
    }

    override fun fraOgMed() = foregåendeTidslinjer
        .map { it.fraOgMed() }
        .minsteEllerNull()!!

    override fun tilOgMed() = foregåendeTidslinjer
        .map { it.tilOgMed() }
        .størsteEllerNull()!!
}
