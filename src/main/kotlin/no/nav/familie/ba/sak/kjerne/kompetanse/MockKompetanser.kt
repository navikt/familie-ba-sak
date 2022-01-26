package no.nav.familie.ba.sak.kjerne.kompetanse

import no.nav.familie.ba.sak.kjerne.kompetanse.domene.Kompetanse
import java.time.LocalDate

object MockKompetanser {

    val barn1 = 111111L
    val barn2 = 222222L
    val barn3 = 333333L

    private val kompetanser = mutableListOf(
        Kompetanse(
            id = 1,
            fom = LocalDate.of(2021, 2, 1),
            tom = LocalDate.of(2021, 11, 30),
            barn = listOf(barn1, barn2, barn3)
        )
    )

    fun hentKompetanser(behandlingId: Long): List<Kompetanse> = kompetanser

    fun oppdaterKompetanse(kompetanse: Kompetanse): List<Kompetanse> {
        return kompetanser
    }
}
