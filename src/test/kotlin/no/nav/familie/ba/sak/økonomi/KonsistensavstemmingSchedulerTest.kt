package no.nav.familie.ba.sak.økonomi

import io.mockk.*
import no.nav.familie.ba.sak.util.DbContainerInitializer
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
    lateinit var batchRepository: BatchRepository

    @Autowired
    lateinit var konsistensavstemmingScheduler: KonsistensavstemmingScheduler

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        konsistensavstemmingScheduler = KonsistensavstemmingScheduler(taskRepository, batchRepository)
        taskRepository = spyk(taskRepository)
    }

    @Test
    fun `Skal ikke trigge avstemming når det ikke er noen ledige batchkjøringer for dato`() {
        val dagensDato = LocalDate.now()
        val nyBatch = Batch(kjøreDato = dagensDato, status = KjøreStatus.TATT)
        batchRepository.save(nyBatch)

        konsistensavstemmingScheduler.utførKonsistensavstemming()

        verify { taskRepository wasNot called }
    }

    @Test
    fun `Skal ikke trigge avstemming når det ikke finnes batchkjøringer for dato`() {
        val imorgen = LocalDate.now().plusDays(1)
        val nyBatch = Batch(kjøreDato = imorgen)
        batchRepository.save(nyBatch)

        konsistensavstemmingScheduler.utførKonsistensavstemming()

        verify { taskRepository wasNot called }
    }

    @Test
    fun `Skal trigge en avstemming når det er ledig batchkjøring for dato`() {
        val dagensDato = LocalDate.now()
        val nyBatch = Batch(kjøreDato = dagensDato)
        batchRepository.save(nyBatch)

        konsistensavstemmingScheduler.utførKonsistensavstemming()

        Assertions.assertEquals(1, taskRepository.finnTasksTilFrontend(Status.UBEHANDLET, Pageable.unpaged()).size)
    }
}