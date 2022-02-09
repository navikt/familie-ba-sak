package no.nav.familie.ba.sak.integrasjoner.skyggesak

import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class SkyggesakScheduler(
    val skyggesakRepository: SkyggesakRepository,
    val skyggesakService: SkyggesakService
) {

    @Scheduled(fixedDelay = 60000)
    fun opprettSkyggesaker() {
        if (LeaderClient.isLeader() == true) {
            sendSkyggesaker()
        }
    }

    @Transactional
    fun sendSkyggesaker() {
        val skyggesaker = skyggesakRepository.finnSkyggesakerKlareForSending(Pageable.ofSize(400))

        for (skyggesak in skyggesaker) {
            try {
                skyggesakService.opprettSkyggesak(skyggesak.fagsak.aktør, skyggesak.fagsak.id)
                skyggesakRepository.save(skyggesak.copy(sendtTidspunkt = LocalDateTime.now()))
            } catch (e: Exception) {
                logger.warn("Kunne ikke opprette skyggesak for fagsak ${skyggesak.fagsak.id}")
                secureLogger.warn("Kunne ikke opprette skyggesak for fagsak ${skyggesak.fagsak.id}", e)
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SkyggesakScheduler::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
