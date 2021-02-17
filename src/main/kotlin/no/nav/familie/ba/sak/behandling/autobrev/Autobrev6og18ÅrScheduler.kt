package no.nav.familie.ba.sak.behandling.autobrev

import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.util.VirkedagerProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class Autobrev6og18ÅrScheduler(val taskRepository: TaskRepository) {

    /*
     * Hver måned skal løpende fagsaker med barn som fyller 6- eller 18 år i løpet av måneden slås opp og tasker for å sjekke om
     * autobrev skal sendes ut opprettes for disse. Dette skal da gjøres første virkedag i hver måned. Denne klassen kjøres
     * skedulert kl.7 den første dagen i måneden og setter da triggertid på tasken til kl.8 den første virkedagen i måneden.
     * For testformål kan funksjonen opprettTask også kalles direkte via et restendepunkt, og da settes triggertiden 30 sek
     * frem i tid.
     */
    @Transactional
    @Scheduled(cron = "0 0 $klokketimeSchedulerTrigges 1 * *")
    fun opprettTaskAutoBrev6og18år() {
        when (LeaderClient.isLeader()) {
            true -> {
                // Timen for triggertid økes med en. Det er nødvendig å sette klokkeslettet litt frem dersom den 1. i
                // måneden også er en virkedag (slik at både denne skeduleren og tasken som opprettes vil kjøre på samme dato).
                opprettTask(triggerTid = VirkedagerProvider.nesteVirkedag(
                        LocalDate.now().minusDays(1)).atTime(klokketimeSchedulerTrigges.inc(), 0))
            }
            false -> LOG.info("Poden er ikke satt opp som leader - oppretter ikke FinnAlleBarn6og18ÅrTask")
            null -> LOG.info("Poden svarer ikke om den er leader eller ikke - oppretter ikke FinnAlleBarn6og18ÅrTask")
        }
    }

    fun opprettTask(triggerTid: LocalDateTime = LocalDateTime.now().plusSeconds(30)) {
        LOG.info("Opprett task som skal finne alle barn 6 og 18 år")
        taskRepository.save(Task.nyTaskMedTriggerTid(
                type = FinnAlleBarn6og18ÅrTask.TASK_STEP_TYPE,
                payload = "",
                triggerTid = triggerTid))
    }

    companion object {

        val LOG = LoggerFactory.getLogger(Autobrev6og18ÅrScheduler::class.java)
        const val klokketimeSchedulerTrigges = 7
    }
}