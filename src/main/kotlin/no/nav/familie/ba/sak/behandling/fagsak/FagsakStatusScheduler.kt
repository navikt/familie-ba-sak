package no.nav.familie.ba.sak.behandling.fagsak

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FagsakStatusScheduler(val fagsakRepository: FagsakRepository) {

    @Scheduled(cron = "0 0 17 * * *") // TODO: Passende tidspunkt?
    fun oppdaterFagsakStatuser() {
        val antallOppdaterte = fagsakRepository.oppdaterLøpendeStatusPåFagsaker()
        LOG.info("Oppdatert status på $antallOppdaterte fagsaker til ${FagsakStatus.AVSLUTTET.name}")
    }

    companion object {
        val LOG = LoggerFactory.getLogger(FagsakStatusScheduler::class.java)
    }
}