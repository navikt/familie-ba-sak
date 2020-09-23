package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.task.StatusFraOppdragTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Henter status fra oppdrag", maxAntallFeil = 100)
class StatusFraOppdragTask(
        private val stegService: StegService,
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    /**
     * Metoden prøver å hente kvittering i ét døgn.
     * Får tasken kvittering som ikke er OK feiler vi tasken.
     */
    override fun doTask(task: Task) {
        val statusFraOppdragDTO = objectMapper.readValue(task.payload, StatusFraOppdragDTO::class.java)

        stegService.håndterStatusFraØkonomi(
                behandling = behandlingService.hent(behandlingId = statusFraOppdragDTO.behandlingsId),
                statusFraOppdragMedTask = StatusFraOppdragMedTask(statusFraOppdragDTO = statusFraOppdragDTO, task = task)
        )
    }

    override fun onCompletion(task: Task) {
        val statusFraOppdragDTO = objectMapper.readValue(task.payload, StatusFraOppdragDTO::class.java)
        val nyTask = PubliserVedtakTask.opprettTask(statusFraOppdragDTO.personIdent, statusFraOppdragDTO.behandlingsId)
        taskRepository.save(nyTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "statusFraOppdrag"
        val LOG: Logger = LoggerFactory.getLogger(StatusFraOppdragTask::class.java)
    }
}
