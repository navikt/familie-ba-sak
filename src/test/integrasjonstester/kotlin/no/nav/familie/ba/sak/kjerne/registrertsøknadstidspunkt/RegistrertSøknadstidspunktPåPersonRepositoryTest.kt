package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

class RegistrertSøknadstidspunktPåPersonRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val registrertSøknadstidspunktRepository: RegistrertSøknadstidspunktPåPersonRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class FindByBehandlingId {
        @Test
        fun `skal returnere lagrede rader for behandlingen med korrekt søknadstidspunkt og aktør`() {
            // Arrange
            val behandling = opprettBehandling()
            val barn = aktørIdRepository.save(randomAktør())
            val søknadstidspunkt = LocalDate.of(2024, 3, 15)

            registrertSøknadstidspunktRepository.saveAndFlush(
                RegistrertSøknadstidspunktPåPerson(behandlingId = behandling.id, aktør = barn, søknadstidspunkt = søknadstidspunkt),
            )

            // Act
            val rader = registrertSøknadstidspunktRepository.findByBehandlingId(behandling.id)

            // Assert
            assertThat(rader).hasSize(1)
            assertThat(rader.single().aktør.aktørId).isEqualTo(barn.aktørId)
            assertThat(rader.single().søknadstidspunkt).isEqualTo(søknadstidspunkt)
        }

        @Test
        fun `skal ikke returnere rader fra en annen behandling`() {
            // Arrange
            val behandling = opprettBehandling()
            val annenBehandling = opprettBehandling()
            registrertSøknadstidspunktRepository.saveAndFlush(
                RegistrertSøknadstidspunktPåPerson(
                    behandlingId = annenBehandling.id,
                    aktør = aktørIdRepository.save(randomAktør()),
                    søknadstidspunkt = LocalDate.of(2024, 1, 1),
                ),
            )

            // Act & assert
            assertThat(registrertSøknadstidspunktRepository.findByBehandlingId(behandling.id)).isEmpty()
        }
    }

    @Nested
    inner class DeleteByBehandlingId {
        @Test
        fun `skal kun slette rader for angitt behandling`() {
            // Arrange
            val behandling = opprettBehandling()
            val annenBehandling = opprettBehandling()
            registrertSøknadstidspunktRepository.saveAndFlush(
                RegistrertSøknadstidspunktPåPerson(
                    behandlingId = behandling.id,
                    aktør = aktørIdRepository.save(randomAktør()),
                    søknadstidspunkt = LocalDate.of(2024, 2, 2),
                ),
            )
            registrertSøknadstidspunktRepository.saveAndFlush(
                RegistrertSøknadstidspunktPåPerson(
                    behandlingId = annenBehandling.id,
                    aktør = aktørIdRepository.save(randomAktør()),
                    søknadstidspunkt = LocalDate.of(2024, 3, 3),
                ),
            )

            // Act
            registrertSøknadstidspunktRepository.deleteByBehandlingId(behandling.id)

            // Assert
            assertThat(registrertSøknadstidspunktRepository.findByBehandlingId(behandling.id)).isEmpty()
            assertThat(registrertSøknadstidspunktRepository.findByBehandlingId(annenBehandling.id)).hasSize(1)
        }
    }

    @Nested
    inner class UnikConstraint {
        @Test
        fun `skal kaste DataIntegrityViolationException ved to rader for samme behandling og aktør`() {
            // Arrange
            val behandling = opprettBehandling()
            val barn = aktørIdRepository.save(randomAktør())
            registrertSøknadstidspunktRepository.saveAndFlush(
                RegistrertSøknadstidspunktPåPerson(behandlingId = behandling.id, aktør = barn, søknadstidspunkt = LocalDate.of(2024, 1, 1)),
            )

            // Act & assert
            assertThrows<DataIntegrityViolationException> {
                registrertSøknadstidspunktRepository.saveAndFlush(
                    RegistrertSøknadstidspunktPåPerson(behandlingId = behandling.id, aktør = barn, søknadstidspunkt = LocalDate.of(2024, 5, 5)),
                )
            }
        }
    }

    private fun opprettBehandling(): Behandling {
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        return behandlingRepository.save(lagBehandlingUtenId(fagsak))
    }
}
