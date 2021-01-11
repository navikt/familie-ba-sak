package no.nav.familie.ba.sak.behandling.autobrev

import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class Autobrev6og18ÅrScheduler(val taskRepository: TaskRepository,
                               val featureToggleService: FeatureToggleService) {

    /*
     * Den 1. i hver måned skal løpende behandlinger med barn som fyller 6- eller 18 år i løpet av denne måneden slås opp
     * og tasker for å sjekke om autobrev skal sendes ut opprettes for disse.
     */

    @Transactional
    @Scheduled(cron = "0 0 7 1 * *")
    //@Scheduled(cron = "0 38 * * * *")
    fun opprettTaskAutoBrev6og18år() {
        if (LeaderClient.isLeader() != null) {
            return
        }

        opprettTask()
    }

    fun opprettTask() {
        if (featureToggleService.isEnabled("familie-ba-sak.omregning_6_og_18_år", false)) {
            LOG.info("Omregning 6 og 18 år, feature er skrudd på i Unleash")

            LOG.info("Opprett task som skal finne alle barn 6 og 18 år")
            taskRepository.save(Task.nyTask(type = FinnAlleBarn6og18ÅrTask.TASK_STEP_TYPE, payload = ""))
        } else {
            LOG.info("Omregning 6 og 18 år, feature er skrudd av i Unleash")
        }
    }

    companion object {

        val LOG = LoggerFactory.getLogger(Autobrev6og18ÅrScheduler::class.java)
    }
}