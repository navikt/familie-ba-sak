package no.nav.familie.ba.sak.integrasjoner.skyggesak

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.leader.LeaderClient
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Component
class SkyggesakScheduler(
    val skyggesakRepository: SkyggesakRepository,
    val integrasjonClient: IntegrasjonClient
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
                MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
                integrasjonClient.opprettSkyggesak(skyggesak.fagsak.aktør, skyggesak.fagsak.id)
                skyggesakRepository.save(skyggesak.copy(sendtTidspunkt = LocalDateTime.now()))
            } catch (e: Exception) {
                logger.warn("Kunne ikke opprette skyggesak for fagsak ${skyggesak.fagsak.id}")
                secureLogger.warn("Kunne ikke opprette skyggesak for fagsak ${skyggesak.fagsak.id}", e)
            } finally {
                MDC.clear()
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SkyggesakScheduler::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
