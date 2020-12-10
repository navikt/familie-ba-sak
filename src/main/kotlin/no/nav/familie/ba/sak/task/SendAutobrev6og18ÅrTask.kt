package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                     beskrivelse = "Send autobrev for barn som fyller 6 og 18 år til Dokdist",
                     maxAntallFeil = 3,
                     triggerTidVedFeilISekunder = 60 * 60 * 24)
class SendAutobrev6og18ÅrTask(
        private val behandlingService: BehandlingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val autobrevDTO = objectMapper.readValue(task.payload, Autobrev6og18ÅrDTO::class.java)
    }

    companion object {
        const val TASK_STEP_TYPE = "sendAutobrevVed6og18År"
        val LOG: Logger = LoggerFactory.getLogger(SendAutobrev6og18ÅrTask::class.java)
    }
}

