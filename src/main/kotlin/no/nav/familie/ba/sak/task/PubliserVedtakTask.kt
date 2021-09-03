package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.ba.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.ba.sak.task.PubliserVedtakTask.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
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
    }

    companion object {

        const val TASK_STEP_TYPE = "publiserVedtakTask"

        fun opprettTask(personIdent: String, behandlingsId: Long): Task {
            return Task(type = TASK_STEP_TYPE,
                               payload = behandlingsId.toString(),
                               properties = Properties().apply {
                                   this["personIdent"] = personIdent
                                   this["behandlingsId"] = behandlingsId.toString()
                               }
            )
        }
    }
}
