package no.nav.familie.ba.sak.kjerne.kompetanse

import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.MockKompetanseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class KompetanseServiceTest {

    @Test
    fun `bare endring av periode skal ikke ha effekt`() {
        val kompetanseService = KompetanseService(
            mockk(),
            MockKompetanseRepository()
        )

        val kompetanser = kompetanseService.hentKompetanser(1L)
        assertEquals(1, kompetanser.size)

        val kompetanse = kompetanser.first()
        val oppdatertKompetanse = kompetanse.copy(
            fom = kompetanse.fom!!.plusMonths(2),
            tom = kompetanse.tom!!.minusMonths(2),
        ).also { it.id = kompetanse.id }

        val kompetanserEtterOppdatering = kompetanseService.oppdaterKompetanse(oppdatertKompetanse).toList()
        assertEquals(1, kompetanserEtterOppdatering.size)
    }

    @Test
    fun `oppdatering som splitter kompetanse fulgt av sletting skal returnere til utgangspunktet`() {
        val kompetanseService = KompetanseService(
            mockk(),
            MockKompetanseRepository()
        )

        val kompetanser = kompetanseService.hentKompetanser(1L)
        assertEquals(1, kompetanser.size)

        val kompetanse = kompetanser.first()
        val oppdatertKompetanse = kompetanse.copy(
            fom = kompetanse.fom!!.plusMonths(2),
            tom = kompetanse.tom!!.minusMonths(2),
            søkersAktivitet = "Jobb"
        ).also { it.id = kompetanse.id }

        val kompetanserEtterOppdatering = kompetanseService.oppdaterKompetanse(oppdatertKompetanse).toList()
        assertEquals(3, kompetanserEtterOppdatering.size)

        val tilSletting = kompetanserEtterOppdatering.find { it.søkersAktivitet == "Jobb" }

        val kompetanserEtterSletting = kompetanseService.slettKompetanse(tilSletting!!.id)
        assertEquals(1, kompetanserEtterSletting.size)
    }
}
