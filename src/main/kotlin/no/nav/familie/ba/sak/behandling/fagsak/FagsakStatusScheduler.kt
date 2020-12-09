package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.task.OppdaterLøpendeFlagg
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FagsakStatusScheduler(val taskRepository: TaskRepository) {

    /*
     * Siden barnetrygd er en månedsytelse vil en fagsak alltid løpe ut en måned
     * Det er derfor nok å finne alle fagsaker som ikke lenger har noen løpende utbetalinger den 1 hver måned.
     */

    @Scheduled(cron = "0 0 7 1 * *")
    fun oppdaterFagsakStatuser() {

        when (LeaderClient.isLeader()) {
            true -> {
                val oppdaterLøpendeFlaggTask = Task.nyTask(type = OppdaterLøpendeFlagg.TASK_STEP_TYPE, payload = "")
                taskRepository.save(oppdaterLøpendeFlaggTask)
                LOG.info("Opprettet oppdaterLøpendeFlaggTask")
            }
            false -> {
                LOG.info("Ikke opprettet oppdaterLøpendeFlaggTask på denne poden")
            }
        }
    }

    companion object {

        val LOG = LoggerFactory.getLogger(FagsakStatusScheduler::class.java)
    }
}