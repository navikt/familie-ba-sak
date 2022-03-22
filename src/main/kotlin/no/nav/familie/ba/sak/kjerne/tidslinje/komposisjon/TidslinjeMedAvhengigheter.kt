package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerNull
import java.time.temporal.Temporal

abstract class TidslinjeMedAvhengigheter<DATA, TID : Temporal>(
    private val foregåendeTidslinjer: Collection<Tidslinje<*, TID>>
) : Tidslinje<DATA, TID>() {

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
