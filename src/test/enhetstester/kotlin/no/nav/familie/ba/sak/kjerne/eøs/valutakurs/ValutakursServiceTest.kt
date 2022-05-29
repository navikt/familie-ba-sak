package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.mockPeriodeBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ValutakursServiceTest {
    val valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs> = mockPeriodeBarnSkjemaRepository()
    val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService = mockk()

    val valutakursService = ValutakursService(
        valutakursRepository,
        emptyList()
    )

    val tilpassValutakurserTilUtenlandskePeriodebeløpService = TilpassValutakurserTilUtenlandskePeriodebeløpService(
        valutakursRepository,
        utenlandskPeriodebeløpService,
        emptyList(),
    )

    @BeforeEach
    fun init() {
        valutakursRepository.deleteAll()
    }

    @Test
    fun `skal tilpasse utenlandsk periodebeløp til endrede kompetanser`() {
        val behandlingId = BehandlingId(10L)

        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())
        val barn3 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())

        ValutakursBuilder(jan(2020), behandlingId)
            .medKurs("4444   555 666", "EUR", barn1, barn2, barn3)
            .lagreTil(valutakursRepository)

        val utenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("  777777777", "EUR", barn1)
            .bygg()

        every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns utenlandskePeriodebeløp

        tilpassValutakurserTilUtenlandskePeriodebeløpService.tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId)

        val faktiskeValutakurser = valutakursService.hentValutakurser(behandlingId)

        val forventedeValutakurser = ValutakursBuilder(jan(2020), behandlingId)
            .medKurs("  44$$$555$", "EUR", barn1)
            .bygg()

        assertEqualsUnordered(forventedeValutakurser, faktiskeValutakurser)
    }
}
