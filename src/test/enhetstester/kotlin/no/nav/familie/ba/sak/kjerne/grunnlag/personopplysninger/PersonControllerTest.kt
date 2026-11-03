package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.ManuellDødsfallDto
import no.nav.familie.ba.sak.ekstern.restDomene.PersonInfoDto
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FalskIdentitetPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.PersonTilgangService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate

class PersonControllerTest {
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val personidentService = mockk<PersonidentService>()
    private val personTilgangService = mockk<PersonTilgangService>()
    private val utvidetBehandlingService = mockk<UtvidetBehandlingService>()
    private val tilgangService = mockk<TilgangService>()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()
    private val personController =
        PersonController(
            personopplysningerService = personopplysningerService,
            persongrunnlagService = persongrunnlagService,
            personidentService = personidentService,
            personTilgangService = personTilgangService,
            utvidetBehandlingService = utvidetBehandlingService,
            tilgangService = tilgangService,
            tilbakestillBehandlingService = tilbakestillBehandlingService,
        )

    @Nested
    inner class HentPerson {
        @Test
        fun `skal returnere fullstendig informasjon om person basert på data fra PDL når saksbehandler har tilgang`() {
            // Arrange
            val person = lagPerson()
            val personInfo =
                PersonInfo(
                    fødselsdato = person.fødselsdato,
                    navn = person.navn,
                    kjønn = person.kjønn,
                )
            val pdlPersonInfo = PdlPersonInfo.Person(personInfo = personInfo)

            every { personidentService.hentAktør(person.aktør.aktivFødselsnummer()) } returns person.aktør
            every { personTilgangService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør) } returns null
            every { personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør) } returns pdlPersonInfo

            // Act
            val respons = personController.hentPerson(PersonIdent(person.aktør.aktivFødselsnummer()))

            // Assert
            assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(respons.body).isNotNull
            assertThat(respons.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body!!.data).isNotNull

            val data: PersonInfoDto = respons.body!!.data!!
            assertThat(data.navn).isEqualTo(person.navn)
            assertThat(data.fødselsdato).isEqualTo(person.fødselsdato)
            assertThat(data.kjønn).isEqualTo(person.kjønn)
        }

        @Test
        fun `skal returnere informasjon om person basert på eksisterende personopplysninger i database dersom person har falsk identitet`() {
            // Arrange
            val person = lagPerson()
            val falskIdentitetPersonInfo =
                FalskIdentitetPersonInfo(
                    fødselsdato = person.fødselsdato,
                    navn = person.navn,
                    kjønn = person.kjønn,
                )
            val pdlPersonInfo = PdlPersonInfo.FalskPerson(falskIdentitetPersonInfo = falskIdentitetPersonInfo)

            every { personidentService.hentAktør(person.aktør.aktivFødselsnummer()) } returns person.aktør
            every { personTilgangService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør) } returns null
            every { personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør) } returns pdlPersonInfo

            // Act
            val respons = personController.hentPerson(PersonIdent(person.aktør.aktivFødselsnummer()))

            // Assert
            assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(respons.body).isNotNull
            assertThat(respons.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body!!.data).isNotNull

            val data: PersonInfoDto = respons.body!!.data!!
            assertThat(data.navn).isEqualTo(person.navn)
            assertThat(data.fødselsdato).isEqualTo(person.fødselsdato)
            assertThat(data.kjønn).isEqualTo(person.kjønn)
        }

        @Test
        fun `skal returnere maskert informasjon om person dersom saksbehandler ikke har tilgang`() {
            // Arrange
            val person = lagPerson()

            val personInfoDto =
                PersonInfoDto(
                    personIdent = person.aktør.aktivFødselsnummer(),
                    adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
                    harTilgang = false,
                )

            every { personidentService.hentAktør(person.aktør.aktivFødselsnummer()) } returns person.aktør
            every { personTilgangService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør) } returns personInfoDto

            // Act
            val respons = personController.hentPerson(PersonIdent(person.aktør.aktivFødselsnummer()))

            // Assert
            assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(respons.body).isNotNull
            assertThat(respons.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body!!.data).isNotNull

            val data: PersonInfoDto = respons.body!!.data!!
            assertThat(data).isEqualTo(personInfoDto)
        }
    }

    @Nested
    inner class HentOgOppdaterRegisteropplysninger {
        @Test
        fun `skal tilbakestille behandlingen til vilkårsvurdering når registeropplysningene er endret`() {
            // Arrange
            val behandling = lagBehandling()

            justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns PersonopplysningGrunnlag(id = 1, behandlingId = behandling.id)
            every { persongrunnlagService.oppdaterRegisteropplysninger(behandling.id) } returns PersonopplysningGrunnlag(id = 2, behandlingId = behandling.id)
            every { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) } returns behandling
            every { utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id) } returns mockk()

            // Act
            personController.hentOgOppdaterRegisteropplysninger(behandling.id)

            // Assert
            verifyOrder {
                persongrunnlagService.oppdaterRegisteropplysninger(behandling.id)
                tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id)
            }
        }

        @Test
        fun `skal ikke tilbakestille behandlingen når registeropplysningene er uendret`() {
            // Arrange
            val behandling = lagBehandling()
            val personopplysningGrunnlag = PersonopplysningGrunnlag(id = 1, behandlingId = behandling.id)

            justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns personopplysningGrunnlag
            every { persongrunnlagService.oppdaterRegisteropplysninger(behandling.id) } returns personopplysningGrunnlag
            every { utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id) } returns mockk()

            // Act
            personController.hentOgOppdaterRegisteropplysninger(behandling.id)

            // Assert
            verify(exactly = 0) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(any()) }
        }
    }

    @Nested
    inner class RegistrerManuellDødsfallPåPerson {
        @Test
        fun `skal tilbakestille behandlingen til vilkårsvurdering etter at dødsfall er registrert`() {
            // Arrange
            val behandling = lagBehandling()
            val personIdent = randomFnr()
            val dødsfallDato = LocalDate.of(2025, 3, 10)

            justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
            justRun { tilgangService.validerKanRedigereBehandling(any()) }
            justRun { persongrunnlagService.registrerManuellDødsfallPåPerson(any(), any(), any(), any()) }
            every { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) } returns behandling
            every { utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id) } returns mockk()

            // Act
            personController.registrerManuellDødsfallPåPerson(
                behandlingId = behandling.id,
                manuellDødsfallDto = ManuellDødsfallDto(dødsfallDato = dødsfallDato, personIdent = personIdent, begrunnelse = "Begrunnelse"),
            )

            // Assert
            verifyOrder {
                tilgangService.validerKanRedigereBehandling(behandling.id)
                persongrunnlagService.registrerManuellDødsfallPåPerson(any(), any(), any(), any())
                tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id)
            }
        }
    }
}
