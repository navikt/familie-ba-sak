package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnAllOpphørAvFullOvergangstonadTask.TASK_STEP_TYPE,
    beskrivelse = "Send autobrev for saker med opphør av full overgangsstonad",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = (60 * 60 * 24).toLong()
)
class FinnAllOpphørAvFullOvergangstonadTask(
    private val fagsakRepository: FagsakRepository,
    private val opprettTaskService: OpprettTaskService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val fagsakerMedOpphørAvOvergangsstonadInneværendeMåned: Set<Fagsak> =
            fagsakRepository.finnLøpendeFagsakerMedOpphørAvFullOvergangsstonadIInterval(
                LocalDate.now().førsteDagIInneværendeMåned(), LocalDate.now().sisteDagIMåned()
            )
        fagsakerMedOpphørAvOvergangsstonadInneværendeMåned.forEach {
            opprettTaskService.opprettAutovedtakForOvergangsstonadOpphørTask(it.id)
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "FinnAllOpphørAvFullOvergangstonadTask"
        private val logger: Logger = LoggerFactory.getLogger(FinnAllOpphørAvFullOvergangstonadTask::class.java)
    }
}
