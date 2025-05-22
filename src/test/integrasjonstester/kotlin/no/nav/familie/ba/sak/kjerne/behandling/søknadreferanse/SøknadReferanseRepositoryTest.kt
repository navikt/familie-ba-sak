package no.nav.familie.ba.sak.kjerne.behandling.søknadreferanse

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException

class SøknadReferanseRepositoryTest(
    @Autowired private val søknadReferanseRepository: SøknadReferanseRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `save lagrer SøknadReferanse`() {
        // Arrange
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak))
        val søknadReferanse =
            SøknadReferanse(
                behandlingId = behandling.id,
                journalpostId = "1",
            )

        // Act
        val lagretSøknadReferanse = søknadReferanseRepository.save(søknadReferanse)

        // Assert
        val hentetSøknadReferanse = søknadReferanseRepository.findById(lagretSøknadReferanse.id)

        assertThat(hentetSøknadReferanse.get()).isEqualTo(søknadReferanse)
    }

    @Test
    fun `save kaster feil hvis SøknadReferanse blir lagret med samme behandlingId to ganger`() {
        // Arrange
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak))
        val søknadReferanse =
            SøknadReferanse(
                behandlingId = behandling.id,
                journalpostId = "1",
            )

        søknadReferanseRepository.save(søknadReferanse)

        // Act & Assert
        assertThrows<DataIntegrityViolationException> {
            søknadReferanseRepository.save(
                SøknadReferanse(
                    behandlingId = behandling.id,
                    journalpostId = "2",
                ),
            )
        }
    }

    @Test
    fun `findByBehandlingId returnerer lagret SøknadReferanse`() {
        // Arrange
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak))
        val søknadReferanse =
            SøknadReferanse(
                behandlingId = behandling.id,
                journalpostId = "1",
            )

        søknadReferanseRepository.save(søknadReferanse)

        // Act
        val hentetSøknadReferanse = søknadReferanseRepository.findByBehandlingId(behandling.id)

        // Assert
        assertThat(hentetSøknadReferanse).isEqualTo(søknadReferanse)
    }

    @Test
    fun `findByBehandlingId returnerer null hvis SøknadReferanse ikke eksisterer`() {
        // Act
        val hentetSøknadReferanse = søknadReferanseRepository.findByBehandlingId(0)

        // Assert
        assertThat(hentetSøknadReferanse).isNull()
    }
}
