package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsrom
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerUendelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerUendelig

abstract class TidslinjeMedAvhengigheter<T>(
    private val foregåendeTidslinjer: Collection<Tidslinje<*>>
) : Tidslinje<T>() {

    init {
        if (foregåendeTidslinjer.size == 0)
            throw IllegalStateException("Skal ha avhengigheter, men listen over avhengigher er tom")
    }

    override fun tidsrom(): Tidsrom {
        val fom = foregåendeTidslinjer.map { it.tidsrom().start }.minsteEllerUendelig()
        val tom = foregåendeTidslinjer.map { it.tidsrom().endInclusive }.størsteEllerUendelig()

        return fom..tom
    }
}
