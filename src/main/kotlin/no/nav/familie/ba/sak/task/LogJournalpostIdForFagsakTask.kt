package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.task.LogJournalpostIdForFagsakTask.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = TASK_STEP_TYPE,
    beskrivelse = "Logger journalpost id for fagsak i secure logs",
    maxAntallFeil = 1,
)
class LogJournalpostIdForFagsakTask(
    val integrasjonKlient: IntegrasjonKlient,
    val fagsakRepository: FagsakRepository,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val secureLogger = LoggerFactory.getLogger("secureLogger")

        val fagsakId = task.payload
        val fagsak =
            fagsakRepository.finnFagsak(fagsakId.toLong())
                ?: throw Feil("Fagsak med id $fagsakId ikke funnet ved forsøk på oppslag av journalposter")

        secureLogger.info(
            "Henter journalpost id'er tilhørende fagsak $fagsakId",
        )

        val request =
            JournalposterForBrukerRequest(
                brukerId = Bruker(id = fagsak.aktør.aktivFødselsnummer(), type = BrukerIdType.FNR),
                antall = 50,
            )

        val journalpostIder = integrasjonKlient.hentJournalposterForBruker(request).map { it.journalpostId }

        secureLogger.info(
            "Fant følgende journalposter for fagsak $fagsakId $journalpostIder",
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "logJournalpostIdForFagsakTask"

        fun opprettTask(
            fagsakId: String,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = fagsakId,
            )
    }
}
