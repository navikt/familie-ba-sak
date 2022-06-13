package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.util.mockPeriodeBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PeriodeOgBarnSkjemaServiceTest {

    val mockKompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse> = mockPeriodeBarnSkjemaRepository()

    val periodeOgBarnSkjemaServiceKompetanse = PeriodeOgBarnSkjemaService(
        mockKompetanseRepository,
        emptyList(),
    )

    @Test
    fun `Skal kaste feil dersom vi prøver å lagre ugyldige verdier`() {
        val behandlingId1 = BehandlingId(10L)
        val behandlingId2 = BehandlingId(22L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)

        val kompetanser1 = KompetanseBuilder(jan(2020), behandlingId1)
            .medKompetanse("S>", barn1)
            .bygg()

        val kompetanser2 = KompetanseBuilder(jan(2020), behandlingId2)
            .medKompetanse("S>", barn2)
            .medKompetanse(" S>", barn2)
            .bygg()

        assertThrows<Tidslinje.Companion.TidslinjeFeilException> {
            periodeOgBarnSkjemaServiceKompetanse.lagreDifferanseOgVarsleAbonnenter(
                behandlingId2,
                kompetanser1,
                kompetanser2
            )
        }
    }
}
