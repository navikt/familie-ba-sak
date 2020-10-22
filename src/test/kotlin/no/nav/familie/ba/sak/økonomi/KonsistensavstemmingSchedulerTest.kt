package no.nav.familie.ba.sak.økonomi

import io.mockk.MockKAnnotations
import io.mockk.called
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen")
@Tag("integration")
class KonsistensavstemmingSchedulerTest {

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Autowired
    lateinit var batchService: BatchService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var konsistensavstemmingScheduler: KonsistensavstemmingScheduler

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        konsistensavstemmingScheduler = KonsistensavstemmingScheduler(batchService, behandlingService, fagsakService, taskRepository)
        taskRepository = spyk(taskRepository)
    }

    @Test
    fun `Skal ikke trigge avstemming når det ikke er noen ledige batchkjøringer for dato`() {
        val dagensDato = LocalDate.now()
        val nyBatch = Batch(kjøreDato = dagensDato, status = KjøreStatus.TATT)
        batchService.lagreNyStatus(nyBatch, KjøreStatus.TATT)

        konsistensavstemmingScheduler.utførKonsistensavstemming()

        verify { taskRepository wasNot called }
    }

    @Test
    fun `Skal ikke trigge avstemming når det ikke finnes batchkjøringer for dato`() {
        val imorgen = LocalDate.now().plusDays(1)
        val nyBatch = Batch(kjøreDato = imorgen)
        batchService.lagreNyStatus(nyBatch, KjøreStatus.LEDIG)

        konsistensavstemmingScheduler.utførKonsistensavstemming()

        verify { taskRepository wasNot called }
    }

    @Test
    fun `Skal trigge en avstemming når det er ledig batchkjøring for dato`() {
        val dagensDato = LocalDate.now()
        val nyBatch = Batch(kjøreDato = dagensDato)
        batchService.lagreNyStatus(nyBatch, KjøreStatus.LEDIG)
        fagsakService.hentLøpendeFagsaker().forEach { fagsakService.oppdaterStatus(it, FagsakStatus.AVSLUTTET) }

        konsistensavstemmingScheduler.utførKonsistensavstemming()

        val tasks = taskRepository.finnTasksMedStatus(listOf(Status.UBEHANDLET), Pageable.unpaged())

        Assertions.assertEquals(1, tasks.size)

        // Setter task til Ferdig for å unngå at den kjøres fra andre tester.
        tasks[0].status = Status.FERDIG
        taskRepository.save(tasks[0])
    }
}