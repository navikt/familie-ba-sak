package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.ba.sak.task.PubliserVedtakTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.vedtak.producer.KafkaProducer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Publiser vedtak til kafka", maxAntallFeil = 1)
class PubliserVedtakTask(val kafkaProducer: KafkaProducer,
                         val stønadsstatistikkService: StønadsstatistikkService,
                         val taskRepository: TaskRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val vedtakDVH = stønadsstatistikkService.hentVedtak(task.payload.toLong())
        task.metadata["offset"] = kafkaProducer.sendMessageForTopicVedtak(vedtakDVH).toString()

        taskRepository.save(task)
    }

    companion object {

        const val TASK_STEP_TYPE = "publiserVedtakTask"
        val LOG: Logger = LoggerFactory.getLogger(PubliserVedtakTask::class.java)


        fun opprettTask(personIdent: String, behandlingsId: Long): Task {
            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = behandlingsId.toString(),
                               properties = Properties().apply {
                                   this["personIdent"] = personIdent
                                   this["behandlingsId"] = behandlingsId.toString()
                               }
            )
        }
    }
}
