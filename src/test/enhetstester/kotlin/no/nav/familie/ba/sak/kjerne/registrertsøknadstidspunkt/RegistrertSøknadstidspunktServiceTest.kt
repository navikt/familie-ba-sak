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
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RegistrertSøknadstidspunktServiceTest {
    private val mockRegistrertSøknadstidspunktRepository = mockk<RegistrertSøknadstidspunktRepository>()
    private val mockPersongrunnlagService = mockk<PersongrunnlagService>()

    private val registrertSøknadstidspunktService =
        RegistrertSøknadstidspunktService(
            registrertSøknadstidspunktRepository = mockRegistrertSøknadstidspunktRepository,
            persongrunnlagService = mockPersongrunnlagService,
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
}
