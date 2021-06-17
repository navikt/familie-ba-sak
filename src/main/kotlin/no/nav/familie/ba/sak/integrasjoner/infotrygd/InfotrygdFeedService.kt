package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.task.SendFeedTilInfotrygdTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class InfotrygdFeedService(val taskRepository: TaskRepository) {

    @Transactional
    fun sendTilInfotrygdFeed(barnsIdenter: List<String>) {
        logger.info("Send ${barnsIdenter.size} av fødselsmeldinger til Infotrygd.")
        taskRepository.save(SendFeedTilInfotrygdTask.opprettTask(barnsIdenter))
    }



    companion object {
        private val logger = LoggerFactory.getLogger(InfotrygdFeedService::class.java)
    }
}