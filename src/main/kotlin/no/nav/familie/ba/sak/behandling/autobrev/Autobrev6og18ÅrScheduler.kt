package no.nav.familie.ba.sak.behandling.autobrev

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.task.SendAutobrev6og18ÅrTask
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class Autobrev6og18ÅrScheduler(
        val taskRepository: TaskRepository,
        val behandlingRepository: BehandlingRepository) {

    /*
     * Den 1. i hver måned skal løpende behandlinger med barn som fyller 6- eller 18 år i løpet av denne måneden slås opp
     * og tasker for å sjekke om autobrev skal sendes ut opprettes for disse.
     */

    // TODO: Hvordan bør feilhåndtering gjøres? Kjøre i en transaksjon? Hvordan rekjøres uten at nye tasker opprettes for de samme sakene på nytt etc?
    //@Scheduled(cron = "0 0 7 1 * *")
    //@Scheduled(cron = "*/30 * * * * *")
    @Scheduled(cron = "0 */5 * * * *")
    fun oppdaterFagsakStatuser() {

        when (LeaderClient.isLeader() == null) {
            true -> {
                LOG.info("*** Kjører Autobrev6og18ÅrScheduler!!!")
                listOf<Long>(6, 4, 18).forEach { alder ->
                    val behandlingerMedBursdagsbarnInneværendeMåned =
                            behandlingRepository.finnBehandlingerMedPersonerMedFødselsdatoInnenfor(
                                    LocalDate.now().førsteDagIInneværendeMåned().minusYears(alder),
                                    LocalDate.now().sisteDagIMåned().minusYears(alder))

                    LOG.info("Funnet ${behandlingerMedBursdagsbarnInneværendeMåned.size} behandlinger med barn som fyller $alder år.")

                    behandlingerMedBursdagsbarnInneværendeMåned.forEach {
                        val sendAutobrevTask = Task.nyTask(type = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                                                           payload = objectMapper.writeValueAsString(
                                                                   Autobrev6og18ÅrDTO(
                                                                           behandlingsId = it.id,
                                                                           alder = alder.toInt())
                                                           ))
                        taskRepository.save(sendAutobrevTask)
                        LOG.info("Opprettet SendAutobrev6og18ÅrTask")
                    }
                }
            }
            false -> {
                LOG.info("Ikke opprettet SendAutobrev6og18ÅrTask på denne poden")
            }
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(Autobrev6og18ÅrScheduler::class.java)
    }
}