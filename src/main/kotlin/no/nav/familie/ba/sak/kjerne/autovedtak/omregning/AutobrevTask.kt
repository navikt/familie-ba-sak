package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
    taskStepType = AutobrevTask.TASK_STEP_TYPE,
    beskrivelse = "Opprett oppgaver for sending av autobrev",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = (60 * 60 * 24).toLong()
)
class AutobrevTask(
    private val fagsakRepository: FagsakRepository,
    private val opprettTaskService: OpprettTaskService,
    private val featureToggleService: FeatureToggleService,
    private val behandlingService: BehandlingService
) : AsyncTaskStep {

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
        if (featureToggleService.isEnabled(FeatureToggleConfig.AUTOBREV_OPPHØR_SMÅBARNSTILLEGG)) {
            logger.info("Sending av autobrev ved opphør av småbarnstillegg er enabled.")
            behandlingService.hentAlleBehandlingsIderMedOpphørSmåbarnstilleggIMåned(YearMonth.now().minusMonths(1))
                .forEach { behandlingId: Long ->
                    opprettTaskService.opprettAutovedtakForOpphørSmåbarnstilleggTask(
                        behandlingId = behandlingId
                    )
                }
        }
    }

    private fun finnAlleBarnMedFødselsdagInneværendeMåned(alder: Long): Set<Fagsak> =
        LocalDate.now().minusYears(alder).let {
            fagsakRepository.finnLøpendeFagsakMedBarnMedFødselsdatoInnenfor(
                it.førsteDagIInneværendeMåned(),
                it.sisteDagIMåned()
            )
        }

    companion object {

        const val TASK_STEP_TYPE = "FinnAlleBarn6og18ÅrTask"
        private val logger: Logger = LoggerFactory.getLogger(AutobrevTask::class.java)
    }
}
