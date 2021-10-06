package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.task.OpprettTaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class InfotrygdFeedService(val opprettTaskService: OpprettTaskService) {

    @Transactional
    fun sendTilInfotrygdFeed(barnsIdenter: List<String>) {
        logger.info("Send ${barnsIdenter.size} av fødselsmeldinger til Infotrygd.")
        opprettTaskService.opprettSendFeedTilInfotrygdTask(barnsIdenter)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(InfotrygdFeedService::class.java)
    }
}
