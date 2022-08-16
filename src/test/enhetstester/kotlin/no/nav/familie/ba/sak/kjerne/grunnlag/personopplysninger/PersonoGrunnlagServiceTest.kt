package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.junit.jupiter.api.Test
import java.time.YearMonth

class PersonoGrunnlagServiceTest {
    val personidentService = mockk<PersonidentService>()
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()

    val persongrunnlagService = spyk(
        PersongrunnlagService(
            personopplysningGrunnlagRepository = mockk(),
            statsborgerskapService = mockk(),
            arbeidsfordelingService = mockk(),
            personopplysningerService = mockk(),
            personidentService = personidentService,
            saksstatistikkEventPublisher = mockk(),
            behandlingRepository = mockk(),
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            loggService = mockk(),
            arbeidsforholdService = mockk()
        )
    )

    @Test
    fun `Skal sende med barna fra forrige behandling ved førstegangsbehandling nummer to`() {
        val søker = lagPerson()
        val barnFraForrigeBehanlding = lagPerson(type = PersonType.BARN)
        val barn = lagPerson(type = PersonType.BARN)

        val barnFnr = barn.aktør.aktivFødselsnummer()
        val søkerFnr = søker.aktør.aktivFødselsnummer()

        val forrigeBehandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        val forrigeBehandlingPersongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = forrigeBehandling.id,
                personer = arrayOf(søker, barnFraForrigeBehanlding)
            )

        val søknadDTO = lagSøknadDTO(
            søkerIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr)
        )

        every { personidentService.hentOgLagreAktør(søkerFnr, true) } returns søker.aktør
        every { personidentService.hentOgLagreAktør(barnFnr, true) } returns barn.aktør

        every { persongrunnlagService.hentAktiv(forrigeBehandling.id) } returns forrigeBehandlingPersongrunnlag

        every {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(
                forrigeBehandling.id,
                barnFraForrigeBehanlding.aktør
            )
        } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now(), tom = YearMonth.now()))

        every {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns PersonopplysningGrunnlag(behandlingId = behandling.id)

        persongrunnlagService.registrerBarnFraSøknad(
            søknadDTO = søknadDTO,
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling
        )
        verify(exactly = 1) {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = søker.aktør,
                barnFraInneværendeBehandling = listOf(barn.aktør),
                barnFraForrigeBehandling = listOf(barnFraForrigeBehanlding.aktør),
                behandling = behandling,
                målform = søknadDTO.søkerMedOpplysninger.målform
            )
        }
    }
}
