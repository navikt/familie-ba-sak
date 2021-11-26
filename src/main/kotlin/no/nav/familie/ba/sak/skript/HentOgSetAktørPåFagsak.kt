package no.nav.familie.ba.sak.skript

import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class HentOgSetAktørPåFagsak(val fagsakRepository: FagsakRepository, val personidentService: PersonidentService) {

    @Scheduled(initialDelay = 1000 * 30, fixedDelay = Long.MAX_VALUE)
    fun hentOgSetAktørPåFagsak() {
        val fagsaker = fagsakRepository.finnAlleFagsakerUtenAktør()

        logger.info("Sett aktørid på fagsak ${fagsaker.size}")
        // Prod inneholder 33 fagsaker uten aktør id, mens preprod inneholder 240 
        if (fagsaker.size > 250) throw error("Skal maks sette aktør id på 250 fagsaker fant ${fagsaker.size}.")

        var teller = 0
        fagsaker.forEach { teller += hentOgSetAktørPåFagsak(it) }
        logger.info("Ferdig satt aktørid på fagsak: $teller")
    }

    @Transactional
    fun hentOgSetAktørPåFagsak(fagsak: Fagsak): Int {

        try {
            logger.info("Sett aktørid på fagsak ${fagsak.id}")
            val aktør = personidentService.hentOgLagreAktørId(fagsak.hentAktivIdent().ident)
            fagsak.aktør = aktør
            fagsakRepository.save(fagsak)
            return 1
        } catch (e: Exception) {
            logger.warn("Feilet å sette aktør på fagsak: ${fagsak.aktør}")
            return 0
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(HentOgSetAktørPåFagsak::class.java)
    }
}
