package no.nav.familie.ba.sak.behandling.autobrev

import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class Autobrev6og18ÅrScheduler(val taskRepository: TaskRepository) {

    /*
     * Den 1. i hver måned skal løpende behandlinger med barn som fyller 6- eller 18 år i løpet av denne måneden slås opp
     * og tasker for å sjekke om autobrev skal sendes ut opprettes for disse.
     */

    // TODO: Diskuter med Henning, hvordan skal dette trigges sånn at det er robust (prosesseringen gjøres idempotent så samme fagsak skal kunne kjøres flere ganger)
    // 1. Implementere lignende KonsistensavstemmingScheduler med batch-tabell, kjører processeringen ikke en dag kan man manuelt sette inn kjøringen en senere dag.
    // 2. Rest-api for å manuelt trigge rekjøring.
    @Transactional
    @Scheduled(cron = "0 0 7 1 * *")
    //@Scheduled(cron = "0 38 * * * *")
    fun opprettTaskAutoBrev6og18år() {
        if (LeaderClient.isLeader() != null) {
            return
        }

        LOG.info("Opprett task som skal finne alle barn 6 og 18 år")
        taskRepository.save(Task.nyTask(type = FinnAlleBarn6og18ÅrTask.TASK_STEP_TYPE, payload = ""))
    }

    companion object {

        val LOG = LoggerFactory.getLogger(Autobrev6og18ÅrScheduler::class.java)
    }
}