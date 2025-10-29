package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.task.LogFagsakIdForJournalpostTask.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = TASK_STEP_TYPE,
    beskrivelse = "Logger fagsak id for journalpost i secure logs",
    maxAntallFeil = 1,
)
class LogFagsakIdForJournalpostTask(
    val integrasjonKlient: IntegrasjonKlient,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val secureLogger = LoggerFactory.getLogger("secureLogger")

        val journalpostId = task.payload

        secureLogger.info("Henter journalpost m/ id $journalpostId for oppslag av fagsak id")

        val journalpost = integrasjonKlient.hentJournalpost(journalpostId)

        journalpost.sak?.let { secureLogger.info(it.convertDataClassToJson()) } ?: throw Feil("Fant ikke fagsak informasjon i journalpost $journalpostId")
    }

    companion object {
        const val TASK_STEP_TYPE = "logFagsakIdForJournalpost"

        fun opprettTask(
            journalpostId: String,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = journalpostId,
            )
    }
}
