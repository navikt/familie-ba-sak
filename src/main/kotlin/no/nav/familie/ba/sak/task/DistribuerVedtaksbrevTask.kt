package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevTask.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send vedtaksbrev til Dokdist", maxAntallFeil = 3)
class DistribuerVedtaksbrevTask(
        private val stegService: StegService,
        private val behandlingService: BehandlingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val distribuerVedtaksbrevDTO = objectMapper.readValue(task.payload, DistribuerVedtaksbrevDTO::class.java)
        stegService.håndterDistribuerVedtaksbrev(behandling = behandlingService.hent(distribuerVedtaksbrevDTO.behandlingId),
                                                 distribuerVedtaksbrevDTO = distribuerVedtaksbrevDTO)
    }

    companion object {
        const val TASK_STEP_TYPE = "distribuerVedtaksbrev"
    }
}

data class DistribuerVedtaksbrevDTO(
        val behandlingId: Long,
        val journalpostId: String,
        val personIdent: String
)
