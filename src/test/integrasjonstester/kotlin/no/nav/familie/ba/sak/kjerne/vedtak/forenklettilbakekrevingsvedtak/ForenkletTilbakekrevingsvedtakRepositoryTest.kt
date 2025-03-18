package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingsvedtak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException

class ForenkletTilbakekrevingsvedtakRepositoryTest(
    @Autowired
    private val aktørIdRepository: AktørIdRepository,
    @Autowired
    private val fagsakRepository: FagsakRepository,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val forenkletTilbakekrevingsvedtakRepository: ForenkletTilbakekrevingsvedtakRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class SaveTest {
        @Test
        fun `save skal kaste feil dersom det forsøkes å lagre ny ForenkletTilbakekrevingsvedtak når det allerede finnes en for behandlingen`() {
            // Arrange
            val søker = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            // Act
            forenkletTilbakekrevingsvedtakRepository.saveAndFlush(ForenkletTilbakekrevingsvedtak(behandling = behandling, fritekst = "Fritekst", samtykke = false))

            // Assert
            assertThrows<DataIntegrityViolationException> { forenkletTilbakekrevingsvedtakRepository.saveAndFlush(ForenkletTilbakekrevingsvedtak(behandling = behandling, fritekst = "Fritekst", samtykke = false)) }
        }
    }
}
