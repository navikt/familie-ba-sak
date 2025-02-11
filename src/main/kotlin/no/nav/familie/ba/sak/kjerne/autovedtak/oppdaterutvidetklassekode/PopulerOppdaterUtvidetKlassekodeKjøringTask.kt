package no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode

import no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.domene.OppdaterUtvidetKlassekodeKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.domene.OppdaterUtvidetKlassekodeKjøringRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import kotlin.time.measureTimedValue

@Service
@TaskStepBeskrivelse(
    taskStepType = PopulerOppdaterUtvidetKlassekodeKjøringTask.TASK_STEP_TYPE,
    beskrivelse = "Populerer tabellen NyUtvidetKlassekodeKjøring med fagsaker som bruker gammel klassekode",
    maxAntallFeil = 1,
)
class PopulerOppdaterUtvidetKlassekodeKjøringTask(
    private val fagsakRepository: FagsakRepository,
    private val oppdaterUtvidetKlassekodeKjøringRepository: OppdaterUtvidetKlassekodeKjøringRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val fagsakerSomErLagret = oppdaterUtvidetKlassekodeKjøringRepository.findAll().map { it.fagsakId }.toSet()

        logger.info("Fant ${fagsakerSomErLagret.size} fagsaker som allerede er lagret")

        val (fagsakerSomSkalLagres, time) =
            measureTimedValue {
                fagsakRepository
                    .finnFagsakerMedIverksattRevurderingEtterOppdaterUtvidetKlassekodeBehandling()
                    .minus(fagsakerSomErLagret)
                    .map { OppdaterUtvidetKlassekodeKjøring(fagsakId = it) }
            }

        logger.info("Fant ${fagsakerSomSkalLagres.size} fagsaker som skal lagres på ${time.inWholeSeconds} sekunder")

        oppdaterUtvidetKlassekodeKjøringRepository.saveAll(fagsakerSomSkalLagres)
    }

    companion object {
        const val TASK_STEP_TYPE = "populerOppdaterUtvidetKlassekodeKjøringTask"
        private val logger: Logger = LoggerFactory.getLogger(PopulerOppdaterUtvidetKlassekodeKjøringTask::class.java)

        fun lagTask(): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = "",
                properties =
                    mapOf(
                        "callId" to (MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()),
                    ).toProperties(),
            )
    }
}
