package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerNull

abstract class TidslinjeMedAvhengigheter<T>(
    private val foregåendeTidslinjer: Collection<Tidslinje<*>>
) : Tidslinje<T>() {

    // Når fraOgMed er større enn tilOgMed, så er effektivt tidslinjen tom
    override fun fraOgMed() = foregåendeTidslinjer
        .map { it.fraOgMed() }
        .minsteEllerNull() ?: Tidspunkt.iDag().neste()

    override fun tilOgMed() = foregåendeTidslinjer
        .map { it.tilOgMed() }
        .størsteEllerNull() ?: Tidspunkt.iDag().forrige()
}
