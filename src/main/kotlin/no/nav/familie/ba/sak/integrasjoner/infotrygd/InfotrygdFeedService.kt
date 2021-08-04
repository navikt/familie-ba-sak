package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.task.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class InfotrygdFeedService(val taskService: TaskService) {

    @Transactional
    fun sendTilInfotrygdFeed(barnsIdenter: List<String>) {
        logger.info("Send ${barnsIdenter.size} av fødselsmeldinger til Infotrygd.")
        taskService.opprettSendFeedTilInfotrygdTask(barnsIdenter)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(InfotrygdFeedService::class.java)
    }
}