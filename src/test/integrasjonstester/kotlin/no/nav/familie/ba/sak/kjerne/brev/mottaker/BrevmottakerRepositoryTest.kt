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
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

class BrevmottakerRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val brevmottakerRepository: BrevmottakerRepository,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class FinnBrevmottakerePåBehandling {
        @Test
        fun `skal returnere brevmottakere sortert på opprettet tidspunkt`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            val brevmottaker1 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.FULLMEKTIG))
            val brevmottaker2 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.VERGE))
            val brevmottaker3 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.DØDSBO))

            // Gir rekkefølge på opprettet tidspunkt som avviker fra id-rekkefølgen
            val nå = LocalDateTime.now()
            jdbcTemplate.update("UPDATE brevmottaker SET opprettet_tid = ? WHERE id = ?", nå, brevmottaker1.id)
            jdbcTemplate.update("UPDATE brevmottaker SET opprettet_tid = ? WHERE id = ?", nå.minusMinutes(2), brevmottaker2.id)
            jdbcTemplate.update("UPDATE brevmottaker SET opprettet_tid = ? WHERE id = ?", nå.minusMinutes(1), brevmottaker3.id)

            // Act
            val brevmottakere = brevmottakerRepository.finnBrevmottakerePåBehandling(behandling.id)

            // Assert
            assertThat(brevmottakere.map { it.id }).containsExactly(brevmottaker2.id, brevmottaker3.id, brevmottaker1.id)
        }

        @Test
        fun `skal sortere på id når opprettet tidspunkt er likt`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            val brevmottaker1 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.FULLMEKTIG))
            val brevmottaker2 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.VERGE))

            val opprettetTidspunkt = LocalDateTime.now()
            jdbcTemplate.update("UPDATE brevmottaker SET opprettet_tid = ? WHERE id = ?", opprettetTidspunkt, brevmottaker1.id)
            jdbcTemplate.update("UPDATE brevmottaker SET opprettet_tid = ? WHERE id = ?", opprettetTidspunkt, brevmottaker2.id)

            // Oppdateringen gjør at Postgres normalt returnerer raden sist uten ORDER BY.
            // Ikke garantert (f.eks. ved indeksskanning etter HOT-oppdatering), men gjør testen mer treffsikker.
            jdbcTemplate.update("UPDATE brevmottaker SET navn = 'Oppdatert navn' WHERE id = ?", brevmottaker1.id)

            // Act
            val brevmottakere = brevmottakerRepository.finnBrevmottakerePåBehandling(behandling.id)

            // Assert
            assertThat(brevmottakere.map { it.id }).containsExactly(brevmottaker1.id, brevmottaker2.id)
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
