package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.task.dto.SimuleringTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = SimuleringTask.TASK_STEP_TYPE,
                     beskrivelse = "Simulering av regelkjøring for fødselshendelse",
                     maxAntallFeil = 3)
class SimuleringTask(
        private val stegService: StegService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val simuleringTaskDTO = objectMapper.readValue(task.payload, SimuleringTaskDTO::class.java)
        try {
            LOG.info("Kjører simulering task")
            stegService.regelkjørBehandling(simuleringTaskDTO.nyBehandling, simuleringTaskDTO.skalBehandlesHosInfotrygd)
        } catch (e: SimulationException) {
            LOG.info("Simulering av behandling. Data ikke persistert.")
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "simuleringTask"
        val LOG = LoggerFactory.getLogger(this::class.java)

        fun opprettTask(simuleringTaskDTO: SimuleringTaskDTO): Task {
            return Task.nyTask(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(simuleringTaskDTO)
            )
        }
    }
}

class SimulationException : RuntimeException("Simulering OK")