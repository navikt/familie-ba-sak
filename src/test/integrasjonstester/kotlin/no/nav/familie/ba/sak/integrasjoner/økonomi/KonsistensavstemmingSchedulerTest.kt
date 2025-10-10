package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.fake.FakeTaskRepositoryWrapper
import no.nav.familie.ba.sak.fake.tilKonkretTask
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragStartTask
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingStartTaskDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class KonsistensavstemmingSchedulerTest(
    @Autowired private val batchService: BatchService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val fakeTaskRepositoryWrapper: FakeTaskRepositoryWrapper,
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
        val lagredeTaskerAvType =
            fakeTaskRepositoryWrapper
                .hentLagredeTaskerAvType(KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE)
                .tilKonkretTask<KonsistensavstemmingStartTaskDTO>()

        val lagretTask = lagredeTaskerAvType.singleOrNull { it.batchId == nyBatch.id }

        assertThat(lagretTask).isNull()
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
        val lagredeTaskerAvType =
            fakeTaskRepositoryWrapper
                .hentLagredeTaskerAvType(KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE)
                .tilKonkretTask<KonsistensavstemmingStartTaskDTO>()

        val lagretTask = lagredeTaskerAvType.singleOrNull { it.batchId == nyBatch.id }

        assertThat(lagretTask).isNull()
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
        val lagredeTaskerAvType =
            fakeTaskRepositoryWrapper
                .hentLagredeTaskerAvType(KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE)
                .tilKonkretTask<KonsistensavstemmingStartTaskDTO>()

        val lagretTask = lagredeTaskerAvType.singleOrNull { it.batchId == nyBatch.id }

        assertThat(lagretTask).isNotNull
    }
}
