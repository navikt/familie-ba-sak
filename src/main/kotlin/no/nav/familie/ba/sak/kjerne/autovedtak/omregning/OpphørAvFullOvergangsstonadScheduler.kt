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
class OpphørAvFullOvergangsstonadScheduler(val taskRepository: TaskRepositoryWrapper) {

    /*
     * Hver måned skal løpende fagsaker med småbarnstillegg sjekkes for om tom dato for full
     * overgangsstonad er inneværende måned. Hvis de er det skal det kjøres en automatisk
     * behandling for trekke ifra småbarnstillegg (med mindre yngste barnet fylte 3 år
     * forrige måned, da håndteres det av opprettTaskAutoBrev3og6og18år).
     */
    @Transactional
    @Scheduled(cron = "0 0 $KLOKKETIME_SCHEDULER_TRIGGES 1 * *")
    fun opprettTaskAutoBrevOpphørAvFullOvergangsstonad() {
        when (LeaderClient.isLeader()) {
            true -> {
                // Timen for triggertid økes med en. Det er nødvendig å sette klokkeslettet litt frem dersom den 1. i
                // måneden også er en virkedag (slik at både denne skeduleren og tasken som opprettes vil kjøre på samme dato).
                opprettTask(
                    triggerTid = VirkedagerProvider.nesteVirkedag(
                        LocalDate.now().minusDays(1)
                    ).atTime(KLOKKETIME_SCHEDULER_TRIGGES.inc(), 0)
                )
            }
            false -> logger.info("Poden er ikke satt opp som leader - oppretter ikke FinnAlleBarn3og6og18ÅrTask")
            null -> logger.info("Poden svarer ikke om den er leader eller ikke - oppretter ikke FinnAlleBarn3og6og18ÅrTask")
        }
    }

    fun opprettTask(triggerTid: LocalDateTime = LocalDateTime.now().plusSeconds(30)) {
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

        private val logger = LoggerFactory.getLogger(OpphørAvFullOvergangsstonadScheduler::class.java)
        const val KLOKKETIME_SCHEDULER_TRIGGES = 7
    }
}
