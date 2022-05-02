package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties

class BehandleAnnullerFødselTaskTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Autowired
    lateinit var behandleAnnullertFødselTask: BehandleAnnullertFødselTask

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal avslutte riktig task for annullert fødsel`() {
        val tasker = arrayOf(
            taskRepository.save(
                Task(
                    type = DistribuerVedtaksbrevTask.TASK_STEP_TYPE,
                    payload = ""
                ).copy(
                    metadataWrapper = PropertiesWrapper(
                        Properties().apply {
                            this["callId"] = "ooo"
                        }
                    )
                )
            ),
            taskRepository.save(
                Task(
                    type = BehandleFødselshendelseTask.TASK_STEP_TYPE,
                    payload = ""
                ).copy(
                    metadataWrapper = PropertiesWrapper(
                        Properties().apply {
                            this["callId"] = "xxx"
                        }
                    )
                )
            ),

            taskRepository.save(
                Task(
                    type = BehandleFødselshendelseTask.TASK_STEP_TYPE,
                    payload = ""
                ).copy(
                    metadataWrapper = PropertiesWrapper(
                        Properties().apply {
                            this["callId"] = "ooo"
                        }
                    )
                ).ferdigstill()
            ),

            taskRepository.save(
                Task(
                    type = BehandleFødselshendelseTask.TASK_STEP_TYPE,
                    payload = ""
                ).copy(
                    metadataWrapper = PropertiesWrapper(
                        Properties().apply {
                            this["callId"] = "ooo"
                        }
                    )
                )
            ),
        )

        behandleAnnullertFødselTask.doTask(
            BehandleAnnullertFødselTask.opprettTask(
                BehandleAnnullerFødselDto(
                    emptyList(),
                    "ooo"
                )
            )
        )

        assertThat(taskRepository.findById(tasker[0].id).get().status).isEqualTo(Status.UBEHANDLET)
        assertThat(taskRepository.findById(tasker[1].id).get().status).isEqualTo(Status.UBEHANDLET)
        assertThat(taskRepository.findById(tasker[2].id).get().status).isEqualTo(Status.FERDIG)
        assertThat(taskRepository.findById(tasker[3].id).get().status).isEqualTo(Status.AVVIKSHÅNDTERT)
    }
}
