package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minste
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.største

abstract class TidslinjeMedAvhengigheter<T>(
    private val foregåendeTidslinjer: Collection<Tidslinje<*>>
) : Tidslinje<T>() {

    init {
        if (foregåendeTidslinjer.isEmpty())
            throw IllegalStateException("Skal ha avhengigheter, men listen over avhengigher er tom")
    }

    override fun fraOgMed() = foregåendeTidslinjer.map { it.fraOgMed() }.minste()
    override fun tilOgMed() = foregåendeTidslinjer.map { it.tilOgMed() }.største()
}
