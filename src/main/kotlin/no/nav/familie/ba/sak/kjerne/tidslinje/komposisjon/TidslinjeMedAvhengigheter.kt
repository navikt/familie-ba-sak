package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerUendelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerUendelig

abstract class TidslinjeMedAvhengigheter<T>(
    private val foregåendeTidslinjer: Collection<Tidslinje<*>>
) : Tidslinje<T>() {

    init {
        if (foregåendeTidslinjer.isEmpty())
            throw IllegalStateException("Skal ha avhengigheter, men listen over avhengigher er tom")
    }

    override fun fraOgMed() = foregåendeTidslinjer.map { it.fraOgMed() }.minsteEllerUendelig()
    override fun tilOgMed() = foregåendeTidslinjer.map { it.tilOgMed() }.størsteEllerUendelig()
}
