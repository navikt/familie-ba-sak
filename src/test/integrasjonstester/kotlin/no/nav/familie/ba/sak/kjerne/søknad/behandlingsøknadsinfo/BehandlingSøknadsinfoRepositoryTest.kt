package no.nav.familie.ba.sak.kjerne.søknad.behandlingsøknadsinfo

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class BehandlingSøknadsinfoRepositoryTest(
    @Autowired private val behandlingSøknadsinfoRepository: BehandlingSøknadsinfoRepository,
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
    fun `save lagrer BehandlingSøknadsinfo`() {
        // Arrange
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak))
        val mottattDato = LocalDate.of(2025, 1, 1)
        val behandlingSøknadsinfo =
            BehandlingSøknadsinfo(
                behandling = behandling,
                journalpostId = "1",
                mottattDato = mottattDato.atStartOfDay(),
                brevkode = "BREV123",
                erDigital = true,
            )

        // Act
        val lagretBehandlingSøknadsinfo = behandlingSøknadsinfoRepository.save(behandlingSøknadsinfo)

        // Assert
        val hentetBehandlingSøknadsinfo = behandlingSøknadsinfoRepository.findById(lagretBehandlingSøknadsinfo.id)

        assertThat(hentetBehandlingSøknadsinfo.get())
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes("behandling(?!\\.id).*", "endretTidspunkt", "opprettetTidspunkt")
            .isEqualTo(behandlingSøknadsinfo)
    }

    @Test
    fun `findByBehandlingId returnerer lagret BehandlingSøknadsinfo`() {
        // Arrange
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak))
        val mottattDato = LocalDate.of(2025, 1, 1)
        val behandlingSøknadsinfo =
            BehandlingSøknadsinfo(
                behandling = behandling,
                journalpostId = "1",
                mottattDato = mottattDato.atStartOfDay(),
                brevkode = "BREV123",
                erDigital = true,
            )

        behandlingSøknadsinfoRepository.save(behandlingSøknadsinfo)

        // Act
        val hentetBehandlingSøknadsinfo = behandlingSøknadsinfoRepository.findByBehandlingId(behandling.id).single()

        // Assert
        assertThat(hentetBehandlingSøknadsinfo)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes("behandling(?!\\.id).*", "endretTidspunkt", "opprettetTidspunkt")
            .isEqualTo(behandlingSøknadsinfo)
    }

    @Test
    fun `findByBehandlingId returnerer tom liste hvis BehandlingSøknadsinfo ikke eksisterer`() {
        // Act
        val hentetBehandlingSøknadsinfo = behandlingSøknadsinfoRepository.findByBehandlingId(0)

        // Assert
        assertThat(hentetBehandlingSøknadsinfo).isEmpty()
    }
}
