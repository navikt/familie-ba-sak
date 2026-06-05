package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RegistrertSøknadstidspunktPåPersonServiceTest {
    private val mockRegistrertSøknadstidspunktPåPersonRepository = mockk<RegistrertSøknadstidspunktPåPersonRepository>()
    private val mockPersongrunnlagService = mockk<PersongrunnlagService>()
    private val mockBehandlingSøknadsinfoService = mockk<BehandlingSøknadsinfoService>()
    private val mockSøknadGrunnlagService = mockk<SøknadGrunnlagService>()
    private val mockFeatureToggleService = mockk<FeatureToggleService>()

    private val registrertSøknadstidspunktService =
        RegistrertSøknadstidspunktPåPersonService(
            registrertSøknadstidspunktRepository = mockRegistrertSøknadstidspunktPåPersonRepository,
            persongrunnlagService = mockPersongrunnlagService,
            behandlingSøknadsinfoService = mockBehandlingSøknadsinfoService,
            søknadGrunnlagService = mockSøknadGrunnlagService,
            featureToggleService = mockFeatureToggleService,
        )

    @BeforeEach
    fun setup() {
        every { mockFeatureToggleService.isEnabled(FeatureToggle.KAN_REGISTRERE_SØKNADSTIDSPUNKT) } returns true
    }

    @Nested
    inner class LagreSøknadstidspunkterPåBarn {
        @Test
        fun `skal erstatte eksisterende rader og lagre én rad per person, også for person uten andel`() {
            // Arrange
            val behandling = lagBehandling()
            val barnA = lagPerson(type = PersonType.BARN)
            val barnB = lagPerson(type = PersonType.BARN)
            val søknadstidspunkt = LocalDate.of(2025, 4, 15)

            every { mockPersongrunnlagService.hentPersonerPåBehandling(any(), behandling) } returns listOf(barnA, barnB)
            every { mockRegistrertSøknadstidspunktPåPersonRepository.deleteByBehandlingId(any()) } just Runs
            val lagretSlot = slot<List<RegistrertSøknadstidspunktPåPerson>>()
            every { mockRegistrertSøknadstidspunktPåPersonRepository.saveAll(capture(lagretSlot)) } answers { firstArg() }

            // Act
            registrertSøknadstidspunktService.lagreSøknadstidspunkterPåBarn(
                behandling = behandling,
                søknadstidspunktPerPerson =
                    listOf(
                        RegistrertSøknadstidspunkt(barnA.aktør.aktivFødselsnummer(), søknadstidspunkt),
                        RegistrertSøknadstidspunkt(barnB.aktør.aktivFødselsnummer(), søknadstidspunkt),
                    ),
            )

            // Assert
            verify(exactly = 1) { mockRegistrertSøknadstidspunktPåPersonRepository.deleteByBehandlingId(behandling.id) }
            assertThat(lagretSlot.captured).containsExactlyInAnyOrder(
                RegistrertSøknadstidspunktPåPerson(behandlingId = behandling.id, aktør = barnA.aktør, søknadstidspunkt = søknadstidspunkt),
                RegistrertSøknadstidspunktPåPerson(behandlingId = behandling.id, aktør = barnB.aktør, søknadstidspunkt = søknadstidspunkt),
            )
        }

        @Test
        fun `skal kaste funksjonell feil hvis søknadstidspunkt er frem i tid`() {
            // Arrange
            val behandling = lagBehandling()
            val barn = lagPerson(type = PersonType.BARN)

            // Act & assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    registrertSøknadstidspunktService.lagreSøknadstidspunkterPåBarn(
                        behandling = behandling,
                        søknadstidspunktPerPerson =
                            listOf(
                                RegistrertSøknadstidspunkt(barn.aktør.aktivFødselsnummer(), LocalDate.now().plusDays(1)),
                            ),
                    )
                }
            assertThat(feil.message).isEqualTo("Søknadstidspunkt kan ikke være frem i tid.")
        }

        @Test
        fun `skal kaste funksjonell feil hvis samme person sendes inn flere ganger`() {
            // Arrange
            val behandling = lagBehandling()
            val barn = lagPerson(type = PersonType.BARN)
            val søknadstidspunkt = LocalDate.of(2025, 4, 15)

            // Act & assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    registrertSøknadstidspunktService.lagreSøknadstidspunkterPåBarn(
                        behandling = behandling,
                        søknadstidspunktPerPerson =
                            listOf(
                                RegistrertSøknadstidspunkt(barn.aktør.aktivFødselsnummer(), søknadstidspunkt),
                                RegistrertSøknadstidspunkt(barn.aktør.aktivFødselsnummer(), søknadstidspunkt),
                            ),
                    )
                }
            assertThat(feil.message).isEqualTo("Kan ikke sette søknadstidspunkt flere ganger for samme person.")
            verify(exactly = 0) { mockRegistrertSøknadstidspunktPåPersonRepository.deleteByBehandlingId(any()) }
        }

        @Test
        fun `skal kaste funksjonell feil ved tom liste (skal ikke slette eksisterende)`() {
            // Arrange
            val behandling = lagBehandling()

            // Act & assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    registrertSøknadstidspunktService.lagreSøknadstidspunkterPåBarn(behandling = behandling, søknadstidspunktPerPerson = emptyList())
                }
            assertThat(feil.message).isEqualTo("Må sette søknadstidspunkt for minst én person.")
            verify(exactly = 0) { mockRegistrertSøknadstidspunktPåPersonRepository.deleteByBehandlingId(any()) }
        }
    }

    @Nested
    inner class HentForBehandling {
        @Test
        fun `skal returnere lagrede rader for behandlingen`() {
            // Arrange
            val behandling = lagBehandling()
            val barn = lagPerson(type = PersonType.BARN)
            val søknadstidspunkt = LocalDate.of(2025, 4, 15)
            val lagretRad = RegistrertSøknadstidspunktPåPerson(behandlingId = behandling.id, aktør = barn.aktør, søknadstidspunkt = søknadstidspunkt)

            every { mockRegistrertSøknadstidspunktPåPersonRepository.findByBehandlingId(behandling.id) } returns listOf(lagretRad)

            // Act
            val resultat = registrertSøknadstidspunktService.hentForBehandling(behandling.id)

            // Assert
            assertThat(resultat).containsExactly(lagretRad)
        }
    }

    @Nested
    inner class SettSøknadstidspunktForBarn {
        @Test
        fun `skal sette søknad mottatt-dato for barn som mangler, men ikke overskrive eksisterende`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
            val barnMedEksisterende = lagPerson(type = PersonType.BARN)
            val barnUtenEksisterende = lagPerson(type = PersonType.BARN)
            val søknadMottattDato = LocalDate.of(2025, 4, 15)

            every { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id) } returns søknadMottattDato.atStartOfDay()
            every { mockSøknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = null) } returns
                listOf(barnMedEksisterende.aktør, barnUtenEksisterende.aktør)
            every { mockRegistrertSøknadstidspunktPåPersonRepository.findByBehandlingId(behandling.id) } returns
                listOf(
                    RegistrertSøknadstidspunktPåPerson(
                        behandlingId = behandling.id,
                        aktør = barnMedEksisterende.aktør,
                        søknadstidspunkt = LocalDate.of(2020, 1, 1),
                    ),
                )
            every { mockPersongrunnlagService.hentBarna(behandling) } returns listOf(barnMedEksisterende, barnUtenEksisterende)
            val lagretSlot = slot<List<RegistrertSøknadstidspunktPåPerson>>()
            every { mockRegistrertSøknadstidspunktPåPersonRepository.saveAll(capture(lagretSlot)) } answers { firstArg() }

            // Act
            registrertSøknadstidspunktService.settSøknadstidspunktForBarn(behandling)

            // Assert – kun barnet uten eksisterende rad får default satt
            assertThat(lagretSlot.captured).containsExactly(
                RegistrertSøknadstidspunktPåPerson(
                    behandlingId = behandling.id,
                    aktør = barnUtenEksisterende.aktør,
                    søknadstidspunkt = søknadMottattDato,
                ),
            )
        }

        @Test
        fun `skal ikke gjøre noe når årsak ikke er søknad`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)

            // Act
            registrertSøknadstidspunktService.settSøknadstidspunktForBarn(behandling)

            // Assert
            verify(exactly = 0) { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(any()) }
            verify(exactly = 0) { mockRegistrertSøknadstidspunktPåPersonRepository.saveAll(any<List<RegistrertSøknadstidspunktPåPerson>>()) }
        }

        @Test
        fun `skal også sette default for EØS-behandling (korrigeres via menyen)`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD, behandlingKategori = BehandlingKategori.EØS)
            val barn = lagPerson(type = PersonType.BARN)
            val søknadMottattDato = LocalDate.of(2025, 4, 15)

            every { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id) } returns søknadMottattDato.atStartOfDay()
            every { mockSøknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = null) } returns listOf(barn.aktør)
            every { mockRegistrertSøknadstidspunktPåPersonRepository.findByBehandlingId(behandling.id) } returns emptyList()
            every { mockPersongrunnlagService.hentBarna(behandling) } returns listOf(barn)
            val lagretSlot = slot<List<RegistrertSøknadstidspunktPåPerson>>()
            every { mockRegistrertSøknadstidspunktPåPersonRepository.saveAll(capture(lagretSlot)) } answers { firstArg() }

            // Act
            registrertSøknadstidspunktService.settSøknadstidspunktForBarn(behandling)

            // Assert
            assertThat(lagretSlot.captured).containsExactly(
                RegistrertSøknadstidspunktPåPerson(behandlingId = behandling.id, aktør = barn.aktør, søknadstidspunkt = søknadMottattDato),
            )
        }

        @Test
        fun `skal kun sette default for barn det er framstilt krav for`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
            val barnSøktFor = lagPerson(type = PersonType.BARN)
            val barnIkkeSøktFor = lagPerson(type = PersonType.BARN)
            val søknadMottattDato = LocalDate.of(2025, 4, 15)

            every { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id) } returns søknadMottattDato.atStartOfDay()
            every { mockSøknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = null) } returns listOf(barnSøktFor.aktør)
            every { mockRegistrertSøknadstidspunktPåPersonRepository.findByBehandlingId(behandling.id) } returns emptyList()
            every { mockPersongrunnlagService.hentBarna(behandling) } returns listOf(barnSøktFor, barnIkkeSøktFor)
            val lagretSlot = slot<List<RegistrertSøknadstidspunktPåPerson>>()
            every { mockRegistrertSøknadstidspunktPåPersonRepository.saveAll(capture(lagretSlot)) } answers { firstArg() }

            // Act
            registrertSøknadstidspunktService.settSøknadstidspunktForBarn(behandling)

            // Assert – barnet det ikke er framstilt krav for får ikke default satt
            assertThat(lagretSlot.captured).containsExactly(
                RegistrertSøknadstidspunktPåPerson(behandlingId = behandling.id, aktør = barnSøktFor.aktør, søknadstidspunkt = søknadMottattDato),
            )
        }

        @Test
        fun `skal ikke gjøre noe når søknad mottatt-dato mangler`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
            every { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id) } returns null

            // Act
            registrertSøknadstidspunktService.settSøknadstidspunktForBarn(behandling)

            // Assert
            verify(exactly = 0) { mockRegistrertSøknadstidspunktPåPersonRepository.saveAll(any<List<RegistrertSøknadstidspunktPåPerson>>()) }
        }

        @Test
        fun `skal ikke gjøre noe når feature toggle er av`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
            every { mockFeatureToggleService.isEnabled(FeatureToggle.KAN_REGISTRERE_SØKNADSTIDSPUNKT) } returns false

            // Act
            registrertSøknadstidspunktService.settSøknadstidspunktForBarn(behandling)

            // Assert
            verify(exactly = 0) { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(any()) }
            verify(exactly = 0) { mockRegistrertSøknadstidspunktPåPersonRepository.saveAll(any<List<RegistrertSøknadstidspunktPåPerson>>()) }
        }
    }
}
