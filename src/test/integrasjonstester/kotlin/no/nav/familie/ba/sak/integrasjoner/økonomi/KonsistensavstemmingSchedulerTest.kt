package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.verify
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class KonsistensavstemmingSchedulerTest(
    @Autowired private val batchService: BatchService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val taskRepository: TaskRepositoryWrapper,
    @Autowired private val konsistensavstemmingScheduler: KonsistensavstemmingScheduler,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal ikke trigge avstemming når det ikke er noen ledige batchkjøringer for dato`() {
        // Arrange
        val dagensDato = LocalDate.now()
        val nyBatch = Batch(kjøreDato = dagensDato, status = KjøreStatus.TATT)
        batchService.lagreNyStatus(nyBatch, KjøreStatus.TATT)

        // Act
        konsistensavstemmingScheduler.utførKonsistensavstemming()

        // Assert
        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    fun `Skal ikke trigge avstemming når det ikke finnes batchkjøringer for dato`() {
        // Arrange
        val imorgen = LocalDate.now().plusDays(1)
        val nyBatch = Batch(kjøreDato = imorgen)
        batchService.lagreNyStatus(nyBatch, KjøreStatus.LEDIG)

        // Act
        konsistensavstemmingScheduler.utførKonsistensavstemming()

        // Assert
        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    fun `Skal trigge en avstemming når det er ledig batchkjøring for dato`() {
        // Arrange
        val dagensDato = LocalDate.now()
        val nyBatch = Batch(kjøreDato = dagensDato)
        batchService.lagreNyStatus(nyBatch, KjøreStatus.LEDIG)
        fagsakService.hentLøpendeFagsaker().forEach { fagsakService.oppdaterStatus(it, FagsakStatus.AVSLUTTET) }

        // Act
        konsistensavstemmingScheduler.utførKonsistensavstemming()

        // Assert
        verify(exactly = 1) { taskRepository.save(any()) }
    }
}
