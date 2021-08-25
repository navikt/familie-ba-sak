package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.dokument.hentBrevtype
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevTask.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send vedtaksbrev til Dokdist", maxAntallFeil = 3)
class DistribuerVedtaksbrevTask(
        private val stegService: StegService,
        private val behandlingService: BehandlingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val distribuerVedtaksbrevDTO = objectMapper.readValue(task.payload, DistribuerVedtaksbrevDTO::class.java)

        val behandling = behandlingService.hent(distribuerVedtaksbrevDTO.behandlingId)
        val distribuerDokumentDTO = DistribuerDokumentDTO(behandlingId = distribuerVedtaksbrevDTO.behandlingId,
                                                          journalpostId = distribuerVedtaksbrevDTO.journalpostId,
                                                          personIdent = distribuerVedtaksbrevDTO.personIdent,
                                                          brevmal = hentBrevtype(behandling),
                                                          erManueltSendt = false
        )
        stegService.håndterDistribuerVedtaksbrev(behandling = behandlingService.hent(distribuerVedtaksbrevDTO.behandlingId),
                                                 distribuerDokumentDTO = distribuerDokumentDTO)
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
