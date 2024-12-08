package no.nav.familie.ba.sak.kjerne.autovedtak.nyutvidetklassekode

import no.nav.familie.ba.sak.kjerne.autovedtak.nyutvidetklassekode.domene.NyUtvidetKlassekodeKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.nyutvidetklassekode.domene.NyUtvidetKlassekodeKjøringRepository
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
    taskStepType = PopulerNyUtvidetKlassekodeKjøringTask.TASK_STEP_TYPE,
    beskrivelse = "Opprett oppgaver for sending av autobrev",
    maxAntallFeil = 1,
)
class PopulerNyUtvidetKlassekodeKjøringTask(
    private val fagsakRepository: FagsakRepository,
    private val nyUtvidetKlassekodeKjøringRepository: NyUtvidetKlassekodeKjøringRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val fagsakerSomErLagret = nyUtvidetKlassekodeKjøringRepository.findAll().map { it.fagsakId }.toSet()

        logger.info("Fant ${fagsakerSomErLagret.size} fagsaker som allerede er lagret")

        val (fagsakerSomSkalLagres, time) =
            measureTimedValue {
                fagsakRepository
                    .finnFagsakerMedLøpendeUtvidetBarnetrygdSomBrukerGammelKlassekode()
                    .minus(fagsakerSomErLagret)
                    .map { NyUtvidetKlassekodeKjøring(fagsakId = it) }
            }

        logger.info("Fant ${fagsakerSomSkalLagres.size} fagsaker som skal lagres på ${time.inWholeSeconds} sekunder")

        nyUtvidetKlassekodeKjøringRepository.saveAll(fagsakerSomSkalLagres)
    }

    companion object {
        const val TASK_STEP_TYPE = "PopulerNyUtvidetKlassekodeKjøringTask"
        private val logger: Logger = LoggerFactory.getLogger(PopulerNyUtvidetKlassekodeKjøringTask::class.java)

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
