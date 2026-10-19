package no.nav.familie.ba.sak.statistikk.automatiskbehandlingavSoknadStatistikk

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.AutovedtakSøknadMåBehandlesManueltÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class AutovedtakSøknadStatistikkIntegrasjonTest(
    @Autowired private val statistikkService: AutovedtakSøknadStatistikkService,
    @Autowired private val repository: AutomatiskBehandlingAvSøknadStatistikkRepository,
    @Autowired transactionManager: PlatformTransactionManager,
) : AbstractSpringIntegrationTest() {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @Test
    fun `skal lagre identiske årsakskoder som separate rader i eksisterende tabell`() {
        // Arrange
        val behandlingId = -901234L
        val årsaker =
            listOf(
                AutovedtakSøknadMåBehandlesManueltÅrsak.SØKER_VILKÅR_BOSATT_I_RIKET,
                AutovedtakSøknadMåBehandlesManueltÅrsak.BARN_VILKÅR_BOSATT_I_RIKET,
                AutovedtakSøknadMåBehandlesManueltÅrsak.BARN_VILKÅR_BOSATT_I_RIKET,
            )

        try {
            // Act
            transactionTemplate.executeWithoutResult { statistikkService.lagreÅrsaker(behandlingId, årsaker) }

            // Assert
            val rader = repository.findAll().filter { it.behandlingId == behandlingId }
            assertThat(rader).hasSize(3)
            assertThat(rader.map { it.årsakTilManuellBehandling }).containsExactlyInAnyOrderElementsOf(årsaker.map { it.name })
            assertThat(rader.map { it.journalpostId }).containsOnlyNulls()
        } finally {
            repository.deleteAll(repository.findAll().filter { it.behandlingId == behandlingId })
        }
    }

    @Test
    fun `skal rulle tilbake statistikk når transaksjonen feiler og tillate nytt forsøk`() {
        // Arrange
        val behandlingId = -901235L
        val årsaker = listOf(AutovedtakSøknadMåBehandlesManueltÅrsak.INGEN_UTBETALING)

        try {
            // Act & Assert
            assertThrows<IllegalStateException> {
                transactionTemplate.executeWithoutResult {
                    statistikkService.lagreÅrsaker(behandlingId, årsaker)
                    throw IllegalStateException("Simulert feil før commit")
                }
            }
            assertThat(repository.findAll().filter { it.behandlingId == behandlingId }).isEmpty()

            // Act
            transactionTemplate.executeWithoutResult { statistikkService.lagreÅrsaker(behandlingId, årsaker) }

            // Assert
            assertThat(repository.findAll().filter { it.behandlingId == behandlingId }.map { it.årsakTilManuellBehandling }).containsExactly("INGEN_UTBETALING")
        } finally {
            repository.deleteAll(repository.findAll().filter { it.behandlingId == behandlingId })
        }
    }
}
