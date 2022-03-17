package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = VedtakOmOvergangsstønadTask.TASK_STEP_TYPE,
    beskrivelse = "Håndterer vedtak om overgangsstønad",
    maxAntallFeil = 3
)
class VedtakOmOvergangsstønadTask(
    private val autovedtakSmåbarnstilleggService: AutovedtakSmåbarnstilleggService,
    private val personidentService: PersonidentService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val personIdent = task.payload
        logger.info("Håndterer vedtak om overgangsstønad. Se secureLog for detaljer")
        secureLogger.info("Håndterer vedtak om overgangsstønad for person $personIdent.")

        val aktør = personidentService.hentAktør(personIdent)

        val responeFraService = autovedtakSmåbarnstilleggService.kjørBehandling(
            aktør
        )
        secureLogger.info("Håndterte vedtak om overgangsstønad for person $personIdent:\n$responeFraService")
    }

    companion object {

        const val TASK_STEP_TYPE = "vedtakOmOvergangsstønadTask"
        private val logger = LoggerFactory.getLogger(VedtakOmOvergangsstønadTask::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

        fun opprettTask(personIdent: String): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = personIdent,
                properties = Properties().apply {
                    this["personIdent"] = personIdent
                }
            )
        }
    }
}
