package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.dokument.JournalførBrevTaskDTO
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.task.JournalførVedtaksbrev.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.objectMapper
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
    private val taskRepository: TaskRepository
) : AsyncTaskStep {
    private lateinit var journalpostId: String

    override fun doTask(task: Task) {
        val journalførTask = objectMapper.readValue(task.payload, JournalførBrevTaskDTO::class.java)
        LOG.debug("Journalfører vedtaksbrev for vedtak med ID ${journalførTask.behandlingsVedtakId}")
        integrasjonTjeneste.journalFørVedtaksbrev(journalførTask) { journalpostId: String ->
            this.journalpostId = journalpostId
        }
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