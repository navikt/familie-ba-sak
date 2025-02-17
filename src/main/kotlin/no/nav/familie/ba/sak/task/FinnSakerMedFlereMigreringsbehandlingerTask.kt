package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnSakerMedFlereMigreringsbehandlingerTask.TASK_STEP_TYPE,
    beskrivelse = "Finn fagsaker med flere migreringersbehandlinger",
    maxAntallFeil = 1,
)
class FinnSakerMedFlereMigreringsbehandlingerTask(
    val fagsakRepository: FagsakRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val fraOgMedÅrMåned = YearMonth.parse(task.payload)

        val sakerMedFlereMigreringsbehandlinger =
            fagsakRepository
                .finnFagsakerMedFlereMigreringsbehandlinger(
                    fraOgMedÅrMåned.førsteDagIInneværendeMåned().atStartOfDay(),
                ).map { Pair(it.fagsakId, it.fødselsnummer) }

        if (sakerMedFlereMigreringsbehandlinger.isNotEmpty()) {
            error("$FEILMELDING fraOgMedÅrMåned=$fraOgMedÅrMåned \n${sakerMedFlereMigreringsbehandlinger.joinToString("\n")}")
        }
    }

    companion object {
        private const val FEILMELDING = "Det er nye fagsaker som har har flere enn 1 migrering fra Infotrygd. Send liste til fag og avvikshåndter tasken."

        const val TASK_STEP_TYPE = "finnSakerMedFlereMigreringsbehandlinger"
    }
}
