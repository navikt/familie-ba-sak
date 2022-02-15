package no.nav.familie.ba.sak.kjerne.kompetanse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class KompetanseServiceTest {

    @Test
    fun test() {
        val kompetanseService = KompetanseService(MockKompetanseRepository())

        val kompetanser = kompetanseService.hentKompetanser(1L)
        assertEquals(1, kompetanser.size)

        val kompetanse = kompetanser.first()
        val oppdatertKompetanse = kompetanse.copy(
            fom = kompetanse.fom!!.plusMonths(2),
            tom = kompetanse.tom!!.minusMonths(2)
        )

        val kompetanserEtterOppdatering = kompetanseService.oppdaterKompetanse(oppdatertKompetanse)
        assertEquals(3, kompetanserEtterOppdatering.size)

        assertEquals(1, kompetanser.size)
    }
}
