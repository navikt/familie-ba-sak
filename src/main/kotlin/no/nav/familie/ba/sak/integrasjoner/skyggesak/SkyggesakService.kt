package no.nav.familie.ba.sak.integrasjoner.skyggesak

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class SkyggesakService(
    private val skyggesakRepository: SkyggesakRepository,
    val fagsakRepository: FagsakRepository,
    val integrasjonKlient: IntegrasjonKlient,
) {
    @Transactional
    fun sendSkyggesaker() {
        val skyggesaker = skyggesakRepository.finnSkyggesakerKlareForSending(Pageable.ofSize(400))

        for (skyggesak in skyggesaker) {
            try {
                MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
                val fagsak = fagsakRepository.finnFagsak(skyggesak.fagsakId)!!
                logger.info("Oppretter skyggesak for fagsak ${fagsak.id}")
                integrasjonKlient.opprettSkyggesak(fagsak.aktør, fagsak.id)
                skyggesakRepository.save(skyggesak.copy(sendtTidspunkt = LocalDateTime.now()))
            } catch (e: Exception) {
                logger.warn("Kunne ikke opprette skyggesak for fagsak ${skyggesak.fagsakId}")
                secureLogger.warn("Kunne ikke opprette skyggesak for fagsak ${skyggesak.fagsakId}", e)
            } finally {
                MDC.clear()
            }
        }
    }

    @Transactional
    fun fjernGamleSkyggesakInnslag() {
        skyggesakRepository
            .finnSkyggesakerSomErSendt()
            .filter { it.sendtTidspunkt!!.isBefore(LocalDateTime.now().minusDays(SKYGGESAK_RETENTION_DAGER.toLong())) }
            .run {
                logger.info("Fjerner ${this.size} rader fra Skyggesak, sendt for mer enn $SKYGGESAK_RETENTION_DAGER dager siden")
                secureLogger.info("Fjerner følgende rader eldre enn $SKYGGESAK_RETENTION_DAGER fra Skyggesak:\n$this ")
                skyggesakRepository.deleteAll(this)
            }
    }

    fun opprettSkyggesak(fagsak: Fagsak) {
        skyggesakRepository.save(Skyggesak(fagsakId = fagsak.id))
    }

    companion object {
        private const val SKYGGESAK_RETENTION_DAGER = 14

        private val logger = LoggerFactory.getLogger(SkyggesakService::class.java)
    }
}
