package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.DistribuerDokumentTask.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send vedtaksbrev til Dokdist", maxAntallFeil = 3)
class DistribuerDokumentTask(
        private val stegService: StegService,
        private val behandlingService: BehandlingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val distribuerDokumentDTO = objectMapper.readValue(task.payload, DistribuerDokumentDTO::class.java)

        if (distribuerDokumentDTO.erVedtak) {
            stegService.håndterDistribuerVedtaksbrev(behandling = behandlingService.hent(distribuerDokumentDTO.behandlingId),
                                                     distribuerDokumentDTO = distribuerDokumentDTO)
        } else {
            // TODO: Håndtering av manuelle brev
        }
    }

    companion object {

        fun opprettDistribuerDokumentTask(distribuerDokumentDTO: DistribuerDokumentDTO,
                                          properties: Properties): Task {
            return Task.nyTask(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(distribuerDokumentDTO),
                    properties = properties,
            ).copy(
                    triggerTid = nesteGyldigeTriggertidForBehandlingIHverdager()
            )
        }

        const val TASK_STEP_TYPE = "distribuerVedtaksbrev"
    }
}

data class DistribuerDokumentDTO(
        val behandlingId: Long,
        val journalpostId: String,
        val personIdent: String,
        val erVedtak: Boolean = false
)
