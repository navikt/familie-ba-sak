package no.nav.familie.ba.sak.kjerne.autobrev

import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(taskStepType = FinnAlleBarn6og18ÅrTask.TASK_STEP_TYPE,
                     beskrivelse = "Send autobrev for barn som fyller 6 og 18 år til Dokdist",
                     maxAntallFeil = 3,
                     triggerTidVedFeilISekunder = 60 * 60 * 24)
class FinnAlleBarn6og18ÅrTask(
        private val fagsakRepository: FagsakRepository,
        private val opprettTaskService: OpprettTaskService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        listOf<Long>(6, 18).forEach { alder ->
            val berørteFagsaker = finnAlleBarnMedFødselsdagInneværendeMåned(alder)
            logger.info("Oppretter tasker for ${berørteFagsaker.size} fagsaker med barn som fyller $alder år inneværende måned.")
            berørteFagsaker.forEach { fagsak ->
                opprettTaskService.opprettAutovedtakFor6Og18ÅrBarn(
                        fagsakId = fagsak.id,
                        alder = alder.toInt()
                )
            }
        }
    }

    private fun finnAlleBarnMedFødselsdagInneværendeMåned(alder: Long): Set<Fagsak> =
            LocalDate.now().minusYears(alder).let {
                fagsakRepository.finnLøpendeFagsakMedBarnMedFødselsdatoInnenfor(it.førsteDagIInneværendeMåned(), it.sisteDagIMåned())
            }

    companion object {

        const val TASK_STEP_TYPE = "FinnAlleBarn6og18ÅrTask"
        private val logger: Logger = LoggerFactory.getLogger(FinnAlleBarn6og18ÅrTask::class.java)
    }
}

