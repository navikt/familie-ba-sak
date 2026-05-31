package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrertSøknadstidspunktDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class RegistrertSøknadstidspunktServiceTest {
    private val mockRegistrertSøknadstidspunktRepository = mockk<RegistrertSøknadstidspunktRepository>()
    private val mockPersongrunnlagService = mockk<PersongrunnlagService>()
    private val mockBehandlingSøknadsinfoService = mockk<BehandlingSøknadsinfoService>()

    private val registrertSøknadstidspunktService =
        RegistrertSøknadstidspunktService(
            registrertSøknadstidspunktRepository = mockRegistrertSøknadstidspunktRepository,
            persongrunnlagService = mockPersongrunnlagService,
            behandlingSøknadsinfoService = mockBehandlingSøknadsinfoService,
        )

    @Test
    fun `lagre skal erstatte eksisterende rader og lagre én rad per person, også for person uten andel`() {
        // Arrange
        val behandling = lagBehandling()
        val barnA = lagPerson(type = PersonType.BARN)
        val barnB = lagPerson(type = PersonType.BARN)
        val søknadstidspunkt = LocalDate.of(2025, 4, 15)

        every { mockPersongrunnlagService.hentPersonerPåBehandling(any(), behandling) } returns listOf(barnA, barnB)
        every { mockRegistrertSøknadstidspunktRepository.deleteByBehandlingId(any()) } just Runs
        val lagretSlot = slot<List<RegistrertSøknadstidspunkt>>()
        every { mockRegistrertSøknadstidspunktRepository.saveAll(capture(lagretSlot)) } answers { firstArg() }

        // Act
        registrertSøknadstidspunktService.lagre(
            behandling = behandling,
            søknadstidspunktPerPerson =
                listOf(
                    RegistrertSøknadstidspunktDto(barnA.aktør.aktivFødselsnummer(), søknadstidspunkt),
                    RegistrertSøknadstidspunktDto(barnB.aktør.aktivFødselsnummer(), søknadstidspunkt),
                ),
        )

        // Assert
        verify(exactly = 1) { mockRegistrertSøknadstidspunktRepository.deleteByBehandlingId(behandling.id) }
        assertThat(lagretSlot.captured).containsExactlyInAnyOrder(
            RegistrertSøknadstidspunkt(behandlingId = behandling.id, aktør = barnA.aktør, søknadstidspunkt = søknadstidspunkt),
            RegistrertSøknadstidspunkt(behandlingId = behandling.id, aktør = barnB.aktør, søknadstidspunkt = søknadstidspunkt),
        )
    }

    @Test
    fun `lagre skal kaste funksjonell feil hvis søknadstidspunkt er frem i tid`() {
        // Arrange
        val behandling = lagBehandling()
        val barn = lagPerson(type = PersonType.BARN)

        // Act & assert
        val feil =
            assertThrows<FunksjonellFeil> {
                registrertSøknadstidspunktService.lagre(
                    behandling = behandling,
                    søknadstidspunktPerPerson =
                        listOf(
                            RegistrertSøknadstidspunktDto(barn.aktør.aktivFødselsnummer(), LocalDate.now().plusDays(1)),
                        ),
                )
            }
        assertThat(feil.message).isEqualTo("Søknadstidspunkt kan ikke være frem i tid.")
    }

    @Test
    fun `hentForBehandling skal mappe entitet til dto med personident`() {
        // Arrange
        val behandling = lagBehandling()
        val barn = lagPerson(type = PersonType.BARN)
        val søknadstidspunkt = LocalDate.of(2025, 4, 15)

        every { mockRegistrertSøknadstidspunktRepository.findByBehandlingId(behandling.id) } returns
            listOf(RegistrertSøknadstidspunkt(behandlingId = behandling.id, aktør = barn.aktør, søknadstidspunkt = søknadstidspunkt))

        // Act
        val resultat = registrertSøknadstidspunktService.hentForBehandling(behandling.id)

        // Assert
        assertThat(resultat).containsExactly(
            RegistrertSøknadstidspunktDto(personIdent = barn.aktør.aktivFødselsnummer(), søknadstidspunkt = søknadstidspunkt),
        )
    }

    @Test
    fun `settDefaultSøknadstidspunktForBarn skal sette søknad mottatt-dato for barn som mangler, men ikke overskrive eksisterende`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val barnMedEksisterende = lagPerson(type = PersonType.BARN)
        val barnUtenEksisterende = lagPerson(type = PersonType.BARN)
        val søknadMottattDato = LocalDate.of(2025, 4, 15)

        every { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id) } returns søknadMottattDato.atStartOfDay()
        every { mockRegistrertSøknadstidspunktRepository.findByBehandlingId(behandling.id) } returns
            listOf(
                RegistrertSøknadstidspunkt(
                    behandlingId = behandling.id,
                    aktør = barnMedEksisterende.aktør,
                    søknadstidspunkt = LocalDate.of(2020, 1, 1),
                ),
            )
        every { mockPersongrunnlagService.hentBarna(behandling) } returns listOf(barnMedEksisterende, barnUtenEksisterende)
        val lagretSlot = slot<List<RegistrertSøknadstidspunkt>>()
        every { mockRegistrertSøknadstidspunktRepository.saveAll(capture(lagretSlot)) } answers { firstArg() }

        // Act
        registrertSøknadstidspunktService.settDefaultSøknadstidspunktForBarn(behandling)

        // Assert – kun barnet uten eksisterende rad får default satt
        assertThat(lagretSlot.captured).containsExactly(
            RegistrertSøknadstidspunkt(
                behandlingId = behandling.id,
                aktør = barnUtenEksisterende.aktør,
                søknadstidspunkt = søknadMottattDato,
            ),
        )
    }

    @Test
    fun `settDefaultSøknadstidspunktForBarn skal ikke gjøre noe når årsak ikke er søknad`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)

        // Act
        registrertSøknadstidspunktService.settDefaultSøknadstidspunktForBarn(behandling)

        // Assert
        verify(exactly = 0) { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(any()) }
        verify(exactly = 0) { mockRegistrertSøknadstidspunktRepository.saveAll(any<List<RegistrertSøknadstidspunkt>>()) }
    }

    @Test
    fun `settDefaultSøknadstidspunktForBarn skal også sette default for EØS-behandling (korrigeres via menyen)`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD, behandlingKategori = BehandlingKategori.EØS)
        val barn = lagPerson(type = PersonType.BARN)
        val søknadMottattDato = LocalDate.of(2025, 4, 15)

        every { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id) } returns søknadMottattDato.atStartOfDay()
        every { mockRegistrertSøknadstidspunktRepository.findByBehandlingId(behandling.id) } returns emptyList()
        every { mockPersongrunnlagService.hentBarna(behandling) } returns listOf(barn)
        val lagretSlot = slot<List<RegistrertSøknadstidspunkt>>()
        every { mockRegistrertSøknadstidspunktRepository.saveAll(capture(lagretSlot)) } answers { firstArg() }

        // Act
        registrertSøknadstidspunktService.settDefaultSøknadstidspunktForBarn(behandling)

        // Assert
        assertThat(lagretSlot.captured).containsExactly(
            RegistrertSøknadstidspunkt(behandlingId = behandling.id, aktør = barn.aktør, søknadstidspunkt = søknadMottattDato),
        )
    }

    @Test
    fun `settDefaultSøknadstidspunktForBarn skal ikke gjøre noe når søknad mottatt-dato mangler`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        every { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id) } returns null

        // Act
        registrertSøknadstidspunktService.settDefaultSøknadstidspunktForBarn(behandling)

        // Assert
        verify(exactly = 0) { mockRegistrertSøknadstidspunktRepository.saveAll(any<List<RegistrertSøknadstidspunkt>>()) }
    }

    @Test
    fun `lagre skal kaste funksjonell feil ved tom liste (skal ikke slette eksisterende)`() {
        // Arrange
        val behandling = lagBehandling()

        // Act & assert
        val feil =
            assertThrows<FunksjonellFeil> {
                registrertSøknadstidspunktService.lagre(behandling = behandling, søknadstidspunktPerPerson = emptyList())
            }
        assertThat(feil.message).isEqualTo("Må sette søknadstidspunkt for minst én person.")
        verify(exactly = 0) { mockRegistrertSøknadstidspunktRepository.deleteByBehandlingId(any()) }
    }
}
