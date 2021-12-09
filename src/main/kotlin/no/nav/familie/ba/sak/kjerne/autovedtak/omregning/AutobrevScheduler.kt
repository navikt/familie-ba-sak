package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.task.FinnAllOpphørAvFullOvergangstonadTask
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.util.VirkedagerProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class AutobrevScheduler(val taskRepository: TaskRepositoryWrapper) {

    /*
     * Hver måned skal løpende fagsaker hvor
     *  - barn fyller 6- eller 18 år i løpet av måneden
     *  - barn fylte 3 år forrige måned og småbarnstillegg skal fjernes
     *  - overgangsstønader for løpende fagsaker med småbarnstillegg utløper
     * slås opp og tasker opprettes for å sjekke om automatisk revurdering skal utføres og autobrev skal sendes ut.
     * Dette skal da gjøres første virkedag i hver måned. Denne klassen kjøres skedulert kl.7 den første dagen i måneden
     * og setter da triggertid på tasken til kl.8 den første virkedagen i måneden. For testformål kan funksjonen
     * opprettTask også kalles direkte via et restendepunkt, og da settes triggertiden 30 sek frem i tid.
     */
    @Transactional
    @Scheduled(cron = "0 0 $KLOKKETIME_SCHEDULER_TRIGGES 1 * *")
    fun opprettAutoBrevTask() {
        when (LeaderClient.isLeader()) {
            true -> {
                // Timen for triggertid økes med en. Det er nødvendig å sette klokkeslettet litt frem dersom den 1. i
                // måneden også er en virkedag (slik at både denne skeduleren og tasken som opprettes vil kjøre på samme dato).
                opprettFinnBarnTask(
                    triggerTid = VirkedagerProvider.nesteVirkedag(
                        LocalDate.now().minusDays(1)
                    ).atTime(KLOKKETIME_SCHEDULER_TRIGGES.inc(), 0)
                )
                opprettEvaluerOvergangsstonadTask(
                    triggerTid = VirkedagerProvider.nesteVirkedag(
                        LocalDate.now().minusDays(1)
                    ).atTime(KLOKKETIME_SCHEDULER_TRIGGES.inc(), 0)
                )
            }
            false -> logger.info("Poden er ikke satt opp som leader - oppretter ikke FinnAlleBarn3og6og18ÅrTask")
            null -> logger.info("Poden svarer ikke om den er leader eller ikke - oppretter ikke FinnAlleBarn3og6og18ÅrTask")
        }
    }

    fun opprettFinnBarnTask(triggerTid: LocalDateTime = LocalDateTime.now().plusSeconds(30)) {
        logger.info("Opprett task som skal finne alle barn 3 og 6 og 18 år")
        taskRepository.save(
            Task(
                type = FinnAlleBarnTask.TASK_STEP_TYPE,
                payload = ""
            ).medTriggerTid(
                triggerTid = triggerTid
            )
        )
    }

    fun opprettEvaluerOvergangsstonadTask(triggerTid: LocalDateTime = LocalDateTime.now().plusSeconds(30)) {
        logger.info("Opprett task som skal håndtere alle løpende behandlinger som har opphør av overgangsstønad inneværende måned.")
        taskRepository.save(
            Task(
                type = FinnAllOpphørAvFullOvergangstonadTask.TASK_STEP_TYPE,
                payload = ""
            ).medTriggerTid(
                triggerTid = triggerTid
            )
        )
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AutobrevScheduler::class.java)
        const val KLOKKETIME_SCHEDULER_TRIGGES = 7
    }
}
