package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.ba.sak.dokument.JournalførBrevTaskDTO
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.task.JournalførVedtaksbrev.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.Error

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Journalfør brev i Joark", maxAntallFeil = 3)
class JournalførVedtaksbrev(
    private val integrasjonTjeneste: IntegrasjonTjeneste,
    private val behandlingService: BehandlingService,
    private val taskRepository: TaskRepository
) : AsyncTaskStep {
    private lateinit var journalpostId: String

    override fun doTask(task: Task) {
        val behandlingVedtakId = task.payload.toLong()
        LOG.debug("Journalfører vedtaksbrev for vedtak med ID $behandlingVedtakId")
        val behandlingVedtak = behandlingService.hentBehandlingVedtak(behandlingVedtakId)
            ?: throw Error("Fant ikke vedtak med id $behandlingVedtakId i forbindelse med Journalføring av vedtaksbrev")
        integrasjonTjeneste.journalFørVedtaksbrev(fra(behandlingVedtak)) { journalpostId: String ->
            this.journalpostId = journalpostId
        }
    }

    private fun fra(behandlingVedtak: BehandlingVedtak): JournalførBrevTaskDTO {
        return JournalførBrevTaskDTO(
            fnr = behandlingVedtak.behandling.fagsak.personIdent?.ident!!,
            tittel = "Vedtak om innvilgelse av barnetrygd",
            pdf = behandlingService.hentPdfForBehandlingVedtak(behandlingVedtak),
            brevkode = "")
    }

    override fun onCompletion(task: Task) {
        val nyTask = Task.nyTask(IverksettMotDokdist.TASK_STEP_TYPE, journalpostId)
        taskRepository.save(nyTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "journalførTilJoark"
        val LOG = LoggerFactory.getLogger(JournalførVedtaksbrev::class.java)
    }
}