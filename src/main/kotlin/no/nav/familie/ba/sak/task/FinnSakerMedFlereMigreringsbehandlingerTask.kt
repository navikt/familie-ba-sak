package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.internal.ForvalterService
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
    val forvalterService: ForvalterService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val fraOgMedÅrMåned = YearMonth.parse(task.payload)

        val sakerMedFlereMigreringsbehandlinger = forvalterService.finnÅpneFagsakerMedFlereMigreringsbehandlinger(fraOgMedÅrMåned)

        if (sakerMedFlereMigreringsbehandlinger.isNotEmpty()) {
            error("$FEILMELDING fraOgMedÅrMåned=$fraOgMedÅrMåned \n${sakerMedFlereMigreringsbehandlinger.joinToString("\n")}")
        }
    }

    companion object {
        private const val FEILMELDING = "Det er nye fagsaker som har har flere enn 1 migrering fra Infotrygd. Send liste til fag og avvikshåndter tasken."

        const val TASK_STEP_TYPE = "finnSakerMedFlereMigreringsbehandlinger"
    }
}
