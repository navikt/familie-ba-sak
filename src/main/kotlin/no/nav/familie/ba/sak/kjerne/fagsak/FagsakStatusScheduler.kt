package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.task.FinnFagsakerSomSkalLåsesTask
import no.nav.familie.ba.sak.task.OppdaterLøpendeFlagg
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FagsakStatusScheduler(
    private val taskRepository: TaskRepositoryWrapper,
    private val envService: EnvService,
    private val leaderClientService: LeaderClientService,
    private val featureToggleService: FeatureToggleService,
) {
    /*
     * Siden barnetrygd er en månedsytelse vil en fagsak alltid løpe ut en måned
     * Det er derfor nok å finne alle fagsaker som ikke lenger har noen løpende utbetalinger den 1 hver måned.
     */

    @Scheduled(cron = "\${CRON_FAGSAKSTATUS_SCHEDULER}")
    fun oppdaterFagsakStatuser() {
        when (leaderClientService.isLeader() || envService.erDev()) {
            true -> {
                val oppdaterLøpendeFlaggTask = Task(type = OppdaterLøpendeFlagg.TASK_STEP_TYPE, payload = "")
                taskRepository.save(oppdaterLøpendeFlaggTask)
                logger.info("Opprettet oppdaterLøpendeFlaggTask")
            }

            false -> {
                logger.info("Ikke opprettet oppdaterLøpendeFlaggTask på denne poden")
            }
        }
    }

    @Scheduled(cron = "\${CRON_LÅS_FAGSAK_SCHEDULER}")
    fun startFagsakLåsingScheduled() {
        if (leaderClientService.isLeader() || envService.erDev()) {
            startFagsakLåsing(maksAntall = STANDARD_MAKS_ANTALL_FAGSAKER_PER_KJØRING)
        }
    }

    fun startFagsakLåsing(maksAntall: Int): Boolean {
        if (!featureToggleService.isEnabled(FeatureToggle.FAGSAKLÅSING_SCHEDULER)) {
            logger.info("Fagsaklåsing-scheduler-toggle er av, hopper over batch")
            return false
        }

        taskRepository.save(FinnFagsakerSomSkalLåsesTask.opprettTask(maksAntall = maksAntall))
        logger.info("Opprettet FinnFagsakerSomSkalLåsesTask med maks $maksAntall fagsaker")
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FagsakStatusScheduler::class.java)

        // Maks antall fagsaker som låses per automatiske kjøring. Holdes lavt i oppstarten og økes etter hvert.
        const val STANDARD_MAKS_ANTALL_FAGSAKER_PER_KJØRING = 100
    }
}
