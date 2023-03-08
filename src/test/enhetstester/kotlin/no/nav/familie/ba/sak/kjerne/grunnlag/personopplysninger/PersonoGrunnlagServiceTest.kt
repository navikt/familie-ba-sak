package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class PersonoGrunnlagServiceTest {
    val personidentService = mockk<PersonidentService>()
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    val personopplysningerService = mockk<PersonopplysningerService>()
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()

    val persongrunnlagService = spyk(
        PersongrunnlagService(
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            statsborgerskapService = mockk(),
            arbeidsfordelingService = mockk(relaxed = true),
            personopplysningerService = personopplysningerService,
            personidentService = personidentService,
            saksstatistikkEventPublisher = mockk(relaxed = true),
            behandlingHentOgPersisterService = mockk(),
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

    @Test
    fun `hentOgLagreSøkerOgBarnINyttGrunnlag skal på inst- og EM-saker kun lagre èn instans av barnet, med personType BARN`() {
        val barnet = lagPerson()
        val behandlinger = listOf(FagsakType.INSTITUSJON, FagsakType.BARN_ENSLIG_MINDREÅRIG).map { fagsakType ->
            lagBehandling(fagsak = defaultFagsak().copy(type = fagsakType))
        }
        behandlinger.forEach { behandling ->
            val nyttGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

            every {
                persongrunnlagService.lagreOgDeaktiverGammel(any())
            } returns nyttGrunnlag

            every {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnet.aktør)
            } returns PersonInfo(barnet.fødselsdato, barnet.navn, barnet.kjønn)

            every { personopplysningGrunnlagRepository.save(nyttGrunnlag) } returns nyttGrunnlag

            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = barnet.aktør,
                barnFraInneværendeBehandling = listOf(barnet.aktør),
                barnFraForrigeBehandling = listOf(barnet.aktør),
                behandling = behandling,
                målform = Målform.NB
            ).apply {
                Assertions.assertThat(this.personer)
                    .hasSize(1)
                    .extracting("type")
                    .containsExactly(PersonType.BARN)
            }
        }
    }
}
