package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.TidslinjeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class KompetanseServiceTest {

    val tidslinjeService: TidslinjeService = mockk()
    val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk(relaxed = true)
    val kompetanseService = KompetanseService(
        tidslinjeService,
        tilbakestillBehandlingService,
        MockKompetanseRepository()
    )

    @Test
    fun `bare reduksjon av periode skal ikke føre til endring i kompetansen`() {

        val kompetanser = kompetanseService.hentKompetanser(1L)
        assertEquals(1, kompetanser.size)

        val gjeldendeKompetanse = kompetanser.first()
        val oppdatertKompetanse = gjeldendeKompetanse.copy(
            fom = gjeldendeKompetanse.fom!!.plusMonths(2),
            tom = gjeldendeKompetanse.tom!!.minusMonths(2),
        ).also { it.id = gjeldendeKompetanse.id }

        val kompetanserEtterOppdatering =
            kompetanseService.oppdaterKompetanse(gjeldendeKompetanse.id, oppdatertKompetanse).toList()
        assertEquals(1, kompetanserEtterOppdatering.size)
    }

    @Test
    fun `oppdatering som splitter kompetanse fulgt av sletting skal returnere til utgangspunktet`() {

        val kompetanser = kompetanseService.hentKompetanser(1L)
        assertEquals(1, kompetanser.size)

        val gjeldendeKompetanse = kompetanser.first()
        val oppdatertKompetanse = gjeldendeKompetanse.copy(
            fom = gjeldendeKompetanse.fom!!.plusMonths(2),
            tom = gjeldendeKompetanse.tom!!.minusMonths(2),
            søkersAktivitet = "Jobb"
        ).also { it.id = gjeldendeKompetanse.id }

        val kompetanserEtterOppdatering =
            kompetanseService.oppdaterKompetanse(gjeldendeKompetanse.id, oppdatertKompetanse).toList()
        assertEquals(3, kompetanserEtterOppdatering.size)

        val tilSletting = kompetanserEtterOppdatering.find { it.søkersAktivitet == "Jobb" }

        val kompetanserEtterSletting = kompetanseService.slettKompetanse(tilSletting!!.id)
        assertEquals(1, kompetanserEtterSletting.size)
    }
}
