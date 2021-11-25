package no.nav.familie.ba.sak.skript

import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class HentOgSetAktørPåFagsak(val fagsakRepository: FagsakRepository, val personidentService: PersonidentService) {

    @Transactional
    @Scheduled(initialDelay = 1000 * 30, fixedDelay = Long.MAX_VALUE)
    fun hentOgSetAktørPåFagsak() {
        val fagsaker = fagsakRepository.finnAlleFagsakerUtenAktør()

        if (LeaderClient.isLeader() == true) {
            logger.info("Sett aktørid på fagsak ${fagsaker.size}")
            // Prod inneholder 33 fagsaker uten aktør id, mens preprod inneholder 240 
            if (fagsaker.size > 250) throw error("Skal maks sette aktør id på 250 fagsaker fant ${fagsaker.size}.")

            fagsaker.forEach {
                logger.info("Sett aktørid på fagsak ${it.id}")
                val aktør = personidentService.hentOgLagreAktørId(it.hentAktivIdent().ident)
                it.aktør = aktør
                fagsakRepository.save(it)
            }
            logger.info("Ferdig satt aktørid på fagsak")
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(HentOgSetAktørPåFagsak::class.java)
    }
}
