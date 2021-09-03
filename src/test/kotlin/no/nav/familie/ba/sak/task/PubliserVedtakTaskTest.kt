package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.ba.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(MockKExtension::class)
class PubliserVedtakTaskTest {

    @MockK(relaxed = true)
    private lateinit var taskRepositoryMock: TaskRepository

    @MockK(relaxed = true)
    private lateinit var kafkaProducerMock: KafkaProducer

    @InjectMockKs
    lateinit var publiserVedtakTask: PubliserVedtakTask

    @MockK(relaxed = true)
    private lateinit var stønadsstatistikkService: StønadsstatistikkService

    @Test
    fun skalOppretteTask() {
        val task = PubliserVedtakTask.opprettTask("ident", 42)

        assertThat(task.payload).isEqualTo("42")
        assertThat(task.metadata["personIdent"]).isEqualTo("ident")
        assertThat(task.type).isEqualTo("publiserVedtakTask")
    }


    @Test
    fun `skal kjøre task`() {
        every { kafkaProducerMock.sendMessageForTopicVedtak(ofType(VedtakDVH::class)) }.returns(100)
        every { taskRepositoryMock.save(any()) } returns Task(type = "test", payload = "")

        val task = PubliserVedtakTask.opprettTask("ident", 42)
        publiserVedtakTask.doTask(task)
        taskRepositoryMock.save(task)

        val slot = slot<Task>()
        verify(exactly = 1) { taskRepositoryMock.save(capture(slot)) }
        assertThat(slot.captured.metadata["offset"]).isEqualTo("100")
    }
}