package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.task.JournalførVedtaksbrev.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.objectMapper
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
        private val vedtakService: VedtakService,
        private val dokumentService: DokumentService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val vedtakId = task.payload.toLong()
        val vedtak = vedtakService.hent(vedtakId)!!

        val pdf = dokumentService.hentPdfForVedtak(vedtak)
        val fnr = vedtak.behandling.fagsak.personIdent.ident
        val fagsakId = "${vedtak.behandling.fagsak.id}"

        val journalpostId = integrasjonTjeneste.journalFørVedtaksbrev(pdf, fnr, fagsakId)

        val nyTask = Task.nyTask(
                type = DistribuerVedtaksbrev.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                        DistribuerVedtaksbrevDTO(
                                personIdent = vedtak.behandling.fagsak.personIdent.ident,
                                behandlingId = vedtak.behandling.id,
                                journalpostId = journalpostId
                        )),
                properties = task.metadata)
        taskRepository.save(nyTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "journalførTilJoark"
        val LOG: Logger = LoggerFactory.getLogger(JournalførVedtaksbrev::class.java)
    }
}
