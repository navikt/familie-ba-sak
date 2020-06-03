package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

class SimuleringException(val behandlingResultatType: BehandlingResultatType?) : RuntimeException("Simulering Ok")

@Service
@TaskStepBeskrivelse(taskStepType = SimuleringTask.TASK_STEP_TYPE,
        beskrivelse = "Simulering av regelkjøring for fødselshendelse",
        maxAntallFeil = 3)
class SimuleringTask(
        private val stegService: StegService
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val nyBehandling = objectMapper.readValue(task.payload, NyBehandlingHendelse::class.java)
        try {
            stegService.regelkjørBehandling(nyBehandling)
        } catch (e: SimuleringException) {
            LOG.info("Simulering av behandling fullført med resultat: ${e.behandlingResultatType}")
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "simuleringTask"
        val LOG = LoggerFactory.getLogger(this::class.java)

        fun opprettTask(nyBehandling: NyBehandlingHendelse): Task {
            return Task.nyTask(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(nyBehandling)
            )
        }
    }
}