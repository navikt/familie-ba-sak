package no.nav.familie.ba.sak.kjerne.kompetanse

import no.nav.familie.ba.sak.kjerne.kompetanse.domene.Kompetanse
import java.time.LocalDate

object MockKompetanser {

    val barn1 = 111111L
    val barn2 = 222222L
    val barn3 = 333333L

    private val malKompetanse = Kompetanse(
        id = 1L,
        fom = LocalDate.of(2021, 2, 1),
        tom = LocalDate.of(2021, 11, 30),
        barn = listOf(barn1, barn2, barn3)
    )

    private val kompetanser = mutableMapOf(
        Pair(
            1L,
            malKompetanse.copy(id = 1)
        )
    )

    fun hentKompetanser(behandlingId: Long): Collection<Kompetanse> = kompetanser.values
        .map { it.copy(behandlingId = behandlingId) }

    // TODO Bør gjøre noe annet enn å legge til en tom kompetanse ...
    fun oppdaterKompetanse(kompetanse: Kompetanse): Collection<Kompetanse> {
        val nyId = kompetanser.keys.maxOf { it } + 1
        kompetanser[kompetanse.id] = kompetanse
        kompetanser[nyId] = malKompetanse.copy(id = nyId)
        return kompetanser.values
    }
}
