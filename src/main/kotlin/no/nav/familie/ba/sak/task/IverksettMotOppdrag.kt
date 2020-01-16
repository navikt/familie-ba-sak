package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.task.IverksettMotOppdrag.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.økonomi.FAGSYSTEM
import no.nav.familie.ba.sak.økonomi.IverksettingTaskDTO
import no.nav.familie.ba.sak.økonomi.OppdragId
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Iverksett vedtak mot oppdrag", maxAntallFeil = 3)
class IverksettMotOppdrag(
        private val økonomiService: ØkonomiService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val iverksettingTask = objectMapper.readValue(task.payload, IverksettingTaskDTO::class.java)
        LOG.debug("Iverksetter vedtak med ID ${iverksettingTask.behandlingVedtakId} mot oppdrag")
        økonomiService.iverksettVedtak(iverksettingTask.behandlingVedtakId, iverksettingTask.saksbehandlerId)
    }

    override fun onCompletion(task: Task) {
        val iverksettingTask = objectMapper.readValue(task.payload, IverksettingTaskDTO::class.java)
        LOG.debug("Iverksetting av vedtak med ID ${iverksettingTask.behandlingVedtakId} mot oppdrag gikk OK")

        val nyTask = Task.nyTask(StatusFraOppdrag.TASK_STEP_TYPE, objectMapper.writeValueAsString(OppdragId(
                personIdent = iverksettingTask.personIdent,
                fagsystem = FAGSYSTEM,
                behandlingsId = iverksettingTask.behandlingsId.toString()
        )))
        taskRepository.save(nyTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "iverksettMotOppdrag"
        val LOG = LoggerFactory.getLogger(IverksettMotOppdrag::class.java)
    }
}