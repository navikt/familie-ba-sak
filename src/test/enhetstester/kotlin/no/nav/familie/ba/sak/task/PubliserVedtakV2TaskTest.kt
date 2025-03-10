package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.ba.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PubliserVedtakV2TaskTest {
    private val taskRepositoryMock = mockk<TaskRepositoryWrapper>()
    private val kafkaProducerMock = mockk<KafkaProducer>()
    private val env = mockk<EnvService>()
    private val stønadsstatistikkService = mockk<StønadsstatistikkService>(relaxed = true)

    private val publiserVedtakV2Task =
        PubliserVedtakV2Task(
            kafkaProducer = kafkaProducerMock,
            stønadsstatistikkService = stønadsstatistikkService,
            env = env,
        )

    @BeforeEach
    fun initMocks() {
        every { env.erProd() } returns false
    }

    @Test
    fun skalOppretteTask() {
        val task = PubliserVedtakV2Task.opprettTask("ident", 42)

        Assertions.assertThat(task.payload).isEqualTo("42")
        Assertions.assertThat(task.metadata["personIdent"]).isEqualTo("ident")
        Assertions.assertThat(task.type).isEqualTo("publiserVedtakV2Task")
    }

    @Test
    fun `skal kjøre task`() {
        every { kafkaProducerMock.sendMessageForTopicVedtakV2(ofType(VedtakDVHV2::class)) }.returns(100)
        every { taskRepositoryMock.save(any()) } returns Task(type = "test", payload = "")

        val task = PubliserVedtakV2Task.opprettTask("ident", 42)
        publiserVedtakV2Task.doTask(task)
        taskRepositoryMock.save(task)

        val slot = slot<Task>()
        verify(exactly = 1) { taskRepositoryMock.save(capture(slot)) }
        Assertions.assertThat(slot.captured.metadata["offset"]).isEqualTo("100")
    }
}
