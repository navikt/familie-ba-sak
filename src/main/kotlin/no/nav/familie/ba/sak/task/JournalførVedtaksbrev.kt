package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.task.JournalførVedtaksbrev.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Journalfør brev i Joark", maxAntallFeil = 3)
class JournalførVedtaksbrev(
        private val integrasjonTjeneste: IntegrasjonTjeneste,
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val vedtakId = task.payload.toLong()
        val vedtak = behandlingService.hentVedtak(vedtakId)
                     ?: throw Exception("Fant ikke vedtak med id $vedtakId i forbindelse med Journalføring av vedtaksbrev")

        val fnr = vedtak.behandling.fagsak.personIdent.ident
        val pdf = behandlingService.hentPdfForVedtak(vedtak)

        LOG.debug("Journalfører vedtaksbrev for vedtak med ID $vedtakId")
        val journalpostId = integrasjonTjeneste.journalFørVedtaksbrev(pdf, fnr)

        val nyTask = Task.nyTask(DistribuerVedtaksbrev.TASK_STEP_TYPE, journalpostId, task.metadata)
        taskRepository.save(nyTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "journalførTilJoark"
        val LOG: Logger = LoggerFactory.getLogger(JournalførVedtaksbrev::class.java)
    }
}
