package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BrevmottakerRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val brevmottakerRepository: BrevmottakerRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class FinnBrevmottakerePåBehandling {
        @Test
        fun `skal returnere brevmottakere sortert på id`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            val brevmottaker1 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.FULLMEKTIG))
            val brevmottaker2 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.VERGE))
            val brevmottaker3 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.DØDSBO))

            // Oppdateringen gjør at Postgres normalt returnerer raden sist uten ORDER BY.
            // Ikke garantert (f.eks. ved indeksskanning etter HOT-oppdatering), men gjør testen mer treffsikker.
            brevmottakerRepository.saveAndFlush(brevmottaker1.also { it.navn = "Oppdatert navn" })

            // Act
            val brevmottakere = brevmottakerRepository.finnBrevmottakerePåBehandling(behandling.id)

            // Assert
            assertThat(brevmottakere.map { it.id }).containsExactly(brevmottaker1.id, brevmottaker2.id, brevmottaker3.id)
        }

        @Test
        fun `skal kun returnere brevmottakere for gitt behandling`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            val annenAktør = aktørIdRepository.save(randomAktør())
            val annenFagsak = fagsakRepository.save(lagFagsakUtenId(aktør = annenAktør))
            val annenBehandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = annenFagsak))

            val brevmottaker = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id))
            brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = annenBehandling.id))

            // Act
            val brevmottakere = brevmottakerRepository.finnBrevmottakerePåBehandling(behandling.id)

            // Assert
            assertThat(brevmottakere.map { it.id }).containsExactly(brevmottaker.id)
        }

        @Test
        fun `skal returnere tom liste når behandlingen ikke har brevmottakere`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            // Act
            val brevmottakere = brevmottakerRepository.finnBrevmottakerePåBehandling(behandling.id)

            // Assert
            assertThat(brevmottakere).isEmpty()
        }
    }
}
