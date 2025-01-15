package no.nav.familie.ba.sak.kjerne.behandling.domene

import lagFagsak
import lagBehandling
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import randomAktør
import java.time.LocalDate

class BehandlingMigreringsinfoRepositoryTest(
    @Autowired
    private val behandlingMigreringsinfoRepository: BehandlingMigreringsinfoRepository,
    @Autowired
    private val fagsakRepository: FagsakRepository,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val aktørIdRepository: AktørIdRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class FinnSisteBehandlingMigreringsinfoPåFagsak {
        @Test
        fun `Skal finne siste BehandlingMigreringsInfo tilknyttet fagsak`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))
            val behandling1 = behandlingRepository.save(
                lagBehandling(
                    fagsak = fagsak,
                    aktiv = false,
                    status = BehandlingStatus.AVSLUTTET
                )
            )
            val behandling2 = behandlingRepository.save(lagBehandling(fagsak = fagsak, aktiv = true))

            val behandling1Migreringsdato = LocalDate.of(2022, 1, 1)
            val behandling2Migreringsdato = LocalDate.of(2021, 12, 31)

            behandlingMigreringsinfoRepository.save(BehandlingMigreringsinfo(behandling = behandling1, migreringsdato = behandling1Migreringsdato))
            val behandlingMigreringsinfo2 = behandlingMigreringsinfoRepository.save(BehandlingMigreringsinfo(behandling = behandling2, migreringsdato = behandling2Migreringsdato))

            // Act
            val sisteBehandlingMigreringsinfo = behandlingMigreringsinfoRepository.finnSisteBehandlingMigreringsInfoPåFagsak(fagsakId = fagsak.id)

            // Assert
            assertThat(sisteBehandlingMigreringsinfo).isNotNull
            assertThat(sisteBehandlingMigreringsinfo!!.id).isEqualTo(behandlingMigreringsinfo2.id)
        }

        @Test
        fun `Skal returnere null dersom det ikke finnes noen BehandlingMigreringsinfo tilknyttet fagsak`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))

            // Act
            val sisteBehandlingMigreringsinfo = behandlingMigreringsinfoRepository.finnSisteBehandlingMigreringsInfoPåFagsak(fagsakId = fagsak.id)

            // Assert
            assertThat(sisteBehandlingMigreringsinfo).isNull()
        }
    }
}
