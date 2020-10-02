package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.task.SendFeedTilInfotrygdTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class InfotrygdFeedService(val taskRepository: TaskRepository) {

    @Transactional
    fun sendTilInfotrygdFeed(barnsIdenter: List<String>) {
        LOG.info("Send ${barnsIdenter.size} av fødselsmeldinger til Infotrygd.")
        taskRepository.save(SendFeedTilInfotrygdTask.opprettTask(barnsIdenter))
    }



    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}