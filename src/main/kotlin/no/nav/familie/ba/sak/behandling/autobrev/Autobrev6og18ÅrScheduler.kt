package no.nav.familie.ba.sak.behandling.autobrev

import no.nav.familie.ba.sak.task.SendAutobrev6og18ÅrTask
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Autobrev6og18ÅrScheduler(val taskRepository: TaskRepository) {

    /*
     * Den 1. i hver måned skal løpende behandlinger med barn som fyller 6- eller 18 år i løpet av denne måneden slås opp
     * og tasker for å sjekke om autobrev skal sendes ut opprettes for disse.
     */

    @Scheduled(cron = "0 0 7 1 * *")
    fun oppdaterFagsakStatuser() {

        when (LeaderClient.isLeader()) {
            true -> {
                val sendAutobrevTask = Task.nyTask(type = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                                                   payload = objectMapper.writeValueAsString(
                                                           Autobrev6og18ÅrDTO(
                                                                   behandlingsId = 1L,
                                                                   personIdent = "")
                                                   ))
                taskRepository.save(sendAutobrevTask)
                LOG.info("Opprettet oppdaterLøpendeFlaggTask")
            }
            false -> {
                LOG.info("Ikke opprettet sendAutobrevTask på denne poden")
            }
        }
    }

    companion object {

        val LOG = LoggerFactory.getLogger(Autobrev6og18ÅrScheduler::class.java)
    }
}