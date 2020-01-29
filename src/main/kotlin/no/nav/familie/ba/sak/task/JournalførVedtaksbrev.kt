package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.task.JournalførVedtaksbrev.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
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
        val behandlingVedtakId = task.payload.toLong()
        val behandlingVedtak = behandlingService.hentBehandlingVedtak(behandlingVedtakId)
            ?: throw Exception("Fant ikke vedtak med id $behandlingVedtakId i forbindelse med Journalføring av vedtaksbrev")
        val fnr = behandlingVedtak.behandling.fagsak.personIdent?.ident!!
        val pdf = behandlingService.hentPdfForBehandlingVedtak(behandlingVedtak)

        LOG.info("Journalfører vedtaksbrev for vedtak med ID $behandlingVedtakId")
        val journalpostId = integrasjonTjeneste.journalFørVedtaksbrev(pdf, fnr)

        val nyTask = Task.nyTask(DistribuerVedtaksbrev.TASK_STEP_TYPE, journalpostId, task.metadata)
        taskRepository.save(nyTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "journalførTilJoark"
        val LOG = LoggerFactory.getLogger(JournalførVedtaksbrev::class.java)
    }
}