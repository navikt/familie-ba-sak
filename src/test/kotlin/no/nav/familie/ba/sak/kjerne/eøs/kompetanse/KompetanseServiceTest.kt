package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MinnebasertKompetanseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.TidslinjeService
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KompetanseServiceTest {

    val minnebasertKompetanseRepository = MinnebasertKompetanseRepository()
    val mockKompetanseRepository = mockk<KompetanseRepository>()
    val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk(relaxed = true)
    val tidslinjeService: TidslinjeService = mockk()

    val kompetanseService = KompetanseService(
        tidslinjeService,
        mockKompetanseRepository,
        tilbakestillBehandlingService,
    )

    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

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
    fun `bare reduksjon av periode skal ikke føre til endring i kompetansen`() {
        val behandlingId = 10L

        val lagretKompetanse = kompetanse(jan(2020), behandlingId, "SSSSSSSS", barn1)
            .lagreTil(mockKompetanseRepository)

        val oppdatertKompetanse = kompetanse(jan(2020), "  SSSSS  ", barn1)
        kompetanseService.oppdaterKompetanse(lagretKompetanse.id, oppdatertKompetanse)

        val forventedeKompetanser = listOf(lagretKompetanse)

        assertEqualsUnordered(forventedeKompetanser, kompetanseService.hentKompetanser(behandlingId))
    }

    @Test
    fun `oppdatering som splitter kompetanse fulgt av sletting skal returnere til utgangspunktet`() {
        val behandlingId = 10L

        val lagretKompetanse = kompetanse(jan(2020), behandlingId, "---------", barn1, barn2, barn3)
            .lagreTil(mockKompetanseRepository)

        val oppdatertKompetanse = kompetanse(jan(2020), "  PP", barn2, barn3)

        kompetanseService.oppdaterKompetanse(lagretKompetanse.id, oppdatertKompetanse)

        val forventedeKompetanser = KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("--", barn1, barn2, barn3)
            .medKompetanse("  --", barn1)
            .medKompetanse("  PP", barn2, barn3)
            .medKompetanse("    -----", barn1, barn2, barn3)
            .byggKompetanser()

        assertEqualsUnordered(forventedeKompetanser, kompetanseService.hentKompetanser(behandlingId))

        val kompetanseSomSkalSlettes = kompetanseService.finnKompetanse(behandlingId, oppdatertKompetanse)
        kompetanseService.slettKompetanse(kompetanseSomSkalSlettes.id)

        assertEqualsUnordered(listOf(lagretKompetanse), kompetanseService.hentKompetanser(behandlingId))
    }

    @Test
    fun `oppdatering som endrer deler av en kompetanse, skal resultarere i en splitt`() {
        val behandlingId = 10L

        KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("SSS", barn1)
            .medKompetanse("---------", barn2, barn3)
            .medKompetanse("   SSSS", barn1)
            .lagreTil(mockKompetanseRepository)

        val lagretKompetanse = kompetanseService.finnKompetanse(
            behandlingId, kompetanse(jan(2020), "SSS", barn1)
        )

        val oppdatertKompetanse = kompetanse(jan(2020), "PP", barn1)
        kompetanseService.oppdaterKompetanse(lagretKompetanse.id, oppdatertKompetanse)

        val forventedeKompetanser = KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("PP", barn1)
            .medKompetanse("  SSSSS", barn1)
            .medKompetanse("---------", barn2, barn3)
            .byggKompetanser()

        assertEqualsUnordered(forventedeKompetanser, kompetanseService.hentKompetanser(behandlingId))
    }
}

fun kompetanse(tidspunkt: Tidspunkt<Måned>, behandlingId: Long, s: String, vararg barn: Person) =
    KompetanseBuilder(tidspunkt, behandlingId).medKompetanse(s, *barn).byggKompetanser().first()

fun kompetanse(tidspunkt: Tidspunkt<Måned>, s: String, vararg barn: Person) =
    KompetanseBuilder(tidspunkt).medKompetanse(s, *barn).byggKompetanser().first()

private fun KompetanseService.finnKompetanse(behandlingId: Long, kompetanse: Kompetanse): Kompetanse {
    return this.hentKompetanser(behandlingId)
        .first { it == kompetanse }
}

private fun KompetanseBuilder.lagreTil(kompetanseRepository: KompetanseRepository): List<Kompetanse> {
    val byggKompetanser = this.byggKompetanser()
    return kompetanseRepository.saveAll(byggKompetanser)
}

private fun Kompetanse.lagreTil(kompetanseRepository: KompetanseRepository): Kompetanse {
    return kompetanseRepository.saveAll(listOf(this)).first()
}
