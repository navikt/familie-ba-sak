package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MinnebasertKompetanseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KompetanseServiceTest {

    val minnebasertKompetanseRepository = MinnebasertKompetanseRepository()
    val mockKompetanseRepository = mockk<KompetanseRepository>()

    val kompetanseService = KompetanseService(
        mockk(),
        mockKompetanseRepository
    )

    @BeforeEach
    fun init() {
        val idSlot = slot<Long>()
        val kompetanseListeSlot = slot<Iterable<Kompetanse>>()

        every { mockKompetanseRepository.findByBehandlingId(capture(idSlot)) } answers {
            minnebasertKompetanseRepository.hentKompetanser(idSlot.captured)
        }

        every { mockKompetanseRepository.getById(capture(idSlot)) } answers {
            minnebasertKompetanseRepository.hentKompetanse(idSlot.captured)
        }

        every { mockKompetanseRepository.saveAll(capture(kompetanseListeSlot)) } answers {
            minnebasertKompetanseRepository.save(kompetanseListeSlot.captured)
        }

        every { mockKompetanseRepository.deleteAll(capture(kompetanseListeSlot)) } answers {
            minnebasertKompetanseRepository.delete(kompetanseListeSlot.captured)
        }
    }

    @Test
    fun `bare endring av periode skal ikke ha effekt`() {

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
