package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.mockRepo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtenlandskPeriodebeløpServiceTest {

    val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp> = mockRepo()
    val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk(relaxed = true)
    val kompetanseService: KompetanseService = mockk()

    val utenlandskPeriodebeløpService = UtenlandskPeriodebeløpService(
        utenlandskPeriodebeløpRepository,
        tilbakestillBehandlingService,
        kompetanseService
    )

    @BeforeEach
    fun init() {
        utenlandskPeriodebeløpRepository.deleteAll()
    }

    @Test
    fun `skal tilpasse utenlandsk periodebeløp til endrede kompetanser`() {
        val behandlingId = 10L

        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())
        val barn3 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())

        UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("4444   555 666", "EUR", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        val kompetanser = KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("SS   SSSSS", barn1)
            .medKompetanse("  PPP", barn1, barn2, barn3)
            .medKompetanse("--   ----", barn2, barn3)
            .byggKompetanser()

        every { kompetanseService.hentKompetanser(behandlingId) } returns kompetanser

        utenlandskPeriodebeløpService.tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId)

        val faktiskeUtenlandskePeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        val forventedeUtenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("44   --555", "EUR", barn1)
            .bygg()

        assertEqualsUnordered(forventedeUtenlandskePeriodebeløp, faktiskeUtenlandskePeriodebeløp)
    }
}
