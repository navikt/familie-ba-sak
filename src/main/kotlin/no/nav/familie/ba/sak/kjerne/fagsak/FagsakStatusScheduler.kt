package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.task.OppdaterLøpendeFlagg
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FagsakStatusScheduler(val taskRepository: TaskRepositoryWrapper) {

    /*
     * Siden barnetrygd er en månedsytelse vil en fagsak alltid løpe ut en måned
     * Det er derfor nok å finne alle fagsaker som ikke lenger har noen løpende utbetalinger den 1 hver måned.
     */

    @Scheduled(cron = "0 0 7 1 * *")
    fun oppdaterFagsakStatuser() {

        when (LeaderClient.isLeader()) {
            true -> {
                val oppdaterLøpendeFlaggTask = Task(type = OppdaterLøpendeFlagg.TASK_STEP_TYPE, payload = "")
                taskRepository.save(oppdaterLøpendeFlaggTask)
                logger.info("Opprettet oppdaterLøpendeFlaggTask")
            }
            false, null -> {
                logger.info("Ikke opprettet oppdaterLøpendeFlaggTask på denne poden")
            }
        }
    }

    // Kan fjernes etter 08.04.22 klokka 14.30.
    @Scheduled(cron = "0 30 14 8 4 *")
    fun oppdaterFagsakStatuser18April() {
        oppdaterFagsakStatuser()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FagsakStatusScheduler::class.java)
    }
}
